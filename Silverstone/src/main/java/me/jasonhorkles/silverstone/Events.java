package me.jasonhorkles.silverstone;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.*;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@SuppressWarnings("DataFlowIssue")
public class Events extends ListenerAdapter {
    public static int lastNumber;

    @SuppressWarnings("SuspiciousMethodCalls")
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + event.getMember()
            .getEffectiveName() + " used the /" + event.getName() + " command");

        // If in discord server and not a staff member or admin and in the wrong channel, make it private
        boolean ephemeral = event.isFromGuild() && event.getGuild()
            .getIdLong() == 455919765999976461L && !(event.getMember().getRoles()
            .contains("667793980318154783") || event.getMember()
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
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        // Thanks for coming :)
        //todo add hangar links too
        if (event.getMessage().getChannelType() == ChannelType.GUILD_PUBLIC_THREAD)
            if (event.getGuildChannel().asThreadChannel().getParentChannel()
                .getIdLong() == 1023735878075564042L && event.getAuthor()
                .getIdLong() == 277291758503723010L && event.getMessage().getContentStripped().toLowerCase()
                .startsWith("np")) {

                EmbedBuilder embed = new EmbedBuilder();

                embed.setTitle(
                    "Thank you for coming. If you enjoy the plugin and are happy with the support you received, please consider leaving a review on Spigot \\:)");
                embed.setDescription(
                    "[EntityClearer](https://www.spigotmc.org/resources/entityclearer.90802/)\n[ExpensiveDeaths](https://www.spigotmc.org/resources/expensivedeaths.96065/)\n[FileCleaner](https://www.spigotmc.org/resources/filecleaner.93372/)\n[BungeeNicks](https://www.spigotmc.org/resources/bungeenicks.110948/)");
                embed.setColor(new Color(19, 196, 88));

                event.getChannel().sendMessageEmbeds(embed.build()).queue();
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
            Message m = event.getMessage();
            int value;
            try {
                // Errors if invalid int, resulting in catch statement running
                value = Integer.parseInt(m.getContentRaw());

                if (lastNumber == -2) lastNumber = value + 1;

                // If value is 1 less than the last number, update the last number value
                if (value + 1 == lastNumber) lastNumber = value;
                else {
                    System.out.println(new Utils().getTime(
                        Utils.LogColor.YELLOW) + "Deleting invalid number from counting: " + value);
                    m.delete().queue();
                }

            } catch (NumberFormatException ignored) {
                // NaN
                System.out.println(new Utils().getTime(
                    Utils.LogColor.YELLOW) + "Deleting invalid message from counting: " + m.getContentRaw());
                m.delete().queue();
            }
        }

        // Ping
        if (event.getMessage().getContentRaw().contains("<@277291758503723010>"))
            event.getMessage().addReaction(Emoji.fromCustom("piiiiiing", 658749607488127017L, false)).queue();
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

        OffsetDateTime thirtyMinsAgo = OffsetDateTime.now().minus(30, ChronoUnit.MINUTES);
        OffsetDateTime threeDaysAgo = OffsetDateTime.now().minus(3, ChronoUnit.DAYS);

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
        embed.setTitle("Recent chatter " + user.getAsTag() + " has left the server");
        embed.setDescription(user.getAsMention());
        embed.setThumbnail(user.getAvatarUrl());
        embed.setColor(new Color(255, 200, 0));

        channel.sendMessageEmbeds(embed.build()).queue();
    }

    private void sendOPLeaveMessage(ThreadChannel channel, User user) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Original poster " + user.getAsTag() + " has left the server");
        embed.setDescription(user.getAsMention());
        embed.setFooter("This post will now be closed and locked");
        embed.setThumbnail(user.getAvatarUrl());
        embed.setColor(new Color(255, 100, 0));

        channel.sendMessageEmbeds(embed.build()).queue(
            na -> channel.getManager().setArchived(true).setLocked(true).queueAfter(1, TimeUnit.SECONDS));
    }
}
