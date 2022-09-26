package me.jasonhorkles.silverstone;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
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

@SuppressWarnings("ConstantConditions")
public class Events extends ListenerAdapter {
    //todo reaction to convert txt file to code block

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
                """).setEphemeral(ephemeral).queue();

            case "plugins" ->
                event.reply("See Jason's plugins at: <https://www.spigotmc.org/resources/authors/jasonhorkles.339646/>")
                    .setEphemeral(ephemeral).queue();

            case "tutorials" ->
                event.reply("JasonHorkles Tutorials: <https://www.youtube.com/channel/UCIyJ0zf3moNSRN1wIetpbmA>")
                    .setEphemeral(ephemeral).queue();

            case "moss" ->
                event.reply("Get EssentialsX help and more here: https://discord.gg/PHpuzZS").setEphemeral(ephemeral)
                    .queue();

            case "lp" ->
                event.reply("Get LuckPerms help here: https://discord.gg/luckperms").setEphemeral(ephemeral).queue();
        }

        System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + event.getMember()
            .getEffectiveName() + " used the /" + event.getName() + " command");
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        // Ignore my FAQs thread
        if (event.getMessage().getChannelType() == ChannelType.GUILD_PUBLIC_THREAD)
            if (event.getChannel().getIdLong() == 1024004210511057046L)
                if (event.getAuthor().getIdLong() != 277291758503723010L) {
                    System.out.println(
                        new Utils().getTime(Utils.LogColor.YELLOW) + "Deleting message from " + event.getAuthor()
                            .getAsTag() + " in FAQs post!");
                    event.getMessage().delete().queue();
                    return;
                }

        // Thanks for coming :)
        if (event.getMessage().getChannelType() == ChannelType.GUILD_PUBLIC_THREAD)
            if (event.getGuildChannel().asThreadChannel().getParentChannel()
                .getIdLong() == 1023735878075564042L && event.getAuthor()
                .getIdLong() == 277291758503723010L && event.getMessage().getContentStripped().toLowerCase()
                .startsWith("np")) {

                EmbedBuilder embed = new EmbedBuilder();

                embed.setTitle(
                    "Thank you for coming. If you enjoy the plugin and are happy with the support you received, please consider leaving a review on Spigot \\:)");
                embed.setDescription(
                    "[EntityClearer](https://www.spigotmc.org/resources/entityclearer.90802/)\n[ExpensiveDeaths](https://www.spigotmc.org/resources/expensivedeaths.96065/)\n[FileCleaner](https://www.spigotmc.org/resources/filecleaner.93372/)");
                embed.setColor(new Color(19, 196, 88));

                event.getChannel().sendMessageEmbeds(embed.build()).queue();
            }

        // Direct to plugin support
        if (event.getChannel().getIdLong() != 1023735878075564042L && !event.getMember().getRoles().toString()
            .contains("667793980318154783")) {
            String message = event.getMessage().getContentStripped().toLowerCase().replace(" ", "");
            if (message.contains("entityclearer") || message.contains("expensivedeaths") || message.contains(
                "filecleaner"))
                event.getMessage().reply("Please go to <#1023735878075564042> if you need help with Jason's plugins.")
                    .mentionRepliedUser(true).queue();
        }
    }

    // When recent chatter leaves
    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
        System.out.println("\n" + new Utils().getTime(Utils.LogColor.YELLOW) + event.getUser().getName() + " left!");

        OffsetDateTime thirtyMinsAgo = OffsetDateTime.now().minus(30, ChronoUnit.MINUTES);
        OffsetDateTime threeDaysAgo = OffsetDateTime.now().minus(3, ChronoUnit.DAYS);

        for (ThreadChannel channels : event.getGuild().getChannelById(ForumChannel.class, 1023735878075564042L)
            .getThreadChannels()) {
            if (channels.isArchived()) continue;

            System.out.println(
                new Utils().getTime(Utils.LogColor.YELLOW) + "Checking post '" + channels.getName() + "'");

            if (channels.getOwnerIdLong() == event.getUser().getIdLong()) {
                sendOPLeaveMessage(channels, event.getUser());
                continue;
            }

            boolean fromUser = false;
            try {
                // Check the past 15 messages within 30 minutes
                for (Message messages : new Utils().getMessages(channels, 15).get(30, TimeUnit.SECONDS))
                    if (messages.getTimeCreated().isAfter(thirtyMinsAgo) && messages.getAuthor()
                        .getIdLong() == event.getUser().getIdLong()) {
                        fromUser = true;
                        break;
                    }
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                System.out.print(new Utils().getTime(Utils.LogColor.RED));
                e.printStackTrace();
            }

            // If message isn't from the past 30 minutes, see if it's at least the latest message within 3 days
            if (!fromUser) try {
                Message message = new Utils().getMessages(channels, 1).get(30, TimeUnit.SECONDS).get(0);
                if (message.getTimeCreated().isAfter(threeDaysAgo) && message.getAuthor().getIdLong() == event.getUser()
                    .getIdLong()) fromUser = true;
            } catch (ExecutionException | InterruptedException | TimeoutException e) {
                System.out.print(new Utils().getTime(Utils.LogColor.RED));
                e.printStackTrace();
            }

            // If the user that left sent the latest or a recent message, say so
            if (fromUser) sendRecentLeaveMessage(channels, event.getUser());
        }
    }

    private void sendRecentLeaveMessage(ThreadChannel channel, User user) {
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

        channel.sendMessageEmbeds(embed.build())
            .queue((na) -> channel.getManager().setArchived(true).setLocked(true).queueAfter(1, TimeUnit.SECONDS));
    }
}
