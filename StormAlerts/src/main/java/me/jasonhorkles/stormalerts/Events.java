package me.jasonhorkles.stormalerts;

import me.jasonhorkles.stormalerts.Utils.ChannelUtils;
import me.jasonhorkles.stormalerts.Utils.MessageUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Events extends ListenerAdapter {
    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
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

            // Rain confirmations

            case "acceptrain" -> {
                // Save latest snow message ID to a file
                try {
                    Path snowIdFile = Path.of("StormAlerts/accepted-snow.txt");
                    String snowId = new MessageUtils().getMessages(ChannelUtils.snowChannel, 1).get()
                        .getFirst().getId();
                    Files.writeString(snowIdFile, snowId);
                } catch (ExecutionException | InterruptedException | IOException e) {
                    event.reply("An error occurred while saving the snow ID!").setEphemeral(true).queue();
                    return;
                }

                event.deferEdit().queue(na -> event.getMessage().delete().queue());
            }

            case "denyrain" -> {
                Weather.rainDenied = true;
                new Thread(
                    () -> {
                        try (ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor()) {
                            executor.schedule(() -> Weather.rainDenied = false, 5, TimeUnit.HOURS);
                        }
                    }, "Deny Rain").start();

                event.deferEdit().queue(na -> event.getMessage().delete().queue());
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

        ChannelUtils.logChannel.sendMessageEmbeds(embed.build()).queue();
    }
}
