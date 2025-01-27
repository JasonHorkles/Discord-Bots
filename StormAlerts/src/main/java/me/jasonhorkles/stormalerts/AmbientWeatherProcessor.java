package me.jasonhorkles.stormalerts;

import me.jasonhorkles.stormalerts.Utils.ChannelUtils;
import me.jasonhorkles.stormalerts.Utils.LogUtils;
import me.jasonhorkles.stormalerts.Utils.MessageUtils;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AmbientWeatherProcessor {
    public static double lastAlertedWindGust = -1;

    private static boolean notRateLimited = true;

    public void processWeatherData(String data) throws IOException {
        System.out.println(new LogUtils().getTime(LogUtils.LogColor.YELLOW) + "Processing PWS conditions...");

        JSONObject input;
        if (StormAlerts.testing) input = new JSONObject(Files.readString(Path.of(
            "StormAlerts/Tests/pwsweather.json")));
        else input = new JSONObject(data);

        // Set the values
        int humidity = input.getInt("humidity");
        double temperature = input.getDouble("tempf");
        double feelsLike = input.getDouble("feelsLike");
        int uv = input.getInt("uv");
        int wind = Math.toIntExact(Math.round(input.getDouble("windspeedmph")));
        int windGust = Math.toIntExact(Math.round(input.getDouble("windgustmph")));
        int windMax = Math.toIntExact(Math.round(input.getDouble("maxdailygust")));
        int windMaxFps = Math.toIntExact(Math.round(windMax * 1.46667));
        double windDir = input.getDouble("winddir");
        double currentRainRate = input.getDouble("hourlyrainin");
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

        if (notRateLimited && !StormAlerts.testing) {
            ChannelUtils channelUtils = new ChannelUtils();
            channelUtils.updateVoiceChannel(879099218302746694L, "Temperature | " + temperature + "°");
            channelUtils.updateVoiceChannel(927585852396294164L, "Feels Like | " + feelsLike + "°");
            channelUtils.updateVoiceChannel(879099369587081226L, "UV Index | " + uv);
            channelUtils.updateVoiceChannel(879099159574089809L, "Humidity | " + humidity + "%");
            channelUtils.updateVoiceChannel(879098793876934676L, "Daily | " + rainDaily + " in");
            channelUtils.updateVoiceChannel(879098898193449000L, "Weekly | " + rainWeekly + " in");
            channelUtils.updateVoiceChannel(879098953470205972L, "Monthly | " + rainMonthly + " in");
            channelUtils.updateVoiceChannel(879099010420457482L, "Yearly | " + rainYearly + " in");
            channelUtils.updateVoiceChannel(879097601750884423L, "Current | " + wind + " mph");
            channelUtils.updateVoiceChannel(889226727266594876L, "Gusts | " + windGust + " mph");
            channelUtils.updateVoiceChannel(879097671070121995L, "Max Today | " + windMax + " mph");
            channelUtils.updateVoiceChannel(923433184132210698L, "Strikes | " + strikesPerHour + "/hr");
            channelUtils.updateVoiceChannel(923432597789503568L, "Nearby Today | " + lightningToday);

            DateTimeFormatter timeUpdatedFormat = DateTimeFormatter.ofPattern("h:mm a", Locale.US);
            Instant timeUpdatedRaw = Instant.ofEpochMilli(input.getLong("dateutc"));
            String timeUpdated = timeUpdatedFormat.format(ZonedDateTime.ofInstant(
                timeUpdatedRaw,
                ZoneId.of("America/Denver")));
            channelUtils.updateVoiceChannel(941791190704062545L, "Stats Updated: " + timeUpdated);

            notRateLimited = false;
            Executors.newSingleThreadScheduledExecutor().schedule(
                () -> {
                    notRateLimited = true;
                }, 6, TimeUnit.MINUTES);
        }

        MessageUtils messageUtils = new MessageUtils();

        // Wind alerts
        if (windMax >= 20 && lastAlertedWindGust < windMax) {
            boolean pingOverride = windMax >= (lastAlertedWindGust + 5);

            // Convert wind direction to 16-point cardinal direction
            int index = (int) ((windDir + 22.5) / 45) % 8;
            String[] cardinal = {"North", "Northeast", "East", "Southeast", "South", "Southwest", "West", "Northwest"};
            String[] directions = {"⬇️", "↙️", "⬅️", "↖️", "⬆️", "↗️", "➡️", "↘️"};
            String windDirStr = cardinal[index];
            String windDirEmote = directions[index];

            String ping = "";
            if (messageUtils.shouldIPing(ChannelUtils.windChannel) || pingOverride)
                ping = "<@&1046148944108978227>\n";

            String message = ping + "🍃 " + windDirStr + "ern wind gust of **" + windMax + " mph** *(" + windMaxFps + " ft/s)* detected! " + windDirEmote;
            if (windMax >= 50) message += " <a:weewoo:1083615022455992382>";

            ChannelUtils.windChannel.sendMessage(message)
                .setSuppressedNotifications(messageUtils.shouldIBeSilent(ChannelUtils.windChannel) && !pingOverride)
                .queue();
            lastAlertedWindGust = windMax;
        }

        // Lightning alerts
        long lightningTime = input.getLong("lightning_time");
        long prevLightningTime = Long.parseLong(Files.readString(Path.of("StormAlerts/lastlightningid.txt")));

        // Send lightning alert if it's not the same as the last one,
        // is less than 10 minutes old,
        // and there have been more than 1 lightning strikes today
        if (prevLightningTime != lightningTime && lightningTime > System.currentTimeMillis() - 600000 && lightningToday > 1) {
            int lightningDistance = Math.toIntExact(Math.round(input.getDouble("lightning_distance")));
            String s = "s";
            if (lightningDistance == 1) s = "";

            String ping = "";
            if (messageUtils.shouldIPing(ChannelUtils.lightningChannel)) ping = "<@&896877424824954881>\n";

            String message = ping + "🌩️ Lightning detected **~" + lightningDistance + " mile" + s + "** from Eastern Farmington <t:" + (lightningTime / 1000) + ":R>!";
            if (lightningDistance <= 2) message += " <a:weewoo:1083615022455992382>";

            // Always send silent if lightning is more than 15 miles away
            if (lightningDistance > 15)
                ChannelUtils.lightningChannel.sendMessage(message).setSuppressedNotifications(true).queue();
            else ChannelUtils.lightningChannel.sendMessage(message)
                .setSuppressedNotifications(messageUtils.shouldIBeSilent(ChannelUtils.lightningChannel))
                .queue();

            FileWriter fw = new FileWriter("StormAlerts/lastlightningid.txt", StandardCharsets.UTF_8, false);
            fw.write(String.valueOf(lightningTime));
            fw.close();
        }


        // Check the weather conditions
        try {
            new Weather().checkConditions(currentRainRate, temperature);
        } catch (Exception e) {
            LogUtils logUtils = new LogUtils();

            System.out.println(logUtils.getTime(LogUtils.LogColor.RED) + "[ERROR] Couldn't get the weather conditions!");
            e.printStackTrace();
            StormAlerts.jda.getPresence().setStatus(OnlineStatus.DO_NOT_DISTURB);
            StormAlerts.jda.getPresence().setActivity(Activity.playing("Error checking weather!"));
            logUtils.logError(e);
        }
    }
}
