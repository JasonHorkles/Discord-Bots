package me.jasonhorkles.stormalerts;

import net.dv8tion.jda.api.entities.TextChannel;
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
    public static String temperature = "N/A";
    private static boolean rateLimited = false;

    @SuppressWarnings("ConstantConditions")
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

        temperature = new Utils().checkIfNull(new Utils().getJsonKey(input, "tempf", true));

        Double rainRate;
        try {
            rainRate = Double.parseDouble(new Utils().getJsonKey(input, "hourlyrainin", true));
        } catch (NumberFormatException ignored) {
            rainRate = null;
        }
        long rainRateChannel = 879098584421789706L;

        if (rainRate != null) {
            currentRainRate = rainRate;
            if (!rateLimited) if (!StormAlerts.api.getVoiceChannelById(rainRateChannel).getName()
                .equals("Rate | " + rainRate + " in/hr"))
                StormAlerts.api.getVoiceChannelById(rainRateChannel).getManager()
                    .setName("Rate | " + rainRate + " in/hr").queue();
        } else if (!rateLimited)
            if (!StormAlerts.api.getVoiceChannelById(rainRateChannel).getName().equals("Rate | ERROR in/hr"))
                StormAlerts.api.getVoiceChannelById(rainRateChannel).getManager().setName("Rate | ERROR in/hr").queue();

        if (!rateLimited) {
            String humidity = new Utils().checkIfNull(new Utils().getJsonKey(input, "humidity", true));
            long humidityChannel = 879099159574089809L;
            if (!StormAlerts.api.getVoiceChannelById(humidityChannel).getName().equals("Humidity | " + humidity + "%"))
                StormAlerts.api.getVoiceChannelById(humidityChannel).getManager()
                    .setName("Humidity | " + humidity + "%").queue();

            long temperatureChannel = 879099218302746694L;
            if (!StormAlerts.api.getVoiceChannelById(temperatureChannel).getName()
                .equals("Temperature | " + temperature + "??"))
                StormAlerts.api.getVoiceChannelById(temperatureChannel).getManager()
                    .setName("Temperature | " + temperature + "??").queue();

            String feelsLike = new Utils().checkIfNull(new Utils().getJsonKey(input, "feelsLike", true));
            long feelsLikeChannel = 927585852396294164L;
            if (!StormAlerts.api.getVoiceChannelById(feelsLikeChannel).getName()
                .equals("Feels Like | " + feelsLike + "??"))
                StormAlerts.api.getVoiceChannelById(feelsLikeChannel).getManager()
                    .setName("Feels Like | " + feelsLike + "??").queue();

            String uv = new Utils().checkIfNull(new Utils().getJsonKey(input, "uv", true));
            long uvChannel = 879099369587081226L;
            if (!StormAlerts.api.getVoiceChannelById(uvChannel).getName().equals("UV Index | " + uv))
                StormAlerts.api.getVoiceChannelById(uvChannel).getManager().setName("UV Index | " + uv).queue();

            String wind = new Utils().checkIfNull(new Utils().getJsonKey(input, "windspeedmph", true));
            long windCurrentChannel = 879097601750884423L;
            if (!StormAlerts.api.getVoiceChannelById(windCurrentChannel).getName().equals("Current | " + wind + " mph"))
                StormAlerts.api.getVoiceChannelById(windCurrentChannel).getManager()
                    .setName("Current | " + wind + " mph").queue();

            String windGust = new Utils().checkIfNull(new Utils().getJsonKey(input, "windgustmph", true));
            long windGustChannel = 889226727266594876L;
            if (!StormAlerts.api.getVoiceChannelById(windGustChannel).getName().equals("Gusts | " + windGust + " mph"))
                StormAlerts.api.getVoiceChannelById(windGustChannel).getManager()
                    .setName("Gusts | " + windGust + " mph").queue();

            String windMax = new Utils().checkIfNull(new Utils().getJsonKey(input, "maxdailygust", true));
            long windMaxChannel = 879097671070121995L;
            if (!StormAlerts.api.getVoiceChannelById(windMaxChannel).getName()
                .equals("Max Today | " + windMax + " mph"))
                StormAlerts.api.getVoiceChannelById(windMaxChannel).getManager()
                    .setName("Max Today | " + windMax + " mph").queue();

            String rainDaily = new Utils().checkIfNull(new Utils().getJsonKey(input, "dailyrainin", true));
            long rainDailyChannel = 879098793876934676L;
            if (!StormAlerts.api.getVoiceChannelById(rainDailyChannel).getName().equals("Daily | " + rainDaily + " in"))
                StormAlerts.api.getVoiceChannelById(rainDailyChannel).getManager()
                    .setName("Daily | " + rainDaily + " in").queue();

            String rainWeekly = new Utils().checkIfNull(new Utils().getJsonKey(input, "weeklyrainin", true));
            long rainWeeklyChannel = 879098898193449000L;
            if (!StormAlerts.api.getVoiceChannelById(rainWeeklyChannel).getName()
                .equals("Weekly | " + rainWeekly + " in"))
                StormAlerts.api.getVoiceChannelById(rainWeeklyChannel).getManager()
                    .setName("Weekly | " + rainWeekly + " in").queue();

            String rainMonthly = new Utils().checkIfNull(new Utils().getJsonKey(input, "monthlyrainin", true));
            long rainMonthlyChannel = 879098953470205972L;
            if (!StormAlerts.api.getVoiceChannelById(rainMonthlyChannel).getName()
                .equals("Monthly | " + rainMonthly + " in"))
                StormAlerts.api.getVoiceChannelById(rainMonthlyChannel).getManager()
                    .setName("Monthly | " + rainMonthly + " in").queue();

            String rainYearly = new Utils().checkIfNull(new Utils().getJsonKey(input, "yearlyrainin", true));
            long rainYearlyChannel = 879099010420457482L;
            if (!StormAlerts.api.getVoiceChannelById(rainYearlyChannel).getName()
                .equals("Yearly | " + rainYearly + " in"))
                StormAlerts.api.getVoiceChannelById(rainYearlyChannel).getManager()
                    .setName("Yearly | " + rainYearly + " in").queue();

            String strikesPerHour = new Utils().checkIfNull(new Utils().getJsonKey(input, "lightning_hour", true));
            long strikesPerHourChannel = 923433184132210698L;
            if (!StormAlerts.api.getVoiceChannelById(strikesPerHourChannel).getName()
                .equals("Strikes | " + strikesPerHour + "/hr"))
                StormAlerts.api.getVoiceChannelById(strikesPerHourChannel).getManager()
                    .setName("Strikes | " + strikesPerHour + "/hr").queue();

            String lightningToday = new Utils().checkIfNull(new Utils().getJsonKey(input, "lightning_day", true));
            long lightningTodayChannel = 923432597789503568L;
            if (!StormAlerts.api.getVoiceChannelById(lightningTodayChannel).getName()
                .equals("Nearby Today | " + lightningToday))
                StormAlerts.api.getVoiceChannelById(lightningTodayChannel).getManager()
                    .setName("Nearby Today | " + lightningToday).queue();

            DateTimeFormatter timeUpdatedFormat = DateTimeFormatter.ofPattern("h:mm a");
            Instant timeUpdatedRaw = Instant.ofEpochMilli(
                Long.parseLong(new Utils().getJsonKey(input, "dateutc", true)));
            String timeUpdated = timeUpdatedFormat.format(
                ZonedDateTime.ofInstant(timeUpdatedRaw, ZoneId.of("America/Denver")));
            long timeUpdatedChannel = 941791190704062545L;
            if (!StormAlerts.api.getVoiceChannelById(timeUpdatedChannel).getName()
                .equals("Stats Updated: " + timeUpdated))
                StormAlerts.api.getVoiceChannelById(timeUpdatedChannel).getManager()
                    .setName("Stats Updated: " + timeUpdated).queue();

            rateLimited = true;
            Executors.newSingleThreadScheduledExecutor().schedule(() -> {
                rateLimited = false;
            }, 6, TimeUnit.MINUTES);
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

        String lightningDistance = new Utils().checkIfNull(new Utils().getJsonKey(input, "lightning_distance", true));
        TextChannel lightningChannel = StormAlerts.api.getTextChannelById(899876734999089192L);

        String ping = "";
        if (new Utils().shouldIPing(lightningChannel)) ping = "<@&896877424824954881>\n";

        lightningChannel.sendMessage(
                ping + ":cloud_lightning: Lightning detected **" + lightningDistance + " miles** from Eastern Farmington <t:" + (lightningTimeLong / 1000) + ":R>!")
            .queue();

        FileWriter fw = new FileWriter(file, false);
        fw.write(lightningTime);
        fw.close();
    }
}
