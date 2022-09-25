package me.jasonhorkles.quorum;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.text.ParseException;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeoutException;

public class Quorum {
    public static JDA jda;

    public static void main(String[] args) throws InterruptedException, ParseException, ExecutionException, TimeoutException {
        System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Starting...");

        JDABuilder builder = JDABuilder.createDefault(new Secrets().getBotToken());
        builder.disableCache(CacheFlag.ACTIVITY, CacheFlag.VOICE_STATE);
        builder.enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MEMBERS, GatewayIntent.DIRECT_MESSAGES,
            GatewayIntent.MESSAGE_CONTENT);
        builder.setMemberCachePolicy(MemberCachePolicy.ALL);
        builder.setStatus(OnlineStatus.ONLINE);
        builder.setEnableShutdownHook(false);
        builder.addEventListeners(new Events());
        jda = builder.build();

        jda.awaitReady();

        //noinspection ConstantConditions
        jda.getGuildById(853775450680590387L).loadMembers().get();

        OptionData months = new OptionData(OptionType.STRING, "month", "The month of the activities", true).addChoices(
            new Command.Choice("January", "January"), new Command.Choice("February", "February"),
            new Command.Choice("March", "March"), new Command.Choice("April", "April"),
            new Command.Choice("May", "May"), new Command.Choice("June", "June"), new Command.Choice("July", "July"),
            new Command.Choice("August", "August"), new Command.Choice("September", "September"),
            new Command.Choice("October", "October"), new Command.Choice("November", "November"),
            new Command.Choice("December", "December"));

        //noinspection ConstantConditions
        jda.getGuildById(853775450680590387L).updateCommands()
            .addCommands(Commands.slash("suggest", "Create a suggestion"),

                Commands.slash("suggestion-accept", "Accept a suggestion")
                    .addOption(OptionType.STRING, "message-id", "The message ID of the suggestion to accept", true),

                Commands.slash("suggestion-decline", "Decline a suggestion")
                    .addOption(OptionType.STRING, "message-id", "The message ID of the suggestion to decline", true)
                    .addOption(OptionType.STRING, "decline-reason", "Why the suggestion was declined", false),

                Commands.slash("suggestion-edit", "Edit your previous suggestion")
                    .addOption(OptionType.STRING, "message-id", "The message ID of the suggestion to edit", true),

                Commands.slash("activity", "Create/edit/cancel activities")
                    .addSubcommands(new SubcommandData("create", "Create a new activity list").addOptions(months),

                        new SubcommandData("edit", "Edit an existing activity list").addOptions(months,
                            new OptionData(OptionType.INTEGER, "line",
                                "The activity line to cancel (between 1-4, or sometimes 1-5)", true)),

                        new SubcommandData("cancel", "Cancel an activity").addOptions(months,
                            new OptionData(OptionType.INTEGER, "line",
                                "The activity line to cancel (between 1-4, or sometimes 1-5)", true)))).queue();

        new ScheduleDMs().scheduleDMs();
        new ScheduleAnnouncements().scheduleAnnouncements();

        // Add shutdown hooks
        Runtime.getRuntime().addShutdownHook(new Thread(() -> new Quorum().shutdown(), "Shutdown Hook"));
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
        System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Cancelling scheduled DMs...");
        for (ScheduledFuture<?> task : ScheduleDMs.schedules) task.cancel(false);
        System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Cancelling scheduled announcements...");
        for (ScheduledFuture<?> task : ScheduleAnnouncements.schedules) task.cancel(false);
        System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Shutting down...");
        try {
            jda.shutdownNow();
        } catch (NoClassDefFoundError ignored) {
        }
    }
}