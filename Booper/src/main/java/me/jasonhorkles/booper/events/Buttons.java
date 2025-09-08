package me.jasonhorkles.booper.events;

import me.jasonhorkles.booper.Utils;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Buttons extends ListenerAdapter {
    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        boolean isDiscord = event.getComponentId().contains("discord");
        switch (event.getComponentId()) {
            case "live-msg-discord-set", "live-msg-twitch-set" -> {
                event.deferReply(true).queue();
                event.getHook().editOriginalComponents(ActionRow.of(getSelectMenu(isDiscord))).queue();
            }

            case "live-msg-discord-reset", "live-msg-twitch-reset" -> {
                event.deferReply(true).queue();
                event.getHook().editOriginalComponents(ActionRow.of(getResetMenu(
                    event.getGuild(),
                    isDiscord))).queue();
            }
        }
    }

    private SelectMenu getSelectMenu(boolean isDiscord) {
        if (isDiscord) return EntitySelectMenu.create("discord-user-set", EntitySelectMenu.SelectTarget.USER)
            .setPlaceholder("Select which Discord user to set a message for").build();

        else {
            List<SelectOption> options = new ArrayList<>();
            options.add(SelectOption.of("Add New", "add-new"));

            JSONObject customUsers = new Utils().getJsonFromFile("live-msgs.json").getJSONObject("twitch");

            for (String username : customUsers.keySet()) {
                if (options.size() >= 25) break;
                options.add(SelectOption.of(username, username));
            }

            return StringSelectMenu.create("twitch-user-set").setPlaceholder(
                "Select which Twitch user to set a message for").addOptions(options).build();
        }
    }

    private SelectMenu getResetMenu(Guild guild, boolean isDiscord) {
        List<SelectOption> options = new ArrayList<>();
        String platform = isDiscord ? "Discord" : "Twitch";
        String platformLower = platform.toLowerCase();

        JSONObject customUsers = new Utils().getJsonFromFile("live-msgs.json").getJSONObject(platformLower);

        for (String username : customUsers.keySet()) {
            if (options.size() >= 25) break;
            if (isDiscord) {
                Member member = guild.getMemberById(username);
                if (member == null) continue;
                String effectiveName = member.getEffectiveName();
                options.add(SelectOption.of(effectiveName, username));
            } else options.add(SelectOption.of(username, username));
        }

        boolean isEmpty = false;
        String placeholder;
        if (options.isEmpty()) {
            options.add(SelectOption.of("No users set", "null"));
            isEmpty = true;
            placeholder = "No " + platform + " users set";
        } else
            placeholder = "Select which " + (isDiscord ? "Discord" : "Twitch") + " user(s) should be reset";

        return StringSelectMenu.create(platformLower + "-user-reset").setPlaceholder(placeholder).addOptions(
            options).setMaxValues(25).setDisabled(isEmpty).build();
    }
}
