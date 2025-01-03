package me.jasonhorkles.stormalerts;

import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.*;

@SuppressWarnings("DataFlowIssue")
public class Weather extends ListenerAdapter {
    public static TextChannel previousTypeChannel;

    private static boolean acceptRainForDay;
    private static boolean rainDenied;
    private static double rainRate;
    private static ScheduledFuture<?> scheduledSnowMessage;
    private static String previousWeatherName;
    private static String weatherName;

    public void checkConditions() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        Utils utils = new Utils();
        System.out.println(utils.getTime(Utils.LogColor.YELLOW) + "Checking weather...");

        String weather;
        if (StormAlerts.testing) weather = Files.readString(Path.of("StormAlerts/Tests/weather.txt"));
        else {
            Connection conn = Jsoup
                .connect("https://weather.com/weather/today/l/" + new Secrets().weatherCode()).timeout(15000);
            Document doc = conn.get();
            weather = doc.select("[class*=\"CurrentConditions--phraseValue--\"]").first().text();
        }

        if (weather.isBlank()) throw new IOException("Weather type is blank");

        weatherName = null;
        if (weather.toLowerCase().contains("hail") || weather.toLowerCase().contains("sleet"))
            weatherName = "hailing 🧊";
        else if (weather.toLowerCase().contains("snow")) weatherName = "snowing 🌨️";

        rainRate = Pws.currentRainRate;
        String intensity = null;
        Integer rainIntensity = null;
        if (weatherName == null) if (rainRate > 0) {
            weather = "RAIN";

            if (rainRate < 0.2) {
                weatherName = "raining ☂️";
                rainIntensity = 1;
                // 🟩▫️▫️▫️
                intensity = "\uD83D\uDFE9▫️▫️▫️";

            } else if (rainRate < 0.3) {
                weatherName = "moderately raining ☔";
                rainIntensity = 2;
                // 🟩🟨▫️▫️
                intensity = "\uD83D\uDFE9\uD83D\uDFE8▫️▫️";

            } else if (rainRate < 0.4) {
                weatherName = "heavily raining 🌦️";
                rainIntensity = 3;
                // 🟩🟨🟧▫️
                intensity = "\uD83D\uDFE9\uD83D\uDFE8\uD83D\uDFE7▫️";

            } else {
                weatherName = "pouring 🌧️";
                rainIntensity = 4;
                // 🟩🟨🟧🟥
                intensity = "\uD83D\uDFE9\uD83D\uDFE8\uD83D\uDFE7\uD83D\uDFE5";
            }
        }

        String trimmedWeatherName = null;
        boolean sendAlerts = true;
        if (weatherName != null) {
            trimmedWeatherName = trimmedWeatherName();

            if (weatherName.equals(previousWeatherName)) {
                System.out.println(utils.getTime(Utils.LogColor.YELLOW) + "The weather hasn't changed!");
                sendAlerts = false;
            }
        }

        TextChannel heavyRainChannel = StormAlerts.jda.getTextChannelById(843955756596461578L);
        TextChannel rainChannel = StormAlerts.jda.getTextChannelById(900248256515285002L);
        TextChannel snowChannel = StormAlerts.jda.getTextChannelById(845010495865618503L);
        TextChannel hailChannel = StormAlerts.jda.getTextChannelById(845010798367473736L);

        boolean idle = false;

