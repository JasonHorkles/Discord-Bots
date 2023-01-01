package me.jasonhorkles.stormalerts;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
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
import java.util.concurrent.TimeUnit;

public class Pws {
    public static double currentRainRate = 0.00;
    public static double temperature = -1;

    private static boolean rateLimited = false;
    private static double lastAlertedWindGust = -1;

    @SuppressWarnings("DataFlowIssue")
    public void checkConditions() throws IOException {
        System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Checking PWS conditions...");

        JSONObject input;

        if (!StormAlerts.testing) {
            String apiUrl = "https://api.ambientweather.net/v1/devices/?apiKey=" + new Secrets().getAwApiKey() + "&applicationKey=" + new Secrets().getAwAppKey();
            InputStream stream = new URL(apiUrl).openStream();
            String out = new Scanner(stream, StandardCharsets.UTF_8).useDelimiter("\\A").nextLine();
            stream.close();

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
        if (currentRainRate > Records.maxRainRateToday) Records.maxRainRateToday = currentRainRate;
        if (lightningToday > Records.maxLightningToday) Records.maxLightningToday = lightningToday;
        if (rainDaily > Records.maxRainAmountToday) Records.maxRainAmountToday = rainDaily;
        if (strikesPerHour > Records.highestLightningRateToday)
            Records.highestLightningRateToday = strikesPerHour;
        if (temperature < Records.lowestTempToday) Records.lowestTempToday = temperature;
        if (temperature > Records.highestTempToday) Records.highestTempToday = temperature;
        if (uv > Records.highestUvToday) Records.highestUvToday = uv;
        if (windMax > Records.maxWindToday) Records.maxWindToday = windMax;

        if (!rateLimited) {
            long humidityChannel = 879099159574089809L;
            if (!StormAlerts.jda.getVoiceChannelById(humidityChannel).getName()
                .equals("Humidity | " + humidity + "%"))
                StormAlerts.jda.getVoiceChannelById(humidityChannel).getManager()
                    .setName("Humidity | " + humidity + "%").queue();

            long temperatureChannel = 879099218302746694L;
            if (!StormAlerts.jda.getVoiceChannelById(temperatureChannel).getName()
                .equals("Temperature | " + temperature + "°"))
                StormAlerts.jda.getVoiceChannelById(temperatureChannel).getManager()
                    .setName("Temperature | " + temperature + "°").queue();

            long feelsLikeChannel = 927585852396294164L;
            if (!StormAlerts.jda.getVoiceChannelById(feelsLikeChannel).getName()
                .equals("Feels Like | " + feelsLike + "°"))
                StormAlerts.jda.getVoiceChannelById(feelsLikeChannel).getManager()
                    .setName("Feels Like | " + feelsLike + "°").queue();

            long uvChannel = 879099369587081226L;
            if (!StormAlerts.jda.getVoiceChannelById(uvChannel).getName().equals("UV Index | " + uv))
                StormAlerts.jda.getVoiceChannelById(uvChannel).getManager().setName("UV Index | " + uv)
                    .queue();

            long windCurrentChannel = 879097601750884423L;
            if (!StormAlerts.jda.getVoiceChannelById(windCurrentChannel).getName()
                .equals("Current | " + wind + " mph"))
                StormAlerts.jda.getVoiceChannelById(windCurrentChannel).getManager()
                    .setName("Current | " + wind + " mph").queue();

            long windGustChannel = 889226727266594876L;
            if (!StormAlerts.jda.getVoiceChannelById(windGustChannel).getName()
                .equals("Gusts | " + windGust + " mph"))
                StormAlerts.jda.getVoiceChannelById(windGustChannel).getManager()
                    .setName("Gusts | " + windGust + " mph").queue();

            long windMaxChannel = 879097671070121995L;
            if (!StormAlerts.jda.getVoiceChannelById(windMaxChannel).getName()
                .equals("Max Today | " + windMax + " mph"))
                StormAlerts.jda.getVoiceChannelById(windMaxChannel).getManager()
                    .setName("Max Today | " + windMax + " mph").queue();

            long rainDailyChannel = 879098793876934676L;
            if (!StormAlerts.jda.getVoiceChannelById(rainDailyChannel).getName()
                .equals("Daily | " + rainDaily + " in"))
                StormAlerts.jda.getVoiceChannelById(rainDailyChannel).getManager()
                    .setName("Daily | " + rainDaily + " in").queue();

            long rainWeeklyChannel = 879098898193449000L;
            if (!StormAlerts.jda.getVoiceChannelById(rainWeeklyChannel).getName()
                .equals("Weekly | " + rainWeekly + " in"))
                StormAlerts.jda.getVoiceChannelById(rainWeeklyChannel).getManager()
                    .setName("Weekly | " + rainWeekly + " in").queue();

            long rainMonthlyChannel = 879098953470205972L;
            if (!StormAlerts.jda.getVoiceChannelById(rainMonthlyChannel).getName()
                .equals("Monthly | " + rainMonthly + " in"))
                StormAlerts.jda.getVoiceChannelById(rainMonthlyChannel).getManager()
                    .setName("Monthly | " + rainMonthly + " in").queue();

            long rainYearlyChannel = 879099010420457482L;
            if (!StormAlerts.jda.getVoiceChannelById(rainYearlyChannel).getName()
                .equals("Yearly | " + rainYearly + " in"))
                StormAlerts.jda.getVoiceChannelById(rainYearlyChannel).getManager()
                    .setName("Yearly | " + rainYearly + " in").queue();

            long strikesPerHourChannel = 923433184132210698L;
            if (!StormAlerts.jda.getVoiceChannelById(strikesPerHourChannel).getName()
                .equals("Strikes | " + strikesPerHour + "/hr"))
                StormAlerts.jda.getVoiceChannelById(strikesPerHourChannel).getManager()
                    .setName("Strikes | " + strikesPerHour + "/hr").queue();

            long lightningTodayChannel = 923432597789503568L;
            if (!StormAlerts.jda.getVoiceChannelById(lightningTodayChannel).getName()
                .equals("Nearby Today | " + lightningToday))
                StormAlerts.jda.getVoiceChannelById(lightningTodayChannel).getManager()
                    .setName("Nearby Today | " + lightningToday).queue();

            DateTimeFormatter timeUpdatedFormat = DateTimeFormatter.ofPattern("h:mm a");
            Instant timeUpdatedRaw = Instant.ofEpochMilli(
                Long.parseLong(new Utils().getJsonKey(input, "dateutc", true)));
            String timeUpdated = timeUpdatedFormat.format(
                ZonedDateTime.ofInstant(timeUpdatedRaw, ZoneId.of("America/Denver")));
            long timeUpdatedChannel = 941791190704062545L;
            if (!StormAlerts.jda.getVoiceChannelById(timeUpdatedChannel).getName()
                .equals("Stats Updated: " + timeUpdated))
                StormAlerts.jda.getVoiceChannelById(timeUpdatedChannel).getManager()
                    .setName("Stats Updated: " + timeUpdated).queue();

            rateLimited = true;
            Executors.newSingleThreadScheduledExecutor().schedule(() -> {
                rateLimited = false;
            }, 6, TimeUnit.MINUTES);
        }


        // Wind alerts
        if (windGust >= 30 && lastAlertedWindGust < windGust) {
            TextChannel windChannel = StormAlerts.jda.getTextChannelById(1028358818050080768L);

            String ping = "";
            if (new Utils().shouldIPing(windChannel)) ping = "<@&1046148944108978227>\n";

            windChannel.sendMessage(ping + "🍃 Wind gust of **" + windGust + " mph** detected!").queue();
            lastAlertedWindGust = windGust;
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

        int lightningDistance = Math.toIntExact(
            Math.round(Double.parseDouble(new Utils().getJsonKey(input, "lightning_distance", true))));
        String s = "s";
        if (lightningDistance == 1) s = "";

        TextChannel lightningChannel = StormAlerts.jda.getTextChannelById(899876734999089192L);

        String ping = "";
        if (new Utils().shouldIPing(lightningChannel)) ping = "<@&896877424824954881>\n";

        lightningChannel.sendMessage(
                ping + "🌩️ Lightning detected **~" + lightningDistance + " mile" + s + "** from Eastern Farmington <t:" + (lightningTimeLong / 1000) + ":R>!")
            .queue();

        FileWriter fw = new FileWriter(file, false);
        fw.write(lightningTime);
        fw.close();
    }
}
