package me.jasonhorkles.stormalerts;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.text.ParseException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.*;

@SuppressWarnings({"DataFlowIssue"})
public class StormAlerts extends ListenerAdapter {
    public static final List<ScheduledFuture<?>> scheduledTimers = new ArrayList<>();
    public static final boolean testing = false;
    public static JDA jda;

    public static void main(String[] args) throws InterruptedException, ParseException {
        Utils utils = new Utils();
        System.out.println(utils.getTime(Utils.LogColor.YELLOW) + "Starting...");

        JDABuilder builder = JDABuilder.createDefault(new Secrets().botToken());
        builder.disableCache(CacheFlag.ACTIVITY,
            CacheFlag.CLIENT_STATUS,
            CacheFlag.ONLINE_STATUS,
            CacheFlag.VOICE_STATE);
        builder.enableIntents(GatewayIntent.GUILD_MESSAGES,
            GatewayIntent.MESSAGE_CONTENT,
            GatewayIntent.GUILD_MEMBERS);
        builder.setMemberCachePolicy(MemberCachePolicy.ALL);
        builder.setBulkDeleteSplittingEnabled(false);
        builder.setStatus(OnlineStatus.DO_NOT_DISTURB);
        builder.setEnableShutdownHook(false);
        builder.addEventListeners(new Events(), new Weather());
        jda = builder.build();

        jda.awaitReady();

        //noinspection DataFlowIssue
        jda.getGuildById(843919716677582888L).updateCommands().addCommands(Commands.slash("checknow",
                "Force all checks (except records)"),
            Commands.slash("updaterecords", "Force the record checks")).queue();

        // Cache wind speed
        try {
            Message windMessage = new Utils().getMessages(jda.getTextChannelById(1028358818050080768L), 1)
                .get(30, TimeUnit.SECONDS).getFirst();
            if (windMessage != null) {
                OffsetDateTime fiveHoursAgo = OffsetDateTime.now().minusHours(5);
                OffsetDateTime midnight = OffsetDateTime.now().withHour(0).withMinute(0).withSecond(0)
                    .withNano(0);

                if (windMessage.getTimeCreated().isAfter(fiveHoursAgo) || windMessage.getTimeCreated()
                    .isAfter(midnight)) Pws.lastAlertedWindGust = Integer.parseInt(windMessage
                    .getContentStripped().replace("\n", " ").replaceFirst(".*of ", "")
                    .replaceFirst(" mph.*", ""));
            }
        } catch (ExecutionException | TimeoutException e) {
            System.out.print(utils.getTime(Utils.LogColor.RED));
            e.printStackTrace();
            utils.logError(e);
        }

        // Schedule records announcement and cache current records
        try {
            new Records().scheduleRecordCheck();
        } catch (Exception e) {
            System.out.println(utils.getTime(Utils.LogColor.RED) + "Error grabbing the records!");
            e.printStackTrace();
            utils.logError(e);
        }

        // 1.5 mins
        scheduledTimers.add(Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                new Alerts().checkAlerts();
            } catch (Exception e) {
                String reason = "";
                if (e.getMessage().contains("500")) reason = " (Internal Server Error)";
                else if (e.getMessage().contains("502")) reason = " (Bad Gateway)";
                else if (e.getMessage().contains("503")) reason = " (Service Unavailable)";

                System.out.println(utils.getTime(Utils.LogColor.RED) + "[ERROR] Couldn't get the alerts!" + reason);
                if (reason.isBlank()) {
                    System.out.print(utils.getTime(Utils.LogColor.RED));
                    e.printStackTrace();
                    utils.logError(e);
                }
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }

            //todo https://ambientweather.docs.apiary.io/#reference/ambient-realtime-api instead
            try {
                new Pws().checkConditions();
            } catch (Exception e) {
                String reason = "";
                if (e.getMessage().contains("401")) reason = " (Unauthorized)";
                else if (e.getMessage().contains("500")) reason = " (Internal Server Error)";
                else if (e.getMessage().contains("502")) reason = " (Bad Gateway)";
                else if (e.getMessage().contains("503")) reason = " (Service Unavailable)";
                else if (e.getMessage().contains("504")) reason = " (Gateway Timeout)";
                else if (e.getMessage().contains("520")) reason = " (Catch-all error)";
                else if (e.getMessage().contains("524")) reason = " (Timeout)";

                System.out.println(utils.getTime(Utils.LogColor.RED) + "[ERROR] Couldn't get the PWS conditions!" + reason);
                if (reason.isBlank()) {
                    System.out.print(utils.getTime(Utils.LogColor.RED));
                    e.printStackTrace();
                    utils.logError(e);
                }
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }

            /*try {
                new Weather().checkConditions();
            } catch (Exception e) {
                System.out.println(utils.getTime(Utils.LogColor.RED) + "[ERROR] Couldn't get the weather conditions!");
                e.printStackTrace();
                jda.getPresence().setStatus(OnlineStatus.DO_NOT_DISTURB);
                jda.getPresence().setActivity(Activity.playing("Error checking weather!"));
                utils.logError(e);
            }*/
        }, 1, 90, TimeUnit.SECONDS));

        // 6 mins
        scheduledTimers.add(Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                new Visibility().checkConditions();
            } catch (Exception e) {
                String reason = "";
                if (e.getMessage().contains("500")) reason = " (Internal Server Error)";
                else if (e.getMessage().contains("502")) reason = " (Bad Gateway)";
                else if (e.getMessage().contains("503")) reason = " (Service Unavailable)";

                System.out.println(utils.getTime(Utils.LogColor.RED) + "[ERROR] Couldn't get the visibility!" + reason);
                if (reason.isBlank()) {
                    System.out.print(utils.getTime(Utils.LogColor.RED));
                    e.printStackTrace();
                    utils.logError(e);
                }
                new Utils().updateVoiceChannel(899872710233051178L, "Visibility | ERROR");
            }
        }, 3, 360, TimeUnit.SECONDS));

        // Schedule traffic checks
        Traffic traffic = new Traffic();
        traffic.scheduleTrafficCheck("2:37 PM", true);
        traffic.scheduleTrafficCheck("5:50 PM", false);
        traffic.scheduleTrafficCheck("6:00 PM", false);

        // Add shutdown hooks
        Runtime.getRuntime().addShutdownHook(new Thread(() -> new StormAlerts().shutdown(), "Shutdown Hook"));
        Thread input = new Thread(() -> {
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

        System.out.println(utils.getTime(Utils.LogColor.GREEN) + "Done starting up!");
    }

    public void shutdown() {
        Utils utils = new Utils();
        System.out.println(utils.getTime(Utils.LogColor.YELLOW) + "Shutting down...");

        System.out.println(utils.getTime(Utils.LogColor.GREEN) + "Dumping record data...");
        try {
            FileWriter recordsToday = saveRecords();
            recordsToday.close();

        } catch (IOException e) {
            System.out.println(utils.getTime(Utils.LogColor.RED) + "Unable to write to records file! Dumping to DMs...");

            System.out.print(utils.getTime(Utils.LogColor.RED));
            e.printStackTrace();
            utils.logError(e);

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

        if (Weather.previousTypeChannel != null) {
            Message message = null;
            try {
                message = new Utils().getMessages(Weather.previousTypeChannel, 1).get(1, TimeUnit.SECONDS)
                    .getFirst();
                Thread.sleep(500);

            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                System.out.print(utils.getTime(Utils.LogColor.RED));
                e.printStackTrace();
                utils.logError(e);
            }

            if (!message.getContentRaw().contains("Ended") && !message.getContentRaw().contains("restarted"))
                message.editMessage(message.getContentRaw()
                        .replace("!", "! (Bot restarted at <t:" + System.currentTimeMillis() / 1000 + ":t>)"))
                    .complete();
            Weather.previousTypeChannel = null;
        }

        try {
            // Initating the shutdown, this closes the gateway connection and subsequently closes the requester queue
            jda.shutdown();
            // Allow at most 10 seconds for remaining requests to finish
            if (!jda.awaitShutdown(10,
                TimeUnit.SECONDS)) { // returns true if shutdown is graceful, false if timeout exceeded
                jda.shutdownNow(); // Cancel all remaining requests, and stop thread-pools
                jda.awaitShutdown(); // Wait until shutdown is complete (indefinitely)
            }
        } catch (NoClassDefFoundError | InterruptedException ignored) {
        }
    }

    @NotNull
    private static FileWriter saveRecords() throws IOException {
        String filePath = "StormAlerts/records-today.json";

        JSONObject allRecords = new JSONObject();
        allRecords.put("highLightningRate", Records.highestLightningRateToday);
        allRecords.put("highTemp", Records.highestTempToday);
        allRecords.put("lowTemp", Records.lowestTempToday);
        allRecords.put("maxLightning", Records.maxLightningToday);
        allRecords.put("maxRainAmount", Records.maxRainAmountToday);
        allRecords.put("maxRainRate", Records.maxRainRateToday);
        allRecords.put("maxWind", Records.maxWindToday);

        allRecords.put("highLightningRateTime", Records.highestLightningRateTime);
        allRecords.put("highTempTime", Records.highestTempTime);
        allRecords.put("lowTempTime", Records.lowestTempTime);
        allRecords.put("maxLightningTime", Records.maxLightningTime);
        allRecords.put("maxRainAmountTime", Records.maxRainAmountTime);
        allRecords.put("maxRainRateTime", Records.maxRainRateTime);
        allRecords.put("maxWindTime", Records.maxWindTime);

        FileWriter recordsToday = new FileWriter(filePath, StandardCharsets.UTF_8, false);
        recordsToday.write(allRecords.toString());
        return recordsToday;
    }
}
