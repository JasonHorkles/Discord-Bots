package me.jasonhorkles.fancyfriend;

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
import java.util.concurrent.TimeUnit;

public class FancyFriend {
    public static JDA jda;
    public static final long GUILD_ID = 288215803294253066L;

    public static void main(String[] args) throws InterruptedException {
        System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Starting...");

        JDABuilder builder = JDABuilder.createDefault(new Secrets().botToken());
        builder.disableCache(CacheFlag.ACTIVITY, CacheFlag.VOICE_STATE);
        builder.enableIntents(GatewayIntent.GUILD_MEMBERS,
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

        OptionData options = new OptionData(OptionType.STRING,
            "option",
            "The ping protection method to use",
            true).addChoices(new Command.Choice("All Pings", "all"),
            new Command.Choice("Explicit Only (Ignore Replies)", "explicit"),
            new Command.Choice("Off", "off"));

        //noinspection DataFlowIssue
        jda.getGuildById(GUILD_ID).updateCommands().addCommands(Commands
            .slash("noping", "Change the status of your ping protection").addOptions(options)).queue();

        // Add shutdown hooks
        Runtime.getRuntime().addShutdownHook(new Thread(() -> new FancyFriend().shutdown(), "Shutdown Hook"));
        Thread input = new Thread(() -> {
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

        System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + "Done starting up!");
    }

    public void shutdown() {
        System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Shutting down...");
        try {
            // Initating the shutdown, this closes the gateway connection and subsequently closes the requester queue
            jda.shutdown();
            // Allow at most 10 seconds for remaining requests to finish
            if (!jda.awaitShutdown(10,
                TimeUnit.SECONDS)) { // returns true if shutdown is graceful, false if timeout exceeded
                jda.shutdownNow(); // Cancel all remaining requests, and stop thread-pools
                jda.awaitShutdown(); // Wait until shutdown is complete (indefinitely)
            }
        } catch (NoClassDefFoundError | InterruptedException ignored) {
        }
    }
}
