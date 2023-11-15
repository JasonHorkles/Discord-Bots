package me.jasonhorkles.silverstone;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@SuppressWarnings("DataFlowIssue")
public class Events extends ListenerAdapter {
    public static int lastNumber;

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + event.getMember()
            .getEffectiveName() + " used the /" + event.getName() + " command");

        // If in discord server, not a staff member or admin, and in the wrong channel, make it private
        boolean ephemeral = event.isFromGuild() && event.getGuild()
            .getIdLong() == 455919765999976461L && !(event.getMember().getRoles().contains(
            event.getJDA().getGuildById(455919765999976461L)
                .getRoleById(667793980318154783L)) || event.getMember()
            .hasPermission(Permission.ADMINISTRATOR)) && !(event.getChannel()
            .getIdLong() == 456470772207190036L || event.getChannel().getIdLong() == 468416589331562506L);

        switch (event.getName().toLowerCase()) {
            case "paste" -> event.reply("Please copy and paste your `" + event.getOption("what")
                    .getAsString() + "` file(s) to <https://paste.gg/>\nThen, click \"Submit anonymously\" and post the link in this channel.")
                .queue();

            case "ecdebug" -> event.reply(
                    "Please run the command `/ecl debug` in-game.\nOnce everything has completed, upload the newly created debug dump file from the EntityClearer plugin folder (`/plugins/EntityClearer`) to this channel.")
                .queue();

            case "plgh" -> event.reply("""
                **EntityClearer:** <https://github.com/SilverstoneMC/EntityClearer>
                **ExpensiveDeaths:** <https://github.com/SilverstoneMC/ExpensiveDeaths>
                **FileCleaner:** <https://github.com/SilverstoneMC/FileCleaner>
                **BungeeNicks:** <https://github.com/SilverstoneMC/BungeeNicks>
                """).setEphemeral(ephemeral).queue();

            case "plugins" -> event.reply("See Jason's plugins at: <https://hangar.papermc.io/JasonHorkles>")
                .setEphemeral(ephemeral).queue();

            case "tutorials" -> event.reply(
                    "JasonHorkles Tutorials: <https://www.youtube.com/channel/UCIyJ0zf3moNSRN1wIetpbmA>")
                .setEphemeral(ephemeral).queue();

            case "moss" -> event.reply("Get EssentialsX help and more here: https://discord.gg/PHpuzZS")
                .setEphemeral(ephemeral).queue();

            case "lp" ->
                event.reply("Get LuckPerms help here: https://discord.gg/luckperms").setEphemeral(ephemeral)
                    .queue();

            case "config" -> {
                String plugin = event.getOption("plugin").getAsString();
                event.reply(
                        "See the " + plugin + " config at: <https://github.com/SilverstoneMC/" + plugin + "/blob/main/src/main/resources/config.yml>")
                    .setEphemeral(ephemeral).queue();
            }
        }
    }

    @Override
    public void onMessageContextInteraction(MessageContextInteractionEvent event) {
        if (event.getName().equals("Upload file(s) to paste.gg")) new Thread(() -> {
            ArrayList<Message.Attachment> attachments = new ArrayList<>();

            for (Message.Attachment attachment : event.getTarget().getAttachments())
                if (!attachment.isImage() && !attachment.isVideo()) attachments.add(attachment);

            if (attachments.isEmpty()) {
                event.reply("That message has no valid files!").setEphemeral(true).queue();
                return;
            }

            event.deferReply(false).queue();

            // Build the json
            try {
                JSONObject json = new JSONObject();
                json.put("name", "Silverstone");
                json.put("visibility", "unlisted");
                json.put("expires", Instant.now().plus(7, ChronoUnit.DAYS));

                JSONArray files = new JSONArray();
                for (Message.Attachment attachment : attachments) {
                    JSONObject file = new JSONObject();

                    boolean isLog = attachment.getFileExtension().equalsIgnoreCase("log");
                    if (isLog) file.put("name", attachment.getFileName().replace(".log", ".accesslog"));
                    else file.put("name", attachment.getFileName());

                    JSONObject content = new JSONObject();
                    content.put("format", "text");
                    try (InputStream bytes = attachment.getProxy().download().join()) {
                        content.put("value", new String(bytes.readAllBytes(), StandardCharsets.UTF_8));
                    }
                    file.put("content", content);

                    files.put(file);
                }
                json.put("files", files);

                // Send the request
                URL url = new URL("https://api.paste.gg/v1/pastes");
                URLConnection con = url.openConnection();
                HttpURLConnection http = (HttpURLConnection) con;
                http.setRequestMethod("POST");
                http.setDoOutput(true);

                byte[] out = json.toString().getBytes(StandardCharsets.UTF_8);
                int length = out.length;

                http.setFixedLengthStreamingMode(length);
                http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                http.setRequestProperty("Authorization", "Key " + new Secrets().getPasteKey());
                http.connect();
                try (OutputStream os = http.getOutputStream()) {
                    os.write(out);
                }

                // Read the response
                InputStream input = http.getInputStream();
                JSONObject returnedText = new JSONObject(
                    new String(input.readAllBytes(), StandardCharsets.UTF_8));

                if (returnedText.getString("status").equals("success")) {
                    String id = returnedText.getJSONObject("result").getString("id");
                    event.getHook().editOriginal("<https://paste.gg/p/JasonHorkles/" + id + ">").queue();

                } else if (returnedText.getString("status").equals("error")) event.getHook().editOriginal(
                        "## Error: " + returnedText.getString("error") + "\n" + returnedText.getString("message"))
                    .queue();

            } catch (Exception e) {
                System.out.print(new Utils().getTime(Utils.LogColor.RED));
                e.printStackTrace();
                event.getHook()
                    .editOriginal("An error occurred while uploading the file(s)! (" + e.getMessage() + ")")
                    .queue();
            }
        }, "Upload Files to Paste.gg").start();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        // Plugin support thread
        if (event.getMessage().getChannelType() == ChannelType.GUILD_PUBLIC_THREAD)
            if (event.getGuildChannel().asThreadChannel().getParentChannel()
                .getIdLong() == 1023735878075564042L && event.getAuthor()
                .getIdLong() == 277291758503723010L) {
                Message message = event.getMessage();

                // Thanks for coming :)
                if (message.getContentStripped().toLowerCase().startsWith("np")) {

                    EmbedBuilder embed = new EmbedBuilder();

                    embed.addField("Spigot", """
                        [BungeeNicks](https://www.spigotmc.org/resources/bungeenicks.110948/)
                        [EntityClearer](https://www.spigotmc.org/resources/entityclearer.90802/)
                        [ExpensiveDeaths](https://www.spigotmc.org/resources/expensivedeaths.96065/)
                        [FileCleaner](https://www.spigotmc.org/resources/filecleaner.93372/)""", true);
                    embed.addField("Hangar", """
                        [BungeeNicks](https://hangar.papermc.io/JasonHorkles/BungeeNicks)
                        [EntityClearer](https://hangar.papermc.io/JasonHorkles/EntityClearer)
                        [ExpensiveDeaths](https://hangar.papermc.io/JasonHorkles/ExpensiveDeaths)
                        [FileCleaner](https://hangar.papermc.io/JasonHorkles/FileCleaner)""", true);
                    embed.setColor(new Color(43, 45, 49));
                    embed.setFooter("This post will now be closed. Send a message to re-open it.");

                    event.getChannel().sendMessage(
                            "Thank you for coming. If you enjoy the plugin and are happy with the support you received, please consider leaving a review on Spigot, or a star on Hangar \\:)")
                        .addEmbeds(embed.build()).queue(
                            na -> event.getChannel().asThreadChannel().getManager().setArchived(true)
                                .queueAfter(1, TimeUnit.SECONDS));
                    return;
                }

                // Ping OP
                if (message.getMessageReference() == null) try {
                    List<Message> messages = new Utils().getMessages(event.getChannel(), 2)
                        .get(30, TimeUnit.SECONDS);
                    if (messages.size() < 2) return;

                    OffsetDateTime fiveMinsAgo = OffsetDateTime.now().minusMinutes(5);
                    if (messages.get(1).getTimeCreated().isAfter(fiveMinsAgo)) return;

                    Member op = event.getChannel().asThreadChannel().getOwner();
                    if (op == null) return;

                    User author = messages.get(1).getAuthor();
                    if (author != op.getUser()) return;

                    event.getChannel().sendMessage(op.getAsMention()).queue(del -> del.delete()
                        .queueAfter(100, TimeUnit.MILLISECONDS, null,
                            new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE)));

                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    System.out.print(new Utils().getTime(Utils.LogColor.RED));
                    e.printStackTrace();
                }
            }

        // Direct to plugin support (not in thread)
        if (event.getMessage().getChannelType() != ChannelType.GUILD_PUBLIC_THREAD && !event.getMember()
            .getRoles().toString().contains("667793980318154783")) sendToPluginSupport(event);

        // Direct to plugin support (in thread)
        if (event.getMessage().getChannelType() == ChannelType.GUILD_PUBLIC_THREAD && !event.getMember()
            .getRoles().toString().contains("667793980318154783"))
            if (event.getGuildChannel().asThreadChannel().getParentChannel()
                .getIdLong() != 1023735878075564042L) sendToPluginSupport(event);

        // Counting
        if (event.getChannel().getIdLong() == 816885380577230906L) {
            Message message = event.getMessage();
            int value;
            try {
                // Errors if invalid int, resulting in catch statement running
                value = Integer.parseInt(message.getContentRaw());

                if (lastNumber == -2) lastNumber = value + 1;

                // If value is 1 less than the last number, update the last number value
                if (value + 1 == lastNumber) lastNumber = value;
                else {
                    System.out.println(new Utils().getTime(
                        Utils.LogColor.YELLOW) + "Deleting invalid number from counting: " + value);
                    message.delete().queue();
                }

            } catch (NumberFormatException ignored) {
                // NaN
                System.out.println(new Utils().getTime(
                    Utils.LogColor.YELLOW) + "Deleting invalid message from counting: " + message.getContentRaw());
                message.delete().queue();
            }
        }

        // Ping
        if (event.getMessage().getContentRaw().contains("<@277291758503723010>"))
            event.getMessage().addReaction(Emoji.fromCustom("piiiiiing", 658749607488127017L, false)).queue();

        // Animal pics
        if (event.getChannel().getIdLong() == 884169435773009950L) {
            Message message = event.getMessage();
            if (!message.getAttachments().isEmpty()) message.addReaction(Emoji.fromUnicode("❤")).queue();
        }
    }

    private void sendToPluginSupport(MessageReceivedEvent event) {
        String message = event.getMessage().getContentStripped().toLowerCase().replace(" ", "");
        if (message.contains("entityclearer") || message.contains("expensivedeaths") || message.contains(
            "filecleaner") || message.contains("bungeenicks")) event.getMessage()
            .reply("Please go to <#1023735878075564042> if you need help with Jason's plugins.")
            .mentionRepliedUser(true).queue();
    }

    // When recent chatter leaves
    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
        System.out.println(
            "\n" + new Utils().getTime(Utils.LogColor.YELLOW) + event.getUser().getName() + " left!");

        OffsetDateTime thirtyMinsAgo = OffsetDateTime.now().minusMinutes(30);
        OffsetDateTime threeDaysAgo = OffsetDateTime.now().minusDays(3);

        for (ThreadChannel thread : event.getGuild().getChannelById(ForumChannel.class, 1023735878075564042L)
            .getThreadChannels()) {
            if (thread.isArchived()) continue;

            System.out.println(
                new Utils().getTime(Utils.LogColor.YELLOW) + "Checking post '" + thread.getName() + "'");

            if (thread.getOwnerIdLong() == event.getUser().getIdLong()) {
                sendOPLeaveMessage(thread, event.getUser());
                continue;
            }

            // If the user that left sent the latest or a recent message, say so
            if (checkIfFromUser(thirtyMinsAgo, threeDaysAgo, thread, event.getUser().getIdLong()))
                sendRecentLeaveMessage(thread, event.getUser());
        }

        Long[] textChannels = {592208420602380328L, 456108521210118146L};
        for (long channelId : textChannels) {
            TextChannel channel = event.getGuild().getTextChannelById(channelId);
            System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Checking #" + channel.getName());

            // If the user that left sent the latest or a recent message, say so
            if (checkIfFromUser(thirtyMinsAgo, threeDaysAgo, channel, event.getUser().getIdLong()))
                sendRecentLeaveMessage(channel, event.getUser());
        }
    }

    private boolean checkIfFromUser(OffsetDateTime thirtyMinsAgo, OffsetDateTime threeDaysAgo, MessageChannel channel, Long userId) {
        boolean fromUser = false;

        try {
            // Check the past 15 messages within 30 minutes
            for (Message messages : new Utils().getMessages(channel, 15).get(30, TimeUnit.SECONDS))
                if (messages.getTimeCreated().isAfter(thirtyMinsAgo) && messages.getAuthor()
                    .getIdLong() == userId) {
                    fromUser = true;
                    break;
                }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            System.out.print(new Utils().getTime(Utils.LogColor.RED));
            e.printStackTrace();
        }

        // If message isn't from the past 30 minutes, see if it's at least the latest message within 3 days
        if (!fromUser) try {
            Message message = new Utils().getMessages(channel, 1).get(30, TimeUnit.SECONDS).get(0);
            if (message.getTimeCreated().isAfter(threeDaysAgo) && message.getAuthor().getIdLong() == userId)
                fromUser = true;
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            System.out.print(new Utils().getTime(Utils.LogColor.RED));
            e.printStackTrace();
        }

        return fromUser;
    }

    private void sendRecentLeaveMessage(MessageChannel channel, User user) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Recent chatter " + user.getName() + " has left the server");
        embed.setDescription(user.getAsMention());
        embed.setThumbnail(user.getAvatarUrl());
        embed.setColor(new Color(255, 200, 0));

        channel.sendMessageEmbeds(embed.build()).queue();
    }

    private void sendOPLeaveMessage(ThreadChannel channel, User user) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Original poster " + user.getName() + " has left the server");
        embed.setDescription(user.getAsMention());
        embed.setFooter("This post will now be closed and locked");
        embed.setThumbnail(user.getAvatarUrl());
        embed.setColor(new Color(255, 100, 0));

        channel.sendMessageEmbeds(embed.build()).queue(
            na -> channel.getManager().setArchived(true).setLocked(true).queueAfter(1, TimeUnit.SECONDS));
    }
}
