package me.jasonhorkles.quorum;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import javax.security.auth.login.LoginException;
import java.text.ParseException;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeoutException;

@SuppressWarnings("BusyWait")
public class Quorum {
    public static JDA api;

    public static void main(String[] args) throws LoginException, InterruptedException, ParseException, ExecutionException, TimeoutException {
        System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Starting...");

        JDABuilder builder = JDABuilder.createDefault(new Secrets().getBotToken());
        builder.disableIntents(GatewayIntent.GUILD_MESSAGE_TYPING);
        builder.disableCache(CacheFlag.ACTIVITY, CacheFlag.VOICE_STATE);
        builder.enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MEMBERS, GatewayIntent.DIRECT_MESSAGES);
        builder.setMemberCachePolicy(MemberCachePolicy.ALL);
        builder.setStatus(OnlineStatus.ONLINE);
        builder.setEnableShutdownHook(false);
        api = builder.build();

        // Wait until the api works
        while (api.getGuildById(853775450680590387L) == null) Thread.sleep(100);

        //noinspection ConstantConditions
        api.getGuildById(853775450680590387L).loadMembers().get();

        api.addEventListener(new Events());

        //noinspection ConstantConditions
        CommandListUpdateAction commands = api.getGuildById(853775450680590387L).updateCommands();

        commands.addCommands(Commands.slash("suggest", "Create a suggestion")
                .addOption(OptionType.STRING, "suggestion", "Your suggestion", true),

            Commands.slash("acceptsuggestion", "Accept a suggestion")
                .addOption(OptionType.STRING, "message-id", "The message ID of the suggestion to accept", true),

            Commands.slash("declinesuggestion", "Decline a suggestion")
                .addOption(OptionType.STRING, "message-id", "The message ID of the suggestion to decline", true)
                .addOption(OptionType.STRING, "decline-reason", "Why the suggestion was declined", false),

            Commands.slash("editsuggestion", "Edit your previous suggestion")
                .addOption(OptionType.STRING, "message-id", "The message ID of the suggestion to edit", true)
                .addOption(OptionType.STRING, "suggestion", "Your edited suggestion", true),

            Commands.slash("activity", "Admin command").addSubcommands(
                    new SubcommandData("create", "Create a new activity list").addOptions(
                        new OptionData(OptionType.STRING, "month", "The month of the activities", true).addChoices(
                            new Command.Choice("January", "January"), new Command.Choice("February", "February"),
                            new Command.Choice("March", "March"), new Command.Choice("April", "April"),
                            new Command.Choice("May", "May"), new Command.Choice("June", "June"),
                            new Command.Choice("July", "July"), new Command.Choice("August", "August"),
                            new Command.Choice("September", "September"), new Command.Choice("October", "October"),
                            new Command.Choice("November", "November"), new Command.Choice("December", "December"))),

                    new SubcommandData("cancel", "Cancel an activity").addOptions(
                        new OptionData(OptionType.INTEGER, "line", "The activity line to edit", true)))

                .addSubcommandGroups(new SubcommandGroupData("edit", "Edit an existing activity list").addSubcommands(
                    new SubcommandData("date-time", "Edit the date & time of the activity").addOptions(
                        new OptionData(OptionType.INTEGER, "line", "The activity line to edit", true),
                        new OptionData(OptionType.INTEGER, "date", "The new date of the activity", true),
                        new OptionData(OptionType.STRING, "time", "The new time of the activity", true)),
                    new SubcommandData("activity", "Edit the activity").addOptions(
                        new OptionData(OptionType.INTEGER, "line", "The activity line to edit", true),
                        new OptionData(OptionType.STRING, "activity", "The updated activity", true))))).queue();

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
            api.shutdownNow();
        } catch (NoClassDefFoundError ignored) {
        }
    }
}