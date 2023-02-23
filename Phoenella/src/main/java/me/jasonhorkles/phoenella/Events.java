package me.jasonhorkles.phoenella;

import com.mattmalec.pterodactyl4j.PteroBuilder;
import com.mattmalec.pterodactyl4j.client.entities.ClientServer;
import com.mattmalec.pterodactyl4j.client.entities.PteroClient;
import me.jasonhorkles.phoenella.games.RPS;
import me.jasonhorkles.phoenella.games.Wordle;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("DataFlowIssue")
public class Events extends ListenerAdapter {
    private static final Map<TextChannel, Integer> channelCooldown = new HashMap<>();
    private static final ArrayList<String> messageCooldown = new ArrayList<>();

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.isFromGuild()) return;
        if (event.getGuild().getIdLong() != 729083627308056597L) return;
        if (event.getAuthor().isBot()) return;

        Member member = event.getMember();
        TextChannel channel = event.getChannel().asTextChannel();
        Message message = event.getMessage();

        boolean isReply = false;
        if (message.getMessageReference() != null)
            if (message.getMessageReference().getMessage().getAuthor().equals(Phoenella.jda.getSelfUser()))
                isReply = true;

        String text = message.getContentRaw().toLowerCase().replaceAll("\\bphoenella\\b", "PHOENELLA")
            .replaceAll("\\bphoe\\b", "PHOENELLA").strip();

        if (!text.contains("PHOENELLA") && !isReply && channel.getIdLong() != 892802385301352548L) return;

        text = text.replace("  ", " ").replaceAll(" ?PHOENELLA ?", "");

        // Utility

        if (text.contains("update members")) {
            if (member.getIdLong() != 277291758503723010L) {
                message.reply("no").queue();
                return;
            }

            new Utils().sendMessage(null, message, "Updating members... See console for details.", false);
            new Utils().runNameCheckForGuild(event.getGuild());
            return;
        }

        if (text.contains("stop") || text.contains("shut down"))
            if (member.getIdLong() == 277291758503723010L) {
                message.reply("Shutting down...").mentionRepliedUser(false).queue();
                PteroClient ptero = PteroBuilder.createClient(new Secrets().getPteroUrl(),
                    new Secrets().getPteroApiKey());
                ptero.retrieveServerByIdentifier("5243694c").flatMap(ClientServer::stop).executeAsync();
                return;
            }

        if (text.contains("restart")) if (member.getIdLong() == 277291758503723010L) {
            message.reply("Restarting...").mentionRepliedUser(false).complete();
            System.exit(0);
            return;
        }

        // Don't say anything if in a game channel
        if (channel.getParentCategory() != null)
            if (channel.getParentCategoryIdLong() == 900747596245639238L) return;

        ArrayList<String> disabledChannels = new ArrayList<>();
        try {
            File file = new File("Phoenella/channel-blacklist.txt");
            Scanner fileScanner = new Scanner(file);

            while (fileScanner.hasNextLine()) disabledChannels.add(fileScanner.nextLine());
        } catch (NoSuchElementException ignored) {
        } catch (FileNotFoundException e) {
            System.out.print(new Utils().getTime(Utils.LogColor.RED));
            e.printStackTrace();
        }

        if (text.contains("un-shush") || text.contains("unshush") || text.contains("speak")) {
            if (!member.hasPermission(Permission.MESSAGE_MANAGE)) return;

            try {
                disabledChannels.remove(channel.getId());
                FileWriter fileWriter = new FileWriter("Phoenella/channel-blacklist.txt", false);
                for (String channels : disabledChannels) fileWriter.write(channels + "\n");
                fileWriter.close();
            } catch (IOException e) {
                System.out.print(new Utils().getTime(Utils.LogColor.RED));
                e.printStackTrace();
                message.reply(":see_no_evil: Uh oh! There's an error! <@277291758503723010>")
                    .mentionRepliedUser(false).queue();
            }

            message.addReaction(Emoji.fromUnicode("\uD83D\uDE2E")).queue();
            return;
        }

        if (disabledChannels.contains(channel.getId())) return;

        if (text.contains("shut up") || text.contains("shush") || text.contains("be quiet")) {
            if (!member.hasPermission(Permission.MESSAGE_MANAGE)) return;

            try {
                disabledChannels.add(channel.getId());
                FileWriter fileWriter = new FileWriter("Phoenella/channel-blacklist.txt", false);
                for (String channels : disabledChannels) fileWriter.write(channels + "\n");
                fileWriter.close();
            } catch (IOException e) {
                System.out.print(new Utils().getTime(Utils.LogColor.RED));
                e.printStackTrace();
                message.reply(":see_no_evil: Uh oh! There's an error! <@277291758503723010>")
                    .mentionRepliedUser(false).queue();
            }

            message.addReaction(Emoji.fromUnicode("🙊")).queue();
            return;
        }

        // Fun

        if (text.contains("say ")) {
            if (member.getIdLong() != 277291758503723010L) {
                new Utils().sendMessage(channel, null, "no", false);
                return;
            }

            channel.sendMessage(text.replaceAll("(?i).*say ", "")).queue();
            message.delete().queue();
            return;
        }

        boolean allCaps = message.getContentStripped().equals(message.getContentStripped().toUpperCase());

        String msg = "Well dadgum, something went wrong!";

        if (text.contains("play rock paper scissors") || text.contains("play rps")) {
            channel.sendTyping().complete();

            if (message.getMentions().getMembers().isEmpty() || message.getMentions().getMembers()
                .get(0) == member || message.getMentions().getMembers().get(0).getUser().isBot()) {
                message.reply("You must ping an opponent in your message!").queue();
                return;

            } else {
                ArrayList<Member> players = new ArrayList<>();
                players.add(member);
                players.add(message.getMentions().getMembers().get(0));

                TextChannel gameChannel = new RPS().startGame(players);
                message.addReaction(Emoji.fromUnicode("👍")).queue();
                message.reply("Game created in " + gameChannel.getAsMention())
                    .queue(del -> del.delete().queueAfter(15, TimeUnit.SECONDS));
            }
            return;
        }

        // Message cooldowns
        for (String s : messageCooldown)
            if (s.equalsIgnoreCase(text)) {
                message.addReaction(Emoji.fromUnicode("\uD83E\uDD2B")).queue();
                return;
            }

        if (channel.getIdLong() != 892802385301352548L) if (channelCooldown.containsKey(channel)) {
            if (channelCooldown.get(channel) >= 6) return;

            channelCooldown.put(channel, channelCooldown.get(channel) + 1);

            if (channelCooldown.get(channel) == 6) {
                TimerTask task = new TimerTask() {
                    @Override
                    public void run() {
                        channelCooldown.remove(channel);
                    }
                };
                Timer timer = new Timer("Cooldown");
                timer.schedule(task, 600000); // 600000 = 10 minutes

                channel.sendMessage(
                        ":yawning_face: I'm tired of talking here for a while. Use <#892802385301352548> to talk to me more.")
                    .complete().editMessage(":face_with_hand_over_mouth:").queueAfter(10, TimeUnit.MINUTES);
                return;
            } else {
                TimerTask task = new TimerTask() {
                    @Override
                    public void run() {
                        channelCooldown.remove(channel);
                    }
                };
                Timer timer = new Timer("Cooldown");
                timer.schedule(task, 900000); // 900000 = 15 minutes
            }
        } else channelCooldown.put(channel, 1);

        messageCooldown.add(text);
        String finalText = text;
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                messageCooldown.remove(finalText);
            }
        };
        Timer timer = new Timer("Message Cooldown");
        timer.schedule(task, 180000); // 180000 = 3 minutes

        channel.sendTyping().complete();

        Random r = new Random();

        if (text.equals("")) {
            msg = new Utils().getFirstName(member);

            new Utils().sendMessage(channel, null, msg, allCaps);
            return;
        }

        if (text.equals("?")) {
            msg = new Utils().getFirstName(member) + "?";

            new Utils().sendMessage(channel, null, msg, allCaps);
            return;
        }

        if (text.equals("!")) {
            msg = "WHAT DO YOU WANT " + new Utils().getFirstName(member) + " AAAAAAAAAAAAAAAAAAAAA";

            new Utils().sendMessage(channel, null, msg, true);
            return;
        }

        if (text.length() <= 1) {
            msg = "can you speak english please";

            new Utils().sendMessage(channel, null, msg, allCaps);
            return;
        }

        if (text.startsWith("hi") || text.endsWith("hi") || text.contains("hello")) {
            int number = r.nextInt(4);
            switch (number) {
                case 0 -> msg = "hi " + new Utils().getFirstName(member);
                case 1 -> msg = "howdy, " + new Utils().getFirstName(member);
                case 2 -> msg = "sup";
                case 3 -> msg = "hello " + new Utils().getFirstName(member);
            }

            new Utils().sendMessage(channel, null, msg, allCaps);
            return;
        }

        if (text.startsWith("ily") || text.endsWith("ily")) {
            int number = r.nextInt(4);
            switch (number) {
                case 0 -> msg = "I love **YOU**!";
                case 1 -> msg = "likewise";
                case 2 -> msg = "LIKEWISE";
                case 3 -> msg = "\\:)";
            }

            new Utils().sendMessage(channel, null, msg, allCaps);
            return;
        }

        if (text.contains("thank") || text.contains("thx")) {
            int number = r.nextInt(5);
            switch (number) {
                case 0 -> msg = "you're welcome!";
                case 1 -> msg = "no problem!";
                case 2 -> msg = "my pleasure";
                case 3 -> msg = "np";
                case 4 -> msg = "yw";
            }

            new Utils().sendMessage(channel, null, msg, allCaps);
            return;
        }

        if (text.contains("color")) {
            int red = r.nextInt(256);
            int green = r.nextInt(256);
            int blue = r.nextInt(256);

            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("This color!");
            embed.setDescription("(" + red + ", " + green + ", " + blue + ")");
            embed.setColor(new Color(red, green, blue));

            channel.sendMessageEmbeds(embed.build()).queue();
            return;
        }

        if ((text.contains("what") || text.contains("plz")) && (text.contains("time") || text.contains(
            "date") || text.contains("day") || text.contains("month") || text.contains("year"))) {
            msg = "It's currently";

            if (allCaps) msg = msg.toUpperCase();
            msg = msg + " <t:" + System.currentTimeMillis() / 1000 + ":F>";

            new Utils().sendMessage(channel, null, msg, false);
            return;
        }

        if (text.replace(" ", "").contains("9+10")) {
            msg = "21";

            new Utils().sendMessage(channel, null, msg, allCaps);
            return;
        }

        if (text.contains("random person")) {
            List<Member> members = event.getGuild().getMembers();
            Member randomMember = members.get(r.nextInt(members.size() - 1));
            msg = new Utils().getFullName(randomMember);

            new Utils().sendMessage(channel, null, msg, allCaps);
            return;
        }

        if (text.contains("game list")) {
            msg = "Rock Paper Scissors (RPS)\nWordle";

            channel.sendMessage(msg).queue();
            return;
        }

        if (text.startsWith("search ")) {
            String search = text.replaceFirst("search ", "");
            String page = "https://www.google.com/search?q=" + URLEncoder.encode(search,
                StandardCharsets.UTF_8).strip();

            try {
                Connection conn = Jsoup.connect(page).timeout(15000);
                Document doc = conn.get();
                msg = doc.body().getElementsByClass("ILfuVd").get(0).text();

                EmbedBuilder embed = new EmbedBuilder();
                embed.setTitle(search.toUpperCase(), page);
                embed.setColor(new Color(15, 157, 88));
                embed.setDescription(msg);

                message.replyEmbeds(embed.build()).mentionRepliedUser(false).queue();

            } catch (IOException e) {
                message.reply(e.getMessage()).mentionRepliedUser(false).queue();
                System.out.print(new Utils().getTime(Utils.LogColor.RED));
                e.printStackTrace();

            } catch (IndexOutOfBoundsException ignored) {
                message.reply("I couldn't find a result for that!\n<" + page + ">").mentionRepliedUser(false)
                    .queue();
            }
            return;
        }

        if (text.startsWith("define ")) {
            MessageEmbed embed = new Utils().defineWord(text.replace("define ", ""));

            if (embed.getDescription() != null) if (embed.getDescription().startsWith("Couldn't find "))
                message.replyEmbeds(embed).mentionRepliedUser(false).queue();

            else message.replyEmbeds(embed).setActionRow(
                        Button.danger("definitionreport", "Report definition").withEmoji(Emoji.fromUnicode("🚩")))
                    .mentionRepliedUser(false).queue();

            return;
        }

        msg = new Utils().lookUp(text, new Utils().getFirstName(member));
        if (msg.equals("501")) {
            int number = r.nextInt(20);
            switch (number) {
                case 0 -> msg = "no";
                case 1 -> msg = "yes";
                case 2 -> msg = "meh";
                case 3 -> msg = "ok";
                case 4 -> msg = "<:kek:841681203278774322>";
                case 5 -> msg = "bruh";
                case 6 -> msg = "hmm";
                case 7 -> msg = "no way!";
                case 8 -> msg = "¯\\_(ツ)_/¯";
                case 9 -> msg = "heck no dawg!";
                case 10 -> msg = "yup";
                case 11 -> msg = "nah";
                case 12 -> msg = "of course!";
                case 13 -> msg = "heh\nno";
                case 14 -> msg = "i'm not sure if you'd want to know that answer...";
                case 15 -> msg = "of course not!";
                case 16 -> msg = "sure";
                case 17 -> msg = "https://tenor.com/ovll.gif";
                case 18 -> msg = "https://tenor.com/bihu2.gif";
                case 19 -> msg = "https://tenor.com/bQZAM.gif";
            }
        }
        new Utils().sendMessage(null, message, msg, allCaps);
    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        if (event.getUser().isBot()) return;

        // kek
        if (event.getReaction().getEmoji().getName().equalsIgnoreCase("kek")) {
            event.retrieveMessage().complete()
                .addReaction(event.getGuild().getEmojiById("841681203278774322")).queue();
            return;
        }

        // Shush users
        if (event.getReaction().getEmoji().getName().equals("\uD83E\uDD2B")) {
            // Verify if mod or coach
            if (event.getMember().getRoles().toString().contains("751166721624375435") || event.getMember()
                .getRoles().toString().contains("729108220479537202"))
                event.retrieveMessage().queue(message -> {
                    Member member = message.getMember();

                    if (member.isTimedOut()) {
                        event.getChannel().sendMessage(
                            event.getMember().getAsMention() + ", that person is already shushed!").queue(
                            (m) -> m.delete().queueAfter(5, TimeUnit.SECONDS, null,
                                new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE)));
                        return;
                    }

                    member.timeoutFor(10, TimeUnit.MINUTES).queue((na) -> event.getChannel()
                        .sendMessage(new Utils().getFirstName(member) + " just got shushed!").queue(del -> {
                            del.delete().queueAfter(10, TimeUnit.MINUTES, null,
                                new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
                            event.getChannel().sendMessage("https://tenor.com/vfW7.gif").queue(
                                del2 -> del2.delete().queueAfter(10, TimeUnit.MINUTES, null,
                                    new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE)));
                        }), (na1) -> event.getChannel()
                        .sendMessage(event.getMember().getAsMention() + ", I can't shush that person!").queue(
                            (del) -> del.delete().queueAfter(5, TimeUnit.SECONDS, null,
                                new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE))));
                });
            return;
        }

        // Add / remove word from dictionary
        if (event.getReaction().getEmoji().getName().equals("✅")) {
            if (event.getChannel().getIdLong() == 960213547944661042L) {
                if (event.getUserIdLong() != 277291758503723010L) {
                    System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + event.getUser()
                        .getAsTag() + " just tried to accept a Wordle word!");
                    return;
                }

                Message message = event.retrieveMessage().complete();
                File file = new File("Phoenella/Wordle/words.txt");

                try {
                    if (message.getContentStripped().toLowerCase().contains("word request")) {
                        FileWriter fileWriter = new FileWriter(file, true);
                        String word = message.getContentStripped().replaceAll(".*: ", "").toUpperCase();
                        fileWriter.write(word + "\n");
                        fileWriter.close();

                    } else if (message.getContentStripped().contains("Word report")) {
                        Scanner words = new Scanner(file);
                        ArrayList<String> wordList = new ArrayList<>();

                        while (words.hasNext()) try {
                            String next = words.next();
                            if (!next.equalsIgnoreCase(message.getContentStripped().replaceAll(".*: ", "")))
                                wordList.add(next.toUpperCase());
                        } catch (NoSuchElementException ignored) {
                        }

                        FileWriter fileWriter = new FileWriter(file, false);
                        for (String word : wordList) fileWriter.write(word + "\n");
                        fileWriter.close();
                    }

                    message.delete().queue();

                } catch (IOException e) {
                    message.reply("Failed to write word! See console for details.")
                        .queue(msg -> msg.delete().queueAfter(5, TimeUnit.SECONDS));
                    System.out.print(new Utils().getTime(Utils.LogColor.RED));
                    e.printStackTrace();
                }
            }
            return;
        }

        // Delete message
        if (event.getReaction().getEmoji().getName().equals("❌") && event.getChannel().asTextChannel()
            .getParentCategoryIdLong() != 900747596245639238L) {
            event.retrieveMessage().queue(message -> {
                if (message.getAuthor().equals(Phoenella.jda.getSelfUser())) message.delete().queue();
            });
            return;
        }

        // Prevent future requests for that word
        if (event.getEmoji().getName().equals("⛔"))
            if (event.getChannel().getIdLong() == 960213547944661042L) {
                if (event.getUserIdLong() != 277291758503723010L) {
                    System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + event.getUser()
                        .getAsTag() + " just tried to decline a Wordle word!");
                    return;
                }

                Message message = event.retrieveMessage().complete();
                File file = new File("Phoenella/Wordle/banned-requests.txt");

                try {
                    if (message.getContentStripped().contains("Auto word request")) {
                        FileWriter fileWriter = new FileWriter(file, true);
                        fileWriter.write(
                            message.getContentStripped().replaceAll(".*: ", "").toUpperCase() + "\n");
                        fileWriter.close();
                    }
                    message.delete().queue();

                } catch (IOException e) {
                    message.reply("Failed to write word! See console for details.")
                        .queue(msg -> msg.delete().queueAfter(5, TimeUnit.SECONDS));
                    System.out.print(new Utils().getTime(Utils.LogColor.RED));
                    e.printStackTrace();
                }
            }
    }

    @Override
    public void onGuildMemberUpdateNickname(GuildMemberUpdateNicknameEvent event) {
        if (event.getUser().isBot()) return;

        Member member = event.getMember();
        Guild guild = event.getGuild();
        String newNickname = event.getNewNickname();

        System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + event.getMember().getUser()
            .getAsTag() + " changed their nickname from '" + event.getOldNickname() + "' to '" + newNickname + "'");

        // If not sushed
        if (!member.getRoles().toString().contains("842490529744945192"))
            new Utils().runNameCheckForUser(newNickname, member, guild);
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + new Utils().getFullName(
            event.getMember()) + " used the /" + event.getName() + " command");

        //noinspection SwitchStatementWithTooFewBranches
        switch (event.getName().toLowerCase()) {
            case "wordle" -> {
                switch (event.getSubcommandName()) {
                    case "create" -> {
                        TextInput word = TextInput.create("word", "Word", TextInputStyle.SHORT)
                            .setPlaceholder("Standard words are 5 characters").setMinLength(4).setMaxLength(8)
                            .build();
                        TextInput tries = TextInput.create("tries", "Tries", TextInputStyle.SHORT)
                            .setPlaceholder("Must be between 4-8").setMinLength(1).setMaxLength(1)
                            .setValue(String.valueOf(6)).build();

                        Modal modal = Modal.create("customwordle", "Create Custom Wordle").addActionRow(word)
                            .addActionRow(tries).build();

                        event.replyModal(modal).queue();
                    }

                    case "play" -> {
                        event.reply("Creating a game...").setEphemeral(true).queue();
                        try {
                            TextChannel gameChannel = new Wordle().startGame(event.getMember(), null, false,
                                false, null);
                            if (gameChannel == null) event.getHook().editOriginal(
                                    "Either you already have an ongoing game with that word or you have too many games active at once!")
                                .queue();
                            else event.getHook().editOriginal("Game created in " + gameChannel.getAsMention())
                                .queue();
                        } catch (IOException e) {
                            event.getHook()
                                .editOriginal("Couldn't generate a random word! Please try again later.")
                                .queue();
                            System.out.print(new Utils().getTime(Utils.LogColor.RED));
                            e.printStackTrace();
                        }
                    }

                    case "daily" -> {
                        File daily = new File("Phoenella/Wordle/played-daily.txt");
                        try {
                            Scanner dailyPlays = new Scanner(daily);
                            ArrayList<String> plays = new ArrayList<>();
                            while (dailyPlays.hasNextLine()) plays.add(dailyPlays.nextLine());

                            if (plays.toString().contains(event.getMember().getId())) {
                                event.reply("You've already played today's Wordle!").setEphemeral(true)
                                    .queue();
                                return;
                            }
                        } catch (FileNotFoundException e) {
                            System.out.print(new Utils().getTime(Utils.LogColor.RED));
                            e.printStackTrace();
                        }

                        event.reply("Creating a game...").setEphemeral(true).queue();
                        try {
                            File dailyWord = new File("Phoenella/Wordle/daily.txt");
                            String word = new Scanner(dailyWord).next();

                            FileWriter fw = new FileWriter(daily, true);
                            fw.write(event.getMember().getId() + "\n");
                            fw.close();

                            TextChannel gameChannel = new Wordle().startGame(event.getMember(), word, false,
                                true, null);
                            if (gameChannel == null) event.getHook().editOriginal(
                                    "Either you already have an ongoing game with that word or you have too many games active at once!")
                                .queue();
                            else event.getHook().editOriginal("Game created in " + gameChannel.getAsMention())
                                .queue();
                        } catch (IOException e) {
                            event.getHook()
                                .editOriginal("Couldn't generate a random word! Please try again later.")
                                .queue();
                            System.out.print(new Utils().getTime(Utils.LogColor.RED));
                            e.printStackTrace();
                        }
                    }

                    case "leaderboard" -> {
                        if (Phoenella.localWordleBoard)
                            event.reply("The leaderboard is currently disabled!").setEphemeral(true).queue();
                        else {
                            boolean ephemeral = true;
                            if (event.getChannel().asTextChannel()
                                .getParentCategoryIdLong() != 900747596245639238L)
                                if (event.getOption("show") != null)
                                    ephemeral = !event.getOption("show").getAsBoolean();

                            event.deferReply(ephemeral).queue();

                            Scanner leaderboard = null;
                            try {
                                leaderboard = new Scanner(new File("Phoenella/Wordle/leaderboard.txt"));
                            } catch (FileNotFoundException e) {
                                System.out.print(new Utils().getTime(Utils.LogColor.RED));
                                e.printStackTrace();
                            }
                            HashMap<Member, Integer> lines = new HashMap<>();

                            while (leaderboard.hasNextLine()) try {
                                String line = leaderboard.nextLine();
                                long id = Long.parseLong(line.replaceFirst(":.*", ""));
                                int score = Integer.parseInt(line.replaceFirst(".*:", ""));
                                Member member = event.getGuild().getMemberById(id);
                                lines.put(member, score);
                            } catch (NoSuchElementException ignored) {
                            }

                            LinkedHashMap<Member, Integer> sortedLeaderboard = new LinkedHashMap<>();
                            lines.entrySet().stream()
                                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                                .forEachOrdered(x -> sortedLeaderboard.put(x.getKey(), x.getValue()));

                            StringBuilder finalLeaderboard = new StringBuilder("```\n");
                            int index = 1;
                            for (Member member : sortedLeaderboard.keySet()) {
                                if (index > 10) break;
                                finalLeaderboard.append(index).append(". ")
                                    .append(new Utils().getFullName(member)).append(" | ")
                                    .append(sortedLeaderboard.get(member)).append("\n");
                                index++;
                            }
                            finalLeaderboard.append("```");

                            EmbedBuilder embed = new EmbedBuilder();
                            embed.setColor(new Color(47, 49, 54));
                            embed.setTitle("Wordle Leaderboard");
                            embed.setFooter("User-generated words are not counted");
                            embed.setDescription(finalLeaderboard);

                            event.getHook().editOriginalEmbeds(embed.build()).queue();
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (event.getModalId().equals("customwordle")) {
            String word = event.getValue("word").getAsString().replaceAll("[^a-zA-Z]", "");

            if (word.length() < 4) {
                event.reply("**Error:** Invalid characters!").setEphemeral(true).queue();
                return;
            }

            int tries;
            try {
                tries = Integer.parseInt(event.getValue("tries").getAsString());
            } catch (NumberFormatException ignored) {
                tries = 6;
            }

            if (tries < 4 || tries > 8) {
                event.reply("**Error:** Tries must be between 4-8!").setEphemeral(true).queue();
                return;
            }

            event.reply("Creating challenge for word **" + word + "** in <#956267174727671869>")
                .setEphemeral(true).queue();

            EmbedBuilder embed = new EmbedBuilder();
            embed.setAuthor(new Utils().getFullName(event.getMember()) + " has created a Wordle!", null,
                event.getMember().getEffectiveAvatarUrl());
            embed.setColor(new Color(47, 49, 54));
            embed.addField("Plays", "0", true);
            embed.addField("Passes", "0", true);
            embed.addField("Fails", "0", true);

            event.getJDA().getTextChannelById(956267174727671869L).sendMessageEmbeds(embed.build())
                .setActionRow(Button.success("playwordle:" + word + ":" + tries, "Play it!")).queue();
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        switch (event.getComponentId()) {
            case "definitionreport" -> {
                Phoenella.jda.openPrivateChannelById(277291758503723010L).flatMap(
                        channel -> channel.sendMessage(
                            ":warning: Definition report from **" + new Utils().getFullName(
                                event.getMember()) + ":**").setEmbeds(event.getMessage().getEmbeds().get(0)))
                    .queue();

                event.deferEdit().queue();

                Message message = event.getMessage();
                if (!message.isEphemeral()) message.delete().queue();
                else event.getHook().editOriginalComponents(ActionRow.of(event.getButton().asDisabled()))
                    .queue();
            }

            case "viewroles" -> {
                event.deferReply(true).queue();

                StringBuilder roleList = new StringBuilder();
                StringBuilder notRoleList = new StringBuilder();
                for (SelectOption option : ((StringSelectMenu) event.getMessage().getActionRows().get(0)
                    .getComponents().get(0)).getOptions()) {
                    Role role = event.getGuild().getRoleById(option.getValue());

                    if (event.getMember().getRoles().contains(role))
                        roleList.append(role.getAsMention()).append("\n");
                    else notRoleList.append(role.getAsMention()).append("\n");
                }

                if (roleList.isEmpty()) roleList.append("None");
                if (notRoleList.isEmpty()) notRoleList.append("None");

                EmbedBuilder embed = new EmbedBuilder();
                embed.addField("You have", roleList.toString(), true);
                embed.addBlankField(true);
                embed.addField("You don't have", notRoleList.toString(), true);
                embed.setColor(new Color(47, 49, 54));

                event.getHook().editOriginalEmbeds(embed.build()).queue();
            }
        }
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (event.getComponentId()) {
            case "role-select" -> {
                if (event.getSelectedOptions().isEmpty()) {
                    event.deferEdit().queue();
                    return;
                }

                event.deferReply(true).queue();
                Guild guild = event.getGuild();
                Member member = event.getMember();

                new Thread(() -> {
                    for (SelectOption option : event.getSelectedOptions()) {
                        Role role = guild.getRoleById(option.getValue());
                        if (member.getRoles().contains(role)) {
                            System.out.println(
                                new Utils().getTime(Utils.LogColor.YELLOW) + "Removing " + role.getName()
                                    .toLowerCase() + " role from '" + member.getEffectiveName() + "'");
                            guild.removeRoleFromMember(member, role).complete();

                        } else {
                            System.out.println(
                                new Utils().getTime(Utils.LogColor.YELLOW) + "Adding " + role.getName()
                                    .toLowerCase() + " role to '" + member.getEffectiveName() + "'");
                            guild.addRoleToMember(member, role).complete();
                        }
                    }

                    ArrayList<Role> roles = new ArrayList<>(member.getRoles().stream().toList());
                    // Student
                    roles.remove(guild.getRoleById(892267754730709002L));
                    // Button
                    roles.remove(guild.getRoleById(892453842241859664L));

                    // If the person has no other roles
                    if (roles.isEmpty())
                        new Utils().removeRoleFromMember(member, guild, Utils.RoleType.BUTTON);
                        // If the person does have other roles
                    else new Utils().addRoleToMember(member, guild, Utils.RoleType.BUTTON);

                    // Show user's roles
                    StringBuilder roleList = new StringBuilder();
                    StringBuilder notRoleList = new StringBuilder();
                    for (SelectOption option : ((StringSelectMenu) event.getMessage().getActionRows().get(0)
                        .getComponents().get(0)).getOptions()) {
                        Role role = event.getGuild().getRoleById(option.getValue());

                        if (event.getMember().getRoles().contains(role))
                            roleList.append(role.getAsMention()).append("\n");
                        else notRoleList.append(role.getAsMention()).append("\n");
                    }

                    if (roleList.isEmpty()) roleList.append("None");
                    if (notRoleList.isEmpty()) notRoleList.append("None");

                    EmbedBuilder embed = new EmbedBuilder();
                    embed.addField("You have", roleList.toString(), true);
                    embed.addBlankField(true);
                    embed.addField("You don't have", notRoleList.toString(), true);
                    embed.setColor(new Color(47, 49, 54));

                    event.getHook().editOriginalEmbeds(embed.build()).queue();
                }, "Add Roles - " + new Utils().getFirstName(member)).start();
            }
        }
    }
}
