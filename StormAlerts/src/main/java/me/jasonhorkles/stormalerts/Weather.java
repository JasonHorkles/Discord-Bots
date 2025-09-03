package me.jasonhorkles.stormalerts;

import me.jasonhorkles.stormalerts.Utils.ChannelUtils;
import me.jasonhorkles.stormalerts.Utils.LogUtils;
import me.jasonhorkles.stormalerts.Utils.MessageUtils;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static me.jasonhorkles.stormalerts.Utils.ChannelUtils.*;

public class Weather {
    public static boolean rainDenied;
    public static WeatherType previousWeatherType;

    private static RainLevel previousRainLevel;
    private static Long allowedSnowTime;

    public void checkConditions(double currentRainRate, double temperature) throws IOException, ExecutionException, InterruptedException, TimeoutException {
        LogUtils logUtils = new LogUtils();
        System.out.println(logUtils.getTime(LogUtils.LogColor.YELLOW) + "Checking weather...");

        String rawWeatherType;
        if (StormAlerts.testing) rawWeatherType = Files.readString(Path.of("StormAlerts/Tests/weather.txt"));
        else {
            // Don't do anything with timeouts - we'll just try again next time
            Connection conn = Jsoup
                .connect("https://weather.com/weather/today/l/" + new Secrets().weatherCode()).timeout(30000)
                .cookieStore(null);
            try {
                Document doc = conn.get();
                //noinspection DataFlowIssue
                rawWeatherType = doc.select("[class*=\"CurrentConditions--phraseValue--\"]").first().text();
            } catch (SocketTimeoutException ignored) {
                System.out.println(logUtils.getTime(LogUtils.LogColor.RED) + "Weather request timed out.\n ");
                return;
            }
        }

        if (rawWeatherType.isBlank()) throw new IOException("Weather type is blank");

        // Won't be null here or later if the weather is something we care about
        WeatherType weatherType = null;
        if (rawWeatherType.toLowerCase().contains("hail") || rawWeatherType.toLowerCase().contains("sleet"))
            weatherType = WeatherType.HAIL;
        else if (rawWeatherType.toLowerCase().contains("snow")) weatherType = WeatherType.SNOW;

        // Check if the weather has changed since last time (excluding rain levels)
        // Previous type will be null if not exciting and will be set at the end of everything
        boolean weatherIsDifferent = previousWeatherType != weatherType;

        MessageUtils messageUtils = new MessageUtils();

        // If snow is queued but not yet sent, don't cancel the snow message later on
        //noinspection BooleanVariableAlwaysNegated
        boolean snowQueued = false;

        // If the weather is still exciting, we'll want to update at least the bot status
        // If the weather has also changed since then, we'll want to send a message too
        if (weatherType != null) switch (weatherType) {
            case HAIL -> {
                StormAlerts.jda.getPresence().setPresence(
                    OnlineStatus.ONLINE,
                    Activity.customStatus("It's " + getWeatherText(
                        WeatherType.HAIL,
                        true) + " (" + rawWeatherType + ")"));

                if (!weatherIsDifferent) break;

                String ping = messageUtils.shouldIPing(hailChannel) ? "<@&845055784156397608>\n" : "";

                // 🧊
                hailChannel.sendMessage(ping + "\uD83E\uDDCA It's " + getWeatherText(
                        WeatherType.HAIL,
                        false) + "! (" + rawWeatherType + ")")
                    .setSuppressedNotifications(messageUtils.shouldIBeSilent(hailChannel)).queue();
            }

            case SNOW -> {
                System.out.println(logUtils.getTime(LogUtils.LogColor.GREEN) + "Snow detected.");

                Message message = messageUtils.getMessages(snowChannel, 1).get(30, TimeUnit.SECONDS)
                    .getFirst();

                // If the bot had just restarted, send snow message instantly and silently
                // If the message was edited within the last 3 minutes and it contains the restart message
                boolean canScheduleMessage = true;
                if (message.isEdited()) //noinspection DataFlowIssue - We assume there are already messages in the channel
                    if (message.getTimeEdited().isAfter(OffsetDateTime.now().minusMinutes(3)) && message
                        .getContentRaw().contains("(Bot restarted at")) {
                        // Set the allowed time to now to prevent it from scheduling on the next check
                        allowedSnowTime = System.currentTimeMillis();

                        canScheduleMessage = false;
                        sendSnowMessage(snowChannel, rawWeatherType, weatherIsDifferent, messageUtils);
                    }

                // We'll want to send the snow message after 20 minutes IF it's still snowing by then
                // First check if we can even schedule the message
                if (canScheduleMessage)
                    // If the allowed snow time is null, set it to 20 minutes in advance
                    if (allowedSnowTime == null) { //noinspection NonThreadSafeLazyInitialization - There shouldn't be any case where the thread runs more than once per minute or so
                        allowedSnowTime = System.currentTimeMillis() + 1200000;
                        System.out.println(logUtils.getTime(LogUtils.LogColor.YELLOW) + "Allowing snow messages in 20 minutes.");

                        snowQueued = true;
                        weatherType = null;
                        weatherIsDifferent = previousWeatherType != null;
                    }

                    // Otherwise, if it has already passed, send the snow message
                    else if (System.currentTimeMillis() >= allowedSnowTime) sendSnowMessage(
                        snowChannel,
                        rawWeatherType,
                        weatherIsDifferent,
                        messageUtils);

                    else { // Finally, if the snow is still waiting, set the weatherType to null
                        System.out.println(logUtils.getTime(LogUtils.LogColor.YELLOW) + "We're still waiting, though.");
                        snowQueued = true;
                        weatherType = null;
                        weatherIsDifferent = previousWeatherType != null;
                    }
            }
        }

        // If it's currently snowing or hailing, don't start to check the rain
        if (weatherType == null) {
            RainLevel rainLevel = null;
            boolean isRaining = false;

            // If we know it's raining and it's not blocked
            // We need to get a level for the enum
            if (currentRainRate > 0 && !rainDenied && temperature >= 30) {
                weatherType = WeatherType.RAIN;
                rainLevel = getRainLevel(currentRainRate);
                isRaining = true;
            }

            if (isRaining) {
                // Set weatherIsDifferent again now that we know it's raining
                weatherIsDifferent = previousWeatherType != weatherType;
                rawWeatherType = "RAIN";

                // Send the rain message if further checks have passed and the rain level has changed
                processRain(rainLevel, previousRainLevel != rainLevel, currentRainRate, messageUtils);
                previousRainLevel = rainLevel;
            }
        }

        // Determine if the weather has changed since our last alert
        if (!weatherIsDifferent)
            System.out.println(logUtils.getTime(LogUtils.LogColor.YELLOW) + "The weather hasn't changed!");

        // If not snowing, reset the allowed snow time
        if (weatherType != WeatherType.SNOW && !snowQueued) if (allowedSnowTime != null) {
            allowedSnowTime = null;
            System.out.println(logUtils.getTime(LogUtils.LogColor.YELLOW) + "Snow stopped - resetting allowed time");
        }

        // If not raining, reset the last rain level
        if (weatherType != WeatherType.RAIN) if (previousRainLevel != null) previousRainLevel = null;

        // If the previous weather was something we cared about and it's not the same as the current type
        if (previousWeatherType != null && weatherIsDifferent) {
            // Update the previous weather's message
            Message message = messageUtils.getMessages(
                new ChannelUtils().getWeatherChannel(
                    previousWeatherType), 1).get(30, TimeUnit.SECONDS).getFirst();

            // Only if the message hasn't already been updated
            if (!message.getContentRaw().contains("Ended") && !message.getContentRaw().contains("restarted"))
                message.editMessage(message.getContentRaw()
                    .replace("!", "! (Ended at <t:" + System.currentTimeMillis() / 1000 + ":t>)")).queue();
        }

        if (weatherType == null) StormAlerts.jda.getPresence().setPresence(
            OnlineStatus.IDLE,
            Activity.watching("for gnarly weather"));

        previousWeatherType = weatherType;

        System.out.println(logUtils.getTime(LogUtils.LogColor.GREEN) + "Raw weather: " + rawWeatherType + "\n ");
    }