        // If the weather is something we care about, it won't be null
        if (weatherName != null && sendAlerts) {
            // If not snowing, cancel any pending snow messages
            if (!weatherName.startsWith("snowing")) if (scheduledSnowMessage != null) {
                scheduledSnowMessage.cancel(true);
                scheduledSnowMessage = null;
            }

            if (weatherName.startsWith("hailing")) {
                String ping = "";
                if (utils.shouldIPing(hailChannel)) ping = "<@&845055784156397608>\n";
                // 🧊
                hailChannel
                    .sendMessage(ping + "\uD83E\uDDCA It's " + trimmedWeatherName + "! (" + weather + ")")
                    .setSuppressedNotifications(utils.shouldIBeSilent(hailChannel)).queue();
                previousTypeChannel = hailChannel;

            } else if (weatherName.startsWith("snowing")) {
                boolean scheduleMessage = true;

                // If the bot had just restarted, send snow message instantly and silently
                try {
                    Message message = utils.getMessages(snowChannel, 1).get(30, TimeUnit.SECONDS).getFirst();

                    // If the message was edited within the last 3 minutes and it contains the restart message
                    if (message.isEdited()) if (message.getTimeEdited().isAfter(OffsetDateTime.now()
                        .minusMinutes(3)) && message.getContentRaw().contains("(Bot restarted at")) {
                        scheduleMessage = false;
                        sendSnowMessage(snowChannel, weather);
                    }

                } catch (Exception e) {
                    System.out.print(utils.getTime(Utils.LogColor.RED));
                    e.printStackTrace();
                    utils.logError(e);
                }

                // Send the snow message after 30 minutes IF it's still snowing by then
                if (scheduleMessage) {
                    String finalWeather = weather;
                    try (ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor()) {
                        scheduledSnowMessage = executor.schedule(
                            () -> sendSnowMessage(
                                snowChannel,
                                finalWeather), 30, TimeUnit.MINUTES);
                    }
                }

            } else if (weather.equals("RAIN") && Pws.temperature >= 30) {
                String ping = "";
                if (utils.shouldIPing(rainChannel)) ping = "<@&843956362059841596>\n";

                // If it has NOT snowed in the last 3 days
                boolean notSnowMelt = utils.getMessages(snowChannel, 1).get(30, TimeUnit.SECONDS).getFirst()
                    .getTimeCreated().isBefore(OffsetDateTime.now().minusDays(5));

                String message = null;
                switch (rainIntensity) {
                    case 4 -> {
                        String heavyPing = "";
                        if (utils.shouldIPing(heavyRainChannel)) heavyPing = "<@&843956325690900503>\n";
                        // 🌧️
                        heavyRainChannel
                            .sendMessage(heavyPing + "\uD83C\uDF27️ It's " + trimmedWeatherName + "! (" + rainRate + " in/hr)")
                            .queue();

                        message = ping + "\uD83C\uDF27️ It's " + trimmedWeatherName + "!\n" + intensity + " (" + rainRate + " in/hr) <a:weewoo:1083615022455992382>";
                    }

                    case 3 -> // 🌦️
                        message = ping + "\uD83C\uDF26️ It's " + trimmedWeatherName + "!\n" + intensity + " (" + rainRate + " in/hr)";

                    case 2 ->
                        message = ping + "☔ It's " + trimmedWeatherName + "!\n" + intensity + " (" + rainRate + " in/hr)";

                    case 1 ->
                        message = ping + "☂️ It's " + trimmedWeatherName + "!\n" + intensity + " (" + rainRate + " in/hr)";

                    default ->
                        System.out.println(utils.getTime(Utils.LogColor.RED) + "[ERROR] It's raining, but there's no valid intensity! (" + rainIntensity + ")");
                }

                if (acceptRainForDay || notSnowMelt)
                    rainChannel.sendMessage(message).setSuppressedNotifications(utils.shouldIBeSilent(
                        rainChannel)).queue();
                else {
                    sendConfirmationMessage("[CONFIRMATION NEEDED] " + message);
                    idle = true;
                }

                previousTypeChannel = rainChannel;
            }
        }

        // Weather is no longer exciting, so cancel any existing scheduled snow
        if (weatherName == null) {
            if (scheduledSnowMessage != null) {
                scheduledSnowMessage.cancel(true);
                scheduledSnowMessage = null;
            }
            idle = true;
        }

        if (!idle) StormAlerts.jda.getPresence().setStatus(OnlineStatus.ONLINE);

        if (idle) {
            StormAlerts.jda.getPresence().setActivity(Activity.watching("for gnarly weather"));
            StormAlerts.jda.getPresence().setStatus(OnlineStatus.IDLE);

            if (previousTypeChannel != null) {
                Message message = utils.getMessages(previousTypeChannel, 1).get(30, TimeUnit.SECONDS)
                    .getFirst();
                if (!message.getContentRaw().contains("Ended") && !message.getContentRaw().contains(
                    "restarted")) message.editMessage(message.getContentRaw()
                    .replace("!", "! (Ended at <t:" + System.currentTimeMillis() / 1000 + ":t>)")).queue();
                previousTypeChannel = null;
            }

        } else if (weather.equals("RAIN")) {
            StormAlerts.jda.getPresence().setActivity(Activity.watching("the rain @ " + rainRate + " in/hr"));
            System.out.println(utils.getTime(Utils.LogColor.GREEN) + "Raining @ " + rainRate + " in/hr");

        } else StormAlerts.jda.getPresence()
            .setActivity(Activity.customStatus("It's " + weatherName + " (" + weather + ")"));

        previousWeatherName = weatherName;

