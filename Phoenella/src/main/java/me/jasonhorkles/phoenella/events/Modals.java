package me.jasonhorkles.phoenella.events;

import me.jasonhorkles.phoenella.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.*;

@SuppressWarnings("DataFlowIssue")
public class Modals extends ListenerAdapter {
    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (event.getModalId().equals("customwordle")) {
            String word = event.getValue("word").getAsString().replaceAll("[^a-zA-Z]", "");

            if (word.length() < 4) {
                event.reply("**Error:** Invalid characters!").setEphemeral(true).queue();
                return;
            }

            int tries;
            try {
                tries = Integer.parseInt(event.getValue("tries").getAsString());
            } catch (NumberFormatException ignored) {
                tries = 6;
            }

            if (tries < 4 || tries > 8) {
                event.reply("**Error:** Tries must be between 4-8!").setEphemeral(true).queue();
                return;
            }

            if (new Utils().containsBadWord(word)) {
                event.reply("**Error:** That word is not allowed!").setEphemeral(true).queue();
                return;
            }

            event.reply("Creating challenge for word **" + word + "** in <#956267174727671869>")
                .setEphemeral(true).queue();

            EmbedBuilder embed = new EmbedBuilder();
            embed.setAuthor(new Utils().getFullName(event.getMember()) + " has created a Wordle!", null,
                event.getMember().getEffectiveAvatarUrl());
            embed.setColor(new Color(43, 45, 49));
            embed.addField("Plays", "0", true);
            embed.addField("Passes", "0", true);
            embed.addField("Fails", "0", true);

            event.getJDA().getTextChannelById(956267174727671869L).sendMessageEmbeds(embed.build())
                .setActionRow(Button.success("playwordle:" + word + ":" + tries, "Play it!")).queue();
        }
    }
}
