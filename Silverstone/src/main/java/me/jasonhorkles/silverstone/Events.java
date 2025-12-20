package me.jasonhorkles.silverstone;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.ErrorResponse;

import java.util.List;

@SuppressWarnings("DataFlowIssue")
public class Events extends ListenerAdapter {
    //    public static int lastNumber;

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + event.getMember()
            .getEffectiveName() + " used the /" + event.getName() + " command");

        // Make private if not from staff nor in bot channel
        boolean ephemeral = !(event.getMember().getRoles().contains(event.getGuild()
            .getRoleById(667793980318154783L)) || event.getMember()
            .hasPermission(Permission.ADMINISTRATOR)) && !(event.getChannel()
            .getIdLong() == 456470772207190036L || event.getChannel().getIdLong() == 468416589331562506L);

        if (event.getName().equalsIgnoreCase("moss")) event.reply(
                "Get help with EssentialsX, Jason's plugins, and more here: https://discord.gg/PHpuzZS")
            .setEphemeral(ephemeral).queue();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // github-spam, minigame-status
        List<Long> excludeAutoPublish = List.of(885627082116317244L, 1395537310694641704L);

        // Auto publish announcements
        if (event.getChannelType() == ChannelType.NEWS && !excludeAutoPublish.contains(event.getChannel()
            .getIdLong())) {
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
                    "This server is no longer dedicated to plugin support. Please go to https://discord.gg/5XFBx8uZVN if you need help with Jason's plugins.")
                .mentionRepliedUser(true).queue();
        }

        // Counting
        /*if (event.getChannel().getIdLong() == 816885380577230906L) {
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
        }*/

        // Ping
        if (event.getMessage().getContentRaw().contains("<@277291758503723010>"))
            event.getMessage().addReaction(Emoji.fromCustom("piiiiiing", 658749607488127017L, false)).queue();

        // Animal pics
        if (event.getChannel().getIdLong() == 884169435773009950L) {
            Message message = event.getMessage();
            if (!message.getAttachments().isEmpty()) message.addReaction(Emoji.fromUnicode("❤")).queue();
        }
    }
}
