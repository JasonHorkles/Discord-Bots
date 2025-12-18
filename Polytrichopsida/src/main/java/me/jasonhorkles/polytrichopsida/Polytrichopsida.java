package me.jasonhorkles.polytrichopsida;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Polytrichopsida {
    public static JDA jda;

    static void main() throws InterruptedException {
        System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Starting...");

        JDABuilder builder = JDABuilder.createDefault(new Secrets().botToken());
        builder.disableCache(CacheFlag.ACTIVITY, CacheFlag.VOICE_STATE);
        builder.enableIntents(
            GatewayIntent.GUILD_MEMBERS,
            GatewayIntent.GUILD_MESSAGES,
            GatewayIntent.MESSAGE_CONTENT);
        builder.setMemberCachePolicy(MemberCachePolicy.ALL);
        builder.setBulkDeleteSplittingEnabled(false);
        builder.setStatus(OnlineStatus.ONLINE);
        builder.setEnableShutdownHook(false);
        builder.addEventListeners(new Events());
        jda = builder.build();

        jda.awaitReady();

        OptionData plugins = new OptionData(OptionType.STRING, "plugin", "The plugin", true).addChoices(
            new Command.Choice("EntityClearer", "EntityClearer"),
            new Command.Choice("ExpensiveDeaths", "ExpensiveDeaths"),
            new Command.Choice("FileCleaner", "FileCleaner"));

        //noinspection DataFlowIssue
        jda.getGuildById(390942438061113344L).updateCommands().addCommands(
            Commands.slash("ecldebug", "EntityClearer debug")
                .addOption(OptionType.BOOLEAN, "replyop", "Reply to the OP", false),
            Commands.slash("faqs", "FAQs link").addOptions(plugins),
            Commands.slash("plgh", "Links to the plugins on GitHub"),
            Commands.slash("plugins", "Get a list of Jason's plugins"),
            Commands.slash("tutorials", "Link to the tutorial channel"),
            Commands.slash("config", "Get a link to the latest config file of a plugin").addOptions(plugins),
            Commands.slash("close", "Close a plugin support thread")).queue();

        // Add shutdown hooks
        Runtime.getRuntime().addShutdownHook(new Thread(
            () -> new Polytrichopsida().shutdown(),
            "Shutdown Hook"));
        Thread input = new Thread(
            () -> {
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

        try {
            new CloseStale().closeStale();
        } catch (ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }

        System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + "Done starting up!");
    }

    public void shutdown() {
        System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Shutting down...");
        try {
            // Initating the shutdown, this closes the gateway connection and subsequently closes the requester queue
            jda.shutdown();
            // Allow at most 10 seconds for remaining requests to finish
            if (!jda.awaitShutdown(
                10,
                TimeUnit.SECONDS)) { // returns true if shutdown is graceful, false if timeout exceeded
                jda.shutdownNow(); // Cancel all remaining requests, and stop thread-pools
                jda.awaitShutdown(); // Wait until shutdown is complete (indefinitely)
            }
        } catch (NoClassDefFoundError | InterruptedException ignored) {
        }
    }
}
