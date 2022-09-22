package me.jasonhorkles.mcb;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import javax.security.auth.login.LoginException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SuppressWarnings({"BusyWait", "ConstantConditions"})
public class MCB {
    public static JDA api;

    public static void main(String[] args) throws LoginException, InterruptedException {
        System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Starting...");

        JDABuilder builder = JDABuilder.createDefault(new Secrets().getBotToken());
        builder.disableIntents(GatewayIntent.GUILD_MESSAGE_TYPING);
        builder.disableCache(CacheFlag.ACTIVITY, CacheFlag.VOICE_STATE);
        builder.enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT);
        builder.setMemberCachePolicy(MemberCachePolicy.ALL);
        builder.setBulkDeleteSplittingEnabled(true);
        builder.setStatus(OnlineStatus.ONLINE);
        builder.setActivity(Activity.watching("you"));
        builder.setEnableShutdownHook(false);
        api = builder.build();

        // Wait until the api works
        while (api.getGuildById(603190205393928193L) == null) Thread.sleep(100);

        //noinspection ConstantConditions
        api.getGuildById(603190205393928193L).loadMembers().get();

        api.addEventListener(new Events(), new AntiScam());

        api.getGuildById(603190205393928193L).updateCommands().addCommands(
            Commands.slash("buildrequest", "Create/edit/delete a request for a build/builder").addSubcommandGroups(
                new SubcommandGroupData("create", "Create a request").addSubcommands(
                    new SubcommandData("builders", "Searching for a builder or builders").addOptions(
                        new OptionData(OptionType.STRING, "platform", "The main platform that will be used",
                            true).addChoices(new Command.Choice("Java", "java"),
                            new Command.Choice("Bedrock", "bedrock")),
                        new OptionData(OptionType.STRING, "version", "The Minecraft version that will be used", true),
                        new OptionData(OptionType.INTEGER, "count", "The amount of builders needed", true),
                        new OptionData(OptionType.INTEGER, "budget",
                            "The budget each builder should expect to be paid, converted to USD (set to 0 for a free request)",
                            true), new OptionData(OptionType.STRING, "description",
                            "The description of the request with additional info (optional)", false)),

                    new SubcommandData("builds", "Searching for a specific build").addOptions(
                        new OptionData(OptionType.STRING, "platform", "The main platform that will be used",
                            true).addChoices(new Command.Choice("Java", "java"),
                            new Command.Choice("Bedrock", "bedrock")),
                        new OptionData(OptionType.STRING, "version", "The Minecraft version that will be used", true),
                        new OptionData(OptionType.STRING, "build", "A brief overview of the build needed", true),
                        new OptionData(OptionType.INTEGER, "budget",
                            "The budget the builder/team should expect to be paid, converted to USD (set to 0 for a free request)",
                            true), new OptionData(OptionType.STRING, "description",
                            "The description of the request with additional info (optional)", false)))).addSubcommands(

                new SubcommandData("edit", "Edit an existing request").addOptions(
                    new OptionData(OptionType.STRING, "id", "The ID of the message to delete", true),
                    new OptionData(OptionType.STRING, "option", "What needs changed?", true).addChoices(
                        new Command.Choice("Platform", "platform"), new Command.Choice("Version", "version"),
                        new Command.Choice("Build", "build"), new Command.Choice("Count", "count"),
                        new Command.Choice("Budget", "budget"), new Command.Choice("Description", "description")),
                    new OptionData(OptionType.STRING, "new-data", "The new data you're editing", true)),

                new SubcommandData("help", "Show help message for this command"),

                new SubcommandData("delete", "Delete a build/builder request").addOption(OptionType.STRING, "id",
                    "The ID of the message to delete", true))).queue();

        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime nextRun = now.withHour(0).withMinute(0).withSecond(0);
        if (now.compareTo(nextRun) > 0) nextRun = nextRun.plusDays(1);

        Duration duration = Duration.between(now, nextRun);
        long initalDelay = duration.getSeconds();

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> new Events().deleteMessages(api, 250), initalDelay,
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
            api.shutdownNow();
        } catch (NoClassDefFoundError ignored) {
        }
    }
}
