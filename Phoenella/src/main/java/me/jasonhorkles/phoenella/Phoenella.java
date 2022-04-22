package me.jasonhorkles.phoenella;

import me.jasonhorkles.phoenella.games.RPS;
import me.jasonhorkles.phoenella.games.Wordle;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.*;

@SuppressWarnings({"BusyWait", "ConstantConditions"})
public class Phoenella extends ListenerAdapter {
    public static JDA api;
    public static boolean localWordleBoard = false;

    private static final ArrayList<ScheduledFuture<?>> schedules = new ArrayList<>();

    public static void main(String[] args) throws LoginException, InterruptedException, IOException, ExecutionException, TimeoutException, ParseException {
        System.out.println(new Utils().getTime(Utils.Color.YELLOW) + "Starting...");

        JDABuilder builder = JDABuilder.createDefault(new Secrets().getBotToken());
        builder.disableIntents(GatewayIntent.GUILD_MESSAGE_TYPING);
        builder.disableCache(CacheFlag.ACTIVITY);
        builder.enableCache(CacheFlag.VOICE_STATE);
        builder.enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGES,
            GatewayIntent.GUILD_VOICE_STATES);
        builder.setMemberCachePolicy(MemberCachePolicy.ALL);
        builder.setEnableShutdownHook(false);
        builder.setStatus(OnlineStatus.ONLINE);
        builder.setActivity(Activity.playing("Wordle"));
        api = builder.build();

        // Wait until the api works
        while (api.getGuildById(729083627308056597L) == null) Thread.sleep(100);

        api.getGuildById(729083627308056597L).loadMembers().get();

        CommandListUpdateAction commands = api.getGuildById(729083627308056597L).updateCommands();

        commands.addCommands(
            Commands.slash("shush", "Shush a user").addOption(OptionType.USER, "user", "Who to shush", true)
                .addOption(OptionType.INTEGER, "duration", "The duration in minutes to shush them for", true),

            Commands.slash("unshush", "Un-shush a user").addOption(OptionType.USER, "user", "Who to un-shush", true),

            Commands.slash("wordle", "Wordle!").addSubcommands(new SubcommandData("play", "Play with a random word"),
                new SubcommandData("create", "Create a Wordle for others to play").addOption(OptionType.STRING, "word",
                    "Must be between 4-8 characters", true),
                new SubcommandData("leaderboard", "View the Wordle leaderboard").addOption(OptionType.BOOLEAN, "show",
                    "Show the leaderboard message publicly?", false),
                new SubcommandData("daily", "Play the daily Wordle"))).queue();

        System.out.println(new Utils().getTime(Utils.Color.GREEN) + "Starting nickname check...");
        new Utils().runNameCheckForGuild(api.getGuildById(729083627308056597L));

        // Check shushed users
        File shushes = new File("Phoenella/Shush Data");
        if (shushes.listFiles().length > 0) for (File f : shushes.listFiles()) {
            Scanner scanner = new Scanner(f);
            long delay = scanner.nextLong() - System.currentTimeMillis();
            if (delay < 0) delay = 0;
            scanner.close();

            Member member = api.getGuildById(729083627308056597L).getMemberById(f.getName().replace(".txt", ""));
            schedules.add(Executors.newSingleThreadScheduledExecutor()
                .schedule(() -> new Utils().unshush(member), delay, TimeUnit.MILLISECONDS));
            System.out.println(new Utils().getTime(Utils.Color.YELLOW) + "Scheduling the removal of " + member.getUser()
                .getAsTag() + "'s shush in " + delay / 60000 + " minutes.");
        }

        // Scan Wordle leaderboard for nonexistent players
        System.out.println(new Utils().getTime(Utils.Color.GREEN) + "Starting leaderboard check...");
        File leaderboardFile = new File("Phoenella/Wordle/leaderboard.txt");
        Scanner leaderboard = new Scanner(leaderboardFile);
        ArrayList<String> lines = new ArrayList<>();

        while (leaderboard.hasNextLine()) try {
            lines.add(leaderboard.nextLine());
        } catch (NoSuchElementException ignored) {
        }

        if (!lines.isEmpty()) if (lines.get(0).equalsIgnoreCase("local")) {
            System.out.println(new Utils().getTime(Utils.Color.GREEN) + "Leaderboard set to local mode!");
            localWordleBoard = true;
        }

