package me.jasonhorkles.phoenella.events;

import me.jasonhorkles.phoenella.Phoenella;
import me.jasonhorkles.phoenella.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

import java.awt.*;

@SuppressWarnings("DataFlowIssue")
public class Buttons extends ListenerAdapter {
    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
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

            case "viewroles" -> {
                event.deferReply(true).queue();

                StringBuilder roleList = new StringBuilder();
                StringBuilder notRoleList = new StringBuilder();
                for (SelectOption option : ((StringSelectMenu) event.getMessage().getActionRows().getFirst()
                    .getComponents().getFirst()).getOptions()) {
                    Role role = event.getGuild().getRoleById(option.getValue());

                    if (event.getMember().getRoles().contains(role))
                        roleList.append(role.getAsMention()).append("\n");
                    else notRoleList.append(role.getAsMention()).append("\n");
                }

                if (roleList.isEmpty()) roleList.append("None");
                if (notRoleList.isEmpty()) notRoleList.append("None");

                EmbedBuilder embed = new EmbedBuilder();
                embed.addField("You have", roleList.toString(), true);
                embed.addBlankField(true);
                embed.addField("You don't have", notRoleList.toString(), true);
                embed.setColor(new Color(43, 45, 49));

                event.getHook().editOriginalEmbeds(embed.build()).queue();
            }
        }
    }
}
