package me.jasonhorkles.phoenella;

import com.mattmalec.pterodactyl4j.PteroBuilder;
import com.mattmalec.pterodactyl4j.client.entities.ClientServer;
import com.mattmalec.pterodactyl4j.client.entities.PteroClient;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.ErrorResponse;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@SuppressWarnings("ConstantConditions")
public class Events extends ListenerAdapter {
    private static final Map<TextChannel, Integer> channelCooldown = new HashMap<>();
    private static final ArrayList<String> messageCooldown = new ArrayList<>();

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.isFromGuild()) return;
        if (event.getGuild().getIdLong() != 729083627308056597L) return;
        Member member = event.getMember();
        if (member.getUser().isBot()) return;

        boolean isReply = false;
        if (event.getMessage().getMessageReference() != null)
            if (event.getMessage().getMessageReference().getMessage().getAuthor().equals(Phoenella.api.getSelfUser()))
                isReply = true;

        if (event.getMessage().getContentStripped().toLowerCase().contains("phoe") || isReply) {

            if ((event.getMessage().getContentRaw().toLowerCase().contains("phoenix") || event.getMessage()
                .getContentRaw().toLowerCase().contains("`phoe`")) && !isReply) return;

            String message = event.getMessage().getContentStripped().toLowerCase().replace("phoenella", "")
                .replace("phoe", "").trim();

            // Utility

            if (message.contains("update members")) {
                if (member.getIdLong() != 277291758503723010L) {
                    event.getMessage().reply("no").queue();
                    return;
                }

                new Utils().sendMessage(null, event.getMessage(), "Updating members... See console for details.",
                    false);
                new Utils().runNameCheckForGuild(event.getGuild());
                return;
            }

            if (message.contains("stop") || message.contains("shut down"))
                if (member.getIdLong() == 277291758503723010L) {
                    event.getMessage().reply("Shutting down...").mentionRepliedUser(false).queue();
                    PteroClient api = PteroBuilder.createClient(new Secrets().getPteroUrl(),
                        new Secrets().getPteroApiKey());
                    api.retrieveServerByIdentifier("af9d05bc").flatMap(ClientServer::stop).executeAsync();
                    return;
                }

            if (message.contains("restart")) if (member.getIdLong() == 277291758503723010L) {
                event.getMessage().reply("Restarting...").mentionRepliedUser(false).complete();
                System.exit(0);
                return;
            }

            TextChannel channel = event.getTextChannel();

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
                e.printStackTrace();
            }

            if (message.contains("un-shush") || message.contains("unshush") || message.contains("speak")) {
                if (!member.hasPermission(Permission.MESSAGE_MANAGE)) return;

                try {
                    disabledChannels.remove(channel.getId());
                    FileWriter fileWriter = new FileWriter("Phoenella/channel-blacklist.txt", false);
                    for (String channels : disabledChannels) fileWriter.write(channels + "\n");
                    fileWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    event.getMessage().reply(":see_no_evil: Uh oh! There's an error! <@277291758503723010>")
                        .mentionRepliedUser(false).queue();
                }

                event.getMessage().addReaction("\uD83D\uDE2E").queue();
                return;
            }

            if (disabledChannels.contains(channel.getId())) return;

            if (message.contains("shut up") || message.contains("shush") || message.contains("be quiet")) {
                if (!member.hasPermission(Permission.MESSAGE_MANAGE)) return;

                try {
                    disabledChannels.add(channel.getId());
                    FileWriter fileWriter = new FileWriter("Phoenella/channel-blacklist.txt", false);
                    for (String channels : disabledChannels) fileWriter.write(channels + "\n");
                    fileWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    event.getMessage().reply(":see_no_evil: Uh oh! There's an error! <@277291758503723010>")
                        .mentionRepliedUser(false).queue();
                }

                event.getMessage().addReaction("🙊").queue();
                return;
            }

            // Fun

            if (message.contains("say ")) {
                if (member.getIdLong() != 277291758503723010L) {
                    new Utils().sendMessage(channel, null, "no", false);
                    return;
                }

                channel.sendMessage(event.getMessage().getContentRaw().replaceAll("(?i).*say ", "")).queue();
                event.getMessage().delete().queue();
                return;
            }

            boolean allCaps = event.getMessage().getContentStripped()
                .equals(event.getMessage().getContentStripped().toUpperCase());

            String msg = "Well dadgum, something went wrong!";

            if (message.contains("play rock paper scissors") || message.contains("play rps")) {
                channel.sendTyping().complete();
                TextChannel gameChannel = new GameManager().createGame(GameManager.Game.RPS, event.getMessage(), member,
                    true, null);
                if (gameChannel == null) event.getMessage().reply("You must ping an opponent in your message!").queue();
                else {
                    event.getMessage().addReaction("👍").queue();
                    event.getMessage().reply("Game created in " + gameChannel.getAsMention()).complete().delete()
                        .queueAfter(15, TimeUnit.SECONDS);
                }
                return;
            }

            if (message.contains("play wordle") || message.contains("wordle me")) {
                channel.sendTyping().complete();
                TextChannel gameChannel = new GameManager().createGame(GameManager.Game.WORDLE, event.getMessage(),
                    member, false, null);
                event.getMessage().addReaction("👍").queue();
                event.getMessage().reply("Game created in " + gameChannel.getAsMention()).complete().delete()
                    .queueAfter(15, TimeUnit.SECONDS);
                return;
            }

            // Ignore shushed people
            if (member.getRoles().toString().contains("842490529744945192")) return;

            // Message cooldowns
            for (String s : messageCooldown)
                if (s.equalsIgnoreCase(message)) {
                    event.getMessage().addReaction("\uD83E\uDD2B").queue();
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

            messageCooldown.add(message);
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    messageCooldown.remove(message);
                }
            };
            Timer timer = new Timer("Message Cooldown");
            timer.schedule(task, 180000); // 180000 = 3 minutes

            channel.sendTyping().complete();

            Random r = new Random();

            if (message.equals("")) {
                msg = new Utils().getFirstName(member);

                new Utils().sendMessage(channel, null, msg, allCaps);
                return;
            }

            if (message.equals("?")) {
                msg = new Utils().getFirstName(member) + "?";

                new Utils().sendMessage(channel, null, msg, allCaps);
                return;
            }

            if (message.equals("!")) {
                msg = "WHAT DO YOU WANT " + new Utils().getFirstName(member) + " AAAAAAAAAAAAAAAAAAAAA";

                new Utils().sendMessage(channel, null, msg, true);
                return;
            }

            if (message.length() <= 1) {
                msg = "can you speak english please";

                new Utils().sendMessage(channel, null, msg, allCaps);
                return;
            }

            if (message.startsWith("hi") || message.endsWith("hi") || message.contains("hello")) {
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

            if (message.startsWith("ily") || message.endsWith("ily")) {
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

            if (message.contains("thank") || message.contains("thx")) {
                int number = r.nextInt(4);
                switch (number) {
                    case 0 -> msg = "you're welcome!";
                    case 1 -> msg = "no problem!";
                    case 2 -> msg = "my pleasure";
                    case 3 -> msg = "yuh np dude";
                }

                new Utils().sendMessage(channel, null, msg, allCaps);
                return;
            }

            if (message.contains("color")) {
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

            if ((message.contains("what") || message.contains("plz")) && (message.contains("time") || message.contains(
                "date") || message.contains("day") || message.contains("month") || message.contains("year"))) {
                msg = "It's currently";

                if (allCaps) msg = msg.toUpperCase();
                msg = msg + " <t:" + System.currentTimeMillis() / 1000 + ":F>";

                new Utils().sendMessage(channel, null, msg, false);
                return;
            }

            if (message.replace(" ", "").contains("9+10")) {
                msg = "21";

                new Utils().sendMessage(channel, null, msg, allCaps);
                return;
            }

            if (message.contains("random person")) {
                List<Member> members = event.getGuild().getMembers();
                Member randomMember = members.get(r.nextInt(members.size() - 1));
                msg = new Utils().getFirstName(randomMember);

                new Utils().sendMessage(channel, null, msg, allCaps);
                return;
            }

            if (message.contains("game list")) {
                msg = "Rock Paper Scissors (RPS)\nWordle";

                channel.sendMessage(msg).queue();
                return;
            }

            if (message.contains("search ")) {
                msg = new Utils().lookUp(message.replace("search ", ""), new Utils().getFirstName(member))
                    .toLowerCase();
                if (msg.equals("501")) msg = "i'm not gonna search that";

                new Utils().sendMessage(null, event.getMessage(), msg, allCaps);
                return;
            }

            msg = new Utils().lookUp(message, new Utils().getFirstName(member));
            if (msg.equals("501")) {
                int number = r.nextInt(19);
                switch (number) {
                    case 0 -> msg = "no";
                    case 1 -> msg = "yes";
                    case 2 -> msg = "what";
                    case 3 -> msg = "ok";
                    case 4 -> msg = "<:kek:841681203278774322>";
                    case 5 -> msg = "what u want";
                    case 6 -> msg = "hmm";
                    case 7 -> msg = "excuse me?";
                    case 8 -> msg = "pardon?";
                    case 9 -> msg = "no way!";
                    case 10 -> msg = "¯\\_(ツ)_/¯";
                    case 11 -> msg = "heck no dawg!";
                    case 12 -> msg = "yup";
                    case 13 -> msg = "nah";
                    case 14 -> msg = "of course!";
                    case 15 -> msg = "heh";
                    case 16 -> msg = "i'm not sure if you'd want to know that answer...";
                    case 17 -> msg = "<:thonk:893175427684765737>";
                    case 18 -> msg = "of course not!";
                }
            }
            new Utils().sendMessage(null, event.getMessage(), msg, allCaps);
        }
    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        // Button clicker role
        if (event.getChannel().getIdLong() == 892104640567578674L)
            event.getGuild().addRoleToMember(event.getMember(), event.getGuild().getRoleById(892453842241859664L))
                .queue();

        if (event.getUser().isBot()) return;

        // kekw
        if (event.getReactionEmote().getName().equalsIgnoreCase("kek"))
            event.retrieveMessage().complete().addReaction(event.getGuild().getEmoteById("841681203278774322")).queue();

        // Shush users
        if (!event.getReactionEmote().isEmoji()) return;
        if (event.getReactionEmote().getEmoji().equals("\uD83E\uDD2B"))
            if (event.getMember().getRoles().toString().contains("751166721624375435") || event.getMember().getRoles()
                .toString().contains("729108220479537202")) {
                Member member = event.retrieveMessage().complete().getMember();
                // 600000 = 10 mins
                if (member.getRoles().toString().contains("751166721624375435") || member.getRoles().toString()
                    .contains("729108220479537202"))
                    event.getChannel().sendMessage(event.getMember().getAsMention() + ", I can't shush that person!")
                        .complete().delete().queueAfter(5, TimeUnit.SECONDS, null,
                            new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
                else if (new Utils().shush(member, 600000)) event.getChannel()
                    .sendMessage(new Utils().getFirstName(member) + " just got shushed\nhttps://tenor.com/vfW7.gif")
                    .complete().delete()
                    .queueAfter(15, TimeUnit.SECONDS, null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
                else event.getChannel().sendMessage(event.getMember()
                            .getAsMention() + ", that person is either already shushed or there was an error!").complete()
                        .delete().queueAfter(5, TimeUnit.SECONDS, null,
                            new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
            }
    }

    @Override
    public void onMessageReactionRemove(MessageReactionRemoveEvent event) {
        if (event.getChannel().getIdLong() != 892104640567578674L) return;

        try {
            for (MessageReaction msgReactions : new Utils().getMessages(event.getChannel(), 1).get(30, TimeUnit.SECONDS)
                .get(0).getReactions())
                for (User reactionUsers : msgReactions.retrieveUsers().complete())
                    if (reactionUsers == event.getUser()) return;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }

        event.getGuild().removeRoleFromMember(event.getMember(), event.getGuild().getRoleById(892453842241859664L))
            .queue();
    }

    @Override
    public void onGuildMemberUpdateNickname(GuildMemberUpdateNicknameEvent event) {
        Member member = event.getMember();
        Guild guild = event.getGuild();
        String newNickname = event.getNewNickname();

        System.out.println(new Utils().getTime(Utils.Color.GREEN) + event.getMember().getUser()
            .getAsTag() + " changed their nickname from '" + event.getOldNickname() + "' to '" + newNickname + "'");

        // If not sushed
        if (!member.getRoles().toString().contains("842490529744945192"))
            new Utils().runNameCheckForUser(newNickname, member, guild);
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName().toLowerCase()) {
            case "shush" -> {
                Member member = event.getOption("user").getAsMember();
                long duration = event.getOption("duration").getAsLong() * 60000;

                if (!event.getMember().getRoles().toString().contains("751166721624375435") && !event.getMember()
                    .getRoles().toString().contains("729108220479537202")) {
                    event.reply("You can't shush people!").setEphemeral(true).queue();
                    return;
                }

                if (member.getRoles().toString().contains("751166721624375435") || member.getRoles().toString()
                    .contains("729108220479537202")) {
                    event.reply("I can't shush " + new Utils().getFirstName(member) + "!").setEphemeral(true).queue();
                    return;
                }

                if (new Utils().shush(member, duration)) event.reply("Successfully shushed " + new Utils().getFirstName(
                    member) + " for " + duration / 60000 + " minute(s)!").setEphemeral(true).queue();
                else event.reply("Unable to shush " + new Utils().getFirstName(member) + "! Are they already shushed?")
                    .setEphemeral(true).queue();
            }

            case "unshush" -> {
                if (!event.getMember().getRoles().toString().contains("751166721624375435") && !event.getMember()
                    .getRoles().toString().contains("729108220479537202")) {
                    event.reply("You can't shush people!").setEphemeral(true).queue();
                    return;
                }

                Member member = event.getOption("user").getAsMember();
                new Utils().unshush(member);
                event.reply("Unshushing " + new Utils().getFirstName(member) + "!").setEphemeral(true).queue();
            }

            case "wordle" -> {
                String word = event.getOption("word").getAsString().replaceAll("[^a-zA-Z]", "");
                if (word.length() < 4 || word.length() > 8) {
                    event.reply("Your word must be between 4-8 characters!").setEphemeral(true).queue();
                    return;
                }

                event.reply("Creating challenge for word **" + word + "** in <#956267174727671869>").setEphemeral(true)
                    .queue();
                event.getJDA().getTextChannelById(956267174727671869L)
                    .sendMessage(event.getMember().getAsMention() + " has created a Wordle!")
                    .setActionRow(Button.success("playwordle:" + word, "Play it!")).queue();
            }
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (event.getComponentId().startsWith("playwordle:")) {
            String word = event.getComponentId().replace("playwordle:", "");
            event.reply("Creating a game...").setEphemeral(true).queue();
            TextChannel gameChannel = new GameManager().createGame(GameManager.Game.WORDLE, null, event.getMember(),
                false, word);
            event.getHook().editOriginal("Game created in " + gameChannel.getAsMention()).queue();
        }
    }
}
