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
    private static final Map<TextChannel, ArrayList<Message>> messages = new HashMap<>();
    private static final HashMap<TextChannel, Boolean> daily = new HashMap<>();
    private static final HashMap<TextChannel, Boolean> isNonReal = new HashMap<>();
    private static final HashMap<TextChannel, Integer> attempt = new HashMap<>();
    private static final HashMap<TextChannel, Integer> maxTries = new HashMap<>();
    private static final HashMap<TextChannel, Long> originalMessage = new HashMap<>();
    private static final HashMap<TextChannel, Member> players = new HashMap<>();
    private static final HashMap<TextChannel, Message> keyboard = new HashMap<>();
    private static final Map<TextChannel, ScheduledFuture<?>> deleteChannel = new HashMap<>();
    private static final HashMap<TextChannel, String> answers = new HashMap<>();

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

        TextChannel channel = new GameManager().createChannel(GameManager.Game.WORDLE,
            new ArrayList<>(Collections.singleton(player)),
            isDaily);

        isNonReal.put(channel, isUserGenerated);
        players.put(channel, player);
        answers.put(channel, answer.toUpperCase());
        attempt.put(channel, 0);
        daily.put(channel, isDaily);

        channel.getManager().setTopic(obfuscatedAnswer).queue(null,
            new ErrorHandler().ignore(ErrorResponse.UNKNOWN_CHANNEL));

        if (tries == null) maxTries.put(channel, 6);
        else maxTries.put(channel, tries);

        Thread game = new Thread(() -> {
            ArrayList<Message> lines = new ArrayList<>();
            StringBuilder empties = new StringBuilder(140);
            empties.append("<:empty:959950240516046868> ".repeat(answers.get(channel).length()));
            try {
                for (int x = 0; x < maxTries.get(channel); x++)
                    lines.add(channel.sendMessage(empties).complete());
                messages.put(channel, lines);

                keyboard.put(channel,
                    channel.sendMessage("~~==========================~~\n    " + getLetter('Q',
                        LetterType.NOT_GUESSED) + getLetter('W', LetterType.NOT_GUESSED) + getLetter('E',
                        LetterType.NOT_GUESSED) + getLetter('R', LetterType.NOT_GUESSED) + getLetter('T',
                        LetterType.NOT_GUESSED) + getLetter('Y', LetterType.NOT_GUESSED) + getLetter('U',
                        LetterType.NOT_GUESSED) + getLetter('I', LetterType.NOT_GUESSED) + getLetter('O',
                        LetterType.NOT_GUESSED) + getLetter('P',
                        LetterType.NOT_GUESSED) + "\n       " + getLetter('A',
                        LetterType.NOT_GUESSED) + getLetter('S', LetterType.NOT_GUESSED) + getLetter('D',
                        LetterType.NOT_GUESSED) + getLetter('F', LetterType.NOT_GUESSED) + getLetter('G',
                        LetterType.NOT_GUESSED) + getLetter('H', LetterType.NOT_GUESSED) + getLetter('J',
                        LetterType.NOT_GUESSED) + getLetter('K', LetterType.NOT_GUESSED) + getLetter('L',
                        LetterType.NOT_GUESSED) + "\n             " + getLetter('Z',
                        LetterType.NOT_GUESSED) + getLetter('X', LetterType.NOT_GUESSED) + getLetter('C',
                        LetterType.NOT_GUESSED) + getLetter('V', LetterType.NOT_GUESSED) + getLetter('B',
                        LetterType.NOT_GUESSED) + getLetter('N', LetterType.NOT_GUESSED) + getLetter('M',
                        LetterType.NOT_GUESSED)).complete());

                channel.sendMessage(player.getAsMention()).queue(del -> del.delete().queueAfter(100,
                    TimeUnit.MILLISECONDS,
                    null,
                    new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE)));

                channel.upsertPermissionOverride(player).setAllowed(Permission.MESSAGE_SEND,
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
                newKeyboard = newKeyboard.replace(getLetter(inputChars.get(index), LetterType.IN_WORD),
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
                newKeyboard = newKeyboard.replace(getLetter(inputChars.get(index), LetterType.NOT_GUESSED),
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
            newKeyboard = newKeyboard.replace(getLetter(inputChars.get(index), LetterType.NOT_GUESSED),
                letter);
        }

        StringBuilder builder = new StringBuilder(125);
        for (String character : output) {
            builder.append(character);
            builder.append(" ");
        }

        try {
            messages.get(channel).get(attempt.get(channel)).editMessage(builder).queue();
        } catch (IndexOutOfBoundsException ignored) {
        }
        attempt.put(channel, attempt.get(channel) + 1);
        String finalNewKeyboard = newKeyboard;
        Thread updateKeyboard = new Thread(
            () -> keyboard.put(channel,
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

                            EmbedBuilder newEmbed = getEmbedBuilder(embed,
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

            sendRetryMsg(channel, "Well done!", answer);
            if (daily.get(channel)) channel.sendMessage("Share score?").setActionRow(Button
                .primary("sharewordlescore", "Yes!").withEmoji(Emoji.fromUnicode("✅"))).queue();
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

                            EmbedBuilder newEmbed = getEmbedBuilder(embed,
                                embed.getFields().get(0).getValue(),
                                embed.getFields().get(1).getValue(),
                                String.valueOf(fails));

                            original.editMessageEmbeds(newEmbed.build()).queue();
                        }
                    });

            sendRetryMsg(channel, "The word was **" + answer.toLowerCase() + "**!", answer);
        }
    }

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

                sendRetryMsg(event.getChannel().asTextChannel(),
                    "The word was **" + answers.get(event.getChannel().asTextChannel()).toLowerCase() + "**!",
                    answers.get(event.getChannel().asTextChannel()));
            }

            case "restartgame:wordle" -> {
                event.deferReply().queue();

                try {
                    TextChannel gameChannel = new Wordle().startGame(event.getMember(),
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

                new Thread(() -> {
                    try (ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor()) {
                        executor.schedule(() -> endGame(event.getChannel().asTextChannel()),
                            10,
                            TimeUnit.SECONDS);
                    }
                }, "End Game-Wordle").start();
            }

            case "sharewordlescore" -> {
                Thread thread = new Thread(() -> {
                    event.editButton(event.getButton().asDisabled()).complete();
                    //noinspection DataFlowIssue
                    event.getGuild().getTextChannelById(956267174727671869L)
                        .sendMessage("**" + new Utils().getFullName(event.getMember()) + "** just finished the daily Wordle in **" + attempt.get(
                            event.getChannel().asTextChannel()) + "** tries!").complete();
                    endGame(event.getChannel().asTextChannel());
                }, "Share Wordle Score - " + new Utils().getFirstName(event.getMember()));
                thread.start();
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

                        EmbedBuilder embed = getEmbedBuilder(message,
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
                    .withEmoji(Emoji.fromUnicode("❔"))).queue(msg -> msg.addReaction(Emoji.fromUnicode("✅"))
                    .queue(na -> msg.addReaction(Emoji.fromUnicode("❌")).queue()));
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

    private void sendRetryMsg(TextChannel channel, String message, String answer) {
        channel.upsertPermissionOverride(players.get(channel)).setDenied(Permission.MESSAGE_SEND).queue();

        List<ItemComponent> buttons = new ArrayList<>();
        if (!isNonReal.get(channel)) {
            buttons.add(Button.danger("reportword:" + answer, "Report word")
                .withEmoji(Emoji.fromUnicode("🚩")));
            buttons.add(Button.primary("defineword:" + answer, "Define word")
                .withEmoji(Emoji.fromUnicode("❔")));
        }
        buttons.add(Button.success("restartgame:wordle", "New word").withEmoji(Emoji.fromUnicode("🔁")));

        channel.sendMessage(message).setActionRow(buttons).queue();

        int delay;
        if (daily.get(channel)) delay = 15;
        else delay = 45;
        new Thread(() -> {
            try (ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor()) {
                deleteChannel.put(channel,
                    executor.schedule(() -> new Wordle().endGame(channel), delay, TimeUnit.SECONDS));
            }
        }, "Delete Channel").start();
    }

    private void wordRequest(String word, Member member) {
        List<String> words = new ArrayList<>();
        try {
            Scanner fileScanner = new Scanner(new File("Phoenella/Wordle/banned-requests.txt"),
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
                .queue(m -> msg.addReaction(Emoji.fromUnicode("⛔")).queue()));
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
                return "<:ca:959981731467915274>";
            }
            case 'B' -> {
                return "<:cb:959981731539193966>";
            }
            case 'C' -> {
                return "<:cc:959981731535003698>";
            }
            case 'D' -> {
                return "<:cd:959981731522437130>";
            }
            case 'E' -> {
                return "<:ce:959981731635658772>";
            }
            case 'F' -> {
                return "<:cf:959981731534995516>";
            }
            case 'G' -> {
                return "<:cg:959981731677630524>";
            }
            case 'H' -> {
                return "<:ch:959981731635683348>";
            }
            case 'I' -> {
                return "<:ci:959981731409186897>";
            }
            case 'J' -> {
                return "<:cj:959981731673436240>";
            }
            case 'K' -> {
                return "<:ck:959981731635683378>";
            }
            case 'L' -> {
                return "<:cl:959981731610501230>";
            }
            case 'M' -> {
                return "<:cm:959981731656646656>";
            }
            case 'N' -> {
                return "<:cn:959981731648245890>";
            }
            case 'O' -> {
                return "<:co:959981731627282462>";
            }
            case 'P' -> {
                return "<:cp:959981731727949874>";
            }
            case 'Q' -> {
                return "<:cq:959981731614687283>";
            }
            case 'R' -> {
                return "<:cr:959981731656646676>";
            }
            case 'S' -> {
                return "<:cs:959981731673411654>";
            }
            case 'T' -> {
                return "<:ct:959981731618910258>";
            }
            case 'U' -> {
                return "<:cu:959981731639885854>";
            }
            case 'V' -> {
                return "<:cv:959981731631497266>";
            }
            case 'W' -> {
                return "<:cw:959981731740540978>";
            }
            case 'X' -> {
                return "<:cx:959981731690209370>";
            }
            case 'Y' -> {
                return "<:cy:959981731736330270>";
            }
            case 'Z' -> {
                return "<:cz:959981731589529631>";
            }
        }

        return String.valueOf(letter);
    }

    private String getInWordLetter(Character letter) {
        switch (letter) {
            case 'A' -> {
                return "<:ia:1167158134964502529>";
            }
            case 'B' -> {
                return "<:ib:1167158136134705212>";
            }
            case 'C' -> {
                return "<:ic:1167158138458353674>";
            }
            case 'D' -> {
                return "<:id:1167158139527901284>";
            }
            case 'E' -> {
                return "<:ie:1167158141599879370>";
            }
            case 'F' -> {
                return "<:if:1167158142845599854>";
            }
            case 'G' -> {
                return "<:ig:1167158143994839161>";
            }
            case 'H' -> {
                return "<:ih:1167158145756442724>";
            }
            case 'I' -> {
                return "<:ii:1167158146557558815>";
            }
            case 'J' -> {
                return "<:ij:1167158227365019698>";
            }
            case 'K' -> {
                return "<:ik:1167158228551991327>";
            }
            case 'L' -> {
                return "<:il:1167158149824905227>";
            }
            case 'M' -> {
                return "<:im:1167158231240560800>";
            }
            case 'N' -> {
                return "<:in:959981885025554462>";
            }
            case 'O' -> {
                return "<:io:959981885071700078>";
            }
            case 'P' -> {
                return "<:ip:959981884996223021>";
            }
            case 'Q' -> {
                return "<:iq:959981885017161728>";
            }
            case 'R' -> {
                return "<:ir:959981884979425320>";
            }
            case 'S' -> {
                return "<:is:959981885000384612>";
            }
            case 'T' -> {
                return "<:it:959981885029765160>";
            }
            case 'U' -> {
                return "<:iu:959981884719398984>";
            }
            case 'V' -> {
                return "<:iv:959981885008777226>";
            }
            case 'W' -> {
                return "<:iw:959981885000400946>";
            }
            case 'X' -> {
                return "<:ix:959981884992024586>";
            }
            case 'Y' -> {
                return "<:iy:959981885063299113>";
            }
            case 'Z' -> {
                return "<:iz:959981884987826176>";
            }
        }

        return String.valueOf(letter);
    }

    private String getWrongLetter(Character letter) {
        switch (letter) {
            case 'A' -> {
                return "<:wa:959981914196942949>";
            }
            case 'B' -> {
                return "<:wb:959981914033381417>";
            }
            case 'C' -> {
                return "<:wc:959981914272440330>";
            }
            case 'D' -> {
                return "<:wd:959981914180186142>";
            }
            case 'E' -> {
                return "<:we:959981913827860544>";
            }
            case 'F' -> {
                return "<:wf:959981914222104616>";
            }
            case 'G' -> {
                return "<:wg:959981914305990746>";
            }
            case 'H' -> {
                return "<:wh:959981914238910524>";
            }
            case 'I' -> {
                return "<:wi:959981914121465887>";
            }
            case 'J' -> {
                return "<:wj:959981914213732362>";
            }
            case 'K' -> {
                return "<:wk:959981914238898176>";
            }
            case 'L' -> {
                return "<:wl:959981913953681460>";
            }
            case 'M' -> {
                return "<:wm:959981914289225768>";
            }
            case 'N' -> {
                return "<:wn:959981914054352947>";
            }
            case 'O' -> {
                return "<:wo:959981914272456724>";
            }
            case 'P' -> {
                return "<:wp:959981914448621588>";
            }
            case 'Q' -> {
                return "<:wq:959981914360528956>";
            }
            case 'R' -> {
                return "<:wr:959981914280849449>";
            }
            case 'S' -> {
                return "<:ws:959981914335358986>";
            }
            case 'T' -> {
                return "<:wt:959981914255654913>";
            }
            case 'U' -> {
                return "<:wu:959981914280828968>";
            }
            case 'V' -> {
                return "<:wv:959981914511511572>";
            }
            case 'W' -> {
                return "<:ww:959981914268262400>";
            }
            case 'X' -> {
                return "<:wx:959981914331164722>";
            }
            case 'Y' -> {
                return "<:wy:959981914020790314>";
            }
            case 'Z' -> {
                return "<:wz:959981914331152434>";
            }
        }

        return String.valueOf(letter);
    }

    private String getNotGuessedLetter(Character letter) {
        switch (letter) {
            case 'A' -> {
                return "<:na:960965057842384896>";
            }
            case 'B' -> {
                return "<:nb:960965057737556008>";
            }
            case 'C' -> {
                return "<:nc:960965057867546724>";
            }
            case 'D' -> {
                return "<:nd:960965057536229387>";
            }
            case 'E' -> {
                return "<:ne:960965057787863120>";
            }
            case 'F' -> {
                return "<:nf:960965057892724828>";
            }
            case 'G' -> {
                return "<:ng:960965057552973835>";
            }
            case 'H' -> {
                return "<:nh:960965057800466472>";
            }
            case 'I' -> {
                return "<:ni:960965057829822494>";
            }
            case 'J' -> {
                return "<:nj:960965057678831617>";
            }
            case 'K' -> {
                return "<:nk:960965057846595604>";
            }
            case 'L' -> {
                return "<:nl:960965057821425735>";
            }
            case 'M' -> {
                return "<:nm:960965057771102210>";
            }
            case 'N' -> {
                return "<:nn:960965058173763614>";
            }
            case 'O' -> {
                return "<:no:960965058307952700>";
            }
            case 'P' -> {
                return "<:np:960965057909522432>";
            }
            case 'Q' -> {
                return "<:nq:960965058467348570>";
            }
            case 'R' -> {
                return "<:nr:960965058085666916>";
            }
            case 'S' -> {
                return "<:ns:960965058110840852>";
            }
            case 'T' -> {
                return "<:nt:960965058458972290>";
            }
            case 'U' -> {
                return "<:nu:960965057620095098>";
            }
            case 'V' -> {
                return "<:nv:960965057930493952>";
            }
            case 'W' -> {
                return "<:nw:960965058047926303>";
            }
            case 'X' -> {
                return "<:nx:960965058156982292>";
            }
            case 'Y' -> {
                return "<:ny:960965057951453254>";
            }
            case 'Z' -> {
                return "<:nz:960965058165358642>";
            }
        }

        return String.valueOf(letter);
    }
}
