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
import java.util.concurrent.ScheduledFuture;

@SuppressWarnings("BusyWait")
public class Quorum {
    public static JDA api;

    public static void main(String[] args) throws LoginException, InterruptedException, ParseException {
        System.out.println(new Utils().getTime(Utils.Color.YELLOW) + "Starting...");

        JDABuilder builder = JDABuilder.createDefault(new Secrets().getBotToken());
        builder.disableIntents(GatewayIntent.GUILD_MESSAGE_TYPING);
        builder.disableCache(CacheFlag.ACTIVITY, CacheFlag.VOICE_STATE);
        builder.enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_EMOJIS, GatewayIntent.GUILD_MEMBERS,
            GatewayIntent.DIRECT_MESSAGES);
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
                            new Command.Choice("November", "November"), new Command.Choice("December", "December")),
                        new OptionData(OptionType.INTEGER, "date1", "The date of the activity", true),
                        new OptionData(OptionType.STRING, "time1", "The time of the activity", true),
                        new OptionData(OptionType.STRING, "activity1", "The activity & location, if applicable", true),
                        new OptionData(OptionType.INTEGER, "date2", "The date of the activity", true),
                        new OptionData(OptionType.STRING, "time2", "The time of the activity", true),
                        new OptionData(OptionType.STRING, "activity2", "The activity & location, if applicable", true),
                        new OptionData(OptionType.INTEGER, "date3", "The date of the activity", true),
                        new OptionData(OptionType.STRING, "time3", "The time of the activity", true),
                        new OptionData(OptionType.STRING, "activity3", "The activity & location, if applicable", true),
                        new OptionData(OptionType.INTEGER, "date4", "The date of the activity", true),
                        new OptionData(OptionType.STRING, "time4", "The time of the activity", true),
                        new OptionData(OptionType.STRING, "activity4", "The activity & location, if applicable", true),
                        new OptionData(OptionType.INTEGER, "date5", "The date of the activity", false),
                        new OptionData(OptionType.STRING, "time5", "The time of the activity", false),
                        new OptionData(OptionType.STRING, "activity5", "The activity & location, if applicable", false)),

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

        System.out.println(new Utils().getTime(Utils.Color.GREEN) + "Done starting up!");
    }

    public void shutdown() {
        System.out.println(new Utils().getTime(Utils.Color.YELLOW) + "Cancelling scheduled DMs...");
        for (ScheduledFuture<?> task : ScheduleDMs.schedules) task.cancel(false);
        System.out.println(new Utils().getTime(Utils.Color.YELLOW) + "Cancelling scheduled announcements...");
        for (ScheduledFuture<?> task : ScheduleAnnouncements.schedules) task.cancel(false);
        System.out.println(new Utils().getTime(Utils.Color.YELLOW) + "Shutting down...");
        try {
            api.shutdownNow();
        } catch (NoClassDefFoundError ignored) {
        }
    }
}