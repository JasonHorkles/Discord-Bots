package me.jasonhorkles.stormalerts;

import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
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
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Scanner;
import java.util.concurrent.*;

@SuppressWarnings("ConstantConditions")
public class CheckWeatherConditions extends ListenerAdapter {
    public static TextChannel previousTypeChannel;

    private static String weather;
    private static String previousWeatherName;
    private static String weatherName;
    private static double rainRate;
    private static boolean acceptRainForDay = false;

    public void checkConditions() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        System.out.println(new Utils().getTime(Utils.Color.YELLOW) + "Checking weather...");

        String visibilityInput = "null";

        if (!StormAlerts.testing) try {
            String page = "https://www.google.com/search?q=" + new Secrets().getWeatherSearch();
            Connection conn = Jsoup.connect(page).timeout(15000);
            Document doc = conn.get();
            weather = doc.body().getElementsByClass("wob_dcp").get(0).text();

            String apiUrl = "https://api.weather.gov/stations/" + new Secrets().getNwsStation() + "/observations/latest";
            visibilityInput = new Scanner(new URL(apiUrl).openStream(), StandardCharsets.UTF_8).useDelimiter("\\A")
                .next();
        } catch (SocketTimeoutException ignored) {
            System.out.println(new Utils().getTime(Utils.Color.RED) + "Timed out checking the weather!");
        }
        else {
            Scanner weatherScanner = new Scanner(new File("StormAlerts/Tests/weather.txt"));
            Scanner visibilityScanner = new Scanner(new File("StormAlerts/Tests/visibility.json"));
            weather = weatherScanner.nextLine();
            visibilityInput = visibilityScanner.nextLine();
        }

        int visibility = (int) Math.round(
            new JSONObject(visibilityInput).getJSONObject("properties").getJSONObject("visibility")
                .getInt("value") / 1609d);

        long visibilityChannel = 899872710233051178L;
        if (!StormAlerts.api.getVoiceChannelById(visibilityChannel).getName()
            .equals("Visibility | " + visibility + " mi"))
            StormAlerts.api.getVoiceChannelById(visibilityChannel).getManager()
                .setName("Visibility | " + visibility + " mi")
                .queue();

        weatherName = null;
        if (weather.toLowerCase().contains("hail") || weather.toLowerCase().contains("sleet"))
            weatherName = "hailing 🧊";
        else if (weather.toLowerCase().contains("snow") && !weather.toLowerCase().contains("light"))
            weatherName = "snowing 🌨️";
        else if (weather.toLowerCase().contains("thunder") || weather.toLowerCase()
            .contains("lightning") || weather.toLowerCase().contains("t-storm")) weatherName = "thundering ⛈️";

        rainRate = CheckPwsConditions.currentRainRate;
        String intensity = null;
        Integer rainIntensity = null;
        if (weatherName == null) if (rainRate > 0) {
            weather = "RAIN";

            if (rainRate > 0 && rainRate < 0.1) {
                weatherName = "drizzling 🌂";
                rainIntensity = 1;
                intensity = ":green_square::white_small_square::white_small_square::white_small_square::white_small_square:";

            } else if (rainRate < 0.2) {
                weatherName = "raining ☂️";
                rainIntensity = 2;
                intensity = ":green_square::green_square::white_small_square::white_small_square::white_small_square:";

            } else if (rainRate < 0.35) {
                weatherName = "moderately raining ☔";
                rainIntensity = 3;
                intensity = ":green_square::green_square::yellow_square::white_small_square::white_small_square:";

            } else if (rainRate < 0.5) {
                weatherName = "heavily raining 🌦️";
                rainIntensity = 4;
                intensity = ":green_square::green_square::yellow_square::orange_square::white_small_square:";

            } else {
                weatherName = "pouring 🌧️";
                rainIntensity = 5;
                intensity = ":green_square::green_square::yellow_square::orange_square::red_square:";
            }
        }

        String trimmedWeatherName = null;
        String doubleTrimmedWeatherName = null;
        if (weatherName != null) {
            trimmedWeatherName = weatherName.substring(0, weatherName.length() - 2).trim();
            doubleTrimmedWeatherName = trimmedWeatherName.substring(0, trimmedWeatherName.length() - 1).trim();
            if (weatherName.equals(previousWeatherName)) {
                System.out.println(new Utils().getTime(Utils.Color.YELLOW) + "Weather hasn't changed!");
                return;
            }
        }

        TextChannel thunderChannel = StormAlerts.api.getTextChannelById(843955628644892672L);
        TextChannel heavyRainChannel = StormAlerts.api.getTextChannelById(843955756596461578L);
        TextChannel rainChannel = StormAlerts.api.getTextChannelById(900248256515285002L);
        TextChannel rainConfirmationChannel = StormAlerts.api.getTextChannelById(921113488464695386L);
        TextChannel snowChannel = StormAlerts.api.getTextChannelById(845010495865618503L);
        TextChannel hailChannel = StormAlerts.api.getTextChannelById(845010798367473736L);

