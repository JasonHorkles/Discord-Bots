package me.jasonhorkles.stormalerts;

import me.jasonhorkles.stormalerts.Utils.ChannelUtils;
import me.jasonhorkles.stormalerts.Utils.LogUtils;
import me.jasonhorkles.stormalerts.Utils.MessageUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Records {
    // Stats
    public static double highestTempToday;
    public static double lowestTempToday;
    public static double maxRainAmountToday;
    public static double maxRainRateToday;
    public static int highestLightningRateToday;
    public static int maxLightningToday;
    public static int maxWindToday;

    // Time of stats, in epoch seconds
    public static long highestLightningRateTime;
    public static long highestTempTime;
    public static long lowestTempTime;
    public static long maxLightningTime;
    public static long maxRainAmountTime;
    public static long maxRainRateTime;
    public static long maxWindTime;

    public void scheduleRecordCheck() throws IOException {
        LogUtils logUtils = new LogUtils();
        // Populate the variables
        JSONObject recordsToday = new JSONObject(Files.readString(Path.of("StormAlerts/records-today.json")));

        if (recordsToday.isEmpty()) {
            System.out.println(logUtils.getTime(LogUtils.LogColor.YELLOW) + "No records today found! Populating...");
            resetValues();
        } else {
            // Stats
            highestLightningRateToday = recordsToday.getInt("highLightningRate");
            highestTempToday = recordsToday.getDouble("highTemp");
            lowestTempToday = recordsToday.getDouble("lowTemp");
            maxLightningToday = recordsToday.getInt("maxLightning");
            maxRainAmountToday = recordsToday.getDouble("maxRainAmount");
            maxRainRateToday = recordsToday.getDouble("maxRainRate");
            maxWindToday = recordsToday.getInt("maxWind");

            // Times
            highestLightningRateTime = recordsToday.getLong("highLightningRateTime");
            highestTempTime = recordsToday.getLong("highTempTime");
            lowestTempTime = recordsToday.getLong("lowTempTime");
            maxLightningTime = recordsToday.getLong("maxLightningTime");
            maxRainAmountTime = recordsToday.getLong("maxRainAmountTime");
            maxRainRateTime = recordsToday.getLong("maxRainRateTime");
            maxWindTime = recordsToday.getLong("maxWindTime");

        }

        LocalDateTime future = LocalDateTime.now().withHour(23).withMinute(59).withSecond(0);
        long delay = Duration.between(LocalDateTime.now(), future).getSeconds();
        if (delay < 0) return;

        StormAlerts.scheduledTimers.add(Executors.newSingleThreadScheduledExecutor()
            .schedule(this::checkRecords, delay, TimeUnit.SECONDS));

        System.out.println(logUtils.getTime(LogUtils.LogColor.GREEN) + "Scheduled record check in " + delay / 3600 + " hours.");
    }

    private void checkRecords() {
        LogUtils logUtils = new LogUtils();
        System.out.println(logUtils.getTime(LogUtils.LogColor.YELLOW) + "Checking records...");

        String totalFilePath = "StormAlerts/records.json";
        try {
            JSONObject records = new JSONObject(Files.readString(Path.of(totalFilePath)));

            // Find new records
            if (highestLightningRateToday > records.getInt("highLightningRate")) {
                sendRecordMessage(
                    "High lightning rate",
                    highestLightningRateToday + " strikes per hour",
                    highestLightningRateTime,
                    records.getInt("highLightningRate") + " strikes per hour",
                    records.getLong("highLightningRateTime"));
                records.put("highLightningRate", highestLightningRateToday);
                records.put("highLightningRateTime", highestLightningRateTime);
            }

            if (highestTempToday > records.getDouble("highTemp")) {
                sendRecordMessage(
                    "High temperature",
                    highestTempToday + "°",
                    highestTempTime,
                    records.getDouble("highTemp") + "°",
                    records.getLong("highTempTime"));
                records.put("highTemp", highestTempToday);
                records.put("highTempTime", highestTempTime);
            }

            if (lowestTempToday < records.getDouble("lowTemp")) {
                sendRecordMessage(
                    "Low temperature",
                    lowestTempToday + "°",
                    lowestTempTime,
                    records.getDouble("lowTemp") + "°",
                    records.getLong("lowTempTime"));
                records.put("lowTemp", lowestTempToday);
                records.put("lowTempTime", lowestTempTime);
            }

            if (maxLightningToday > records.getInt("maxLightning")) {
                sendRecordMessage(
                    "Daily lightning strikes",
                    String.valueOf(maxLightningToday),
                    maxLightningTime,
                    records.getInt("maxLightning") + " strikes",
                    records.getLong("maxLightningTime"));
                records.put("maxLightning", maxLightningToday);
                records.put("maxLightningTime", maxLightningTime);
            }

            if (maxRainAmountToday > records.getDouble("maxRainAmount")) {
                sendRecordMessage(
                    "Daily rain amount",
                    maxRainAmountToday + " inches",
                    maxRainAmountTime,
                    records.getDouble("maxRainAmount") + " inches",
                    records.getLong("maxRainAmountTime"));
                records.put("maxRainAmount", maxRainAmountToday);
                records.put("maxRainAmountTime", maxRainAmountTime);
            }

            if (maxRainRateToday > records.getDouble("maxRainRate")) {
                sendRecordMessage(
                    "High rain rate",
                    maxRainRateToday + " inches per hour",
                    maxRainRateTime,
                    records.getDouble("maxRainRate") + " inches per hour",
                    records.getLong("maxRainRateTime"));
                records.put("maxRainRate", maxRainRateToday);
                records.put("maxRainRateTime", maxRainRateTime);
            }

            if (maxWindToday > records.getInt("maxWind")) {
                sendRecordMessage(
                    "High winds",
                    maxWindToday + " mph",
                    maxWindTime,
                    records.getInt("maxWind") + " mph",
                    records.getLong("maxWindTime"));
                records.put("maxWind", maxWindToday);
                records.put("maxWindTime", maxWindTime);
            }

            // Save the records file
            FileWriter recordsFile = new FileWriter(totalFilePath, StandardCharsets.UTF_8, false);
            recordsFile.write(records.toString());
            recordsFile.close();

            System.out.println(logUtils.getTime(LogUtils.LogColor.YELLOW) + "Updating record channels...");

            ChannelUtils channelUtils = new ChannelUtils();
            channelUtils.updateVoiceChannel(
                1059213663506006066L,
                "Temp Low | " + records.getDouble("lowTemp") + "°");
            channelUtils.updateVoiceChannel(
                1059213632531091548L,
                "Temp High | " + records.getDouble("highTemp") + "°");
            channelUtils.updateVoiceChannel(
                1059213828015013948L,
                "Rain | " + records.getDouble("maxRainRate") + " in/hr");
            channelUtils.updateVoiceChannel(
                1059213790333382796L,
                "Daily Rain | " + records.getDouble("maxRainAmount") + " in");
            channelUtils.updateVoiceChannel(
                1059213855164747796L,
                "Wind | " + records.getInt("maxWind") + " mph");
            channelUtils.updateVoiceChannel(
                1059213581675155507L,
                "Lightning | " + records.getInt("highLightningRate") + "/hr");
            channelUtils.updateVoiceChannel(
                1059213753494798396L,
                "Daily Lightning | " + records.getInt("maxLightning"));

        } catch (Exception e) {
            System.out.print(logUtils.getTime(LogUtils.LogColor.RED));
            e.printStackTrace();
            logUtils.logError(e);
        }

        // Reset the values for new day
        resetValues();
    }

    @NotNull
    public FileWriter saveRecords() throws IOException {
        String filePath = "StormAlerts/records-today.json";

        JSONObject allRecords = new JSONObject();
        allRecords.put("highLightningRate", highestLightningRateToday);
        allRecords.put("highTemp", highestTempToday);
        allRecords.put("lowTemp", lowestTempToday);
        allRecords.put("maxLightning", maxLightningToday);
        allRecords.put("maxRainAmount", maxRainAmountToday);
        allRecords.put("maxRainRate", maxRainRateToday);
        allRecords.put("maxWind", maxWindToday);

        allRecords.put("highLightningRateTime", highestLightningRateTime);
        allRecords.put("highTempTime", highestTempTime);
        allRecords.put("lowTempTime", lowestTempTime);
        allRecords.put("maxLightningTime", maxLightningTime);
        allRecords.put("maxRainAmountTime", maxRainAmountTime);
        allRecords.put("maxRainRateTime", maxRainRateTime);
        allRecords.put("maxWindTime", maxWindTime);

        FileWriter recordsToday = new FileWriter(filePath, StandardCharsets.UTF_8, false);
        recordsToday.write(allRecords.toString());
        return recordsToday;
    }

    private void sendRecordMessage(String recordType, String newRecord, long newTimeStamp, String oldRecord, long oldTimeStamp) {
        String ping = "";
        if (new MessageUtils().shouldIPing(ChannelUtils.recordsChannel)) ping = "<@&1046149064519073813>\n";

        // 📊
        ChannelUtils.recordsChannel
            .sendMessage(ping + "\uD83D\uDCCA New record! **" + recordType + "** of **" + newRecord + "** reported on <t:" + newTimeStamp + ":F>, beating the old record of " + oldRecord + " from <t:" + oldTimeStamp + ":F>")
            .complete();
    }

    private void resetValues() {
        highestLightningRateToday = -1;
        highestTempToday = -1;
        lowestTempToday = Integer.MAX_VALUE;
        maxLightningToday = -1;
        maxRainAmountToday = -1;
        maxRainRateToday = -1;
        maxWindToday = -1;

        highestLightningRateTime = 0;
        highestTempTime = 0;
        lowestTempTime = 0;
        maxLightningTime = 0;
        maxRainAmountTime = 0;
        maxRainRateTime = 0;
        maxWindTime = 0;

        System.out.println(new LogUtils().getTime(LogUtils.LogColor.GREEN) + "Record values reset!");
    }
}