    private void sendSnowMessage(TextChannel snowChannel, String rawWeatherType, boolean weatherIsDifferent, MessageUtils messageUtils) {
        StormAlerts.jda.getPresence().setPresence(
            OnlineStatus.ONLINE,
            Activity.customStatus("It's " + getWeatherText(
                WeatherType.SNOW,
                true) + " (" + rawWeatherType + ")"));

        if (!weatherIsDifferent) return;

        String ping = messageUtils.shouldIPing(snowChannel) ? "<@&845055624165064734>\n" : "";

        // 🌨️
        snowChannel.sendMessage(ping + "\uD83C\uDF28️ It's " + getWeatherText(
            WeatherType.SNOW,
            false) + "! (" + rawWeatherType + ")").setSuppressedNotifications(messageUtils.shouldIBeSilent(
            snowChannel)).queue();
    }

    // This will never be called if the rainDenied boolean is true
    private void processRain(RainLevel rainLevel, boolean levelChanged, double currentRainRate, MessageUtils messageUtils) throws IOException, ExecutionException, InterruptedException, TimeoutException {
        // If it has NOT snowed in the last 5 days
        boolean hasNotSnowed = messageUtils.getMessages(snowChannel, 1).get(30, TimeUnit.SECONDS).getFirst()
            .getTimeCreated().isBefore(OffsetDateTime.now().minusDays(5));

        // If the rain has been accepted since the last snow or it hasn't snowed in the last 5 days
        if (rainAcceptedSinceSnow(snowChannel) || hasNotSnowed) {
            // Update the presence
            StormAlerts.jda.getPresence().setPresence(
                OnlineStatus.ONLINE,
                Activity.watching("the rain @ " + currentRainRate + " in/hr"));

            System.out.println(new LogUtils().getTime(LogUtils.LogColor.GREEN) + "Raining @ " + currentRainRate + " in/hr");

            // If the level hasn't actually changed then that's all we needed to do
            if (!levelChanged) return;

            String ping = messageUtils.shouldIPing(rainChannel) ? "<@&843956362059841596>\n" : "";

            // Calculate what the rain message should be
            String message = "";
            switch (rainLevel) {
                case L1 -> // 🟩▫️▫️▫️
                    message = ping + "☂️ It's " + getWeatherText(
                        RainLevel.L1,
                        false) + "!\n\uD83D\uDFE9▫️▫️▫️ (" + currentRainRate + " in/hr)";

                case L2 -> // 🟩🟨▫️▫️
                    message = ping + "☔ It's " + getWeatherText(
                        RainLevel.L2,
                        false) + "!\n\uD83D\uDFE9\uD83D\uDFE8▫️▫️ (" + currentRainRate + " in/hr)";

                case L3 -> // 🟩🟨🟧▫️
                    // 🌦️
                    message = ping + "\uD83C\uDF26️ It's " + getWeatherText(
                        RainLevel.L3,
                        false) + "!\n\uD83D\uDFE9\uD83D\uDFE8\uD83D\uDFE7▫️ (" + currentRainRate + " in/hr)";

                case L4 -> // 🟩🟨🟧🟥
                    // 🌧️
                    message = ping + "\uD83C\uDF27️ It's " + getWeatherText(
                        RainLevel.L4,
                        false) + "!\n\uD83D\uDFE9\uD83D\uDFE8\uD83D\uDFE7\uD83D\uDFE5 (" + currentRainRate + " in/hr) <a:weewoo:1083615022455992382>";
            }

            // Regular rain message
            rainChannel.sendMessage(message).setSuppressedNotifications(messageUtils.shouldIBeSilent(
                rainChannel)).queue();

            // Heavy rain message
            if (rainLevel == RainLevel.L4) {
                String heavyPing = messageUtils.shouldIPing(heavyRainChannel) ? "<@&843956325690900503>\n" : "";
                heavyRainChannel.sendMessage(heavyPing + "\uD83C\uDF27️ It's " + getWeatherText(
                    RainLevel.L4,
                    false) + "! (" + currentRainRate + " in/hr)").queue();
            }

        } else { // Has snowed and rain hasn't been accepted since latest snow
            sendConfirmationMessage();
            StormAlerts.jda.getPresence().setPresence(
                OnlineStatus.IDLE, Activity.customStatus("It's maybe " + getWeatherText(
                    rainLevel,
                    true) + " @ " + currentRainRate + " in/hr"));
        }
    }


