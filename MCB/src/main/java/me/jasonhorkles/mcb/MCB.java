package me.jasonhorkles.mcb;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SuppressWarnings({"ConstantConditions"})
public class MCB {
    public static JDA jda;

    public static void main(String[] args) throws InterruptedException {
        System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Starting...");

        JDABuilder builder = JDABuilder.createDefault(new Secrets().getBotToken());
        builder.disableCache(CacheFlag.ACTIVITY, CacheFlag.VOICE_STATE);
        builder.enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT);
        builder.setMemberCachePolicy(MemberCachePolicy.ALL);
        builder.setBulkDeleteSplittingEnabled(true);
        builder.setStatus(OnlineStatus.ONLINE);
        builder.setActivity(Activity.watching("you"));
        builder.setEnableShutdownHook(false);
        builder.addEventListeners(new Events());
        jda = builder.build();

        jda.awaitReady();

        // Cache members
        jda.getGuildById(603190205393928193L).loadMembers().get();

        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime nextRun = now.withHour(0).withMinute(0).withSecond(0);
        if (now.compareTo(nextRun) > 0) nextRun = nextRun.plusDays(1);

        Duration duration = Duration.between(now, nextRun);
        long initalDelay = duration.getSeconds();

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> new Events().deleteMessages(jda, 250), initalDelay,
            TimeUnit.DAYS.toSeconds(1), TimeUnit.SECONDS);

        // Add shutdown hooks
        Runtime.getRuntime().addShutdownHook(new Thread(() -> new MCB().shutdown(), "Shutdown Hook"));
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
        try {
            jda.shutdownNow();
        } catch (NoClassDefFoundError ignored) {
        }
    }
}
