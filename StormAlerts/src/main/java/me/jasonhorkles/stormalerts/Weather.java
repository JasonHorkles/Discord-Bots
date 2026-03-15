package me.jasonhorkles.stormalerts;

import static me.jasonhorkles.stormalerts.Utils.ChannelUtils.*;

import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.requests.ErrorResponse;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import me.jasonhorkles.stormalerts.Utils.ChannelUtils;
import me.jasonhorkles.stormalerts.Utils.LogUtils;
import me.jasonhorkles.stormalerts.Utils.MessageUtils;

public class Weather {
    public Weather() {
        messageUtils = new MessageUtils();
    }

    public static boolean rainDenied;
    public static WeatherType previousWeatherType;

    private final MessageUtils messageUtils;
    private static final Map<WeatherType, IntensityLevel> previousLevels = new HashMap<>();
    private static Long allowedSnowTime;

    public void checkConditions(double currentRainRate, double temperature) throws IOException, ExecutionException, InterruptedException, TimeoutException, URISyntaxException {
        LogUtils logUtils = new LogUtils();
        System.out.println(logUtils.getTime(LogUtils.LogColor.YELLOW) + "Checking weather...");

        JSONObject input;
        if (StormAlerts.testing) input = new JSONObject(Files.readString(Path.of(
            "StormAlerts/Tests/weather.json"))).getJSONObject("data").getJSONObject("values");
        else {
            InputStream url = new URI(
                "https://api.tomorrow.io/v4/weather/realtime?location=84025%20US&units=imperial&apikey=" + new Secrets().tomorrowApiKey())
                .toURL().openStream();
            input = new JSONObject(new String(url.readAllBytes(), StandardCharsets.UTF_8)).getJSONObject(
                "data").getJSONObject("values");
            url.close();
        }

        // https://docs.tomorrow.io/reference/data-layers-weather-codes
        int weatherCode = input.getInt("weatherCode");
        int weatherCodeType = weatherCode / 1000;

        // Won't be null here or later if the weather is something we care about
        WeatherType weatherType = null;
        if (weatherCodeType == 7) weatherType = WeatherType.HAIL;
        else if (weatherCodeType == 5) weatherType = WeatherType.SNOW;

        // Check if the weather type has changed since last time (excluding precipitation levels)
        // Previous type will be null if not exciting and will be set at the end of everything
        boolean weatherIsDifferent = previousWeatherType != weatherType;

        // If snow is queued but not yet sent, don't cancel the snow message later on
        //noinspection BooleanVariableAlwaysNegated
        boolean snowQueued = false;

        // If the weather is still exciting, we'll want to update at least the bot status
        // If the weather/level has also changed since then, we'll want to send a message too
        if (weatherType != null) switch (weatherType) {
            case HAIL -> processWeather(weatherType, getWeatherLevel(weatherCode));

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
                        processWeather(weatherType, getWeatherLevel(weatherCode));
                    }

                // We'll want to send the snow message after 3 minutes IF it's still snowing by then
                // First check if we can even schedule the message
                if (canScheduleMessage)
                    // If the allowed snow time is null, set it to 3 minutes in advance
                    if (allowedSnowTime == null) { //noinspection NonThreadSafeLazyInitialization - There shouldn't be any case where the thread runs more than once per minute or so
                        allowedSnowTime = System.currentTimeMillis() + 180000;
                        System.out.println(logUtils.getTime(LogUtils.LogColor.YELLOW) + "Allowing snow messages in 5 minutes.");

                        snowQueued = true;
                        weatherType = null;
                        weatherIsDifferent = previousWeatherType != null;
                    }

                    // Otherwise, if it has already passed, send the snow message
                    else if (System.currentTimeMillis() >= allowedSnowTime) processWeather(
                        weatherType,
                        getWeatherLevel(weatherCode));

                    else { // Finally, if the snow is still waiting, set the weatherType to null
                        System.out.println(logUtils.getTime(LogUtils.LogColor.YELLOW) + "We're still waiting, though.");
                        snowQueued = true;
                        weatherType = null;
                        weatherIsDifferent = previousWeatherType != null;
                    }
            }
        }


        // If the weather is something else exciting, don't start to check the rain
        // If we know it's raining and it's not blocked
        // We need to get a level for the enum
        if (weatherType == null) if (currentRainRate > 0 && !rainDenied && temperature >= 30) {
            weatherType = WeatherType.RAIN;

            // Set weatherIsDifferent again now that we know it's raining
            weatherIsDifferent = previousWeatherType != weatherType;

            // Send the rain message if further checks have passed and the rain level has changed
            IntensityLevel rainLevel = getRainLevel(currentRainRate);
            processRain(rainLevel, currentRainRate);
        }


        // Determine if the weather has changed since our last alert
        if (!weatherIsDifferent)
            System.out.println(logUtils.getTime(LogUtils.LogColor.YELLOW) + "The weather hasn't changed!");

        // If no longer snowing, reset the allowed snow time
        if (weatherType != WeatherType.SNOW && !snowQueued) {
            if (allowedSnowTime != null) {
                allowedSnowTime = null;
                System.out.println(logUtils.getTime(LogUtils.LogColor.YELLOW) + "Snow stopped - resetting allowed time");
            }

            // Remove the previous level
            previousLevels.remove(WeatherType.SNOW);
        }

        // If no longer snowing (above), raining, or hailing, remove the previous level
        if (weatherType != WeatherType.RAIN) previousLevels.remove(WeatherType.RAIN);
        if (weatherType != WeatherType.HAIL) previousLevels.remove(WeatherType.HAIL);

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
            Activity.customStatus("Waiting for gnarly weather..."));

        previousWeatherType = weatherType;

        System.out.println(logUtils.getTime(LogUtils.LogColor.GREEN) + "Weather: " + weatherType + " (" + weatherCode + ")\n");
    }

    private void processWeather(WeatherType weatherType, IntensityLevel intensityLevel) {
        // If the level hasn't changed, return
        if (previousLevels.get(weatherType) == intensityLevel) return;
        previousLevels.put(weatherType, intensityLevel);

        // Update the presence
        StormAlerts.jda.getPresence().setPresence(
            OnlineStatus.ONLINE,
            Activity.customStatus("It's " + getWeatherText(weatherType, intensityLevel) + getWeatherEmoji(
                weatherType,
                intensityLevel,
                true)));

        // If the intensity bar should include all 4 levels
        boolean includeLevel4 = weatherType != WeatherType.HAIL;

        // Calculate what the message should be
        String message = getWeatherEmoji(weatherType, intensityLevel, false) + " It's " + getWeatherText(weatherType,
            intensityLevel) + "!\n" + getLevelBar(intensityLevel, includeLevel4);

        // Add weewoo if the level is the highest for that type
        if ((includeLevel4 && intensityLevel == IntensityLevel.L4) || (!includeLevel4 && intensityLevel == IntensityLevel.L3))
            message += " <a:weewoo:1083615022455992382>";

        TextChannel channel = new ChannelUtils().getWeatherChannel(weatherType);
        String roleId = "";
        switch (weatherType) {
            case HAIL -> roleId = "845055784156397608";
            case SNOW -> roleId = "845055624165064734";
        }
        String ping = messageUtils.shouldMsgPing(channel) ? "<@&" + roleId + ">\n" : "";

        channel.sendMessage(ping + message)
            .setSuppressedNotifications(messageUtils.shouldMsgBeSilent(channel)).queue();
    }

    // This will never be called if the rainDenied boolean is true
    private void processRain(IntensityLevel rainLevel, double currentRainRate) throws IOException, ExecutionException, InterruptedException, TimeoutException {
        // If it has NOT snowed in the last 5 days
        boolean hasNotSnowed = messageUtils.getMessages(snowChannel, 1).get(30, TimeUnit.SECONDS).getFirst()
            .getTimeCreated().isBefore(OffsetDateTime.now().minusDays(5));

        // If the rain has been accepted since the last snow or it hasn't snowed in the last 5 days
        if (rainAcceptedSinceSnow(snowChannel) || hasNotSnowed) {
            // Update the presence
            StormAlerts.jda.getPresence().setPresence(
                OnlineStatus.ONLINE,
                Activity.customStatus("It's raining @ " + currentRainRate + " in/hr"));

            System.out.println(new LogUtils().getTime(LogUtils.LogColor.GREEN) + "Raining @ " + currentRainRate + " in/hr");

            // If the level hasn't actually changed then that's all we needed to do
            // This is lower in the code than the other checks since the rain amount can change without the level changing in the status
            if (previousLevels.get(WeatherType.RAIN) == rainLevel) return;

            // Calculate what the rain message should be
            String message = getWeatherEmoji(WeatherType.RAIN, rainLevel, false) + " It's " + getWeatherText(WeatherType.RAIN,
                rainLevel) + "!\n" + getLevelBar(
                rainLevel,
                true) + " (" + currentRainRate + " in/hr)";

            if (rainLevel == IntensityLevel.L4) message += " <a:weewoo:1083615022455992382>";

            // Regular rain message
            String ping = messageUtils.shouldMsgPing(rainChannel) ? "<@&843956362059841596>\n" : "";
            rainChannel.sendMessage(ping + message).setSuppressedNotifications(messageUtils.shouldMsgBeSilent(
                rainChannel)).queue();

            // Heavy rain message
            if (rainLevel == IntensityLevel.L4) {
                String heavyPing = messageUtils.shouldMsgPing(heavyRainChannel) ? "<@&843956325690900503>\n" : "";
                heavyRainChannel.sendMessage(heavyPing + getWeatherEmoji(
                    WeatherType.RAIN,
                    IntensityLevel.L4,
                    false) + " It's " + getWeatherText(
                    WeatherType.RAIN,
                    IntensityLevel.L4) + "! (" + currentRainRate + " in/hr)").queue();
            }

            previousLevels.put(WeatherType.RAIN, rainLevel);

        } else { // Has snowed and rain hasn't been accepted since latest snow
            sendConfirmationMessage();
            StormAlerts.jda.getPresence().setPresence(
                OnlineStatus.IDLE,
                Activity.customStatus("It's maybe " + getWeatherText(
                    WeatherType.RAIN,
                    rainLevel) + getWeatherEmoji(
                    WeatherType.RAIN,
                    rainLevel,
                    true) + " @ " + currentRainRate + " in/hr"));
        }
    }

    // Returns true if the latest snow message id = the id in the file
    // If it doesn't match, then it's assumed to have snowed since the last rain confirmation
    private boolean rainAcceptedSinceSnow(TextChannel snowChannel) throws ExecutionException, InterruptedException, TimeoutException, IOException {
        Long latestId = messageUtils.getMessages(snowChannel, 1).get(30, TimeUnit.SECONDS).getFirst()
            .getIdLong();
        Long savedId = Long.parseLong(Files.readString(Path.of("StormAlerts/accepted-snow.txt")));

        return latestId.equals(savedId);
    }

    private void sendConfirmationMessage() throws ExecutionException, InterruptedException, TimeoutException {
        // Delete the message if it's older than 5 hours
        List<Message> latestMessage = messageUtils.getMessages(rainConfirmationChannel, 1).get(
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
                "<@277291758503723010>\n# PWS reports rain:").addComponents(ActionRow.of(
                Button
                    .success("acceptrain", "Accept since snow").withEmoji(Emoji.fromUnicode("✅")),
                Button.danger("denyrain", "Deny for 5 hours").withEmoji(Emoji.fromUnicode("✖️"))))
            .queue(del -> del.delete().queueAfter(
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

    public enum IntensityLevel {
        L1,
        L2,
        L3,
        L4
    }

    private String getWeatherText(WeatherType weatherType, IntensityLevel intensity) {
        switch (weatherType) {
            case RAIN -> {
                switch (intensity) {
                    case L1 -> {
                        return "raining";
                    }

                    case L2 -> {
                        return "moderately raining";
                    }

                    case L3 -> {
                        return "heavily raining";
                    }

                    case L4 -> {
                        return "pouring";
                    }
                }
            }

            case SNOW -> {
                switch (intensity) {
                    case L1 -> {
                        return "flurrying";
                    }

                    case L2 -> {
                        return "lightly snowing";
                    }

                    case L3 -> {
                        return "snowing";
                    }

                    case L4 -> {
                        return "heavily snowing";
                    }
                }
            }

            case HAIL -> {
                switch (intensity) {
                    case L1 -> {
                        return "lightly hailing";
                    }

                    case L2 -> {
                        return "hailing";
                    }

                    case L3 -> {
                        return "heavily hailing";
                    }

                    case L4 -> {
                        return "pouring";
                    }
                }
            }

            default -> throw new IllegalStateException("Unexpected weather type: " + weatherType);
        }

        throw new IllegalStateException("Unexpected weather intensity for " + weatherType + ": " + intensity);
    }

    private String getWeatherEmoji(WeatherType weatherType, IntensityLevel intensity, boolean prependSpace) {
        String emoji = prependSpace ? " " : "";
        switch (weatherType) {
            case RAIN -> {
                switch (intensity) {
                    case L1 -> emoji += "☂️";
                    case L2 -> emoji += "☔";
                    case L3 -> emoji += "🌦️";
                    case L4 -> emoji += "🌧️";
                }
            }

            case SNOW -> {
                switch (intensity) {
                    case L1, L2, L3, L4 -> emoji += "🌨️";
                }
            }

            case HAIL -> {
                switch (intensity) {
                    case L1, L2, L3 -> emoji += "🧊";
                    case L4 ->
                        throw new IllegalStateException("Unexpected weather intensity for " + weatherType + ": " + intensity);
                }
            }

            default -> throw new IllegalStateException("Unexpected weather type: " + weatherType);
        }

        return emoji;
    }

    private IntensityLevel getRainLevel(double currentRainRate) {
        if (currentRainRate < 0.2) return IntensityLevel.L1;
        else if (currentRainRate < 0.3) return IntensityLevel.L2;
        else if (currentRainRate < 0.4) return IntensityLevel.L3;
        else return IntensityLevel.L4;
    }

    private IntensityLevel getWeatherLevel(int weatherCode) {
        return switch (weatherCode) {
            case 5001, 7102 -> // Flurries, light hail
                IntensityLevel.L1;

            case 5100, 7000 -> // Light snow, hail
                IntensityLevel.L2;

            case 5000, 7101 -> // Snow, heavy hail
                IntensityLevel.L3;

            case 5101 -> // Heavy snow
                IntensityLevel.L4;

            default -> throw new IllegalStateException("Unexpected weather code: " + weatherCode);
        };
    }

    private String getLevelBar(IntensityLevel level, boolean includeLevel4) {
        String level4 = includeLevel4 ? "▫️" : "";
        return switch (level) {
            case L1 -> "🟩▫️▫️" + level4;
            case L2 -> "🟩🟨▫️" + level4;
            case L3 -> {
                if (includeLevel4) yield "🟩🟨🟧" + level4;
                else yield "🟩🟨🟥";
            }
            case L4 -> "🟩🟨🟧🟥";
        };
    }
}
