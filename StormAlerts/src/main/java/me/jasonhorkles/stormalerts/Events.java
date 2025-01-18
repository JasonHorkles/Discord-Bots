package me.jasonhorkles.stormalerts;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SuppressWarnings("DataFlowIssue")
public class Events extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        Utils utils = new Utils();
        System.out.println(utils.getTime(Utils.LogColor.GREEN) + event.getMember()
            .getEffectiveName() + " used the /" + event.getName() + " command");

        switch (event.getName().toLowerCase()) {
            case "checknow" -> utils.updateNow(event);
            case "updaterecords" -> {
                event.reply("Updating records...").setEphemeral(true).queue();
                new Records().checkRecords();
            }
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (event.getComponentId()) {
            case "viewchanges" -> {
                event.deferReply(true).queue();

                Message message = event.getMessage();
                EmbedBuilder embed = new EmbedBuilder(message.getEmbeds().getFirst());

                Path historyPath = Path.of(Alerts.historyDir + "/" + message.getId() + ".txt");
                if (!Files.exists(historyPath)) {
                    event.getHook().editOriginal("No description changes have been made to this alert.")
                        .queue();
                    return;
                }

                // Remove data that isn't compared
                embed.setTitle(null);
                embed.clearFields();

                // Set the description based on its file
                try {
                    embed.setDescription(Files.readString(historyPath));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                event.getHook().editOriginalEmbeds(embed.build()).queue();
            }
        }
    }

    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
        if (event.getGuild().getIdLong() != 843919716677582888L) return;

        User user = event.getUser();
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle(user.getName() + " has left the server");
        embed.setDescription(user.getAsMention());
        embed.setThumbnail(user.getAvatarUrl());
        embed.setColor(new Color(255, 200, 0));

        event.getJDA().getTextChannelById(1093060038265950238L).sendMessageEmbeds(embed.build()).queue();
    }
}
