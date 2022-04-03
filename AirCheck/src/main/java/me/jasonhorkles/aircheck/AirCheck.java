package me.jasonhorkles.aircheck;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import javax.security.auth.login.LoginException;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@SuppressWarnings({"BusyWait"})
public class AirCheck extends ListenerAdapter {
    public static JDA api;
    public static boolean testing = false;

    private static ScheduledFuture<?> airTimer;
    private static ScheduledFuture<?> pollenTimer;

    public static void main(String[] args) throws LoginException, InterruptedException {
        System.out.println(new Utils().getTime(Utils.Color.YELLOW) + "Starting...");

        JDABuilder builder = JDABuilder.createDefault(new Secrets().getBotToken());
        builder.disableIntents(GatewayIntent.GUILD_MESSAGE_TYPING);
        builder.disableCache(CacheFlag.ACTIVITY, CacheFlag.CLIENT_STATUS, CacheFlag.ONLINE_STATUS,
            CacheFlag.VOICE_STATE);
        builder.setMemberCachePolicy(MemberCachePolicy.NONE);
        builder.setBulkDeleteSplittingEnabled(false);
        builder.setStatus(OnlineStatus.IDLE);
        builder.setEnableShutdownHook(false);
        api = builder.build();

        // Wait until the api works
        while (api.getGuildById(843919716677582888L) == null) Thread.sleep(100);

        // Air Quality
        airTimer = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            System.out.println(new Utils().getTime(Utils.Color.GREEN) + "Checking air quality...");
            try {
                new CheckAQI().checkAir();
            } catch (Exception e) {
                System.out.println(new Utils().getTime(Utils.Color.RED) + "[ERROR] Couldn't get the air quality!");
                e.printStackTrace();
                api.getPresence().setStatus(OnlineStatus.DO_NOT_DISTURB);
                api.getPresence().setActivity(Activity.playing("⚠ Error"));
            }
        }, 0, 3600, TimeUnit.SECONDS);


        // Pollen
        pollenTimer = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                new CheckPollen().checkConditions();
            } catch (Exception e) {
                System.out.println(
                    new Utils().getTime(Utils.Color.RED) + "[ERROR] Couldn't get the pollen conditions!");
                e.printStackTrace();
            }
        }, 5, 1800, TimeUnit.SECONDS);

        // Add shutdown hooks
        Runtime.getRuntime().addShutdownHook(new Thread(() -> new AirCheck().shutdown()));
        Thread input = new Thread(() -> {
            while (true) {
                Scanner in = new Scanner(System.in);
                String text = in.nextLine();
                if (text.equalsIgnoreCase("stop")) System.exit(0);
            }
        });
        input.start();

        System.out.println(new Utils().getTime(Utils.Color.GREEN) + "Done starting up!");
    }

    public void shutdown() {
        System.out.println(new Utils().getTime(Utils.Color.YELLOW) + "Shutting down...");
        airTimer.cancel(true);
        pollenTimer.cancel(true);
        try {
            api.shutdownNow();
        } catch (NoClassDefFoundError ignored) {
        }
    }
}
