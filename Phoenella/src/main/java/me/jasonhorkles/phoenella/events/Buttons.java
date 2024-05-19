package me.jasonhorkles.phoenella.events;

import me.jasonhorkles.phoenella.Phoenella;
import me.jasonhorkles.phoenella.Utils;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;

public class Buttons extends ListenerAdapter {
    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (event.getComponentId()) {
            case "definitionreport" -> {
                Phoenella.jda.openPrivateChannelById(277291758503723010L).flatMap(channel -> channel
                    .sendMessage(":warning: Definition report from **" + new Utils().getFullName(event.getMember()) + ":**")
                    .setEmbeds(event.getMessage().getEmbeds().getFirst())).queue();

                event.deferEdit().queue();

                Message message = event.getMessage();
                if (message.isEphemeral()) event.getHook().editOriginalComponents(ActionRow.of(event
                    .getButton().asDisabled())).queue();
                else message.delete().queue();
            }
        }
    }
}