        boolean idle = false;

        // Send messages and change status if a certain weather type
        String ping = "";
        if (weatherName != null) if (weatherName.startsWith("thundering")) {
            if (new Utils().shouldIPing(thunderChannel)) ping = "<@&843956144401416193> ";
            thunderChannel.sendMessage(ping + ":thunder_cloud_rain: There's a thunderstorm! (" + weather + ")").queue();
            previousTypeChannel = thunderChannel;

        } else if (weatherName.startsWith("hailing")) {
            if (new Utils().shouldIPing(hailChannel)) ping = "<@&845055784156397608> ";
            hailChannel.sendMessage(ping + ":ice_cube: It's " + trimmedWeatherName + "! (" + weather + ")").queue();
            previousTypeChannel = hailChannel;

        } else if (weatherName.startsWith("snowing")) {
            if (new Utils().shouldIPing(snowChannel)) ping = "<@&845055624165064734> ";
            snowChannel.sendMessage(ping + ":cloud_snow: It's " + doubleTrimmedWeatherName + "! (" + weather + ")")
                .queue();
            previousTypeChannel = snowChannel;

        } else if (weather.equals("RAIN")) {
            if (new Utils().shouldIPing(rainChannel)) ping = "<@&843956362059841596> ";

            boolean mightBeSnow = false;
            OffsetDateTime lastSnow = new Utils().getMessages(StormAlerts.api.getTextChannelById(845010495865618503L),
                1).get(30, TimeUnit.SECONDS).get(0).getTimeCreated();
            if (lastSnow.isAfter(OffsetDateTime.now().minusDays(3))) mightBeSnow = true;

            boolean isNight = false;
            if (mightBeSnow) {
                LocalTime now = LocalTime.now();
                if (now.isAfter(LocalTime.parse("21:00:00")) || now.isBefore(LocalTime.parse("08:00:00")))
                    isNight = true;
            }

            if (!mightBeSnow || isNight) previousTypeChannel = rainChannel;

            switch (rainIntensity) {
                case 5 -> {
                    String heavyPing = "";
                    if (new Utils().shouldIPing(heavyRainChannel)) heavyPing = "<@&843956325690900503> ";
                    heavyRainChannel.sendMessage(heavyPing + ":cloud_rain: It's " + doubleTrimmedWeatherName + "!")
                        .queue();

                    rainChannel.sendMessage(ping + ":cloud_rain: It's " + doubleTrimmedWeatherName + "!\n" + intensity)
                        .queue();
                }

                case 4 -> rainChannel.sendMessage(
                    ping + ":white_sun_rain_cloud: It's " + doubleTrimmedWeatherName + "!\n" + intensity).queue();

                case 3 -> {
                    if (!acceptRainForDay) if (mightBeSnow && !isNight) {
                        rainConfirmationChannel.sendMessage(
                                ping + ":umbrella: It's " + trimmedWeatherName + "!\n" + intensity)
                            .setActionRow(Button.success("acceptrain", "Accept").withEmoji(Emoji.fromUnicode("✅")),
                                Button.primary("acceptrainforday", "Accept future rain for the day")
                                    .withEmoji(Emoji.fromUnicode("☑️")),
                                Button.secondary("unsurerain", "Unsure").withEmoji(Emoji.fromUnicode("❔")),
                                Button.danger("denyrain", "Deny").withEmoji(Emoji.fromUnicode("✖️"))).complete()
                            .delete().queueAfter(2, TimeUnit.HOURS, null,
                                new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
                        rainConfirmationChannel.sendMessage("<@277291758503723010>").complete().delete()
                            .queueAfter(250, TimeUnit.MILLISECONDS);
                        idle = true;
                    } else rainChannel.sendMessage(ping + ":umbrella: It's " + trimmedWeatherName + "!\n" + intensity)
                        .queue();
                }

                case 2 -> {
                    if (!acceptRainForDay) if (mightBeSnow && !isNight) {
                        rainConfirmationChannel.sendMessage(
                                ping + ":umbrella2: It's " + trimmedWeatherName + "!\n" + intensity)
                            .setActionRow(Button.success("acceptrain", "Accept").withEmoji(Emoji.fromUnicode("✅")),
                                Button.primary("acceptrainforday", "Accept future rain for the day")
                                    .withEmoji(Emoji.fromUnicode("☑️")),
                                Button.secondary("unsurerain", "Unsure").withEmoji(Emoji.fromUnicode("❔")),
                                Button.danger("denyrain", "Deny").withEmoji(Emoji.fromUnicode("✖️"))).complete()
                            .delete().queueAfter(2, TimeUnit.HOURS, null,
                                new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
                        rainConfirmationChannel.sendMessage("<@277291758503723010>").complete().delete()
                            .queueAfter(250, TimeUnit.MILLISECONDS);
                        idle = true;

                    } else rainChannel.sendMessage(ping + ":umbrella2: It's " + trimmedWeatherName + "!\n" + intensity)
                        .queue();
                }

                case 1 -> {
                    if (!acceptRainForDay) if (mightBeSnow && !isNight) {
                        rainConfirmationChannel.sendMessage(
                                ping + ":closed_umbrella: It's " + trimmedWeatherName + "!\n" + intensity)
                            .setActionRow(Button.success("acceptrain", "Accept").withEmoji(Emoji.fromUnicode("✅")),
                                Button.primary("acceptrainforday", "Accept future rain for the day")
                                    .withEmoji(Emoji.fromUnicode("☑️")),
                                Button.secondary("unsurerain", "Unsure").withEmoji(Emoji.fromUnicode("❔")),
                                Button.danger("denyrain", "Deny").withEmoji(Emoji.fromUnicode("✖️"))).complete()
                            .delete().queueAfter(2, TimeUnit.HOURS, null,
                                new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
                        rainConfirmationChannel.sendMessage("<@277291758503723010>").complete().delete()
                            .queueAfter(250, TimeUnit.MILLISECONDS);
                        idle = true;

                    } else rainChannel.sendMessage(
                        ping + ":closed_umbrella: It's " + trimmedWeatherName + "!\n" + intensity).queue();
                }

                default -> System.out.println(
                    new Utils().getTime(Utils.Color.RED) + "[ERROR] It's raining, but there's no intensity!");
            }
        }

        // Nothing exciting
        if (weatherName == null) idle = true;

        if (idle) {
            StormAlerts.api.getPresence().setStatus(OnlineStatus.IDLE);

            if (previousTypeChannel != null) {
                Message message = new Utils().getMessages(previousTypeChannel, 1).get(30, TimeUnit.SECONDS).get(0);
                if (!message.getContentRaw().contains("Ended") && !message.getContentRaw().contains("restarted"))
                    message.editMessage(message.getContentRaw()
                        .replace("!", "! (Ended at <t:" + System.currentTimeMillis() / 1000 + ":t>)")).queue();
                previousTypeChannel = null;
            }
        } else StormAlerts.api.getPresence().setStatus(OnlineStatus.ONLINE);

        if (idle) StormAlerts.api.getPresence().setActivity(Activity.watching("for gnarly weather"));

        else if (weather.equals("RAIN")) StormAlerts.api.getPresence()
            .setActivity(Activity.playing("it's " + weatherName + " @ " + rainRate + " in/hr"));

        else StormAlerts.api.getPresence().setActivity(Activity.playing("it's " + weatherName));
        previousWeatherName = weatherName;

        System.out.println(new Utils().getTime(Utils.Color.GREEN) + "Weather: " + weather);
    }

    // Button press
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
                scheduler.scheduleAtFixedRate(() -> acceptRainForDay = false, initalDelay, TimeUnit.DAYS.toSeconds(1),
                    TimeUnit.SECONDS);
            }

            case "unsurerain" -> {
                event.deferEdit().queue();
                TextChannel rainChannel = StormAlerts.api.getTextChannelById(900248256515285002L);
                rainChannel.sendMessage(event.getMessage().getContentRaw().replace("<@&843956362059841596> ", "")
                    .replace("!", "! (May be snow melting in the rain gauge)")).queue();
                event.getMessage().delete().queue();

                StormAlerts.api.getPresence().setStatus(OnlineStatus.ONLINE);
                StormAlerts.api.getPresence()
                    .setActivity(Activity.playing("it's possibly " + weatherName + " @ " + rainRate + " in/hr"));
                previousWeatherName = weatherName;
                previousTypeChannel = rainChannel;
            }

            case "denyrain" -> {
                event.deferEdit().queue();
                event.getMessage().delete().queue();
            }
        }
    }

    private void acceptRain(ButtonInteractionEvent event) {
        event.deferEdit().queue();
        TextChannel rainChannel = StormAlerts.api.getTextChannelById(900248256515285002L);
        rainChannel.sendMessage(event.getMessage().getContentRaw()).queue();
        event.getMessage().delete().queue();

        StormAlerts.api.getPresence().setStatus(OnlineStatus.ONLINE);
        StormAlerts.api.getPresence()
            .setActivity(Activity.playing("it's " + weatherName + " @ " + rainRate + " in/hr"));
        previousWeatherName = weatherName;
        previousTypeChannel = rainChannel;
    }
}
