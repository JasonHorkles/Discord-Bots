package me.jasonhorkles.stormalerts;

import me.jasonhorkles.stormalerts.Utils.ChannelUtils;
import me.jasonhorkles.stormalerts.Utils.LogUtils;
import me.jasonhorkles.stormalerts.Utils.MessageUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.text.ParseException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.*;

public class StormAlerts extends ListenerAdapter {
    public static final List<ScheduledFuture<?>> scheduledTimers = new ArrayList<>();
    public static final boolean testing = false;
    public static JDA jda;

    private static AmbientWeatherSocket ambientWeatherSocket;

    static void main() throws InterruptedException, ParseException {
        LogUtils logUtils = new LogUtils();
        System.out.println(logUtils.getTime(LogUtils.LogColor.YELLOW) + "Starting...");

        JDABuilder builder = JDABuilder.createLight(new Secrets().botToken());
        builder.enableIntents(
            GatewayIntent.GUILD_MESSAGES,
            GatewayIntent.MESSAGE_CONTENT,
            GatewayIntent.GUILD_MEMBERS);
        builder.setMemberCachePolicy(MemberCachePolicy.ALL);
        builder.setBulkDeleteSplittingEnabled(false);
        builder.setStatus(OnlineStatus.DO_NOT_DISTURB);
        builder.setActivity(Activity.customStatus("Loading..."));
        builder.setEnableShutdownHook(false);
        builder.addEventListeners(new Events());
        jda = builder.build();

        jda.awaitReady();

        if (testing)
            System.out.println(logUtils.getTime(LogUtils.LogColor.RED) + "Warning: Testing mode enabled! Local files will be used.");

        new ChannelUtils().cacheChannels(jda);

        // Cache wind speed
        try {
            Message windMessage = new MessageUtils().getMessages(ChannelUtils.windChannel, 1).get(
                30,
                TimeUnit.SECONDS).getFirst();
            if (windMessage != null) {
                OffsetDateTime fiveHoursAgo = OffsetDateTime.now().minusHours(5);
                OffsetDateTime midnight = OffsetDateTime.now().withHour(0).withMinute(0).withSecond(0)
                    .withNano(0);

                if (windMessage.getTimeCreated().isAfter(fiveHoursAgo) || windMessage.getTimeCreated()
                    .isAfter(midnight)) AmbientWeatherProcessor.lastAlertedWindGust = Integer.parseInt(
                    windMessage.getContentStripped().replace("\n", " ").replaceFirst(".*of ", "")
                        .replaceFirst(" mph.*", ""));
            }
        } catch (ExecutionException | TimeoutException e) {
            System.out.print(logUtils.getTime(LogUtils.LogColor.RED));
            e.printStackTrace();
            logUtils.logError(e);
        }

        // Cache current records
        try {
            new Records().cacheRecordData();
        } catch (Exception e) {
            System.out.println(logUtils.getTime(LogUtils.LogColor.RED) + "Error grabbing the records!");
            e.printStackTrace();
            logUtils.logError(e);
        }

        // Schedule records announcement & wind var reset
        LocalDateTime future = LocalDateTime.now().withHour(23).withMinute(59).withSecond(0);
        long delay = Duration.between(LocalDateTime.now(), future).getSeconds();
        if (delay < 0) return;

        scheduledTimers.add(Executors.newSingleThreadScheduledExecutor().schedule(
            () -> {
                // Check for new records
                new Records().checkRecords();

                // Reset max wind gust 1 minute later
                scheduledTimers.add(Executors.newSingleThreadScheduledExecutor()
                    .schedule(
                        () -> {AmbientWeatherProcessor.lastAlertedWindGust = -1;},
                        1,
                        TimeUnit.MINUTES));
            }, delay, TimeUnit.SECONDS));

        System.out.println(logUtils.getTime(LogUtils.LogColor.GREEN) + "Scheduled record check & wind reset in " + delay / 3600 + " hours.");

        // Start listening for PWS messages
        ambientWeatherSocket = new AmbientWeatherSocket();
        ambientWeatherSocket.connect();

        // 1 min
        // Alert checks
        scheduledTimers.add(Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
            () -> {
                try {
                    new Alerts().checkAlerts();
                } catch (Exception e) {
                    String reason = "";
                    if (e.getMessage().contains("500")) reason = " (Internal Server Error)";
                    else if (e.getMessage().contains("502")) reason = " (Bad Gateway)";
                    else if (e.getMessage().contains("503")) reason = " (Service Unavailable)";
                    else if (e.getMessage().contains("504")) reason = " (Gateway Timeout)";

                    System.out.println(logUtils.getTime(LogUtils.LogColor.RED) + "[ERROR] Couldn't get the alerts!" + reason);
                    if (reason.isBlank()) {
                        System.out.print(logUtils.getTime(LogUtils.LogColor.RED));
                        e.printStackTrace();
                        logUtils.logError(e);
                    }
                }
            }, 1, 60, TimeUnit.SECONDS));

