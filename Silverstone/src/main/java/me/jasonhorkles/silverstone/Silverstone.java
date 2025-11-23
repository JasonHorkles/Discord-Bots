package me.jasonhorkles.silverstone;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.concurrent.*;

public class Silverstone {
    public static JDA jda;

    private static ScheduledFuture<?> liveTimer;

    public static void main(String[] args) throws InterruptedException, ExecutionException, TimeoutException {
        System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Starting...");

        JDABuilder builder = JDABuilder.createDefault(new Secrets().botToken());
        builder.enableIntents(
            GatewayIntent.GUILD_PRESENCES,
            GatewayIntent.GUILD_MEMBERS,
            GatewayIntent.GUILD_MESSAGES,
            GatewayIntent.MESSAGE_CONTENT);
        builder.enableCache(CacheFlag.ONLINE_STATUS, CacheFlag.ACTIVITY);
        builder.setMemberCachePolicy(MemberCachePolicy.ALL);
        builder.setBulkDeleteSplittingEnabled(false);
        builder.setStatus(OnlineStatus.ONLINE);
        builder.setActivity(Activity.playing("play.silverstonemc.net"));
        builder.setEnableShutdownHook(false);
        builder.addEventListeners(new Events(), new Testing());
        jda = builder.build();

        jda.awaitReady();

        //noinspection DataFlowIssue
        jda.getGuildById(455919765999976461L).updateCommands().addCommands(Commands.slash(
            "moss",
            "M.O.S.S. Discord invite")).queue();

        // Cache last counting number and verify last 10 messages
        TextChannel counting = jda.getChannelById(TextChannel.class, 816885380577230906L);
        //noinspection DataFlowIssue
        LinkedList<Message> messages = new LinkedList<>(new Utils().getMessages(counting, 50)
            .get(60, TimeUnit.SECONDS));
        // Sort messages oldest to newest
        messages.sort(Comparator.comparing(ISnowflake::getTimeCreated));

        int lastNumber = -2;
        if (!messages.isEmpty()) {
            boolean isFirstNumber = true;

            for (Message message : messages) {
                if (message == null) break;

                int value;
                try {
                    // Errors if invalid int, resulting in catch statement running
                    value = Integer.parseInt(message.getContentRaw());

                    // Set first number
                    if (isFirstNumber) {
                        lastNumber = value + 1;
                        isFirstNumber = false;
                    }

                    // If value is 1 less than the last number, update the last number value
                    if (value + 1 == lastNumber) lastNumber = value;
                    else {
                        System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Deleting invalid number from counting: " + value);
                        message.delete().queue();
                    }

                } catch (NumberFormatException ignored) {
                    // NaN
                    System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Deleting invalid message from counting: " + message.getContentRaw());
                    message.delete().queue();
                }
            }
        }
        Events.lastNumber = lastNumber;

        // Start the live check timer
        liveTimer = Executors.newSingleThreadScheduledExecutor()
            .scheduleAtFixedRate(() -> new Live().checkIfLive(), 0, 1, TimeUnit.MINUTES);

        // Add shutdown hooks
        Runtime.getRuntime().addShutdownHook(new Thread(() -> new Silverstone().shutdown(), "Shutdown Hook"));
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

        System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + "Done starting up!");
    }

    public void shutdown() {
        System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Shutting down...");
        liveTimer.cancel(true);
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
