package me.jasonhorkles.fancyfriend;

import de.oliver.fancyanalytics.sdk.events.Event;
import me.jasonhorkles.fancyfriend.analytics.BotAnalytics;
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
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class FancyFriend {
    public static JDA jda;
    public static final long GUILD_ID = 1092507996166295644L;

    public static void main(String[] args) throws InterruptedException {
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

        // Cache members
        //noinspection DataFlowIssue
        jda.getGuildById(GUILD_ID).loadMembers().get();

        OptionData pingOptions = new OptionData(
            OptionType.STRING,
            "option", "The ping protection method to use", true).addChoices(
            new Command.Choice("All Pings", "all"),
            new Command.Choice("Explicit Only (Ignore Replies)", "explicit"),
            new Command.Choice("Off", "off"));

        //noinspection DataFlowIssue
        jda.getGuildById(GUILD_ID).updateCommands().addCommands(
            Commands.slash("blankline", "How to add a blank line in a hologram"),
            Commands.slash("clickable", "Clickable FancyHolograms tutorial"),
            Commands.slash("converters", "Converters not available message"),
            Commands.slash("docs", "Get the FancyPlugins documentation"),
            Commands.slash("fixed", "Show how to set a hologram to fixed"),
            Commands.slash("geyser", "Geyser not supported message"),
            Commands.slash("manual-holo", "How to manually edit a hologram properly"),
            Commands.slash("multiline", "Make an NPC name have multiple lines"),
            Commands.slash("noping", "Change the status of your ping protection").addOptions(pingOptions),
            Commands.slash("per-line", "Per-line settings not supported message"),
            Commands.slash("versions", "Get a plugin's supported MC versions")
                .addOptions(new Modrinth().getProjects()),
            Commands.slash("via", "ViaVersion not supported message")).queue();

        // Add shutdown hooks
        Runtime.getRuntime().addShutdownHook(new Thread(() -> new FancyFriend().shutdown(), "Shutdown Hook"));
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

        BotAnalytics.get().getClient().getEventService().createEvent(
            BotAnalytics.get().getProjectId(),
            new Event("BotStarted", new HashMap<>()));

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

        BotAnalytics.get().getClient().getEventService().createEvent(
            BotAnalytics.get().getProjectId(),
            new Event("BotStopped", new HashMap<>()));
    }
}
