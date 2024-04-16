package me.jasonhorkles.silverstone;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.ErrorResponse;

import java.awt.*;
import java.time.OffsetDateTime;
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

        // Make private if not from staff nor in bot channel
        boolean ephemeral = !(event.getMember().getRoles().contains(event.getGuild()
            .getRoleById(667793980318154783L)) || event.getMember()
                                  .hasPermission(Permission.ADMINISTRATOR)) && !(event.getChannel()
                                                                                     .getIdLong() == 456470772207190036L || event
                                                                                                                                .getChannel()
                                                                                                                                .getIdLong() == 468416589331562506L);

        switch (event.getName().toLowerCase()) {
            case "moss" -> event.reply(
                    "Get help with EssentialsX, Jason's plugins, and more here: https://discord.gg/PHpuzZS")
                .setEphemeral(ephemeral).queue();

            case "lp" -> event.reply("Get LuckPerms help here: https://discord.gg/luckperms").setEphemeral(
                ephemeral).queue();
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // Auto publish announcements
        if (event.getChannelType() == ChannelType.NEWS) {
            event.getMessage().crosspost().queue(
                null,
                new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE, ErrorResponse.ALREADY_CROSSPOSTED));
            return;
        }

        if (event.getAuthor().isBot()) return;

        // Direct to plugin support if not staff
        if (!event.getMember().getRoles().toString().contains("667793980318154783")) {
            String message = event.getMessage().getContentStripped().toLowerCase().replace(" ", "");
            if (message.contains("entityclearer") || message.contains("expensivedeaths") || message.contains(
                "filecleaner")) event.getMessage().reply(
                    "This server is no longer dedicated to plugin support. Please go to https://discord.gg/4wRHMyrTgv if you need help with Jason's plugins.")
                .mentionRepliedUser(true).queue();
        }

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
                    System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Deleting invalid number from counting: " + value);
                    message.delete().queue();
                }

            } catch (NumberFormatException ignored) {
                // NaN
                System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Deleting invalid message from counting: " + message.getContentRaw());
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

    // When recent chatter leaves
    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
        System.out.println("\n" + new Utils().getTime(Utils.LogColor.YELLOW) + event.getUser()
            .getName() + " left!");

        OffsetDateTime thirtyMinsAgo = OffsetDateTime.now().minusMinutes(30);
        OffsetDateTime threeDaysAgo = OffsetDateTime.now().minusDays(3);

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
            Message message = new Utils().getMessages(channel, 1).get(30, TimeUnit.SECONDS).getFirst();
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
}