        if (!localWordleBoard) {
            FileWriter writer = new FileWriter(leaderboardFile, false);

            for (String line : lines) {
                long id = Long.parseLong(line.replaceFirst(":.*", ""));
                Member member = api.getGuildById(729083627308056597L).getMemberById(id);
                if (member == null) {
                    System.out.println(new Utils().getTime(Utils.Color.YELLOW) + "Removing user with ID " + id);
                    continue;
                }

                writer.write(line + "\n");
            }
            writer.close();
        }

        System.out.println(new Utils().getTime(Utils.Color.GREEN) + "Leaderboard check complete!");

        // Schedule daily Wordle
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd h:mm a");
        Calendar future = Calendar.getInstance();
        future.setTime(format.parse(LocalDate.now() + " 7:00 AM"));

        long delay = future.getTimeInMillis() - System.currentTimeMillis();

        if (delay >= 0) {
            schedules.add(Executors.newSingleThreadScheduledExecutor()
                .schedule(() -> new Utils().updateDailyWordle(), delay, TimeUnit.MILLISECONDS));
            System.out.println(new Utils().getTime(Utils.Color.GREEN) + "Scheduled new daily Wordle in " + Math.round(
                delay / 3600000.0) + " hours.");
        }


        // Delete game channels
        for (TextChannel channel : api.getCategoryById(900747596245639238L).getTextChannels())
            channel.delete().queue();

        TextChannel soundboardChannel = api.getTextChannelById(903324139195084820L);
        if (new Utils().getMessages(soundboardChannel, 1).get(30, TimeUnit.SECONDS).isEmpty())
            soundboardChannel.sendMessage("**Select a sound!**").setActionRows(
                ActionRow.of(Button.primary("sound:benny", "Benny Hill"), Button.primary("sound:bfg", "BFG Division"),
                    Button.primary("sound:careless", "Careless Whisper"), Button.primary("sound:crickets", "Crickets"),
                    Button.primary("sound:discord", "Discord")),
                ActionRow.of(Button.secondary("sound:dramatic", "Dramatic"),
                    Button.secondary("sound:drumroll", "Drumroll"), Button.secondary("sound:honk", "Honk"),
                    Button.secondary("sound:laugh", "Laughing"), Button.secondary("sound:maya", "Maya Hee")),
                ActionRow.of(Button.primary("sound:metalgear", "Metal Gear Alert"), Button.primary("sound:oof", "Oof"),
                    Button.primary("sound:party", "Party Horn"), Button.primary("sound:phasmophobia", "Phasmophobia"),
                    Button.primary("sound:skibidi", "SKIBIDI")),
                ActionRow.of(Button.secondary("sound:suspense1", "Suspense 1"),
                    Button.secondary("sound:suspense2", "Suspense 2"),
                    Button.secondary("sound:tech", "Technical Difficulties"),
                    Button.secondary("sound:flysave", "What a Save"), Button.secondary("sound:yeet", "Yeet")),
                ActionRow.of(Button.danger("sound:stop", "Stop Sounds").withEmoji(Emoji.fromUnicode("🛑")))).queue();

        api.addEventListener(new Events(), new Soundboard(), new GameManager(), new RPS(), new Wordle(),
            new AntiScam());

        // Add shutdown hooks
        Runtime.getRuntime().addShutdownHook(new Thread(() -> new Phoenella().shutdown()));
        Thread input = new Thread(() -> {
            while (true) {
                Scanner in = new Scanner(System.in);
                String text = in.nextLine();
                if (text.equalsIgnoreCase("stop")) System.exit(0);
                if (text.equalsIgnoreCase("dailywordle")) new Utils().updateDailyWordle();
            }
        });
        input.start();

        System.out.println(new Utils().getTime(Utils.Color.GREEN) + "Done starting up!");
    }

    public void shutdown() {
        System.out.println(new Utils().getTime(Utils.Color.YELLOW) + "Cancelling shushes...");
        for (ScheduledFuture<?> task : schedules) task.cancel(false);
        for (ScheduledFuture<?> task : Utils.schedules) task.cancel(false);
        System.out.println(new Utils().getTime(Utils.Color.YELLOW) + "Shutting down...");
        // Close game channels
        for (TextChannel channel : api.getGuildById(729083627308056597L).getCategoryById(900747596245639238L)
            .getTextChannels())
            channel.sendMessage("Sorry, but I'm now shutting down. This channel will be deleted when I start back up.")
                .complete();
        try {
            api.shutdownNow();
        } catch (NoClassDefFoundError ignored) {
        }
    }
}