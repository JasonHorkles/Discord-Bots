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
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Scanner;
import java.util.concurrent.*;

@SuppressWarnings("DataFlowIssue")
public class Weather extends ListenerAdapter {
    public static TextChannel previousTypeChannel;

    private static boolean acceptRainForDay = false;
    private static double rainRate;
    private static ScheduledFuture<?> scheduledSnowMessage;
    private static String previousWeatherName;
    private static String weather = "null";
    private static String weatherName;

    public void checkConditions() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Checking weather...");

        StringBuilder visibilityInput = new StringBuilder();

        boolean weatherOffline = false;

        if (!StormAlerts.testing) try {
            String page = "https://www.google.com/search?q=" + new Secrets().getWeatherSearch();
            Connection conn = Jsoup.connect(page).timeout(15000);
            Document doc = conn.get();
            weather = doc.body().getElementsByClass("wob_dcp").get(0).text();

            String apiUrl = "https://api.weather.gov/stations/" + new Secrets().getNwsStation() + "/observations/latest";
            InputStream stream = new URL(apiUrl).openStream();
            Scanner scanner = new Scanner(stream, StandardCharsets.UTF_8).useDelimiter("\\A");
            while (scanner.hasNextLine()) visibilityInput.append(scanner.nextLine());
            stream.close();
        } catch (SocketTimeoutException ignored) {
            System.out.println(new Utils().getTime(Utils.LogColor.RED) + "Timed out checking the weather!");
            weatherOffline = true;
        } catch (IndexOutOfBoundsException ignored) {
            System.out.println(
                new Utils().getTime(Utils.LogColor.RED) + "Couldn't get the weather! (No Results)");
            weatherOffline = true;
        }
        else {
            Scanner weatherScanner = new Scanner(new File("StormAlerts/Tests/weather.txt"));
            Scanner visibilityScanner = new Scanner(new File("StormAlerts/Tests/visibility.json"));
            weather = weatherScanner.nextLine();
            visibilityInput.append(visibilityScanner.nextLine());
        }

        String visibility;
        if (!weatherOffline) visibility = String.valueOf((int) Math.round(
            new JSONObject(visibilityInput.toString()).getJSONObject("properties").getJSONObject("visibility")
                .getInt("value") / 1609d));
        else visibility = "ERROR";

        long visibilityChannel = 899872710233051178L;
        if (!StormAlerts.jda.getVoiceChannelById(visibilityChannel).getName()
            .equals("Visibility | " + visibility + " mi"))
            StormAlerts.jda.getVoiceChannelById(visibilityChannel).getManager()
                .setName("Visibility | " + visibility + " mi").queue();

        weatherName = null;
        if (weather.toLowerCase().contains("hail") || weather.toLowerCase().contains("sleet"))
            weatherName = "hailing 🧊";
        else if (weather.toLowerCase().contains("snow") || weather.toLowerCase().contains("freezing rain"))
            weatherName = "snowing 🌨️";

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
        String doubleTrimmedWeatherName = null;
        boolean dontSendAlerts = false;
        if (weatherName != null) {
            trimmedWeatherName = weatherName.substring(0, weatherName.length() - 2).strip();
            doubleTrimmedWeatherName = trimmedWeatherName.substring(0, trimmedWeatherName.length() - 1)
                .strip();
            if (weatherName.equals(previousWeatherName)) {
                System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Weather hasn't changed!");
                dontSendAlerts = true;
            }
        }

        TextChannel heavyRainChannel = StormAlerts.jda.getTextChannelById(843955756596461578L);
        TextChannel rainChannel = StormAlerts.jda.getTextChannelById(900248256515285002L);
        TextChannel rainConfirmationChannel = StormAlerts.jda.getTextChannelById(921113488464695386L);
        TextChannel snowChannel = StormAlerts.jda.getTextChannelById(845010495865618503L);
        TextChannel hailChannel = StormAlerts.jda.getTextChannelById(845010798367473736L);

        boolean idle = false;

