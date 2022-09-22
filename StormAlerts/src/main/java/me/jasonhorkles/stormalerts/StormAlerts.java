package me.jasonhorkles.stormalerts;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import javax.annotation.Nullable;
import javax.security.auth.login.LoginException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Scanner;
import java.util.concurrent.*;

@SuppressWarnings({"BusyWait", "ConstantConditions"})
public class StormAlerts extends ListenerAdapter {
    public static JDA api;
    public static final boolean testing = false;

    private static ScheduledFuture<?> alertTimer;
    private static ScheduledFuture<?> pwsTimer;
    private static ScheduledFuture<?> weatherTimer;
    private static final ArrayList<ScheduledFuture<?>> trafficTimers = new ArrayList<>();

    public static void main(String[] args) throws LoginException, InterruptedException, ParseException {
        System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Starting...");

        JDABuilder builder = JDABuilder.createDefault(new Secrets().getBotToken());
        builder.disableIntents(GatewayIntent.GUILD_MESSAGE_TYPING);
        builder.disableCache(CacheFlag.ACTIVITY, CacheFlag.CLIENT_STATUS, CacheFlag.ONLINE_STATUS,
            CacheFlag.VOICE_STATE);
        builder.enableIntents(GatewayIntent.GUILD_MESSAGES);
        builder.setMemberCachePolicy(MemberCachePolicy.NONE);
        builder.setBulkDeleteSplittingEnabled(false);
        builder.setStatus(OnlineStatus.DO_NOT_DISTURB);
        builder.setEnableShutdownHook(false);
        api = builder.build();

        // Wait until the api works
        while (api.getGuildById(843919716677582888L) == null) Thread.sleep(100);

        api.addEventListener(new StormAlerts());
        api.addEventListener(new Weather());

        //noinspection ConstantConditions
        api.getGuildById(843919716677582888L).updateCommands()
            .addCommands(Commands.slash("checknow", "Force all checks")).queue();


        // Alerts
        alertTimer = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                new Alerts().checkAlerts();
            } catch (Exception e) {
                System.out.println(new Utils().getTime(Utils.LogColor.RED) + "[ERROR] Couldn't get the alerts!");
                e.printStackTrace();
            }
        }, 1, 180, TimeUnit.SECONDS);


        // PWS / Rain / Lightning
        pwsTimer = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                new Pws().checkConditions();
            } catch (Exception e) {
                String reason = "";
                if (e.getMessage().contains("401")) reason = " (Unauthorized)";
                else if (e.getMessage().contains("500")) reason = " (Internal Server Error)";
                else if (e.getMessage().contains("502")) reason = " (Bad Gateway)";
                else if (e.getMessage().contains("503")) reason = " (Service Unavailable)";

                System.out.println(
                    new Utils().getTime(Utils.LogColor.RED) + "[ERROR] Couldn't get the PWS conditions!" + reason);
                if (reason.equals("")) {
                    System.out.print(new Utils().getTime(Utils.LogColor.RED));
                    e.printStackTrace();
                }
            }
        }, 3, 90, TimeUnit.SECONDS);


        // Weather
        weatherTimer = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                new Weather().checkConditions();
            } catch (Exception e) {
                System.out.println(
                    new Utils().getTime(Utils.LogColor.RED) + "[ERROR] Couldn't get the weather conditions!");
                e.printStackTrace();
                api.getPresence().setStatus(OnlineStatus.DO_NOT_DISTURB);
                api.getPresence().setActivity(Activity.playing("Error checking weather!"));
            }
        }, 5, 90, TimeUnit.SECONDS);

        // Schedule traffic checks
        new StormAlerts().scheduleTrafficCheck("2:40 PM", true);
        new StormAlerts().scheduleTrafficCheck("5:50 PM", false);
        new StormAlerts().scheduleTrafficCheck("6:00 PM", false);

        // Add shutdown hooks
        Runtime.getRuntime().addShutdownHook(new Thread(() -> new StormAlerts().shutdown(), "Shutdown Hook"));
        Thread input = new Thread(() -> {
            while (true) {
                Scanner in = new Scanner(System.in);
                String text = in.nextLine();
                if (text.equalsIgnoreCase("stop")) System.exit(0);
                if (text.equalsIgnoreCase("traffic n")) new Traffic().checkTraffic(true);
                if (text.equalsIgnoreCase("traffic s")) new Traffic().checkTraffic(false);
            }
        }, "Console Input");
        input.start();

        System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + "Done starting up!");
    }

    // Slash commands
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + event.getMember()
            .getEffectiveName() + " used the /" + event.getName() + " command");

        //noinspection SwitchStatementWithTooFewBranches
        switch (event.getName().toLowerCase()) {
            case "checknow" -> updateNow(event);
        }
    }

    private void scheduleTrafficCheck(String time, boolean toWork) throws ParseException {
        DayOfWeek day = LocalDate.now().getDayOfWeek();
        if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
            DateFormat format = new SimpleDateFormat("yyyy-MM-dd h:mm a");
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(format.parse(LocalDate.now() + " " + time));

            long delay = calendar.getTimeInMillis() - System.currentTimeMillis();

            if (delay >= 0) {
                trafficTimers.add(Executors.newSingleThreadScheduledExecutor()
                    .schedule(() -> new Traffic().checkTraffic(toWork), delay, TimeUnit.MILLISECONDS));
                System.out.println(
                    new Utils().getTime(Utils.LogColor.GREEN) + "Scheduled traffic check in " + Math.round(
                        delay / 3600000.0) + " hours.");
            }
        }
    }

    public void updateNow(@Nullable SlashCommandInteractionEvent event) {
        String error = "Done!";
        boolean isSlash = event != null;

        if (isSlash) event.deferReply(true).complete();

        // Alerts
        System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Force checking alerts...");
        if (isSlash) event.getHook().editOriginal("Checking alerts...").complete();
        try {
            new Alerts().checkAlerts();
        } catch (Exception e) {
            System.out.println(new Utils().getTime(Utils.LogColor.RED) + "[ERROR] Couldn't get the alerts!");
            e.printStackTrace();
            error = "Couldn't get the alerts!";
        }

        // PWS / Rain
        System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Force checking PWS conditions...");
        if (isSlash) event.getHook().editOriginal("Checking PWS conditions...").complete();
        try {
            new Pws().checkConditions();
        } catch (Exception e) {
            System.out.println(new Utils().getTime(Utils.LogColor.RED) + "[ERROR] Couldn't get the PWS conditions!");
            e.printStackTrace();
            error = "Couldn't get the PWS conditions!";
        }

        // Weather
        System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Force checking weather conditions...");
        if (isSlash) event.getHook().editOriginal("Checking weather conditions...").complete();
        try {
            new Weather().checkConditions();
        } catch (Exception e) {
            System.out.println(
                new Utils().getTime(Utils.LogColor.RED) + "[ERROR] Couldn't get the weather conditions!");
            e.printStackTrace();
            error = "Couldn't get the weather conditions!";
            api.getPresence().setStatus(OnlineStatus.DO_NOT_DISTURB);
            api.getPresence().setActivity(Activity.playing("Error checking weather!"));
        }

        if (isSlash) event.getHook().editOriginal(error).complete();
    }

    public void shutdown() {
        System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Shutting down...");
        alertTimer.cancel(true);
        pwsTimer.cancel(true);
        weatherTimer.cancel(true);
        if (!trafficTimers.isEmpty()) for (ScheduledFuture<?> task : trafficTimers) task.cancel(true);
        if (Weather.previousTypeChannel != null) {
            Message message = null;
            try {
                message = new Utils().getMessages(Weather.previousTypeChannel, 1).get(1, TimeUnit.SECONDS).get(0);
                Thread.sleep(500);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                System.out.print(new Utils().getTime(Utils.LogColor.RED));
                e.printStackTrace();
            }
            if (!message.getContentRaw().contains("Ended") && !message.getContentRaw().contains("restarted"))
                message.editMessage(message.getContentRaw()
                    .replace("!", "! (Bot restarted at <t:" + System.currentTimeMillis() / 1000 + ":t>)")).complete();
            Weather.previousTypeChannel = null;
        }
        try {
            api.shutdownNow();
        } catch (NoClassDefFoundError ignored) {
        }
    }
}
