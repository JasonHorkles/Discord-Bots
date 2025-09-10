package me.jasonhorkles.aircheck;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.nio.charset.StandardCharsets;
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

    public static void main(String[] args) throws InterruptedException {
        System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Starting...");

        JDABuilder builder = JDABuilder.createDefault(new Secrets().botToken());
        builder.disableCache(CacheFlag.ACTIVITY,
            CacheFlag.CLIENT_STATUS,
            CacheFlag.ONLINE_STATUS,
            CacheFlag.VOICE_STATE);
        builder.setMemberCachePolicy(MemberCachePolicy.NONE);
        builder.setBulkDeleteSplittingEnabled(false);
        builder.setStatus(OnlineStatus.IDLE);
        builder.setEnableShutdownHook(false);
        jda = builder.build();

        jda.awaitReady();

        // Air Quality
        scheduledTimers.add(Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                new AQI().checkAir();
            } catch (Exception e) {
                String reason = "";
                if (e.getMessage().contains("500")) reason = " (Internal Server Error)";
                else if (e.getMessage().contains("502")) reason = " (Bad Gateway)";
                else if (e.getMessage().contains("503")) reason = " (Service Unavailable)";

                System.out.println(new Utils().getTime(Utils.LogColor.RED) + "[ERROR] Couldn't get the air quality!" + reason);
                if (reason.isBlank()) {
                    System.out.print(new Utils().getTime(Utils.LogColor.RED));
                    e.printStackTrace();
                    new Utils().logError(e);
                }

                jda.getPresence().setStatus(OnlineStatus.DO_NOT_DISTURB);
                jda.getPresence().setActivity(Activity.customStatus("⚠ Error"));
            }
        }, 1, 1800, TimeUnit.SECONDS));

        // Pollen
        scheduledTimers.add(Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                new Pollen().getPollen();
            } catch (Exception e) {
                String reason = "";
                if (e.getMessage().contains("Read timed out")) reason = " (Read Timed Out)";

                System.out.println(new Utils().getTime(Utils.LogColor.RED) + "[ERROR] Couldn't get the pollen!" + reason);
                if (reason.isBlank()) {
                    System.out.print(new Utils().getTime(Utils.LogColor.RED));
                    e.printStackTrace();
                    new Utils().logError(e);
                }
            }
        }, 2, 2700, TimeUnit.SECONDS));

        // Add shutdown hooks
        Runtime.getRuntime().addShutdownHook(new Thread(() -> new AirCheck().shutdown(), "Shutdown Hook"));
        Thread input = new Thread(() -> {
            Scanner in = new Scanner(System.in, StandardCharsets.UTF_8);
            while (true) {
                String text = in.nextLine();
                if (text.equalsIgnoreCase("stop")) {
                    in.close();
                    System.exit(0);
                }
            }
        }, "Console Input");
        input.start();

        System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + "Done starting up!");
    }

    public void shutdown() {
        System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Shutting down...");
        if (!scheduledTimers.isEmpty()) for (ScheduledFuture<?> task : scheduledTimers) task.cancel(true);
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
}
