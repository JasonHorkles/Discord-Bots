package me.jasonhorkles.booper.events;

import me.jasonhorkles.booper.Booper;
import me.jasonhorkles.booper.Utils;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Modals extends ListenerAdapter {
    @SuppressWarnings("DataFlowIssue")
    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        String id = event.getModalId();

        if (id.startsWith("custom-discord-live-") || id.startsWith("custom-twitch-live-")) {
            String platform = id.contains("discord") ? "discord" : "twitch";

            String usernameOrId = id.replace("custom-" + platform + "-live-", "");
            String message = event.getValue("custom-message").getAsString();

            event.deferReply(true).queue();
            setLiveMessage(platform, usernameOrId, message, event.getHook());
        }

        //noinspection SwitchStatementWithTooFewBranches
        switch (id) {
            case "add-twitch-live-user" -> {
                String username = event.getValue("username").getAsString().toLowerCase();
                String message = event.getValue("custom-message").getAsString();

                event.deferReply(true).queue();
                setLiveMessage("twitch", username, message, event.getHook());
                Booper.twitch.getClientHelper().enableStreamEventListener(username);
            }
        }
    }

    private void setLiveMessage(String platform, String usernameOrId, String message, InteractionHook hook) {
        JSONObject originalData = new Utils().getJsonFromFile("live-msgs.json");
        JSONObject platformData = originalData.getJSONObject(platform);

        platformData.put(usernameOrId, message);
        originalData.put(platform, platformData);

        try {
            FileWriter file = new FileWriter("Booper/Data/live-msgs.json", StandardCharsets.UTF_8, false);
            file.write(originalData.toString(2));
            file.close();

            String name = platform.equals("discord") ? "<@" + usernameOrId + ">" : "**" + usernameOrId + "**";
            hook
                .editOriginal("Successfully set " + name + "'s live message to:\n" + (message.isBlank() ? "DEFAULT" : message))
                .queue();

        } catch (IOException e) {
            System.out.print(new Utils().getTime(Utils.LogColor.RED));
            e.printStackTrace();
            hook.editOriginal("Error updating live-msgs.json file!").queue();
        }
    }
}
