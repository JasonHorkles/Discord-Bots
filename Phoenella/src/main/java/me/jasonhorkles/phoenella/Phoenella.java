package me.jasonhorkles.phoenella;

import me.jasonhorkles.phoenella.games.RPS;
import me.jasonhorkles.phoenella.games.Wordle;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@SuppressWarnings({"DataFlowIssue"})
public class Phoenella {
    private static final ArrayList<ScheduledFuture<?>> schedules = new ArrayList<>();
    public static boolean localWordleBoard = false;
    public static JDA jda;

    public static void main(String[] args) throws InterruptedException, IOException, ParseException {
        System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Starting...");

        JDABuilder builder = JDABuilder.createDefault(new Secrets().getBotToken());
        builder.disableCache(CacheFlag.ACTIVITY);
        builder.enableCache(CacheFlag.VOICE_STATE);
        builder.enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGES,
            GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.MESSAGE_CONTENT);
        builder.setMemberCachePolicy(MemberCachePolicy.ALL);
        builder.setEnableShutdownHook(false);
        builder.setStatus(OnlineStatus.ONLINE);
        builder.setActivity(Activity.playing("Wordle"));
        builder.addEventListeners(new Events(), new GameManager(), new RPS(), new Wordle());
        jda = builder.build();

        jda.awaitReady();

        jda.addEventListener(new Nicknames());

        // Cache members
        jda.getGuildById(729083627308056597L).loadMembers().get();

        jda.getGuildById(729083627308056597L).updateCommands().addCommands(Commands.slash("wordle", "Wordle!")
            .addSubcommands(new SubcommandData("play", "Play with a random word"),
                new SubcommandData("create", "Create a Wordle for others to play"),
                new SubcommandData("leaderboard", "View the Wordle leaderboard").addOption(OptionType.BOOLEAN,
                    "show", "Show the leaderboard message publicly?", false),
                new SubcommandData("daily", "Play the daily Wordle"))).queue();

