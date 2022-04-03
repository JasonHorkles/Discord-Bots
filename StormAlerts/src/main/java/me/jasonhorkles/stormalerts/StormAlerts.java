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
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import javax.annotation.Nullable;
import javax.security.auth.login.LoginException;
import java.util.Scanner;
import java.util.concurrent.*;

@SuppressWarnings({"BusyWait", "ConstantConditions"})
public class StormAlerts extends ListenerAdapter {
    public static JDA api;
    public static boolean testing = false;

    private static ScheduledFuture<?> alertTimer;
    private static ScheduledFuture<?> pwsTimer;
    private static ScheduledFuture<?> weatherTimer;

    public static void main(String[] args) throws LoginException, InterruptedException {
        System.out.println(new Utils().getTime(Utils.Color.YELLOW) + "Starting...");

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
        api.addEventListener(new CheckWeatherConditions());

        //noinspection ConstantConditions
        CommandListUpdateAction commands = api.getGuildById(843919716677582888L).updateCommands();

        commands.addCommands(Commands.slash("checknow", "Force all the checks")).queue();


        // Alerts
        alertTimer = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                new CheckAlerts().checkAlerts();
            } catch (Exception e) {
                System.out.println(new Utils().getTime(Utils.Color.RED) + "[ERROR] Couldn't get the alerts!");
                e.printStackTrace();
            }
        }, 1, 180, TimeUnit.SECONDS);


        // PWS / Rain / Lightning
        pwsTimer = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                new CheckPwsConditions().checkConditions();
            } catch (Exception e) {
                String reason = "";
                if (e.getMessage().contains("401")) reason = " (Unauthorized)";
                else if (e.getMessage().contains("500")) reason = " (Internal Server Error)";
                else if (e.getMessage().contains("502")) reason = " (Bad Gateway)";
                else if (e.getMessage().contains("503")) reason = " (Service Unavailable)";

                System.out.println(
                    new Utils().getTime(Utils.Color.RED) + "[ERROR] Couldn't get the PWS conditions!" + reason);
                if (reason.equals("")) e.printStackTrace();
            }
        }, 3, 90, TimeUnit.SECONDS);


        // Weather
        weatherTimer = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                new CheckWeatherConditions().checkConditions();
            } catch (Exception e) {
                System.out.println(
                    new Utils().getTime(Utils.Color.RED) + "[ERROR] Couldn't get the weather conditions!");
                e.printStackTrace();
                api.getPresence().setStatus(OnlineStatus.DO_NOT_DISTURB);
                api.getPresence().setActivity(Activity.playing("Error checking weather!"));
            }
        }, 5, 90, TimeUnit.SECONDS);

        // Add shutdown hooks
        Runtime.getRuntime().addShutdownHook(new Thread(() -> new StormAlerts().shutdown()));
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

    // Slash commands
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (event.getName().toLowerCase()) {
            case "checknow" -> updateNow(event);
        }
    }

    public void updateNow(@Nullable SlashCommandInteractionEvent event) {
        String error = "Done!";
        boolean isSlash = event != null;

        if (isSlash) event.deferReply(true).complete();

        // Alerts
        System.out.println(new Utils().getTime(Utils.Color.YELLOW) + "Force checking alerts...");
        if (isSlash) event.getHook().editOriginal("Checking alerts...").complete();
        try {
            new CheckAlerts().checkAlerts();
        } catch (Exception e) {
            System.out.println(new Utils().getTime(Utils.Color.RED) + "[ERROR] Couldn't get the alerts!");
            e.printStackTrace();
            error = "Couldn't get the alerts!";
        }

        // PWS / Rain
        System.out.println(new Utils().getTime(Utils.Color.YELLOW) + "Force checking PWS conditions...");
        if (isSlash) event.getHook().editOriginal("Checking PWS conditions...").complete();
        try {
            new CheckPwsConditions().checkConditions();
        } catch (Exception e) {
            System.out.println(new Utils().getTime(Utils.Color.RED) + "[ERROR] Couldn't get the PWS conditions!");
            e.printStackTrace();
            error = "Couldn't get the PWS conditions!";
        }

        // Weather
        System.out.println(new Utils().getTime(Utils.Color.YELLOW) + "Force checking weather conditions...");
        if (isSlash) event.getHook().editOriginal("Checking weather conditions...").complete();
        try {
            new CheckWeatherConditions().checkConditions();
        } catch (Exception e) {
            System.out.println(
                new Utils().getTime(Utils.Color.RED) + "[ERROR] Couldn't get the weather conditions!");
            e.printStackTrace();
            error = "Couldn't get the weather conditions!";
            api.getPresence().setStatus(OnlineStatus.DO_NOT_DISTURB);
            api.getPresence().setActivity(Activity.playing("Error checking weather!"));
        }

        if (isSlash) event.getHook().editOriginal(error).complete();
    }

    public void shutdown() {
        System.out.println(new Utils().getTime(Utils.Color.YELLOW) + "Shutting down...");
        alertTimer.cancel(true);
        pwsTimer.cancel(true);
        weatherTimer.cancel(true);
        if (CheckWeatherConditions.previousTypeChannel != null) {
            Message message = null;
            try {
                message = new Utils().getMessages(CheckWeatherConditions.previousTypeChannel, 1)
                    .get(1, TimeUnit.SECONDS)
                    .get(0);
                Thread.sleep(500);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                e.printStackTrace();
            }
            if (!message.getContentRaw().contains("Ended") && !message.getContentRaw().contains("restarted"))
                message.editMessage(message.getContentRaw()
                    .replace("!", "! (Bot restarted at <t:" + System.currentTimeMillis() / 1000 + ":t>)")).complete();
            CheckWeatherConditions.previousTypeChannel = null;
        }
        try {
            api.shutdownNow();
        } catch (NoClassDefFoundError ignored) {
        }
    }
}
