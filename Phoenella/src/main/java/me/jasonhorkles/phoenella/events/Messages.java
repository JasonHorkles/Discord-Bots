package me.jasonhorkles.phoenella.events;

import com.mattmalec.pterodactyl4j.PteroBuilder;
import com.mattmalec.pterodactyl4j.client.entities.ClientServer;
import com.mattmalec.pterodactyl4j.client.entities.PteroClient;
import me.jasonhorkles.phoenella.Nicknames;
import me.jasonhorkles.phoenella.Phoenella;
import me.jasonhorkles.phoenella.Secrets;
import me.jasonhorkles.phoenella.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("DataFlowIssue")
public class Messages extends ListenerAdapter {
    private static final Map<TextChannel, Integer> channelCooldown = new HashMap<>();
    private static final List<String> messageCooldown = new ArrayList<>();

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.isFromGuild()) return;
        if (event.getGuild().getIdLong() != 729083627308056597L) return;
        if (event.getAuthor().isBot()) return;
        if (event.getChannelType() != ChannelType.TEXT) return;

        Member member = event.getMember();
        TextChannel channel = event.getChannel().asTextChannel();
        Message message = event.getMessage();

        boolean notReply = true;
        if (message.getMessageReference() != null)
            if (message.getMessageReference().getMessage().getAuthor().equals(Phoenella.jda.getSelfUser()))
                notReply = false;

        String text = message.getContentRaw().replaceAll("(?i)\\bphoenella\\b", "PHOENELLA").replaceAll("(?i)\\bphoe\\b",
            "PHOENELLA").strip();

        if (!text.contains("PHOENELLA") && notReply && channel.getIdLong() != 892802385301352548L) return;

        text = text.replace("  ", " ").replaceAll(" ?PHOENELLA ?", "").toLowerCase();

        // Utility

        if (text.equals("update members") || text.equals("check nicknames")) {
            if (member.getIdLong() != 277291758503723010L) {
                message.reply("no").queue();
                return;
            }

            new Utils().sendMessage(null, message, "Updating members... See console for details.", false);
            new Thread(() -> new Nicknames().runNameCheckForGuild(false)).start();
            return;
        }

        if (text.equals("stop") || text.equals("shut down")) if (member.getIdLong() == 277291758503723010L) {
            message.reply("Shutting down...").mentionRepliedUser(false).queue();
            PteroClient ptero = PteroBuilder.createClient(
                new Secrets().pteroUrl(),
                new Secrets().pteroApiKey());
            ptero.retrieveServerByIdentifier("5243694c").flatMap(ClientServer::stop).executeAsync();
            return;
        }

        if (text.equals("restart")) if (member.getIdLong() == 277291758503723010L) {
            message.reply("Restarting...").mentionRepliedUser(false).complete();
            System.exit(0);
            return;
        }

        // Don't say anything if in a game channel
        if (channel.getParentCategory() != null)
            if (channel.getParentCategoryIdLong() == 900747596245639238L) return;

        List<String> disabledChannels = new ArrayList<>();
        try {
            File file = new File("Phoenella/channel-blacklist.txt");
            Scanner fileScanner = new Scanner(file, StandardCharsets.UTF_8);
            while (fileScanner.hasNextLine()) disabledChannels.add(fileScanner.nextLine());
            fileScanner.close();

        } catch (NoSuchElementException ignored) {
        } catch (IOException e) {
            System.out.print(new Utils().getTime(Utils.LogColor.RED));
            e.printStackTrace();
        }

        if (text.contains("un-shush") || text.contains("unshush") || text.contains("speak")) {
            if (!member.hasPermission(Permission.MESSAGE_MANAGE)) return;

            try {
                disabledChannels.remove(channel.getId());
                FileWriter fileWriter = new FileWriter(
                    "Phoenella/channel-blacklist.txt",
                    StandardCharsets.UTF_8,
                    false);
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
                FileWriter fileWriter = new FileWriter(
                    "Phoenella/channel-blacklist.txt",
                    StandardCharsets.UTF_8,
                    false);
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

        if ((text.length() == 3 || text.length() == 4) && !text.equals("ily")) return;

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

        switch (text) {
            case "" -> {
                msg = new Utils().getFirstName(member);

                new Utils().sendMessage(channel, null, msg, allCaps);
                return;
            }

            case "?" -> {
                msg = new Utils().getFirstName(member) + "?";

                new Utils().sendMessage(channel, null, msg, allCaps);
                return;
            }

            case "!" -> {
                msg = "WHAT DO YOU WANT " + new Utils().getFirstName(member) + " AAAAAAAAAAAAAAAAAAAAA";

                new Utils().sendMessage(channel, null, msg, true);
                return;
            }
        }

        if (text.length() == 1) {
            msg = "can you speak english please";

            new Utils().sendMessage(channel, null, msg, allCaps);
            return;
        }

        if (text.startsWith("hi") || text.contains("hello")) {
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

        if (text.equals("ily")) {
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

        if ((text.contains("what") || text.contains("plz")) && (text.contains("time") || text.contains("date") || text.contains(
            "day") || text.contains("month") || text.contains("year"))) {
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
            msg = "1. Rock Paper Scissors (</rps:1328257507394584617>)\n2. Wordle (</wordle play:960007688580919306>)";

            channel.sendMessage(msg).queue();
            return;
        }

        if (text.startsWith("define ")) {
            Utils.DefinitionResult result = new Utils().defineWord(text.replace("define ", ""));
            MessageEmbed embed = result.embed();

            if (embed.getDescription() != null) if (embed.getDescription().startsWith("Couldn't find "))
                message.replyEmbeds(embed).mentionRepliedUser(false).queue();

            else if (result.isSuccessful()) message.replyEmbeds(embed).setActionRow(Button
                    .danger("definitionreport", "Report definition").withEmoji(Emoji.fromUnicode("🚩")))
                .mentionRepliedUser(false).queue();

            else message.replyEmbeds(embed).mentionRepliedUser(false).queue();

            return;
        }

        if (text.startsWith("are you sure")) {
            int number = r.nextInt(3);
            switch (number) {
                case 0 -> msg = "Don't question me foo";
                case 1 -> msg = "foo don't question me";
                case 2 -> msg = "you questioning me?";
            }

            new Utils().sendMessage(channel, null, msg, allCaps);
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
                case 17 -> msg = "https://media.tenor.com/2yowPi7b_K0AAAAC/despicbable-me-minions.gif";
                case 18 -> msg = "https://media.tenor.com/YnWX1mX4ujAAAAAC/hmm-dr-iggy-frome.gif";
                case 19 ->
                    msg = "https://media.tenor.com/zXlmaqK4nDYAAAAC/ok-oh-yes-yes-o-yeah-yes-no-yes-go-on-yea-yes.gif";
            }
        }
        new Utils().sendMessage(null, message, msg, allCaps);
    }
}