        // 6 mins
        // Visibility check
        scheduledTimers.add(Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
            () -> {
                try {
                    new Visibility().checkConditions();
                } catch (Exception e) {
                    String reason = "";
                    if (e.getMessage().contains("500")) reason = " (Internal Server Error)";
                    else if (e.getMessage().contains("502")) reason = " (Bad Gateway)";
                    else if (e.getMessage().contains("503")) reason = " (Service Unavailable)";
                    else if (e.getMessage().contains("504")) reason = " (Gateway Timeout)";

                    System.out.println(logUtils.getTime(LogUtils.LogColor.RED) + "[ERROR] Couldn't get the visibility!" + reason);
                    if (reason.isBlank()) {
                        System.out.print(logUtils.getTime(LogUtils.LogColor.RED));
                        e.printStackTrace();
                        logUtils.logError(e);
                    }
                    new ChannelUtils().updateVoiceChannel(899872710233051178L, "Visibility | ERROR");
                }
            }, 2, 360, TimeUnit.SECONDS));

        // Schedule traffic checks
        Traffic traffic = new Traffic();
        traffic.scheduleTrafficCheck("2:37 PM", true);
        traffic.scheduleTrafficCheck("5:50 PM", false);
        traffic.scheduleTrafficCheck("6:00 PM", false);

        // Add shutdown hooks
        Runtime.getRuntime().addShutdownHook(new Thread(() -> new StormAlerts().shutdown(), "Shutdown Hook"));
        Thread input = new Thread(
            () -> {
                Scanner in = new Scanner(System.in, StandardCharsets.UTF_8);
                while (true) {
                    String text = in.nextLine();
                    if (text.equalsIgnoreCase("stop")) {
                        in.close();
                        System.exit(0);
                    }
                    if (text.equalsIgnoreCase("n")) new Traffic().checkTraffic(true);
                    if (text.equalsIgnoreCase("s")) new Traffic().checkTraffic(false);
                }
            }, "Console Input");
        input.start();

        System.out.println(logUtils.getTime(LogUtils.LogColor.GREEN) + "Done starting up!");
    }

    public void shutdown() {
        LogUtils logUtils = new LogUtils();
        System.out.println(logUtils.getTime(LogUtils.LogColor.YELLOW) + "Shutting down...");

        ambientWeatherSocket.disconnect();

        System.out.println(logUtils.getTime(LogUtils.LogColor.GREEN) + "Dumping record data...");
        try {
            FileWriter recordsToday = new Records().saveRecords();
            recordsToday.close();

        } catch (IOException e) {
            System.out.println(logUtils.getTime(LogUtils.LogColor.RED) + "Unable to write to records file! Dumping to DMs...");

            System.out.print(logUtils.getTime(LogUtils.LogColor.RED));
            e.printStackTrace();
            logUtils.logError(e);

            jda.openPrivateChannelById(277291758503723010L).flatMap(channel -> channel.sendMessage(
                MessageFormat.format(
                    "Error saving records file!\nLightning rate: {0}:{7}\nTemp high: {1}:{8}\nTemp low: {2}:{9}\nLightning today: {3}:{10}\nRain today: {4}:{11}\nRain rate: {5}:{12}\nWind: {6}:{13}",
                    Records.highestLightningRateToday,
                    Records.highestTempToday,
                    Records.lowestTempToday,
                    Records.maxLightningToday,
                    Records.maxRainAmountToday,
                    Records.maxRainRateToday,
                    Records.maxWindToday,
                    Records.highestLightningRateTime,
                    Records.highestTempTime,
                    Records.lowestTempTime,
                    Records.maxLightningTime,
                    Records.maxRainAmountTime,
                    Records.maxRainRateTime,
                    Records.maxWindTime))).complete();
        }

        if (!scheduledTimers.isEmpty()) for (ScheduledFuture<?> task : scheduledTimers) task.cancel(true);

        // Update active weather message
        if (Weather.previousWeatherType != null) {
            Message message = null;
            try {
                message = new MessageUtils()
                    .getMessages(new ChannelUtils().getWeatherChannel(Weather.previousWeatherType), 1).get(
                        1,
                        TimeUnit.SECONDS).getFirst();
                Thread.sleep(500);

            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                System.out.print(logUtils.getTime(LogUtils.LogColor.RED));
                e.printStackTrace();
                logUtils.logError(e);
            }

            //noinspection DataFlowIssue - Don't care, msg should always be from bot
            if (!message.getContentRaw().contains("Ended") && !message.getContentRaw().contains("restarted"))
                message.editMessage(message.getContentRaw()
                        .replace("!", "! (Bot restarted at <t:" + System.currentTimeMillis() / 1000 + ":t>)"))
                    .complete();
        }

        try {
            // Initating the shutdown, this closes the gateway connection and subsequently closes the requester queue
            jda.shutdown();
            // Allow at most 10 seconds for remaining requests to finish
            if (!jda.awaitShutdown(
                10,
                TimeUnit.SECONDS)) { // returns true if shutdown is graceful, false if timeout exceeded
                jda.shutdownNow(); // Cancel all remaining requests, and stop thread-pools
                jda.awaitShutdown(); // Wait until shutdown is complete (indefinitely)
            }
        } catch (NoClassDefFoundError | InterruptedException ignored) {
        }
    }
}