        // Guild nickname check
        System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + "Starting nickname check...");
        new Nicknames().runNameCheckForGuild(true);

        // Scan Wordle leaderboard for nonexistent players
        System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + "Starting leaderboard check...");
        File leaderboardFile = new File("Phoenella/Wordle/leaderboard.txt");
        Scanner leaderboard = new Scanner(leaderboardFile);
        ArrayList<String> lines = new ArrayList<>();

        while (leaderboard.hasNextLine()) try {
            lines.add(leaderboard.nextLine());
        } catch (NoSuchElementException ignored) {
        }

        if (!lines.isEmpty()) if (lines.get(0).equalsIgnoreCase("local")) {
            System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + "Leaderboard set to local mode!");
            localWordleBoard = true;
        }

        if (!localWordleBoard) {
            FileWriter lbWriter = new FileWriter(leaderboardFile, false);
            boolean dontDoCheck = false;

            if (LocalDate.now().getDayOfMonth() == 1) {
                File lastClearedFile = new File("Phoenella/Wordle/last-cleared-leaderboard.txt");
                int month = new Scanner(lastClearedFile).nextInt();
                if (month != LocalDate.now().getMonthValue()) {
                    FileWriter lastCleared = new FileWriter(lastClearedFile, false);
                    lastCleared.write(String.valueOf(LocalDate.now().getMonthValue()));
                    lastCleared.close();

                    System.out.println(new Utils().getTime(
                        Utils.LogColor.YELLOW) + "Clearing the leaderboard for the new month!");
                    lbWriter.close();

                    dontDoCheck = true;
                }
            }

            if (!dontDoCheck) {
                for (String line : lines) {
                    long id = Long.parseLong(line.replaceFirst(":.*", ""));
                    Member member = jda.getGuildById(729083627308056597L).getMemberById(id);
                    if (member == null) {
                        System.out.println(
                            new Utils().getTime(Utils.LogColor.YELLOW) + "Removing user with ID " + id);
                        continue;
                    }

                    lbWriter.write(line + "\n");
                }

                lbWriter.close();
            }
        }

        System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + "Leaderboard check complete!");

        // Remove duplicate words from Wordle list
        System.out.println(
            new Utils().getTime(Utils.LogColor.GREEN) + "Removing duplicate Wordle entries...");
        
        // Allowed words
        File wordsFile = new File("Phoenella/Wordle/words.txt");
        Scanner wordScanner = new Scanner(wordsFile);

        ArrayList<String> originalWordList = new ArrayList<>();
        while (wordScanner.hasNextLine())
            if (wordScanner.hasNextLine()) originalWordList.add(wordScanner.nextLine());
        HashSet<String> wordList = new HashSet<>(originalWordList);

        int duplicates = originalWordList.size() - wordList.size();
        System.out.println(
            new Utils().getTime(Utils.LogColor.GREEN) + "Removed " + duplicates + " duplicate words!");
        originalWordList.clear();

        FileWriter wordWriter = new FileWriter(wordsFile, false);
        for (String word : wordList) wordWriter.write(word + "\n");
        wordWriter.close();
        wordList.clear();

        // Banned words
        wordsFile = new File("Phoenella/Wordle/banned-requests.txt");
        wordScanner = new Scanner(wordsFile);

        originalWordList = new ArrayList<>();
        while (wordScanner.hasNextLine())
            if (wordScanner.hasNextLine()) originalWordList.add(wordScanner.nextLine());
        wordList = new HashSet<>(originalWordList);

        duplicates = originalWordList.size() - wordList.size();
        System.out.println(
            new Utils().getTime(Utils.LogColor.GREEN) + "Removed " + duplicates + " duplicate banned words!");
        originalWordList.clear();

        wordWriter = new FileWriter(wordsFile, false);
        for (String word : wordList) wordWriter.write(word + "\n");
        wordWriter.close();
        wordList.clear();

        System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + "Duplicate word check complete!");

        // Schedule daily Wordle
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd h:mm a");
        Calendar future = Calendar.getInstance();
        future.setTime(format.parse(LocalDate.now() + " 12:00 AM"));
        future.add(Calendar.DAY_OF_MONTH, 1);

        long delay = future.getTimeInMillis() - System.currentTimeMillis();

        if (delay >= 0) new Thread(() -> {
            try (ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor()) {
                schedules.add(
                    executor.schedule(() -> new Utils().updateDailyWordle(), delay, TimeUnit.MILLISECONDS));
                System.out.println(
                    new Utils().getTime(Utils.LogColor.GREEN) + "Scheduled new daily Wordle in " + Math.round(
                        delay / 3600000.0) + " hours.");
            }
        }, "Daily Wordle").start();

        // Delete game channels
        for (TextChannel channel : jda.getCategoryById(900747596245639238L).getTextChannels())
            channel.delete().queue();

        // Add shutdown hooks
        Runtime.getRuntime().addShutdownHook(new Thread(() -> new Phoenella().shutdown(), "Shutdown Hook"));
        new Thread(() -> {
            while (true) {
                Scanner in = new Scanner(System.in);
                String text = in.nextLine();
                if (text.equalsIgnoreCase("stop")) System.exit(0);
                if (text.equalsIgnoreCase("dailywordle")) new Utils().updateDailyWordle();
            }
        }, "Console Input").start();

        Runtime.getRuntime().gc();

        System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + "Done starting up!");
    }

    public void shutdown() {
        for (ScheduledFuture<?> task : schedules) task.cancel(false);
        System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Shutting down...");
        // Close game channels
        for (TextChannel channel : jda.getGuildById(729083627308056597L).getCategoryById(900747596245639238L)
            .getTextChannels())
            channel.sendMessage(
                    "Sorry, but I'm now shutting down. This channel will be deleted when I start back up.")
                .complete();
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