        System.out.println(utils.getTime(Utils.LogColor.GREEN) + "Weather: " + weather);
    }

    // Rain confirmation stuff
    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        switch (event.getComponentId()) {
            case "acceptrain" -> {
                acceptRainForDay = true;

                TextChannel rainChannel = StormAlerts.jda.getTextChannelById(900248256515285002L);
                rainChannel.sendMessage(event.getMessage().getContentRaw()
                    .replaceFirst("\\[CONFIRMATION NEEDED] ", "")).queue();
                event.deferEdit().queue(na -> event.getMessage().delete().queue());

                StormAlerts.jda.getPresence().setStatus(OnlineStatus.ONLINE);
                StormAlerts.jda.getPresence()
                    .setActivity(Activity.watching("the rain @ " + rainRate + " in/hr"));
                previousWeatherName = weatherName;
                previousTypeChannel = rainChannel;

                // Schedule a task to reset the acceptRainForDay variable at midnight
                ZonedDateTime now = ZonedDateTime.now();
                ZonedDateTime nextRun = now.withHour(0).withMinute(0).withSecond(0);
                if (now.compareTo(nextRun) > 0) nextRun = nextRun.plusDays(1);

                Duration duration = Duration.between(now, nextRun);
                long initalDelay = duration.getSeconds();

                new Thread(
                    () -> {
                        try (ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor()) {
                            executor.schedule(() -> acceptRainForDay = false, initalDelay, TimeUnit.SECONDS);
                        }
                    }, "Accept Rain New Day").start();
            }

            case "unsurerain" -> {
                TextChannel rainChannel = StormAlerts.jda.getTextChannelById(900248256515285002L);
                rainChannel.sendMessage(event.getMessage().getContentRaw().replaceFirst(
                        "\\[CONFIRMATION NEEDED] ",
                        "").replaceFirst("<@&843956362059841596>\n", "")
                    .replace("!", "! (May be snow melting in the rain gauge)")).queue();
                event.deferEdit().queue(na -> event.getMessage().delete().queue());

                StormAlerts.jda.getPresence().setStatus(OnlineStatus.ONLINE);
                StormAlerts.jda.getPresence()
                    .setActivity(Activity.playing("it's possibly " + weatherName + " @ " + rainRate + " in/hr"));
                previousWeatherName = weatherName;
                previousTypeChannel = rainChannel;
            }

            case "denyrain" -> {
                rainDenied = true;
                new Thread(
                    () -> {
                        try (ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor()) {
                            executor.schedule(() -> rainDenied = false, 1, TimeUnit.HOURS);
                        }
                    }, "Deny Rain").start();
                event.deferEdit().queue(na -> event.getMessage().delete().queue());
            }
        }
    }

    private String trimmedWeatherName() {
        return weatherName.strip().replaceAll("\\s+\\S*$", "");
    }

    private void sendConfirmationMessage(String message) {
        if (rainDenied) return;

        TextChannel channel = StormAlerts.jda.getTextChannelById(921113488464695386L);
        Utils utils = new Utils();

        // Delete any old messages
        try {
            List<Message> latestMessages = utils.getMessages(channel, 6).get(30, TimeUnit.SECONDS);
            if (!latestMessages.isEmpty())
                for (Message messageToDelete : latestMessages) messageToDelete.delete().queue();
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            System.out.print(utils.getTime(Utils.LogColor.RED));
            e.printStackTrace();
            utils.logError(e);
        }

        channel.sendMessage(message).setActionRow(
            Button.success("acceptrain", "Accept for the day").withEmoji(Emoji.fromUnicode("✅")),
            Button.secondary("unsurerain", "Unsure").withEmoji(Emoji.fromUnicode("❔")),
            Button.danger("denyrain", "Deny for 1 hour").withEmoji(Emoji.fromUnicode("✖️"))).queue(del -> del
            .delete().queueAfter(
                15,
                TimeUnit.MINUTES,
                null,
                new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE)));
    }

    private void sendSnowMessage(TextChannel snowChannel, String weather) {
        // It should already be cancelled if it stopped snowing, but this is a failsafe
        if (!weatherName.startsWith("snowing")) return;

        Utils utils = new Utils();
        String ping = "";
        if (utils.shouldIPing(snowChannel)) ping = "<@&845055624165064734>\n";
        // 🌨️
        snowChannel.sendMessage(ping + "\uD83C\uDF28️ It's " + trimmedWeatherName() + "! (" + weather + ")")
            .setSuppressedNotifications(utils.shouldIBeSilent(snowChannel)).queue();
        scheduledSnowMessage = null;
        previousTypeChannel = snowChannel;
    }
}
