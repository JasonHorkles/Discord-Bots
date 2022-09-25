package me.jasonhorkles.aircheck;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import javax.security.auth.login.LoginException;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class AirCheck {
    public static JDA jda;
    public static final boolean testing = false;

    private static ScheduledFuture<?> airTimer;
    private static ScheduledFuture<?> pollenTimer;

    public static void main(String[] args) throws LoginException, InterruptedException {
        System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Starting...");

        JDABuilder builder = JDABuilder.createDefault(new Secrets().getBotToken());
        builder.disableCache(CacheFlag.ACTIVITY, CacheFlag.CLIENT_STATUS, CacheFlag.ONLINE_STATUS,
            CacheFlag.VOICE_STATE);
        builder.setMemberCachePolicy(MemberCachePolicy.NONE);
        builder.setBulkDeleteSplittingEnabled(false);
        builder.setStatus(OnlineStatus.IDLE);
        builder.setEnableShutdownHook(false);
        jda = builder.build();

        jda.awaitReady();

        // Air Quality
        airTimer = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + "Checking air quality...");
            try {
                new CheckAQI().checkAir();
            } catch (Exception e) {
                System.out.println(new Utils().getTime(Utils.LogColor.RED) + "[ERROR] Couldn't get the air quality!");
                e.printStackTrace();
                jda.getPresence().setStatus(OnlineStatus.DO_NOT_DISTURB);
                jda.getPresence().setActivity(Activity.playing("⚠ Error"));
            }
        }, 1, 1800, TimeUnit.SECONDS);


        // Pollen
        pollenTimer = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + "Checking pollen quality...");
            try {
                new CheckPollen().checkConditions();
            } catch (Exception e) {
                System.out.println(
                    new Utils().getTime(Utils.LogColor.RED) + "[ERROR] Couldn't get the pollen conditions!");
                e.printStackTrace();
            }
        }, 2, 10800, TimeUnit.SECONDS);

        // Add shutdown hooks
        Runtime.getRuntime().addShutdownHook(new Thread(() -> new AirCheck().shutdown(), "Shutdown Hook"));
        Thread input = new Thread(() -> {
            while (true) {
                Scanner in = new Scanner(System.in);
                String text = in.nextLine();
                if (text.equalsIgnoreCase("stop")) System.exit(0);
            }
        }, "Console Input");
        input.start();

        System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + "Done starting up!");
    }

    public void shutdown() {
        System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Shutting down...");
        airTimer.cancel(true);
        pollenTimer.cancel(true);
        try {
            jda.shutdownNow();
        } catch (NoClassDefFoundError ignored) {
        }
    }
}
