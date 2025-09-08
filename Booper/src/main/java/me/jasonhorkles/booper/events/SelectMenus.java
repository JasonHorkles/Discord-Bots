package me.jasonhorkles.booper.events;

import me.jasonhorkles.booper.Utils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class SelectMenus extends ListenerAdapter {
    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        List<String> selected = event.getValues();

        switch (event.getComponentId()) {
            case "twitch-user-set" -> {
                if (selected.getFirst().equals("add-new")) event.replyModal(getAddTwitchModal()).queue();
                else event.replyModal(getSetModal(selected.getFirst(), "twitch")).queue();
            }

            case "discord-user-reset", "twitch-user-reset" -> {
                event.deferReply(true).queue();

                boolean isDiscord = event.getComponentId().contains("discord");
                JSONObject originalData = new Utils().getJsonFromFile("live-msgs.json");
                String platform = isDiscord ? "discord" : "twitch";
                JSONObject platformData = originalData.getJSONObject(platform);

                List<String> success = new ArrayList<>();
                List<String> failed = new ArrayList<>();
                for (String user : selected)
                    if (platformData.remove(user) != null) if (isDiscord) {
                        //noinspection DataFlowIssue
                        Member member = event.getGuild().getMemberById(user);
                        if (member == null) success.add(user);
                        else success.add(member.getEffectiveName());
                    } else success.add(user);
                    else failed.add(user);

                originalData.put(platform, platformData);

                try {
                    FileWriter file = new FileWriter(
                        "Booper/Data/live-msgs.json",
                        StandardCharsets.UTF_8,
                        false);
                    file.write(originalData.toString(2));
                    file.close();

                    event.getHook().editOriginal("Success: `" + success + "`\nFailed: `" + failed + "`")
                        .queue();

                } catch (IOException e) {
                    System.out.print(new Utils().getTime(Utils.LogColor.RED));
                    e.printStackTrace();
                    event.getHook().editOriginal("Error updating live-msgs.json file!").queue();
                }
            }
        }
    }

    @Override
    public void onEntitySelectInteraction(EntitySelectInteractionEvent event) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (event.getComponentId()) {
            case "discord-user-set" -> event.replyModal(getSetModal(
                event.getValues().getFirst().getId(),
                "discord")).queue();
        }
    }

    private Modal getAddTwitchModal() {
        TextInput usernameInput = TextInput.create("username", "Twitch Username", TextInputStyle.SHORT)
            .setPlaceholder("The lowercase username displayed in the Twitch URL").setRequired(true).build();

        TextInput messageInput = TextInput.create(
                "custom-message",
                "Custom Message",
                TextInputStyle.PARAGRAPH).setPlaceholder(
                "Leave blank for the default message. Use {NAME} for the person's username").setRequired(false)
            .build();

        return Modal.create("add-twitch-live-user", "Add Custom Live Message").addActionRow(usernameInput)
            .addActionRow(messageInput).build();
    }

    private Modal getSetModal(String usernameOrId, String platform) {
        JSONObject customMessage = new Utils().getJsonFromFile("live-msgs.json").getJSONObject(platform);

        // Pre-populate with existing message if it exists, otherwise set null if blank (for default msgs)
        String prePopulated = null;
        if (customMessage.has(usernameOrId)) {
            String custom = customMessage.getString(usernameOrId);
            if (!custom.isBlank()) prePopulated = custom;
        }

        String placeholderDefault = platform.equalsIgnoreCase("discord") ? "" : "Leave blank for the default message. ";

        TextInput messageInput = TextInput.create(
                "custom-message",
                "Custom Message", TextInputStyle.PARAGRAPH)
            .setPlaceholder(placeholderDefault + "Use {NAME} for the person's username")
            .setValue(prePopulated).setRequired(platform.equalsIgnoreCase("discord")).build();

        return Modal.create("custom-" + platform + "-live-" + usernameOrId, "Set Custom Live Message")
            .addActionRow(messageInput).build();
    }
}
