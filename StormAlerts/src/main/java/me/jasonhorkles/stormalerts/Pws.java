package me.jasonhorkles.stormalerts;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Pws {
    public static double currentRainRate = 0.00;
    public static double lastAlertedWindGust = -1;
    public static double temperature = -1;
    public static double wm2 = -1;

    private static boolean rateLimited = false;

    @SuppressWarnings("DataFlowIssue")
    public void checkConditions() throws IOException {
        System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Checking PWS conditions...");

        JSONObject input;
        if (!StormAlerts.testing) {
            InputStream url = new URL(
                "https://api.ambientweather.net/v1/devices/?apiKey=" + new Secrets().getAwApiKey() + "&applicationKey=" + new Secrets().getAwAppKey()).openStream();
            String out = new Scanner(url, StandardCharsets.UTF_8).useDelimiter("\\A").nextLine();
            url.close();

            input = new JSONArray(out).getJSONObject(0);
        } else {
            File file = new File("StormAlerts/Tests/pwsweather.json");
            Scanner fileScanner = new Scanner(file);

            String out = fileScanner.nextLine();
            input = new JSONArray(out).getJSONObject(0);
        }
        input = new JSONObject(new Utils().getJsonKey(input, "lastData", true));

        temperature = Double.parseDouble(new Utils().getJsonKey(input, "tempf", true));

        try {
            currentRainRate = Double.parseDouble(new Utils().getJsonKey(input, "hourlyrainin", true));
        } catch (NumberFormatException ignored) {
            currentRainRate = -1;
        }

        try {
            wm2 = Double.parseDouble(new Utils().getJsonKey(input, "solarradiation", true));
        } catch (NumberFormatException ignored) {
            wm2 = -1;
        }

        // Set the values
        int humidity = Integer.parseInt(new Utils().getJsonKey(input, "humidity", true));
        double feelsLike = Double.parseDouble(new Utils().getJsonKey(input, "feelsLike", true));
        int uv = Integer.parseInt(new Utils().getJsonKey(input, "uv", true));
        int wind = Math.toIntExact(
            Math.round(Double.parseDouble(new Utils().getJsonKey(input, "windspeedmph", true))));
        int windGust = Math.toIntExact(
            Math.round(Double.parseDouble(new Utils().getJsonKey(input, "windgustmph", true))));
        int windMax = Math.toIntExact(
            Math.round(Double.parseDouble(new Utils().getJsonKey(input, "maxdailygust", true))));
        double rainDaily = Double.parseDouble(new Utils().getJsonKey(input, "dailyrainin", true));
        double rainWeekly = Double.parseDouble(new Utils().getJsonKey(input, "weeklyrainin", true));
        double rainMonthly = Double.parseDouble(new Utils().getJsonKey(input, "monthlyrainin", true));
        double rainYearly = Double.parseDouble(new Utils().getJsonKey(input, "yearlyrainin", true));
        int strikesPerHour = Integer.parseInt(new Utils().getJsonKey(input, "lightning_hour", true));
        int lightningToday = Integer.parseInt(new Utils().getJsonKey(input, "lightning_day", true));

        // Record checking
        if (currentRainRate > Records.maxRainRateToday) {
            Records.maxRainRateToday = currentRainRate;
            Records.maxRainRateTime = System.currentTimeMillis() / 1000;
        }
        if (lightningToday > Records.maxLightningToday) {
            Records.maxLightningToday = lightningToday;
            Records.maxLightningTime = System.currentTimeMillis() / 1000;
        }
        if (rainDaily > Records.maxRainAmountToday) {
            Records.maxRainAmountToday = rainDaily;
            Records.maxRainAmountTime = System.currentTimeMillis() / 1000;
        }
        if (strikesPerHour > Records.highestLightningRateToday) {
            Records.highestLightningRateToday = strikesPerHour;
            Records.highestLightningRateTime = System.currentTimeMillis() / 1000;
        }
        if (temperature < Records.lowestTempToday) {
            Records.lowestTempToday = temperature;
            Records.lowestTempTime = System.currentTimeMillis() / 1000;
        }
        if (temperature > Records.highestTempToday) {
            Records.highestTempToday = temperature;
            Records.highestTempTime = System.currentTimeMillis() / 1000;
        }
        if (windMax > Records.maxWindToday) {
            Records.maxWindToday = windMax;
            Records.maxWindTime = System.currentTimeMillis() / 1000;
        }

        if (!rateLimited) {
            VoiceChannel humidityChannel = StormAlerts.jda.getVoiceChannelById(879099159574089809L);
            if (!humidityChannel.getName().equals("Humidity | " + humidity + "%"))
                humidityChannel.getManager().setName("Humidity | " + humidity + "%").queue();

            VoiceChannel temperatureChannel = StormAlerts.jda.getVoiceChannelById(879099218302746694L);
            if (!temperatureChannel.getName().equals("Temperature | " + temperature + "°"))
                temperatureChannel.getManager().setName("Temperature | " + temperature + "°").queue();

            VoiceChannel feelsLikeChannel = StormAlerts.jda.getVoiceChannelById(927585852396294164L);
            if (!feelsLikeChannel.getName().equals("Feels Like | " + feelsLike + "°"))
                feelsLikeChannel.getManager().setName("Feels Like | " + feelsLike + "°").queue();

            VoiceChannel uvChannel = StormAlerts.jda.getVoiceChannelById(879099369587081226L);
            if (!uvChannel.getName().equals("UV Index | " + uv))
                uvChannel.getManager().setName("UV Index | " + uv).queue();

            VoiceChannel windCurrentChannel = StormAlerts.jda.getVoiceChannelById(879097601750884423L);
            if (!windCurrentChannel.getName().equals("Current | " + wind + " mph"))
                windCurrentChannel.getManager().setName("Current | " + wind + " mph").queue();

            VoiceChannel windGustChannel = StormAlerts.jda.getVoiceChannelById(889226727266594876L);
            if (!windGustChannel.getName().equals("Gusts | " + windGust + " mph"))
                windGustChannel.getManager().setName("Gusts | " + windGust + " mph").queue();

            VoiceChannel windMaxChannel = StormAlerts.jda.getVoiceChannelById(879097671070121995L);
            if (!windMaxChannel.getName().equals("Max Today | " + windMax + " mph"))
                windMaxChannel.getManager().setName("Max Today | " + windMax + " mph").queue();

            VoiceChannel rainDailyChannel = StormAlerts.jda.getVoiceChannelById(879098793876934676L);
            if (!rainDailyChannel.getName().equals("Daily | " + rainDaily + " in"))
                rainDailyChannel.getManager().setName("Daily | " + rainDaily + " in").queue();

            VoiceChannel rainWeeklyChannel = StormAlerts.jda.getVoiceChannelById(879098898193449000L);
            if (!rainWeeklyChannel.getName().equals("Weekly | " + rainWeekly + " in"))
                rainWeeklyChannel.getManager().setName("Weekly | " + rainWeekly + " in").queue();

            VoiceChannel rainMonthlyChannel = StormAlerts.jda.getVoiceChannelById(879098953470205972L);
            if (!rainMonthlyChannel.getName().equals("Monthly | " + rainMonthly + " in"))
                rainMonthlyChannel.getManager().setName("Monthly | " + rainMonthly + " in").queue();

            VoiceChannel rainYearlyChannel = StormAlerts.jda.getVoiceChannelById(879099010420457482L);
            if (!rainYearlyChannel.getName().equals("Yearly | " + rainYearly + " in"))
                rainYearlyChannel.getManager().setName("Yearly | " + rainYearly + " in").queue();

            VoiceChannel strikesPerHourChannel = StormAlerts.jda.getVoiceChannelById(923433184132210698L);
            if (!strikesPerHourChannel.getName().equals("Strikes | " + strikesPerHour + "/hr"))
                strikesPerHourChannel.getManager().setName("Strikes | " + strikesPerHour + "/hr").queue();

            VoiceChannel lightningTodayChannel = StormAlerts.jda.getVoiceChannelById(923432597789503568L);
            if (!lightningTodayChannel.getName().equals("Nearby Today | " + lightningToday))
                lightningTodayChannel.getManager().setName("Nearby Today | " + lightningToday).queue();

            DateTimeFormatter timeUpdatedFormat = DateTimeFormatter.ofPattern("h:mm a");
            Instant timeUpdatedRaw = Instant.ofEpochMilli(
                Long.parseLong(new Utils().getJsonKey(input, "dateutc", true)));
            String timeUpdated = timeUpdatedFormat.format(
                ZonedDateTime.ofInstant(timeUpdatedRaw, ZoneId.of("America/Denver")));

            VoiceChannel timeUpdatedChannel = StormAlerts.jda.getVoiceChannelById(941791190704062545L);
            if (!timeUpdatedChannel.getName().equals("Stats Updated: " + timeUpdated))
                timeUpdatedChannel.getManager().setName("Stats Updated: " + timeUpdated).queue();

            rateLimited = true;
            new Thread(() -> {
                try (ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor()) {
                    executor.schedule(() -> {
                        rateLimited = false;
                    }, 6, TimeUnit.MINUTES);
                }
            }, "Rate Limit").start();
        }


        // Wind alerts
        if (windMax >= 20 && lastAlertedWindGust < windMax) {
            TextChannel windChannel = StormAlerts.jda.getTextChannelById(1028358818050080768L);

            String ping = "";
            if (new Utils().shouldIPing(windChannel)) ping = "<@&1046148944108978227>\n";

            String message = ping + "🍃 Wind gust of **" + windMax + " mph** detected!";
            if (windMax >= 50) message += " <a:weewoo:1083615022455992382>";

            windChannel.sendMessage(message)
                .setSuppressedNotifications(new Utils().shouldIBeSilent(windChannel)).queue();
            lastAlertedWindGust = windMax;
        }


        // Lightning alerts
        String lightningTime = new Utils().getJsonKey(input, "lightning_time", true);
        if (lightningTime.equals("null")) return;

        File file = new File("StormAlerts/lastlightningid.txt");
        Scanner fileScanner = new Scanner(file);

        Long previousLightningTime = fileScanner.nextLong();
        long lightningTimeLong = Long.parseLong(lightningTime);

        if (previousLightningTime.equals(lightningTimeLong)) return;
        if (lightningTimeLong < System.currentTimeMillis() - 600000) return;

        if (lightningToday > 1) {
            int lightningDistance = Math.toIntExact(
                Math.round(Double.parseDouble(new Utils().getJsonKey(input, "lightning_distance", true))));
            String s = "s";
            if (lightningDistance == 1) s = "";

            TextChannel lightningChannel = StormAlerts.jda.getTextChannelById(899876734999089192L);

            String ping = "";
            if (new Utils().shouldIPing(lightningChannel)) ping = "<@&896877424824954881>\n";

            String message = ping + "🌩️ Lightning detected **~" + lightningDistance + " mile" + s + "** from Eastern Farmington <t:" + (lightningTimeLong / 1000) + ":R>!";
            if (lightningDistance <= 2) message += " <a:weewoo:1083615022455992382>";

            // Always send silent if lightning is more than 15 miles away
            if (lightningDistance > 15)
                lightningChannel.sendMessage(message).setSuppressedNotifications(true).queue();
            else lightningChannel.sendMessage(message)
                .setSuppressedNotifications(new Utils().shouldIBeSilent(lightningChannel)).queue();
        }

        FileWriter fw = new FileWriter(file, false);
        fw.write(lightningTime);
        fw.close();
    }
}
