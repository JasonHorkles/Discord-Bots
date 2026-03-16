package me.jasonhorkles.aircheck;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class AirCheck {
    public static JDA jda;
    public static final boolean testing = false;

    private static final List<ScheduledFuture<?>> scheduledTimers = new ArrayList<>();
    private static final String AQI_FILE_PATH = "AirCheck/max-aqi-level.txt";

    static void main() throws InterruptedException {
        Utils utils = new Utils();
        System.out.println(utils.getTime(Utils.LogColor.YELLOW) + "Starting...");

        JDABuilder builder = JDABuilder.createDefault(new Secrets().botToken());
        builder.disableCache(
            CacheFlag.ACTIVITY,
            CacheFlag.CLIENT_STATUS,
            CacheFlag.ONLINE_STATUS,
            CacheFlag.VOICE_STATE);
        builder.setMemberCachePolicy(MemberCachePolicy.NONE);
        builder.setBulkDeleteSplittingEnabled(false);
        builder.setStatus(OnlineStatus.IDLE);
        builder.setEnableShutdownHook(false);
        jda = builder.build();

        jda.awaitReady();

        // Cache unhealthy AQI max level
        try {
            AQI.maxAqiLevel = Integer.parseInt(Files.readString(Path.of(AQI_FILE_PATH)));
        } catch (IOException e) {
            System.out.print(utils.getTime(Utils.LogColor.RED));
            e.printStackTrace();
            utils.logError(e);
        }

        // Schedule unhealthy AQI var reset
        // This system relies on the bot restarting daily before 6 AM
        OffsetDateTime morning = OffsetDateTime.now().withHour(6).withMinute(0).withSecond(0).withNano(0);
        long delay = Duration.between(LocalDateTime.now(), morning).getSeconds();
        if (delay >= 0) {
            scheduledTimers.add(Executors.newSingleThreadScheduledExecutor().schedule(
                () -> {
                    // Reset max unhealthy AQI level
                    AQI.maxAqiLevel = -1;
                    System.out.println(utils.getTime(Utils.LogColor.GREEN) + "Reset max AQI level.");
                }, delay, TimeUnit.SECONDS));

            System.out.println(utils.getTime(Utils.LogColor.GREEN) + "Scheduled max AQI reset in " + delay / 3600 + " hours.");
        }

        // 30 min
        // Air Quality
        scheduledTimers.add(Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
            () -> {
                try {
                    new AQI().checkAir();
                } catch (Exception e) {
                    String reason = "";
                    if (e.getMessage().contains("500")) reason = " (Internal Server Error)";
                    else if (e.getMessage().contains("502")) reason = " (Bad Gateway)";
                    else if (e.getMessage().contains("503")) reason = " (Service Unavailable)";

                    System.out.println(utils.getTime(Utils.LogColor.RED) + "[ERROR] Couldn't get the air quality!" + reason);
                    if (reason.isBlank()) {
                        System.out.print(utils.getTime(Utils.LogColor.RED));
                        e.printStackTrace();
                        utils.logError(e);

                        jda.getPresence().setStatus(OnlineStatus.DO_NOT_DISTURB);
                        jda.getPresence().setActivity(Activity.customStatus("⚠ Error"));
                    }
                }
            }, 1, 1800, TimeUnit.SECONDS));

        // 45 min
        // Pollen
        scheduledTimers.add(Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
            () -> {
                try {
                    new Pollen().getPollen();
                } catch (Exception e) {
                    String reason = "";
                    if (e.getMessage().contains("Read timed out")) reason = " (Read Timed Out)";

                    System.out.println(utils.getTime(Utils.LogColor.RED) + "[ERROR] Couldn't get the pollen!" + reason);
                    if (reason.isBlank()) {
                        System.out.print(utils.getTime(Utils.LogColor.RED));
                        e.printStackTrace();
                        utils.logError(e);
                    }
                }
            }, 3, 2700, TimeUnit.SECONDS));

        // Add shutdown hooks
        Runtime.getRuntime().addShutdownHook(new Thread(() -> new AirCheck().shutdown(), "Shutdown Hook"));
        Thread input = new Thread(
            () -> {
                Scanner in = new Scanner(System.in, StandardCharsets.UTF_8);
                while (true) {
                    String text = in.nextLine();
                    if (text.equalsIgnoreCase("stop")) {
                        in.close();
                        System.exit(0);
                    }

                    if (text.equalsIgnoreCase("get")) if (testing) try {
                        new AQI().checkAir();
                        new Pollen().getPollen();
                    } catch (Exception e) {
                        System.out.print(utils.getTime(Utils.LogColor.RED));
                        e.printStackTrace();
                    }
                    else
                        System.out.println(utils.getTime(Utils.LogColor.RED) + "Testing mode must be enabled to do that!");
                }
            }, "Console Input");
        input.start();

        System.out.println(utils.getTime(Utils.LogColor.GREEN) + "Done starting up!");
    }

    public void shutdown() {
        System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Shutting down...");

        if (!scheduledTimers.isEmpty()) for (ScheduledFuture<?> task : scheduledTimers) task.cancel(true);
        saveAqiLevel();

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

    private void saveAqiLevel() {
        Utils utils = new Utils();
        System.out.println(utils.getTime(Utils.LogColor.GREEN) + "Dumping max AQI level...");
        try {
            FileWriter maxAqi = new FileWriter(AQI_FILE_PATH, StandardCharsets.UTF_8, false);
            maxAqi.write(String.valueOf(AQI.maxAqiLevel));
            maxAqi.close();

        } catch (IOException e) {
            System.out.print(utils.getTime(Utils.LogColor.RED));
            e.printStackTrace();
            utils.logError(e);
        }
    }
}