    // Returns true if the latest snow message id = the id in the file
    // If it doesn't match, then it's assumed to have snowed since the last rain confirmation
    private boolean rainAcceptedSinceSnow(TextChannel snowChannel) throws ExecutionException, InterruptedException, TimeoutException, IOException {
        Long latestId = new MessageUtils().getMessages(snowChannel, 1).get(30, TimeUnit.SECONDS).getFirst()
            .getIdLong();
        Long savedId = Long.parseLong(Files.readString(Path.of("StormAlerts/accepted-snow.txt")));

        return latestId.equals(savedId);
    }

    private void sendConfirmationMessage() throws ExecutionException, InterruptedException, TimeoutException {
        // Delete the message if it's older than 5 hours
        List<Message> latestMessage = new MessageUtils().getMessages(rainConfirmationChannel, 1).get(
            30,
            TimeUnit.SECONDS);
        boolean shouldSendMessage = true;

        if (!latestMessage.isEmpty()) {
            Message message = latestMessage.getFirst();
            if (message.getTimeCreated().isBefore(OffsetDateTime.now().minusHours(5)))
                message.delete().queue();
                // Don't send the message if a valid one is already there
            else shouldSendMessage = false;
        }

        if (shouldSendMessage) rainConfirmationChannel.sendMessage(
            "<@277291758503723010>\n# PWS reports rain:").setActionRow(
            Button
                .success("acceptrain", "Accept since snow").withEmoji(Emoji.fromUnicode("✅")),
            Button.danger("denyrain", "Deny for 5 hours").withEmoji(Emoji.fromUnicode("✖️"))).queue(del -> del
            .delete().queueAfter(
                60,
                TimeUnit.MINUTES,
                null,
                new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE)));
    }

    public enum WeatherType {
        RAIN,
        SNOW,
        HAIL
    }

    private enum RainLevel {
        L1,
        L2,
        L3,
        L4
    }

    /**
     * Rain levels should be retrieved with {@link #getWeatherText(RainLevel, boolean includeEmote)}
     *
     * @param weatherType  The weather type, excluding rain
     * @param includeEmote If the emote should be included in the text
     * @return The weather's name
     */
    private String getWeatherText(WeatherType weatherType, boolean includeEmote) {
        switch (weatherType) {
            case SNOW -> {
                String name = "snowing";
                return includeEmote ? name + " 🌨️" : name;
            }

            case HAIL -> {
                String name = "hailing";
                return includeEmote ? name + " 🧊" : name;
            }

            default -> throw new IllegalStateException("Unexpected weather value: " + weatherType);
        }
    }

    private String getWeatherText(RainLevel rainLevel, boolean includeEmote) {
        switch (rainLevel) {
            case L1 -> {
                String name = "raining";
                return includeEmote ? name + " ☂️" : name;
            }

            case L2 -> {
                String name = "moderately raining";
                return includeEmote ? name + " ☔" : name;
            }

            case L3 -> {
                String name = "heavily raining";
                return includeEmote ? name + " 🌦️" : name;
            }

            case L4 -> {
                String name = "pouring";
                return includeEmote ? name + " 🌧️" : name;
            }

            default -> throw new IllegalStateException("Unexpected rain value: " + rainLevel);
        }
    }

    private RainLevel getRainLevel(double currentRainRate) {
        if (currentRainRate < 0.2) return RainLevel.L1;
        else if (currentRainRate < 0.3) return RainLevel.L2;
        else if (currentRainRate < 0.4) return RainLevel.L3;
        else return RainLevel.L4;
    }
}
