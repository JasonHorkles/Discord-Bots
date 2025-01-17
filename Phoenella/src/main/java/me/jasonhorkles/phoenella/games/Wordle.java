package me.jasonhorkles.phoenella.games;

import me.jasonhorkles.phoenella.GameManager;
import me.jasonhorkles.phoenella.Phoenella;
import me.jasonhorkles.phoenella.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Wordle extends ListenerAdapter {
    private static final ArrayList<String> wordList = new ArrayList<>();
    private static final HashMap<TextChannel, Boolean> daily = new HashMap<>();
    private static final HashMap<TextChannel, Boolean> isNonReal = new HashMap<>();
    private static final HashMap<TextChannel, Integer> attempt = new HashMap<>();
    private static final HashMap<TextChannel, Integer> maxTries = new HashMap<>();
    private static final HashMap<TextChannel, Long> originalMessage = new HashMap<>();
    private static final HashMap<TextChannel, Member> players = new HashMap<>();
    private static final HashMap<TextChannel, Message> keyboard = new HashMap<>();
    private static final HashMap<TextChannel, String> answers = new HashMap<>();
    private static final List<Long> wonDaily = new ArrayList<>();
    private static final Map<TextChannel, ArrayList<Message>> messages = new HashMap<>();
    private static final Map<TextChannel, ScheduledFuture<?>> deleteChannel = new HashMap<>();

    public String startDailyWordle(Member member) {
        File dailyFile = new File("Phoenella/Wordle/played-daily.txt");
        try {
            Scanner dailyPlays = new Scanner(dailyFile, StandardCharsets.UTF_8);
            ArrayList<String> plays = new ArrayList<>();
            while (dailyPlays.hasNextLine()) plays.add(dailyPlays.nextLine());
            dailyPlays.close();

            if (plays.toString().contains(member.getId())) return "You've already played today's Wordle!";
        } catch (IOException e) {
            System.out.print(new Utils().getTime(Utils.LogColor.RED));
            e.printStackTrace();
        }

        try {
            File dailyWord = new File("Phoenella/Wordle/daily.txt");
            Scanner input = new Scanner(dailyWord, StandardCharsets.UTF_8);
            String word = input.next();
            input.close();

            FileWriter fw = new FileWriter(dailyFile, StandardCharsets.UTF_8, true);
            fw.write(member.getId() + "\n");
            fw.close();

            TextChannel gameChannel = startGame(member, word, false, true, null);
            if (gameChannel == null)
                return "Either you already have an ongoing game with that word or you have too many games active at once!";
            else return "Game created in " + gameChannel.getAsMention();

        } catch (IOException e) {
            System.out.print(new Utils().getTime(Utils.LogColor.RED));
            e.printStackTrace();
            return "Couldn't generate a random word! Please try again later.";
        }
    }

    public @Nullable TextChannel startGame(Member player, @Nullable String answer, boolean isUserGenerated, boolean isDaily, @Nullable Integer tries) throws IOException {
        // Scan for too many channels
        int channelsWithName = 0;
        if (players.containsValue(player)) for (Member member : players.values())
            if (member == player) channelsWithName++;
        if (channelsWithName >= 3) return null;

        // Update words
        Scanner words = new Scanner(new File("Phoenella/Wordle/words.txt"), StandardCharsets.UTF_8);
        wordList.clear();
        while (words.hasNext()) try {
            wordList.add(words.next());
        } catch (NoSuchElementException ignored) {
        }
        words.close();

        if (answer == null || answer.equals("null")) {
            Random r = new Random();
            answer = wordList.get(r.nextInt(wordList.size()));
        }

        String obfuscatedAnswer;
        obfuscatedAnswer = UUID.nameUUIDFromBytes(answer.getBytes(StandardCharsets.UTF_8)).toString();

        // Scan thru for duplicates
        if (players.containsValue(player)) for (Map.Entry<TextChannel, Member> entry : players.entrySet())
            if (entry.getValue() == player) if (Objects.equals(entry.getKey().getTopic(), obfuscatedAnswer))
                return null;

        TextChannel channel = new GameManager().createChannel(
            GameManager.Game.WORDLE,
            new ArrayList<>(Collections.singleton(player)),
            isDaily);

        isNonReal.put(channel, isUserGenerated);
        players.put(channel, player);
        answers.put(channel, answer.toUpperCase());
        attempt.put(channel, 0);
        daily.put(channel, isDaily);

        channel.getManager().setTopic(obfuscatedAnswer).queue(
            null,
            new ErrorHandler().ignore(ErrorResponse.UNKNOWN_CHANNEL));

        if (tries == null) maxTries.put(channel, 6);
        else maxTries.put(channel, tries);

        Thread game = new Thread(
            () -> {
                ArrayList<Message> lines = new ArrayList<>();
                StringBuilder empties = new StringBuilder(140);
                empties.append("<:empty:1279501848780341399> ".repeat(answers.get(channel).length()));
                try {
                    for (int x = 0; x < maxTries.get(channel); x++)
                        lines.add(channel.sendMessage(empties).complete());
                    messages.put(channel, lines);

                    keyboard.put(
                        channel,
                        channel.sendMessage("~~==========================~~\n    " + getLetter(
                            'Q',
                            LetterType.NOT_GUESSED) + getLetter('W', LetterType.NOT_GUESSED) + getLetter(
                            'E',
                            LetterType.NOT_GUESSED) + getLetter('R', LetterType.NOT_GUESSED) + getLetter(
                            'T',
                            LetterType.NOT_GUESSED) + getLetter('Y', LetterType.NOT_GUESSED) + getLetter(
                            'U',
                            LetterType.NOT_GUESSED) + getLetter('I', LetterType.NOT_GUESSED) + getLetter(
                            'O',
                            LetterType.NOT_GUESSED) + getLetter(
                            'P',
                            LetterType.NOT_GUESSED) + "\n       " + getLetter(
                            'A',
                            LetterType.NOT_GUESSED) + getLetter('S', LetterType.NOT_GUESSED) + getLetter(
                            'D',
                            LetterType.NOT_GUESSED) + getLetter('F', LetterType.NOT_GUESSED) + getLetter(
                            'G',
                            LetterType.NOT_GUESSED) + getLetter('H', LetterType.NOT_GUESSED) + getLetter(
                            'J',
                            LetterType.NOT_GUESSED) + getLetter('K', LetterType.NOT_GUESSED) + getLetter(
                            'L',
                            LetterType.NOT_GUESSED) + "\n             " + getLetter(
                            'Z',
                            LetterType.NOT_GUESSED) + getLetter('X', LetterType.NOT_GUESSED) + getLetter(
                            'C',
                            LetterType.NOT_GUESSED) + getLetter('V', LetterType.NOT_GUESSED) + getLetter(
                            'B',
                            LetterType.NOT_GUESSED) + getLetter('N', LetterType.NOT_GUESSED) + getLetter(
                            'M',
                            LetterType.NOT_GUESSED)).complete());

                    channel.sendMessage(player.getAsMention()).queue(del -> del.delete().queueAfter(
                        100,
                        TimeUnit.MILLISECONDS,
                        null,
                        new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE)));

                    channel.upsertPermissionOverride(player).setAllowed(
                        Permission.MESSAGE_SEND,
                        Permission.VIEW_CHANNEL).queue();
                } catch (ErrorResponseException ignored) {
                }
            }, "Create Wordle - " + new Utils().getFirstName(player));
        game.start();

        return channel;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.getMessage().isFromGuild()) return;
        if (!event.getChannel().getName().endsWith("wordle")) return;
        if (event.getChannel().asTextChannel().getParentCategoryIdLong() != 900747596245639238L) return;
        if (event.getAuthor().isBot()) return;

        TextChannel channel = event.getChannel().asTextChannel();

        //noinspection DataFlowIssue
        if (event.getMember().getIdLong() != players.get(channel).getIdLong()) {
            event.getMessage().delete().queueAfter(150, TimeUnit.MILLISECONDS);
            return;
        }

        Message message = event.getMessage();
        String input = message.getContentStripped().replaceAll("[^a-zA-Z]", "").toUpperCase();
        String answer = answers.get(channel);

        if (input.length() != answer.length()) {
            message.reply("Invalid length!").queue(del -> del.delete().queueAfter(3, TimeUnit.SECONDS));
            message.delete().queueAfter(150, TimeUnit.MILLISECONDS);
            return;
        }

        if (new Utils().containsBadWord(input)) {
            message.reply("Nope not doing that").queue(del -> del.delete().queueAfter(3, TimeUnit.SECONDS));
            message.delete().queueAfter(150, TimeUnit.MILLISECONDS);
            return;
        }

        if (!isNonReal.get(channel)) if (!wordList.toString().contains(input)) try {
            URL url = new URI("https://api.dictionaryapi.dev/api/v2/entries/en/" + input.toLowerCase()).toURL();
            try (InputStream ignored1 = url.openStream()) {
                wordRequest(input.toUpperCase(), event.getMember());
            } catch (FileNotFoundException ignored) {
                message.reply("**" + input + "** isn't in the dictionary!").queue(del -> del.delete()
                    .queueAfter(4, TimeUnit.SECONDS));
                message.delete().queueAfter(150, TimeUnit.MILLISECONDS);
                return;
            }

        } catch (IOException | URISyntaxException e) {
            System.out.print(new Utils().getTime(Utils.LogColor.RED));
            e.printStackTrace();

            message.reply("<@277291758503723010> `" + e + "`").queue();
            message.delete().queueAfter(150, TimeUnit.MILLISECONDS);
            return;
        }

        message.delete().queueAfter(150, TimeUnit.MILLISECONDS);

        List<Character> answerChars = new ArrayList<>(answer.chars().mapToObj(c -> (char) c).toList());
        ArrayList<Character> inputChars = new ArrayList<>(input.chars().mapToObj(c -> (char) c).toList());
        List<String> output = new ArrayList<>();

        // Gray / Incorrect - Word
        for (Character character : inputChars) output.add(getLetter(character, LetterType.WRONG));

        String newKeyboard = keyboard.get(channel).getContentRaw();

        // Green / Correct
        for (int index = 0; index < answerChars.size(); index++)
            if (answerChars.get(index) == inputChars.get(index)) {
                String letter = getLetter(inputChars.get(index), LetterType.CORRECT);

                // Replace dark gray and yellow keys
                newKeyboard = newKeyboard.replace(
                    getLetter(inputChars.get(index), LetterType.IN_WORD),
                    letter).replace(getLetter(inputChars.get(index), LetterType.NOT_GUESSED), letter);

                output.set(index, letter);
                answerChars.set(index, '-');
                inputChars.set(index, '-');
            }

        // Yellow / In-Word
        for (int index = 0; index < answerChars.size(); index++) {
            if (inputChars.get(index) == '-') continue;
            if (answerChars.contains(inputChars.get(index))) {
                String letter = getLetter(inputChars.get(index), LetterType.IN_WORD);

                // Replace dark gray keys
                newKeyboard = newKeyboard.replace(
                    getLetter(inputChars.get(index), LetterType.NOT_GUESSED),
                    letter);

                output.set(index, letter);
                answerChars.set(answerChars.indexOf(inputChars.get(index)), '-');
                inputChars.set(inputChars.indexOf(inputChars.get(index)), '-');
            }
        }

        // Gray / Incorrect - Keyboard
        for (int index = 0; index < answerChars.size(); index++) {
            if (inputChars.get(index) == '-') continue;
            String letter = getLetter(inputChars.get(index), LetterType.WRONG);

            // Replace dark gray keys
            newKeyboard = newKeyboard.replace(
                getLetter(inputChars.get(index), LetterType.NOT_GUESSED),
                letter);
        }

        StringBuilder builder = new StringBuilder(125);
        for (String character : output) {
            builder.append(character);
            builder.append(" ");
        }

        messages.get(channel).get(attempt.get(channel)).editMessage(builder).queue();
        attempt.put(channel, attempt.get(channel) + 1);
        String finalNewKeyboard = newKeyboard;
        Thread updateKeyboard = new Thread(
            () -> keyboard.put(
                channel,
                keyboard.get(channel).editMessage(finalNewKeyboard).timeout(15, TimeUnit.SECONDS).complete()),
            "Update Wordle Keyboard - " + new Utils().getFirstName(players.get(channel)));
        updateKeyboard.start();

        // Win
        if (input.equals(answer)) {
            // Is user-generated
            if (isNonReal.get(channel))
                //noinspection DataFlowIssue
                event.getJDA().getTextChannelById(956267174727671869L)
                    .retrieveMessageById(originalMessage.get(channel)).queue(original -> {
                        MessageEmbed embed = original.getEmbeds().getFirst();
                        // Add 1 to wins
                        if (!embed.isEmpty()) {
                            //noinspection DataFlowIssue
                            int wins = Integer.parseInt(embed.getFields().get(1).getValue()) + 1;

                            EmbedBuilder newEmbed = getEmbedBuilder(
                                embed,
                                embed.getFields().get(0).getValue(),
                                String.valueOf(wins),
                                embed.getFields().get(2).getValue());

                            original.editMessageEmbeds(newEmbed.build()).queue();
                        }
                    });

                // Add to the leaderboard if not user-generated
            else if (!Phoenella.localWordleBoard) try {
                File leaderboardFile = new File("Phoenella/Wordle/leaderboard.txt");
                Scanner leaderboard = new Scanner(leaderboardFile, StandardCharsets.UTF_8);
                List<String> lines = new ArrayList<>();

                int index = 0;
                int memberAtIndex = -1;
                while (leaderboard.hasNextLine()) try {
                    String line = leaderboard.nextLine();
                    lines.add(line);
                    if (line.contains(event.getMember().getId())) memberAtIndex = index;
                    index++;
                } catch (NoSuchElementException ignored) {
                }
                leaderboard.close();

                // Calculate score
                int score = 0;
                switch (attempt.get(channel)) {
                    case 6 -> score = 1;
                    case 5 -> score = 2;
                    case 4 -> score = 3;
                    case 3 -> score = 4;
                    case 2 -> score = 5;
                    case 1 -> score = 6;
                }
                if (daily.get(channel)) score *= 2;

                FileWriter writer = new FileWriter(leaderboardFile, StandardCharsets.UTF_8, false);
                if (memberAtIndex == -1) {
                    lines.add(event.getMember().getId() + ":" + score);

                    for (String line : lines) writer.write(line + "\n");
                } else {
                    score += Integer.parseInt(lines.get(memberAtIndex).replaceFirst(".*:", ""));
                    lines.set(memberAtIndex, event.getMember().getId() + ":" + score);

                    for (String line : lines) writer.write(line + "\n");
                }
                writer.close();
            } catch (IOException e) {
                System.out.print(new Utils().getTime(Utils.LogColor.RED));
                e.printStackTrace();
            }

            if (daily.get(channel)) {
                Long id = event.getMember().getIdLong();
                wonDaily.add(id);
                Executors.newSingleThreadScheduledExecutor().schedule(
                    () -> wonDaily.remove(id),
                    1,
                    TimeUnit.MINUTES);
            }

            sendRetryMsg(channel, "Well done!", answer, false);
        }

        // Fail
        else if (attempt.get(channel).equals(maxTries.get(channel))) {
            if (isNonReal.get(channel))
                //noinspection DataFlowIssue
                event.getJDA().getTextChannelById(956267174727671869L)
                    .retrieveMessageById(originalMessage.get(channel)).queue(original -> {
                        MessageEmbed embed = original.getEmbeds().getFirst();
                        // Add 1 to fails
                        if (!embed.isEmpty()) {
                            //noinspection DataFlowIssue
                            int fails = Integer.parseInt(embed.getFields().get(2).getValue()) + 1;

                            EmbedBuilder newEmbed = getEmbedBuilder(
                                embed,
                                embed.getFields().get(0).getValue(),
                                embed.getFields().get(1).getValue(),
                                String.valueOf(fails));

                            original.editMessageEmbeds(newEmbed.build()).queue();
                        }
                    });

            sendRetryMsg(channel, "The word was **" + answer.toLowerCase() + "**!", answer, false);
        }
    }

    @SuppressWarnings("DataFlowIssue")
    @NotNull
    private static EmbedBuilder getEmbedBuilder(MessageEmbed embed, String embed1, String embed2, String fails) {
        EmbedBuilder newEmbed = new EmbedBuilder(embed);
        newEmbed.clearFields();
        newEmbed.addField(embed.getFields().get(0).getName(), embed1, true);
        newEmbed.addField(embed.getFields().get(1).getName(), embed2, true);
        newEmbed.addField(embed.getFields().get(2).getName(), fails, true);
        return newEmbed;
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        switch (event.getComponentId()) {
            case "endgame:wordle" -> {
                event.editButton(event.getButton().asDisabled()).queue();

                sendRetryMsg(
                    event.getChannel().asTextChannel(),
                    "The word was **" + answers.get(event.getChannel().asTextChannel()).toLowerCase() + "**!",
                    answers.get(event.getChannel().asTextChannel()),
                    true);
            }

            case "restartgame:wordle" -> {
                event.deferReply().queue();

                try {
                    TextChannel gameChannel = new Wordle().startGame(
                        event.getMember(),
                        null,
                        false,
                        false,
                        null);
                    if (gameChannel == null) event.getHook().editOriginal(
                            "Either you already have an ongoing game with that word or you have too many games active at once!")
                        .queue();
                    else
                        event.getHook().editOriginal("Game created in " + gameChannel.getAsMention()).queue();
                } catch (IOException e) {
                    event.getHook().editOriginal("Couldn't generate a random word! Please try again later.")
                        .queue();
                    System.out.print(new Utils().getTime(Utils.LogColor.RED));
                    e.printStackTrace();
                }

                new Thread(
                    () -> {
                        try (ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor()) {
                            executor.schedule(
                                () -> endGame(event.getChannel().asTextChannel()),
                                10,
                                TimeUnit.SECONDS);
                        }
                    }, "End Game-Wordle").start();
            }

            case "sharewordlescore" -> new Thread(
                () -> {
                    TextChannel channel = event.getChannel().asTextChannel();
                    event.editButton(event.getButton().asDisabled()).complete();

                    StringBuilder attempts = new StringBuilder();
                    for (Message msg : messages.get(channel)) {
                        String content = channel.retrieveMessageById(msg.getId()).complete().getContentRaw();
                        if (content.startsWith("<:empty:1279501848780341399>")) break;
                        attempts.append(content.replaceAll("[<>:0-9]", "").replaceAll(".(?= )|.$| ", "")
                            .toUpperCase().replace("W", "<:wordle_incorrect:1284188510588506197>")
                            .replace("I", "<:wordle_in_word:1284188488484392962>")
                            .replace("C", "<:wordle_correct:1284188462706200647>")).append("\n");
                    }

                    // Testing channel
                    //                TextChannel winChannel = event.getGuild().getTextChannelById(960213547944661042L);
                    //noinspection DataFlowIssue
                    TextChannel winChannel = event.getGuild().getTextChannelById(956267174727671869L);
                    String name = "**" + new Utils().getFullName(event.getMember()) + "**";

                    Button button = Button.secondary("dailywordle", "Play daily Wordle")
                        .withEmoji(Emoji.fromUnicode("📅"));

                    if (wonDaily.contains(event.getUser().getIdLong())) {
                        //noinspection DataFlowIssue
                        winChannel.sendMessage(name + " just finished the daily Wordle in **" + attempt.get(
                                channel) + "** tries!\n" + attempts).addComponents(ActionRow.of(button))
                            .complete();
                        wonDaily.remove(event.getUser().getIdLong());

                    } else //noinspection DataFlowIssue
                        winChannel.sendMessage(name + " failed the daily Wordle!\n" + attempts).addComponents(
                            ActionRow.of(button)).complete();
                }, "Share Wordle Score - " + new Utils().getFirstName(event.getMember())).start();

            case "dailywordle" -> {
                event.deferReply(true).queue();
                event.getHook().editOriginal(startDailyWordle(event.getMember())).queue();
            }
        }

        if (event.getComponentId().startsWith("playwordle:")) {
            String word = event.getComponentId().replace("playwordle:", "").replaceFirst(":.*", "");
            int tries = Integer.parseInt(event.getComponentId().replaceAll(".*:", ""));

            event.reply("Creating a game...").setEphemeral(true).queue();

            try {
                TextChannel gameChannel = new Wordle().startGame(event.getMember(), word, true, false, tries);
                if (gameChannel == null) event.getHook().editOriginal(
                        "Either you already have an ongoing game with that word or you have too many games active at once!")
                    .queue();

                else {
                    event.getHook().editOriginal("Game created in " + gameChannel.getAsMention()).queue();
                    MessageEmbed message = event.getMessage().getEmbeds().getFirst();
                    // Add 1 to plays
                    if (!message.isEmpty()) {
                        //noinspection DataFlowIssue
                        int plays = Integer.parseInt(message.getFields().get(0).getValue()) + 1;

                        EmbedBuilder embed = getEmbedBuilder(
                            message,
                            String.valueOf(plays),
                            message.getFields().get(1).getValue(),
                            message.getFields().get(2).getValue());

                        event.getMessage().editMessageEmbeds(embed.build()).queue();

                        originalMessage.put(gameChannel, event.getMessage().getIdLong());
                    }
                }

            } catch (IOException e) {
                event.getHook().editOriginal("Couldn't generate a random word! Please try again later.")
                    .queue();
                System.out.print(new Utils().getTime(Utils.LogColor.RED));
                e.printStackTrace();
            }
            return;
        }

        if (event.getComponentId().startsWith("reportword:")) {
            event.editButton(event.getButton().asDisabled()).queue();

            String word = event.getComponentId().replace("reportword:", "");

            //noinspection DataFlowIssue
            event.getJDA().getTextChannelById(960213547944661042L)
                .sendMessage(":warning: Word report from " + new Utils().getFullName(event.getMember()) + ": **" + word + "**")
                .setActionRow(Button.primary("defineword:" + word, "Define word")
                    .withEmoji(Emoji.fromUnicode("❔"))).queue(msg -> msg.addReaction(Emoji.fromUnicode("👌"))
                    .queue(na -> msg.addReaction(Emoji.fromUnicode("🗑️")).queueAfter(1, TimeUnit.SECONDS)));
        }

        if (event.getComponentId().startsWith("defineword:")) {
            event.deferReply(true).queue();

            String word = event.getComponentId().replace("defineword:", "");
            MessageEmbed embed = new Utils().defineWord(word);

            if (embed.getDescription() != null)
                if (embed.getDescription().startsWith("Couldn't find ")) event.getHook().editOriginalEmbeds(
                    embed).queue();

                else event.getHook().editOriginalEmbeds(embed).setActionRow(Button
                        .danger("definitionreport", "Report definition").withEmoji(Emoji.fromUnicode("🚩")))
                    .queue();
        }
    }

    private void endGame(TextChannel channel) {
        answers.remove(channel);
        attempt.remove(channel);
        daily.remove(channel);
        isNonReal.remove(channel);
        keyboard.remove(channel);
        maxTries.remove(channel);
        messages.remove(channel);
        originalMessage.remove(channel);
        players.remove(channel);
        if (deleteChannel.get(channel) != null) {
            deleteChannel.get(channel).cancel(true);
            deleteChannel.remove(channel);
        }
        new GameManager().deleteGame(channel);
    }

    private void sendRetryMsg(TextChannel channel, String message, String answer, boolean gaveUp) {
        channel.upsertPermissionOverride(players.get(channel)).setDenied(Permission.MESSAGE_SEND).queue();

        List<ItemComponent> buttons = new ArrayList<>();
        if (!isNonReal.get(channel)) {
            buttons.add(Button.danger("reportword:" + answer, "Report word")
                .withEmoji(Emoji.fromUnicode("🚩")));
            buttons.add(Button.primary("defineword:" + answer, "Define word")
                .withEmoji(Emoji.fromUnicode("❔")));
        }
        buttons.add(Button.success("restartgame:wordle", "New word").withEmoji(Emoji.fromUnicode("🔁")));
        if (daily.get(channel) && !gaveUp) buttons.add(Button.secondary("sharewordlescore", "Share score")
            .withEmoji(Emoji.fromUnicode("📤")));

        channel.sendMessage(message).setActionRow(buttons).queue();

        new Thread(
            () -> {
                try (ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor()) {
                    deleteChannel.put(
                        channel,
                        executor.schedule(() -> new Wordle().endGame(channel), 45, TimeUnit.SECONDS));
                }
            }, "Delete Channel").start();
    }

    private void wordRequest(String word, Member member) {
        List<String> words = new ArrayList<>();
        try {
            Scanner fileScanner = new Scanner(
                new File("Phoenella/Wordle/banned-requests.txt"),
                StandardCharsets.UTF_8);
            while (fileScanner.hasNextLine()) words.add(fileScanner.nextLine());
            fileScanner.close();
        } catch (NoSuchElementException ignored) {
        } catch (IOException e) {
            System.out.print(new Utils().getTime(Utils.LogColor.RED));
            e.printStackTrace();
        }
        if (words.contains(word.toUpperCase())) return;

        //noinspection DataFlowIssue
        Phoenella.jda.getTextChannelById(960213547944661042L).sendMessage(
                ":inbox_tray: Auto word request from " + new Utils().getFullName(member) + ": **" + word + "**")
            .setActionRow(Button.primary("defineword:" + word, "Define word")
                .withEmoji(Emoji.fromUnicode("❔"))).queue((msg) -> msg.addReaction(Emoji.fromUnicode("✅"))
                .queue(m -> msg.addReaction(Emoji.fromUnicode("⛔")).queueAfter(1, TimeUnit.SECONDS)));
    }

    private enum LetterType {
        WRONG, IN_WORD, CORRECT, NOT_GUESSED
    }

    private String getLetter(Character letter, LetterType letterType) {
        switch (letterType) {
            case CORRECT -> {
                return getCorrectLetter(letter);
            }

            case IN_WORD -> {
                return getInWordLetter(letter);
            }

            case WRONG -> {
                return getWrongLetter(letter);
            }

            case NOT_GUESSED -> {
                return getNotGuessedLetter(letter);
            }
        }
        return "<@277291758503723010> | Char: `" + letter + "` | LetterType: `" + letterType + "`";
    }

    private String getCorrectLetter(Character letter) {
        switch (letter) {
            case 'A' -> {
                return "<:ca:1279501104136061058>";
            }
            case 'B' -> {
                return "<:cb:1279501247665406124>";
            }
            case 'C' -> {
                return "<:cc:1279501295354646581>";
            }
            case 'D' -> {
                return "<:cd:1279501301830389831>";
            }
            case 'E' -> {
                return "<:ce:1279501308629618800>";
            }
            case 'F' -> {
                return "<:cf:1279501317945032787>";
            }
            case 'G' -> {
                return "<:cg:1279501325066965072>";
            }
            case 'H' -> {
                return "<:ch:1279501331819659409>";
            }
            case 'I' -> {
                return "<:ci:1279501338715230289>";
            }
            case 'J' -> {
                return "<:cj:1279501345413398561>";
            }
            case 'K' -> {
                return "<:ck:1279501351558057994>";
            }
            case 'L' -> {
                return "<:cl:1279501358206029885>";
            }
            case 'M' -> {
                return "<:cm:1279501364547813539>";
            }
            case 'N' -> {
                return "<:cn:1279501370541740173>";
            }
            case 'O' -> {
                return "<:co:1279501376724008970>";
            }
            case 'P' -> {
                return "<:cp:1279501389050941575>";
            }
            case 'Q' -> {
                return "<:cq:1279501396118343750>";
            }
            case 'R' -> {
                return "<:cr:1279501402888212560>";
            }
            case 'S' -> {
                return "<:cs:1279501410131644486>";
            }
            case 'T' -> {
                return "<:ct:1279501417157230612>";
            }
            case 'U' -> {
                return "<:cu:1279501424765567017>";
            }
            case 'V' -> {
                return "<:cv:1279501432176902174>";
            }
            case 'W' -> {
                return "<:cw:1279501439365812406>";
            }
            case 'X' -> {
                return "<:cx:1279501446760628345>";
            }
            case 'Y' -> {
                return "<:cy:1279501454163312732>";
            }
            case 'Z' -> {
                return "<:cz:1279501463218819122>";
            }
        }

        return String.valueOf(letter);
    }

    private String getInWordLetter(Character letter) {
        switch (letter) {
            case 'A' -> {
                return "<:ia:1279501576435925064>";
            }
            case 'B' -> {
                return "<:ib:1279501584052785284>";
            }
            case 'C' -> {
                return "<:ic:1279501591681962035>";
            }
            case 'D' -> {
                return "<:id:1279501598321807371>";
            }
            case 'E' -> {
                return "<:ie:1279501677539364998>";
            }
            case 'F' -> {
                return "<:if:1279501683734478911>";
            }
            case 'G' -> {
                return "<:ig:1279501690805948456>";
            }
            case 'H' -> {
                return "<:ih:1279501697814757427>";
            }
            case 'I' -> {
                return "<:ii:1279501706173878362>";
            }
            case 'J' -> {
                return "<:ij:1279501713639870465>";
            }
            case 'K' -> {
                return "<:ik:1279501725874520195>";
            }
            case 'L' -> {
                return "<:il:1279501742345551923>";
            }
            case 'M' -> {
                return "<:im:1279501748104593568>";
            }
            case 'N' -> {
                return "<:in:1279501753636622479>";
            }
            case 'O' -> {
                return "<:io:1279501759907102750>";
            }
            case 'P' -> {
                return "<:ip:1279501766144036914>";
            }
            case 'Q' -> {
                return "<:iq:1279501772733288528>";
            }
            case 'R' -> {
                return "<:ir:1279501779066818560>";
            }
            case 'S' -> {
                return "<:is:1279501784301441097>";
            }
            case 'T' -> {
                return "<:it:1279501790479646890>";
            }
            case 'U' -> {
                return "<:iu:1279501796372385793>";
            }
            case 'V' -> {
                return "<:iv:1279501803070820442>";
            }
            case 'W' -> {
                return "<:iw:1279501809735700624>";
            }
            case 'X' -> {
                return "<:ix:1279501815964110879>";
            }
            case 'Y' -> {
                return "<:iy:1279501822960074862>";
            }
            case 'Z' -> {
                return "<:iz:1279501829956440200>";
            }
        }

        return String.valueOf(letter);
    }

    private String getWrongLetter(Character letter) {
        switch (letter) {
            case 'A' -> {
                return "<:wa:1279502052367794237>";
            }
            case 'B' -> {
                return "<:wb:1279502059732860939>";
            }
            case 'C' -> {
                return "<:wc:1279502064891859099>";
            }
            case 'D' -> {
                return "<:wd:1279502071283843103>";
            }
            case 'E' -> {
                return "<:we:1279502077856452710>";
            }
            case 'F' -> {
                return "<:wf:1279502085578031279>";
            }
            case 'G' -> {
                return "<:wg:1279502093748801692>";
            }
            case 'H' -> {
                return "<:wh:1279502101403271332>";
            }
            case 'I' -> {
                return "<:wi:1279502108306964664>";
            }
            case 'J' -> {
                return "<:wj:1279502113524678666>";
            }
            case 'K' -> {
                return "<:wk:1279502119019348029>";
            }
            case 'L' -> {
                return "<:wl:1279502125583568908>";
            }
            case 'M' -> {
                return "<:wm:1279502131056869480>";
            }
            case 'N' -> {
                return "<:wn:1279502137499455498>";
            }
            case 'O' -> {
                return "<:wo:1279502143489052816>";
            }
            case 'P' -> {
                return "<:wp:1279502149914726601>";
            }
            case 'Q' -> {
                return "<:wq:1279502157107822692>";
            }
            case 'R' -> {
                return "<:wr:1279502163436900553>";
            }
            case 'S' -> {
                return "<:ws:1279502169195942102>";
            }
            case 'T' -> {
                return "<:wt:1279502176514871409>";
            }
            case 'U' -> {
                return "<:wu:1279502182579966025>";
            }
            case 'V' -> {
                return "<:wv:1279502189798232074>";
            }
            case 'W' -> {
                return "<:ww:1279502197142327308>";
            }
            case 'X' -> {
                return "<:wx:1279502206508204174>";
            }
            case 'Y' -> {
                return "<:wy:1279502213323948102>";
            }
            case 'Z' -> {
                return "<:wz:1279502220131438612>";
            }
        }

        return String.valueOf(letter);
    }

    private String getNotGuessedLetter(Character letter) {
        switch (letter) {
            case 'A' -> {
                return "<:na:1279501865624670208>";
            }
            case 'B' -> {
                return "<:nb:1279501873132605594>";
            }
            case 'C' -> {
                return "<:nc:1279501880757715056>";
            }
            case 'D' -> {
                return "<:nd:1279501887430725692>";
            }
            case 'E' -> {
                return "<:ne:1279501893504073749>";
            }
            case 'F' -> {
                return "<:nf:1279501899820962016>";
            }
            case 'G' -> {
                return "<:ng:1279501906183589928>";
            }
            case 'H' -> {
                return "<:nh:1279501913930469406>";
            }
            case 'I' -> {
                return "<:ni:1279501920935088128>";
            }
            case 'J' -> {
                return "<:nj:1279501927410958376>";
            }
            case 'K' -> {
                return "<:nk:1279501933882900631>";
            }
            case 'L' -> {
                return "<:nl:1279501939712720928>";
            }
            case 'M' -> {
                return "<:nm:1279501947019460770>";
            }
            case 'N' -> {
                return "<:nn:1279501953281425541>";
            }
            case 'O' -> {
                return "<:no:1279501960088916021>";
            }
            case 'P' -> {
                return "<:np:1279501967416098927>";
            }
            case 'Q' -> {
                return "<:nq:1279501974294761554>";
            }
            case 'R' -> {
                return "<:nr:1279501981085601903>";
            }
            case 'S' -> {
                return "<:ns:1279501987188314194>";
            }
            case 'T' -> {
                return "<:nt:1279501993827635290>";
            }
            case 'U' -> {
                return "<:nu:1279502000400371722>";
            }
            case 'V' -> {
                return "<:nv:1279502006758674473>";
            }
            case 'W' -> {
                return "<:nw:1279502013016576075>";
            }
            case 'X' -> {
                return "<:nx:1279502019316416564>";
            }
            case 'Y' -> {
                return "<:ny:1279502025343893649>";
            }
            case 'Z' -> {
                return "<:nz:1279502031278702629>";
            }
        }

        return String.valueOf(letter);
    }
}
