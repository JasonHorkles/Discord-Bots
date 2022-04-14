package me.jasonhorkles.phoenella.games;

import me.jasonhorkles.phoenella.GameManager;
import me.jasonhorkles.phoenella.Phoenella;
import me.jasonhorkles.phoenella.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Wordle extends ListenerAdapter {
    //todo list
    // daily wordle
    // timed challenge with threads
    // auto push new words https://github-api.kohsuke.org/
    private static final ArrayList<String> wordList = new ArrayList<>();
    private static final HashMap<TextChannel, ArrayList<Message>> messages = new HashMap<>();
    private static final HashMap<TextChannel, Boolean> isUserGenerated = new HashMap<>();
    private static final HashMap<TextChannel, Integer> attempt = new HashMap<>();
    private static final HashMap<TextChannel, Member> players = new HashMap<>();
    private static final HashMap<TextChannel, Message> keyboard = new HashMap<>();
    private static final HashMap<TextChannel, Long> originalMessage = new HashMap<>();
    private static final HashMap<TextChannel, ScheduledFuture<?>> deleteChannel = new HashMap<>();
    private static final HashMap<TextChannel, String> answers = new HashMap<>();

    public TextChannel startGame(Member player, @Nullable String answer, boolean isUserGenerated) throws IOException {
        if (answer == null || answer.equals("null")) {
            // Get words
            String page = "https://raw.githubusercontent.com/JasonHorkles/Discord-Bots/main/Phoenella/Wordle/words.txt";
            Connection conn = Jsoup.connect(page);

            Document doc = conn.get();
            String words = doc.body().text();
            Scanner scanner = new Scanner(words);

            wordList.clear();
            while (scanner.hasNext()) try {
                wordList.add(scanner.next());
            } catch (NoSuchElementException ignored) {
            }

            Random r = new Random();
            answer = wordList.get(r.nextInt(wordList.size()));
        }

        String obfuscatedAnswer;
        obfuscatedAnswer = UUID.nameUUIDFromBytes(answer.getBytes()).toString();

        // Scan thru for duplicates
        if (players.containsValue(player)) for (TextChannel channels : players.keySet())
            if (players.get(channels) == player) if (Objects.equals(channels.getTopic(), obfuscatedAnswer)) return null;

        TextChannel channel = new GameManager().createChannel(GameManager.Game.WORDLE,
            new ArrayList<>(Collections.singleton(player)));

        Wordle.isUserGenerated.put(channel, isUserGenerated);
        players.put(channel, player);
        answers.put(channel, answer.toUpperCase());
        attempt.put(channel, 0);

        channel.getManager().setTopic(obfuscatedAnswer)
            .queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_CHANNEL));

        Thread game = new Thread(() -> {
            ArrayList<Message> lines = new ArrayList<>();
            StringBuilder empties = new StringBuilder();
            empties.append("<:empty:959950240516046868> ".repeat(answers.get(channel).length()));
            try {
                for (int x = 0; x < 6; x++)
                    lines.add(channel.sendMessage(empties).complete());
                messages.put(channel, lines);

                keyboard.put(channel, channel.sendMessage(
                    "~~==========================~~\n    " + getLetter('Q', LetterType.NOT_GUESSED) + getLetter('W',
                        LetterType.NOT_GUESSED) + getLetter('E', LetterType.NOT_GUESSED) + getLetter('R',
                        LetterType.NOT_GUESSED) + getLetter('T', LetterType.NOT_GUESSED) + getLetter('Y',
                        LetterType.NOT_GUESSED) + getLetter('U', LetterType.NOT_GUESSED) + getLetter('I',
                        LetterType.NOT_GUESSED) + getLetter('O', LetterType.NOT_GUESSED) + getLetter('P',
                        LetterType.NOT_GUESSED) + "\n       " + getLetter('A', LetterType.NOT_GUESSED) + getLetter('S',
                        LetterType.NOT_GUESSED) + getLetter('D', LetterType.NOT_GUESSED) + getLetter('F',
                        LetterType.NOT_GUESSED) + getLetter('G', LetterType.NOT_GUESSED) + getLetter('H',
                        LetterType.NOT_GUESSED) + getLetter('J', LetterType.NOT_GUESSED) + getLetter('K',
                        LetterType.NOT_GUESSED) + getLetter('L',
                        LetterType.NOT_GUESSED) + "\n             " + getLetter('Z',
                        LetterType.NOT_GUESSED) + getLetter('X', LetterType.NOT_GUESSED) + getLetter('C',
                        LetterType.NOT_GUESSED) + getLetter('V', LetterType.NOT_GUESSED) + getLetter('B',
                        LetterType.NOT_GUESSED) + getLetter('N', LetterType.NOT_GUESSED) + getLetter('M',
                        LetterType.NOT_GUESSED)).complete());

                channel.sendMessage(player.getAsMention()).complete().delete()
                    .queueAfter(100, TimeUnit.MILLISECONDS, null,
                        new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));

                channel.putPermissionOverride(player).setAllow(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND)
                    .queue();
            } catch (ErrorResponseException ignored) {
            }
        });
        game.start();

        return channel;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.getMessage().isFromGuild()) return;
        if (!event.getTextChannel().getName().contains("wordle")) return;
        if (event.getTextChannel().getParentCategoryIdLong() != 900747596245639238L) return;
        if (event.getAuthor().isBot()) return;

        Message message = event.getMessage();
        String input = message.getContentStripped().replaceAll("[^a-zA-Z]", "").toUpperCase();
        TextChannel channel = event.getTextChannel();
        String answer = answers.get(channel);

        if (input.length() != answer.length()) {
            message.reply("Invalid length!").complete().delete().queueAfter(3, TimeUnit.SECONDS);
            message.delete().queueAfter(100, TimeUnit.MILLISECONDS);
            return;
        }

        if (!isUserGenerated.get(channel)) if (!wordList.toString().contains(input)) {
            message.reply("**" + input + "** isn't in my dictionary!")
                .setActionRow(Button.primary("wordlerequest:" + input, "Request word!")).complete().delete()
                .queueAfter(4, TimeUnit.SECONDS);
            message.delete().queueAfter(100, TimeUnit.MILLISECONDS);
            return;
        }

        message.delete().queueAfter(100, TimeUnit.MILLISECONDS);

        ArrayList<Character> answerChars = new ArrayList<>(answer.chars().mapToObj(c -> (char) c).toList());
        ArrayList<Character> inputChars = new ArrayList<>(input.chars().mapToObj(c -> (char) c).toList());
        ArrayList<String> output = new ArrayList<>();

        // Gray / Incorrect - Word
        for (Character character : inputChars) output.add(getLetter(character, LetterType.WRONG));

        String newKeyboard = keyboard.get(channel).getContentRaw();

        // Green / Correct
        for (int index = 0; index < answerChars.size(); index++)
            if (answerChars.get(index) == inputChars.get(index)) {
                String letter = getLetter(inputChars.get(index), LetterType.CORRECT);

                // Replace dark gray and yellow keys
                newKeyboard = newKeyboard.replace(getLetter(inputChars.get(index), LetterType.IN_WORD), letter)
                    .replace(getLetter(inputChars.get(index), LetterType.NOT_GUESSED), letter);

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
                newKeyboard = newKeyboard.replace(getLetter(inputChars.get(index), LetterType.NOT_GUESSED), letter);

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
            newKeyboard = newKeyboard.replace(getLetter(inputChars.get(index), LetterType.NOT_GUESSED), letter);
        }

        StringBuilder builder = new StringBuilder();
        for (String character : output) {
            builder.append(character);
            builder.append(" ");
        }

        try {
            messages.get(channel).get(attempt.get(channel)).editMessage(builder).queue();
        } catch (IndexOutOfBoundsException ignored) {
        }
        attempt.put(channel, attempt.get(channel) + 1);
        keyboard.put(channel, keyboard.get(channel).editMessage(newKeyboard).complete());

        // Win
        if (input.equals(answer)) {
            // Is user-generated
            if (isUserGenerated.get(channel))
                //noinspection ConstantConditions
                event.getJDA().getTextChannelById(956267174727671869L).retrieveMessageById(originalMessage.get(channel))
                    .queue((original) -> {
                        MessageEmbed embed = original.getEmbeds().get(0);
                        // Add 1 to wins
                        if (!embed.isEmpty()) {
                            //noinspection ConstantConditions
                            int wins = Integer.parseInt(embed.getFields().get(1).getValue()) + 1;

                            EmbedBuilder newEmbed = new EmbedBuilder(embed);
                            newEmbed.clearFields();
                            newEmbed.addField(embed.getFields().get(0).getName(), embed.getFields().get(0).getValue(),
                                true);
                            newEmbed.addField(embed.getFields().get(1).getName(), String.valueOf(wins), true);
                            newEmbed.addField(embed.getFields().get(2).getName(), embed.getFields().get(2).getValue(),
                                true);

                            original.editMessageEmbeds(newEmbed.build()).queue();
                        }
                    });

                // Add to the leaderboard if not user-generated
            else if (!Phoenella.localWordleBoard) try {
                File leaderboardFile = new File("Phoenella/Wordle/leaderboard.txt");
                Scanner leaderboard = new Scanner(leaderboardFile);
                ArrayList<String> lines = new ArrayList<>();

                int index = 0;
                int memberAtIndex = -1;
                while (leaderboard.hasNextLine()) try {
                    String line = leaderboard.nextLine();
                    lines.add(line);
                    //noinspection ConstantConditions
                    if (line.contains(event.getMember().getId())) {
                        memberAtIndex = index;
                        break;
                    }
                    index++;
                } catch (NoSuchElementException ignored) {
                }

                FileWriter writer;
                if (memberAtIndex == -1) {
                    writer = new FileWriter(leaderboardFile, true);
                    //noinspection ConstantConditions
                    writer.write(event.getMember().getId() + ":1" + "\n");
                } else {
                    writer = new FileWriter(leaderboardFile, false);
                    int score = Integer.parseInt(lines.get(memberAtIndex).replaceFirst(".*:", "")) + 1;
                    lines.set(memberAtIndex, event.getMember().getId() + ":" + score);

                    for (String line : lines) writer.write(line + "\n");
                }
                writer.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            sendRetryMsg(channel, "Well done!", answer);
        }

        // Fail
        else if (attempt.get(channel) == 6) {
            if (isUserGenerated.get(channel))
                //noinspection ConstantConditions
                event.getJDA().getTextChannelById(956267174727671869L).retrieveMessageById(originalMessage.get(channel))
                    .queue((original) -> {
                        MessageEmbed embed = original.getEmbeds().get(0);
                        // Add 1 to fails
                        if (!embed.isEmpty()) {
                            //noinspection ConstantConditions
                            int fails = Integer.parseInt(embed.getFields().get(2).getValue()) + 1;

                            EmbedBuilder newEmbed = new EmbedBuilder(embed);
                            newEmbed.clearFields();
                            newEmbed.addField(embed.getFields().get(0).getName(), embed.getFields().get(0).getValue(),
                                true);
                            newEmbed.addField(embed.getFields().get(1).getName(), embed.getFields().get(1).getValue(),
                                true);
                            newEmbed.addField(embed.getFields().get(2).getName(), String.valueOf(fails), true);

                            original.editMessageEmbeds(newEmbed.build()).queue();
                        }
                    });

            sendRetryMsg(channel, "The word was **" + answer.toLowerCase() + "**!", answer);
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        switch (event.getComponentId()) {
            case "endgame:wordle" -> {
                event.deferEdit().queue();
                event.editButton(event.getButton().asDisabled()).queue();
                sendRetryMsg(event.getTextChannel(), "The word was **" + answers.get(event.getTextChannel()) + "**!",
                    answers.get(event.getTextChannel()));
            }

            case "restartgame:wordle" -> {
                event.deferReply().queue();
                try {
                    TextChannel gameChannel = new Wordle().startGame(event.getMember(), null, false);
                    if (gameChannel == null)
                        event.getHook().editOriginal("You already have a game with that word active!").queue();
                    else event.getHook().editOriginal("Game created in " + gameChannel.getAsMention()).queue();
                } catch (IOException e) {
                    event.getHook().editOriginal("Couldn't generate a random word! Please try again later.").queue();
                    System.out.print(new Utils().getTime(Utils.Color.RED));
                    e.printStackTrace();
                }

                Executors.newSingleThreadScheduledExecutor()
                    .schedule(() -> endGame(event.getTextChannel()), 10, TimeUnit.SECONDS);
            }
        }

        if (event.getComponentId().startsWith("playwordle:")) {
            String word = event.getComponentId().replace("playwordle:", "");
            event.reply("Creating a game...").setEphemeral(true).queue();
            try {
                TextChannel gameChannel = new Wordle().startGame(event.getMember(), word, true);
                if (gameChannel == null)
                    event.getHook().editOriginal("You already have a game with that word active!").queue();
                else {
                    event.getHook().editOriginal("Game created in " + gameChannel.getAsMention()).queue();
                    MessageEmbed message = event.getMessage().getEmbeds().get(0);
                    // Add 1 to plays
                    if (!message.isEmpty()) {
                        //noinspection ConstantConditions
                        int plays = Integer.parseInt(message.getFields().get(0).getValue()) + 1;

                        EmbedBuilder embed = new EmbedBuilder(message);
                        embed.clearFields();
                        embed.addField(message.getFields().get(0).getName(), String.valueOf(plays), true);
                        embed.addField(message.getFields().get(1).getName(), message.getFields().get(1).getValue(),
                            true);
                        embed.addField(message.getFields().get(2).getName(), message.getFields().get(2).getValue(),
                            true);

                        event.getMessage().editMessageEmbeds(embed.build()).queue();

                        originalMessage.put(gameChannel, event.getMessage().getIdLong());
                    }
                }
            } catch (IOException e) {
                event.getHook().editOriginal("Couldn't generate a random word! Please try again later.").queue();
                System.out.print(new Utils().getTime(Utils.Color.RED));
                e.printStackTrace();
            }
            return;
        }

        if (event.getComponentId().startsWith("wordlerequest:")) {
            event.deferEdit().queue();
            event.editButton(event.getButton().asDisabled()).complete();
            //noinspection ConstantConditions
            event.getJDA().getTextChannelById(960213547944661042L).sendMessage(
                    ":inbox_tray: Word request from " + new Utils().getFullName(
                        event.getMember()) + ": **" + event.getComponentId().replace("wordlerequest:", "") + "**")
                .queue((msg) -> msg.addReaction("❌").queue());
            return;
        }

        if (event.getComponentId().startsWith("reportword:")) {
            event.deferEdit().queue();
            event.editButton(event.getButton().asDisabled()).complete();
            //noinspection ConstantConditions
            event.getJDA().getTextChannelById(960213547944661042L).sendMessage(
                    ":warning: Word report from " + new Utils().getFullName(
                        event.getMember()) + ": **" + event.getComponentId().replace("reportword:", "") + "**")
                .queue((msg) -> msg.addReaction("❌").queue());
        }
    }

    private void endGame(TextChannel channel) {
        players.remove(channel);
        answers.remove(channel);
        attempt.remove(channel);
        messages.remove(channel);
        keyboard.remove(channel);
        originalMessage.remove(channel);
        isUserGenerated.remove(channel);
        if (deleteChannel.get(channel) != null) {
            deleteChannel.get(channel).cancel(true);
            deleteChannel.remove(channel);
        }
        new GameManager().deleteGame(channel);
    }

    private void sendRetryMsg(TextChannel channel, String message, String answer) {
        channel.putPermissionOverride(players.get(channel)).setAllow(Permission.VIEW_CHANNEL)
            .setDeny(Permission.MESSAGE_SEND).queue();

        if (isUserGenerated.get(channel)) channel.sendMessage(message)
            .setActionRow(Button.success("restartgame:wordle", "New word").withEmoji(Emoji.fromUnicode("🔁"))).queue();
        else channel.sendMessage(message)
            .setActionRow(Button.danger("reportword:" + answer, "Report word").withEmoji(Emoji.fromUnicode("🚩")),
                Button.success("restartgame:wordle", "New word").withEmoji(Emoji.fromUnicode("🔁"))).queue();

        deleteChannel.put(channel, Executors.newSingleThreadScheduledExecutor()
            .schedule(() -> new Wordle().endGame(channel), 45, TimeUnit.SECONDS));
    }

    private enum LetterType {
        WRONG, IN_WORD, CORRECT, NOT_GUESSED
    }

    private String getLetter(Character letter, LetterType letterType) {
        switch (letterType) {
            case CORRECT -> {
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
            }

            case IN_WORD -> {
                switch (letter) {
                    case 'A' -> {
                        return "<:ia:959981823457361950>";
                    }
                    case 'B' -> {
                        return "<:ib:959981823407038504>";
                    }
                    case 'C' -> {
                        return "<:ic:959981823415418990>";
                    }
                    case 'D' -> {
                        return "<:id:959981823440584704>";
                    }
                    case 'E' -> {
                        return "<:ie:959981823428030464>";
                    }
                    case 'F' -> {
                        return "<:if:959981823117623338>";
                    }
                    case 'G' -> {
                        return "<:ig:959981823025360928>";
                    }
                    case 'H' -> {
                        return "<:ih:959981823432220682>";
                    }
                    case 'I' -> {
                        return "<:ii:959981823658717194>";
                    }
                    case 'J' -> {
                        return "<:ij:959981823474143232>";
                    }
                    case 'K' -> {
                        return "<:ik:959981823616745482>";
                    }
                    case 'L' -> {
                        return "<:il:959981823037947905>";
                    }
                    case 'M' -> {
                        return "<:im:959981823713222666>";
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
            }

            case WRONG -> {
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
            }

            case NOT_GUESSED -> {
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
            }
        }
        return "<@277291758503723010> | Char: `" + letter + "` | LetterType: `" + letterType + "`";
    }
}