        // Send messages and change status if a certain weather type
        if (weatherName != null) {
            if (!weatherName.startsWith("snowing")) if (scheduledSnowMessage != null) {
                scheduledSnowMessage.cancel(true);
                scheduledSnowMessage = null;
            }

            if (weatherName.startsWith("hailing")) {
                if (!dontSendAlerts) {
                    String ping = "";
                    if (new Utils().shouldIPing(hailChannel)) ping = "<@&845055784156397608>\n";
                    // 🧊
                    hailChannel.sendMessage(
                        ping + "\uD83E\uDDCA It's " + trimmedWeatherName + "! (" + weather + ")").queue();
                    previousTypeChannel = hailChannel;
                }

            } else if (weatherName.startsWith("snowing")) {
                if (!dontSendAlerts) {
                    // 🌨️
                    String finalDoubleTrimmedWeatherName = doubleTrimmedWeatherName;

                    // Send the snow message after 45 minutes IF it's still snowing by then
                    scheduledSnowMessage = Executors.newSingleThreadScheduledExecutor().schedule(() -> {
                        // It should already be cancelled if it stopped snowing, but this is a failsafe
                        if (!weatherName.startsWith("snowing")) return;

                        String ping = "";
                        if (new Utils().shouldIPing(snowChannel)) ping = "<@&845055624165064734>\n";
                        snowChannel.sendMessage(
                                ping + "\uD83C\uDF28️ It's " + finalDoubleTrimmedWeatherName + "! (" + weather + ")")
                            .queue();
                        scheduledSnowMessage = null;
                    }, 2705, TimeUnit.SECONDS);
                    previousTypeChannel = snowChannel;
                }

            } else if (weather.equals("RAIN")) {
                String ping = "";
                if (new Utils().shouldIPing(rainChannel)) ping = "<@&843956362059841596>\n";

                boolean mightBeSnow = false;
                OffsetDateTime lastSnow = new Utils().getMessages(
                        StormAlerts.jda.getTextChannelById(845010495865618503L), 1).get(30, TimeUnit.SECONDS)
                    .get(0).getTimeCreated();
                if (lastSnow.isAfter(OffsetDateTime.now().minusDays(3))) mightBeSnow = true;

                boolean isNight = false;
                if (mightBeSnow) {
                    LocalTime now = LocalTime.now();
                    if (now.isAfter(LocalTime.parse("18:00:00")) || now.isBefore(LocalTime.parse("09:00:00")))
                        isNight = true;
                }

                if (!mightBeSnow || isNight) previousTypeChannel = rainChannel;

                switch (rainIntensity) {
                    case 4 -> {
                        if (!dontSendAlerts) {
                            String heavyPing = "";
                            if (new Utils().shouldIPing(heavyRainChannel))
                                heavyPing = "<@&843956325690900503> ";
                            // 🌧️
                            heavyRainChannel.sendMessage(
                                    heavyPing + "\uD83C\uDF27️ It's " + doubleTrimmedWeatherName + "! (" + rainRate + " in/hr)")
                                .queue();

                            rainChannel.sendMessage(
                                    ping + "\uD83C\uDF27️ It's " + doubleTrimmedWeatherName + "!\n" + intensity + " (" + rainRate + " in/hr)")
                                .queue();
                        }
                    }

                    case 3 -> {
                        if (!acceptRainForDay) if (mightBeSnow && !isNight) {
                            // 🌦️
                            //todo Change buttons to accept for day, unsure, and decline for 1 hour
                            if (!dontSendAlerts) rainConfirmationChannel.sendMessage(
                                    "[CONFIRMATION NEEDED] " + ping + "\uD83C\uDF26️ It's " + doubleTrimmedWeatherName + "!\n" + intensity + " (" + rainRate + " in/hr)")
                                .setActionRow(
                                    Button.success("acceptrain", "Accept").withEmoji(Emoji.fromUnicode("✅")),
                                    Button.primary("acceptrainforday", "Accept future rain for the day")
                                        .withEmoji(Emoji.fromUnicode("☑️")),
                                    Button.secondary("unsurerain", "Unsure")
                                        .withEmoji(Emoji.fromUnicode("❔")),
                                    Button.danger("denyrain", "Deny").withEmoji(Emoji.fromUnicode("✖️")))
                                .complete().delete().queueAfter(2, TimeUnit.HOURS, null,
                                    new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
                            idle = true;

                            //bug This doesn't send if rain is accepted for day
                        } else if (!dontSendAlerts) rainChannel.sendMessage(
                                ping + "\uD83C\uDF26️ It's " + doubleTrimmedWeatherName + "!\n" + intensity + " (" + rainRate + " in/hr)")
                            .queue();
                    }

                    case 2 -> {
                        if (!acceptRainForDay) if (mightBeSnow && !isNight) {
                            if (!dontSendAlerts) rainConfirmationChannel.sendMessage(
                                    "[CONFIRMATION NEEDED] " + ping + "☔ It's " + trimmedWeatherName + "!\n" + intensity + " (" + rainRate + " in/hr)")
                                .setActionRow(
                                    Button.success("acceptrain", "Accept").withEmoji(Emoji.fromUnicode("✅")),
                                    Button.primary("acceptrainforday", "Accept future rain for the day")
                                        .withEmoji(Emoji.fromUnicode("☑️")),
                                    Button.secondary("unsurerain", "Unsure")
                                        .withEmoji(Emoji.fromUnicode("❔")),
                                    Button.danger("denyrain", "Deny").withEmoji(Emoji.fromUnicode("✖️")))
                                .complete().delete().queueAfter(2, TimeUnit.HOURS, null,
                                    new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
                            idle = true;

                        } else if (!dontSendAlerts) rainChannel.sendMessage(
                                ping + "☔ It's " + trimmedWeatherName + "!\n" + intensity + " (" + rainRate + " in/hr)")
                            .queue();
                    }

                    case 1 -> {
                        if (!acceptRainForDay) if (mightBeSnow && !isNight) {
                            if (!dontSendAlerts) rainConfirmationChannel.sendMessage(
                                    "[CONFIRMATION NEEDED] " + ping + "☂️ It's " + trimmedWeatherName + "!\n" + intensity + " (" + rainRate + " in/hr)")
                                .setActionRow(
                                    Button.success("acceptrain", "Accept").withEmoji(Emoji.fromUnicode("✅")),
                                    Button.primary("acceptrainforday", "Accept future rain for the day")
                                        .withEmoji(Emoji.fromUnicode("☑️")),
                                    Button.secondary("unsurerain", "Unsure")
                                        .withEmoji(Emoji.fromUnicode("❔")),
                                    Button.danger("denyrain", "Deny").withEmoji(Emoji.fromUnicode("✖️")))
                                .complete().delete().queueAfter(2, TimeUnit.HOURS, null,
                                    new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
                            idle = true;

                        } else if (!dontSendAlerts) rainChannel.sendMessage(
                                ping + "☂️ It's " + trimmedWeatherName + "!\n" + intensity + " (" + rainRate + " in/hr)")
                            .queue();
                    }

                    default -> System.out.println(new Utils().getTime(
                        Utils.LogColor.RED) + "[ERROR] It's raining, but there's no intensity!");
                }
            }
        } else {
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
                Message message = new Utils().getMessages(previousTypeChannel, 1).get(30, TimeUnit.SECONDS)
                    .get(0);
                if (!message.getContentRaw().contains("Ended") && !message.getContentRaw()
                    .contains("restarted")) message.editMessage(message.getContentRaw()
                    .replace("!", "! (Ended at <t:" + System.currentTimeMillis() / 1000 + ":t>)")).queue();
                previousTypeChannel = null;
            }

        } else if (weather.equals("RAIN")) {
            StormAlerts.jda.getPresence().setActivity(Activity.watching("the rain @ " + rainRate + " in/hr"));
            System.out.println(
                new Utils().getTime(Utils.LogColor.GREEN) + "Raining @ " + rainRate + " in/hr");

        } else StormAlerts.jda.getPresence()
            .setActivity(Activity.playing("it's " + weatherName + " (" + weather.toLowerCase() + ")"));

        previousWeatherName = weatherName;

        System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + "Weather: " + weather.toLowerCase());
    }

