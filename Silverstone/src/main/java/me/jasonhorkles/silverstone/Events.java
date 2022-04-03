package me.jasonhorkles.silverstone;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
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

    // Slash commands
    @SuppressWarnings("SuspiciousMethodCalls")
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        // If in discord server and not a staff member or admin and in the wrong channel, make it private
        boolean ephemeral = event.isFromGuild() && event.getGuild()
            .getIdLong() == 455919765999976461L && !(event.getMember().getRoles()
            .contains("667793980318154783") || event.getMember()
            .hasPermission(Permission.ADMINISTRATOR)) && !(event.getChannel()
            .getIdLong() == 456470772207190036L || event.getChannel().getIdLong() == 468416589331562506L);
        boolean pluginCmdOnly = !(event.getMember().getRoles().contains("667793980318154783") || event.getMember()
            .hasPermission(Permission.ADMINISTRATOR)) && !(event.getChannel().getIdLong() == 872977864029511791L);

        switch (event.getName().toLowerCase()) {
            case "paste" -> {
                if (pluginCmdOnly)
                    event.reply("This command was designed for use in the <#872977864029511791> channel.")
                        .setEphemeral(true).queue();
                else event.reply("Please copy and paste your `" + event.getOption("what")
                        .getAsString() + "` file(s) to <https://paste.gg/>\nThen, click \"Submit anonymously\" and post the link in this channel.")
                    .queue();
            }

            case "ecdebug" -> {
                if (pluginCmdOnly)
                    event.reply("This command was designed for use in the <#872977864029511791> channel.")
                        .setEphemeral(true).queue();
                else event.reply(
                        "Please set `debug` to `true` in your config, run `/entityclearer reload`, then reproduce the issue.\nOnce complete, upload the logs to <https://paste.gg/>, click \"Submit anonymously\", and post the link in this channel.")
                    .queue();
            }

            case "plgh" -> event.reply("""
                **EntityClearer:** <https://github.com/SilverstoneMC/EntityClearer>
                **ExpensiveDeaths:** <https://github.com/SilverstoneMC/ExpensiveDeaths>
                **FileCleaner:** <https://github.com/SilverstoneMC/FileCleaner>
                **SimpleBooks:** <https://github.com/SilverstoneMC/SimpleBooks>
                """).setEphemeral(ephemeral && pluginCmdOnly).queue();

            case "plugins" -> event.reply(
                    "See Jason's plugins at: <https://www.spigotmc.org/resources/authors/jasonhorkles.339646/>")
                .setEphemeral(ephemeral && pluginCmdOnly).queue();

            case "tutorials" -> event.reply(
                    "JasonHorkles Tutorials: <https://www.youtube.com/channel/UCIyJ0zf3moNSRN1wIetpbmA>")
                .setEphemeral(ephemeral && pluginCmdOnly).queue();

            case "lp" -> event.reply("Download LuckPerms at: <https://luckperms.net/download>")
                .setEphemeral(ephemeral && pluginCmdOnly).queue();
        }
        System.out.println(new Utils().getTime(Utils.Color.GREEN) + event.getMember()
            .getEffectiveName() + " used the /" + event.getName() + " command");
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        // Thanks for coming :)
        if (event.getChannel().getIdLong() == 872977864029511791L && event.getAuthor()
            .getIdLong() == 277291758503723010L && event.getMessage().getContentStripped().startsWith("np")) {

            EmbedBuilder embed = new EmbedBuilder();

            embed.setTitle(
                "Thank you for coming. If you enjoy the plugin and are happy with the support you received, please consider leaving a review on Spigot \\:)");
            embed.setDescription(
                "[EntityClearer](https://www.spigotmc.org/resources/entityclearer.90802/)\n[ExpensiveDeaths](https://www.spigotmc.org/resources/expensivedeaths.96065/)\n[FileCleaner](https://www.spigotmc.org/resources/filecleaner.93372/)\n[SimpleBooks](https://www.spigotmc.org/resources/simplebooks.95698/)");
            embed.setColor(new Color(19, 196, 88));

            event.getChannel().sendMessageEmbeds(embed.build()).queue();
        }

        // Direct to plugin support
        if (event.getChannel().getIdLong() != 872977864029511791L && !event.getMember().getRoles().toString()
            .contains("667793980318154783")) {
            String message = event.getMessage().getContentStripped().toLowerCase().replace(" ", "");
            if (message.contains("entityclearer") || message.contains("expensivedeaths") || message.contains(
                "filecleaner") || message.contains("simplebooks"))
                event.getMessage().reply("Please go to <#872977864029511791> if you need help with Jason's plugins.")
                    .mentionRepliedUser(true).queue();
        }
    }

    // When recent chatter leaves
    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
        System.out.println(
            "\n" + new Utils().getTime(Utils.Color.YELLOW) + event.getUser().getName() + " left!");

        OffsetDateTime thirtyMinsAgo = OffsetDateTime.now().minus(30, ChronoUnit.MINUTES);
        OffsetDateTime threeDaysAgo = OffsetDateTime.now().minus(3, ChronoUnit.DAYS);

        for (TextChannel channels : event.getGuild().getTextChannels()) {
            if (!event.getGuild().getRoleById(847954304333512714L).hasPermission(channels, Permission.MESSAGE_SEND))
                continue;
            if (channels.getName().toLowerCase().contains("count")) continue;

            System.out.println(
                new Utils().getTime(Utils.Color.YELLOW) + "Checking #" + channels.getName());
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
                e.printStackTrace();
            }

            // If message isn't from the past 30 minutes, see if it's at least the latest message within 3 days
            if (!fromUser) try {
                Message message = new Utils().getMessages(channels, 1).get(30, TimeUnit.SECONDS).get(0);
                if (message.getTimeCreated().isAfter(threeDaysAgo) && message.getAuthor().getIdLong() == event.getUser()
                    .getIdLong()) fromUser = true;
            } catch (ExecutionException | InterruptedException | TimeoutException e) {
                e.printStackTrace();
            }

            // If the user that left sent the latest or a recent message, say so
            if (fromUser) {
                EmbedBuilder embed = new EmbedBuilder();
                embed.setTitle("Recent chatter " + event.getUser().getAsTag() + " has left the server");
                embed.setDescription(event.getUser().getAsMention());
                embed.setThumbnail(event.getUser().getAvatarUrl());
                embed.setColor(new Color(255, 200, 0));

                channels.sendMessageEmbeds(embed.build()).queue();
                break;
            }
        }
    }
}
