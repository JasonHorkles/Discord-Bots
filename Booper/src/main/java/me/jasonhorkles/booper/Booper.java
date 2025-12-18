package me.jasonhorkles.booper;

import com.github.philippheuer.credentialmanager.identityprovider.OAuth2IdentityProvider;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.events.ChannelGoLiveEvent;
import com.github.twitch4j.events.ChannelGoOfflineEvent;
import me.jasonhorkles.booper.events.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Booper {
    public static JDA jda;
    public static TwitchClient twitch;
    public static String authToken;
    public static final Long TWITCH_CHANNEL_ID = 1413244874823569479L;

    static void main() throws InterruptedException {
        System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Starting...");

        JDABuilder builder = JDABuilder.createDefault(new Secrets().botToken());
        builder.enableIntents(
            GatewayIntent.GUILD_PRESENCES,
            GatewayIntent.GUILD_MESSAGES,
            GatewayIntent.GUILD_MEMBERS);
        builder.disableCache(CacheFlag.VOICE_STATE);
        builder.enableCache(CacheFlag.ACTIVITY);
        builder.setMemberCachePolicy(MemberCachePolicy.ALL);
        builder.setStatus(OnlineStatus.ONLINE);
        builder.setActivity(Activity.customStatus("Booper Booping"));
        builder.addEventListeners(new SlashCommands(), new Buttons(), new SelectMenus(), new Modals());
        builder.setEnableShutdownHook(false);
        jda = builder.build();

        jda.awaitReady();

        Guild guild = jda.getGuildById(1299547538445307986L);
        if (guild == null) {
            System.out.println(new Utils().getTime(Utils.LogColor.RED) + "Guild not found! Exiting...");
            new Booper().shutdown();
            return;
        }

        guild.updateCommands().addCommands(
            Commands.slash("hug", "Give someone a hug!")
                .addOption(OptionType.MENTIONABLE, "hug", "Who do you want to hug?", true),

            Commands.slash("tackle", "Tackle someone!")
                .addOption(OptionType.MENTIONABLE, "tackle", "Who do you want to tackle?", true),

            Commands.slash("pillow", "Smack someone with a pillow!")
                .addOption(OptionType.MENTIONABLE, "pillow", "Who do you want to pillow?", true),

            Commands.slash("livemsg", "Set custom live messages").addSubcommands(
                new SubcommandData("set", "Add/update a user's custom live message"),
                new SubcommandData("reset", "Reset a user's custom live message to default"),
                new SubcommandData("list", "List all the users with custom live messages"))).queue();

        System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Logging into Twitch...");

        // Create Twitch client
        TwitchClientBuilder twitchClientBuilder = TwitchClientBuilder.builder().withEnableHelix(true)
            .withClientId("bal4gvuoblzs09ry4wxuurl1du5kqp")
            .withClientSecret(new Secrets().twitchClientSecret());
        twitch = twitchClientBuilder.build();

        // Get OAuth token
        Optional<OAuth2IdentityProvider> provider = twitchClientBuilder.getCredentialManager()
            .getOAuth2IdentityProviderByName("twitch");
        if (provider.isPresent()) {
            authToken = provider.get().getAppAccessToken().getAccessToken();
            System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + "Twitch login successful!");
        } else {
            System.out.println(new Utils().getTime(Utils.LogColor.RED) + "Twitch login failed!");
            authToken = "null";
        }

        // Cache already live users from file & set up listeners for new live users
        new Thread(
            () -> {
                JSONObject liveUsers = new Utils().getJsonFromFile("live-users.json");
                JSONObject discordUsers = liveUsers.getJSONObject("discord");
                JSONObject twitchUsers = liveUsers.getJSONObject("twitch");

                TextChannel channel = guild.getTextChannelById(TWITCH_CHANNEL_ID);
                if (channel == null) {
                    System.out.println(new Utils().getTime(Utils.LogColor.RED) + "Twitch text channel not found!");
                    return;
                }

                // Discord users
                for (String memberId : discordUsers.keySet()) {
                    Member member;
                    Message message;

                    member = guild.getMemberById(memberId);
                    message = channel.retrieveMessageById(discordUsers.getLong(memberId)).complete();

                    if (message == null) continue;
                    if (member == null) {
                        System.out.println(new Utils().getTime(Utils.LogColor.RED) + "Failed to cache live user with ID: " + memberId);
                        continue;
                    }

                    LiveDiscord.liveMembers.put(member, message);
                    System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + "Cached live Discord user: " + member.getEffectiveName());
                    new LiveDiscord().checkIfLive(member);
                }

                // Twitch users
                for (String username : twitchUsers.keySet()) {
                    Message message = channel.retrieveMessageById(twitchUsers.getLong(username)).complete();

                    if (message == null) continue;

                    new LiveTwitch().checkIfLive(username, message);
                }

                // Clean up messages in live channel
                try {
                    //noinspection DataFlowIssue
                    for (Message message : new Utils()
                        .getMessages(guild.getTextChannelById(TWITCH_CHANNEL_ID), 15).get(
                            30,
                            TimeUnit.SECONDS)) {
                        if (message.getAuthor().isBot())
                            if (!LiveDiscord.liveMembers.isEmpty() || !LiveTwitch.liveUsers.isEmpty())
                                continue;
                        message.delete().queue();
                    }
                } catch (ExecutionException | TimeoutException | InterruptedException e) {
                    System.out.print(new Utils().getTime(Utils.LogColor.RED));
                    e.printStackTrace();
                }

                jda.addEventListener(new LiveDiscord());

                // Get all the twitch users to watch
                JSONObject liveMessages = new Utils().getJsonFromFile("live-msgs.json");
                JSONObject allTwitchUsers = liveMessages.getJSONObject("twitch");

                for (String username : allTwitchUsers.keySet())
                    twitch.getClientHelper().enableStreamEventListener(username);

                twitch.getEventManager().onEvent(
                    ChannelGoLiveEvent.class,
                    event -> new LiveTwitch().channelLiveEvent(event));
                twitch.getEventManager().onEvent(
                    ChannelGoOfflineEvent.class,
                    event -> new LiveTwitch().channelOfflineEvent(event));
            }, "Cache Live Users").start();

        // Add shutdown hooks
        Runtime.getRuntime().addShutdownHook(new Thread(() -> new Booper().shutdown(), "Shutdown Hook"));
        new Thread(
            () -> {
                Scanner in = new Scanner(System.in, StandardCharsets.UTF_8);
                while (true) {
                    String text = in.nextLine();
                    if (text.equalsIgnoreCase("stop")) {
                        in.close();
                        System.exit(0);
                    }
                }
            }, "Console Input").start();

        System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + "Done starting up!");
    }

    public void shutdown() {
        System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Shutting down...");
        twitch.close();

        // Dump live users to file
        try {
            FileWriter file = new FileWriter("Booper/Data/live-users.json", StandardCharsets.UTF_8, false);

            JSONObject liveDiscordMembers = new JSONObject();
            for (var entry : LiveDiscord.liveMembers.entrySet())
                liveDiscordMembers.put(entry.getKey().getId(), entry.getValue().getId());

            JSONObject liveTwitchMembers = new JSONObject();
            for (var entry : LiveTwitch.liveUsers.entrySet())
                liveTwitchMembers.put(entry.getKey(), entry.getValue().getId());

            file.write(new JSONObject().put("discord", liveDiscordMembers).put("twitch", liveTwitchMembers)
                .toString(2));
            file.close();
        } catch (IOException e) {
            System.out.print(new Utils().getTime(Utils.LogColor.RED));
            e.printStackTrace();
        }

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