package me.jasonhorkles.stormalerts;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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
            input = new JSONArray(new String(url.readAllBytes(), StandardCharsets.UTF_8)).getJSONObject(0)
                .getJSONObject("lastData");
            url.close();

        } else input = new JSONArray(
            Files.readString(Path.of("StormAlerts/Tests/pwsweather.json"))).getJSONObject(0)
            .getJSONObject("lastData");

        currentRainRate = input.getDouble("hourlyrainin");
        temperature = input.getDouble("tempf");
        wm2 = input.getDouble("solarradiation");

        // Set the values
        int humidity = input.getInt("humidity");
        double feelsLike = input.getDouble("feelsLike");
        int uv = input.getInt("uv");
        int wind = Math.toIntExact(Math.round(input.getDouble("windspeedmph")));
        int windGust = Math.toIntExact(Math.round(input.getDouble("windgustmph")));
        int windMax = Math.toIntExact(Math.round(input.getDouble("maxdailygust")));
        double rainDaily = input.getDouble("dailyrainin");
        double rainWeekly = input.getDouble("weeklyrainin");
        double rainMonthly = input.getDouble("monthlyrainin");
        double rainYearly = input.getDouble("yearlyrainin");
        int strikesPerHour = input.getInt("lightning_hour");
        int lightningToday = input.getInt("lightning_day");

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
            Instant timeUpdatedRaw = Instant.ofEpochMilli(input.getLong("dateutc"));
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
            //todo fix alert on startup if sent today already
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
        long lightningTime = input.getLong("lightning_time");
        long previousLightningTime = Long.parseLong(
            Files.readString(Path.of("StormAlerts/lastlightningid.txt")));

        // Ignore lightning if it's the same as the last one
        if (previousLightningTime == lightningTime) return;
        // Ignore lightning if it's more than 10 minutes old
        if (lightningTime < System.currentTimeMillis() - 600000) return;

        if (lightningToday > 1) {
            int lightningDistance = Math.toIntExact(Math.round(input.getDouble("lightning_distance")));
            String s = "s";
            if (lightningDistance == 1) s = "";

            TextChannel lightningChannel = StormAlerts.jda.getTextChannelById(899876734999089192L);

            String ping = "";
            if (new Utils().shouldIPing(lightningChannel)) ping = "<@&896877424824954881>\n";

            String message = ping + "🌩️ Lightning detected **~" + lightningDistance + " mile" + s + "** from Eastern Farmington <t:" + (lightningTime / 1000) + ":R>!";
            if (lightningDistance <= 2) message += " <a:weewoo:1083615022455992382>";

            // Always send silent if lightning is more than 15 miles away
            if (lightningDistance > 15)
                lightningChannel.sendMessage(message).setSuppressedNotifications(true).queue();
            else lightningChannel.sendMessage(message)
                .setSuppressedNotifications(new Utils().shouldIBeSilent(lightningChannel)).queue();
        }

        FileWriter fw = new FileWriter("StormAlerts/lastlightningid.txt", false);
        fw.write(String.valueOf(lightningTime));
        fw.close();
    }
}