    // Rain confirmation stuff
    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        switch (event.getComponentId()) {
            case "acceptrain" -> acceptRain(event);

            case "acceptrainforday" -> {
                acceptRainForDay = true;
                acceptRain(event);

                ZonedDateTime now = ZonedDateTime.now();
                ZonedDateTime nextRun = now.withHour(0).withMinute(0).withSecond(0);
                if (now.compareTo(nextRun) > 0) nextRun = nextRun.plusDays(1);

                Duration duration = Duration.between(now, nextRun);
                long initalDelay = duration.getSeconds();

                ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
                scheduler.scheduleAtFixedRate(() -> acceptRainForDay = false, initalDelay,
                    TimeUnit.DAYS.toSeconds(1), TimeUnit.SECONDS);
            }

            case "unsurerain" -> {
                event.deferEdit().queue();
                TextChannel rainChannel = StormAlerts.jda.getTextChannelById(900248256515285002L);
                rainChannel.sendMessage(
                    event.getMessage().getContentRaw().replaceFirst("\\[CONFIRMATION NEEDED] ", "")
                        .replace("!", "! (May be snow melting in the rain gauge)")).queue();
                event.getMessage().delete().queue();

                StormAlerts.jda.getPresence().setStatus(OnlineStatus.ONLINE);
                StormAlerts.jda.getPresence().setActivity(
                    Activity.playing("it's possibly " + weatherName + " @ " + rainRate + " in/hr"));
                previousWeatherName = weatherName;
                previousTypeChannel = rainChannel;
            }

            case "denyrain" -> {
                event.deferEdit().queue();
                event.getMessage().delete().queue();
            }
        }
    }

    public void acceptRain(ButtonInteractionEvent event) {
        event.deferEdit().queue();
        TextChannel rainChannel = StormAlerts.jda.getTextChannelById(900248256515285002L);
        rainChannel.sendMessage(
            event.getMessage().getContentRaw().replaceFirst("\\[CONFIRMATION NEEDED] ", "")).queue();
        event.getMessage().delete().queue();

        StormAlerts.jda.getPresence().setStatus(OnlineStatus.ONLINE);
        StormAlerts.jda.getPresence().setActivity(Activity.watching("the rain @ " + rainRate + " in/hr"));
        previousWeatherName = weatherName;
        previousTypeChannel = rainChannel;
    }
}
