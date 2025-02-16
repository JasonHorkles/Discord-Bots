package me.jasonhorkles.fancyfriend;

import de.oliver.fancyanalytics.sdk.events.Event;
import me.jasonhorkles.fancyfriend.analytics.BotAnalytics;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("DataFlowIssue")
public class Events extends ListenerAdapter {
    private final Path pingFilePath = Path.of("FancyFriend/ping-settings.json");

    private final String geyserMsg = "The plugin may not work properly with Geyser as it is not officially supported. Additionally, display entities and other features don't even exist on Bedrock Edition.";
    private final String viaMsg = "The plugin may not work properly with Via plugins as they are not officially supported. Additionally, display entities and other features don't even exist on certain older Minecraft versions.";

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + event.getMember()
            .getEffectiveName() + " used the /" + event.getName() + " command");

        event.deferReply(event.getName().equalsIgnoreCase("noping")).queue();

        switch (event.getName()) {
            case "blankline" -> event.getHook().editOriginal(
                "To add a blank line in a hologram, use `<r>` on a new line.").queue();

            case "clickable" -> event.getHook().editOriginal(
                    "Holograms currently aren't clickable themselves, but [here's](<https://fancyplugins.de/docs/fh-clickable-holograms.html>) a workaround.")
                .queue();

            case "converters" -> {
                String fhMessage = "As of FancyHolograms v2.4.2, holograms can be converted from DecentHolograms using `/fancyholograms convert DecentHolograms *`";
                String npcMessage = "NPC converters currently are not available. You will need to manually convert your data for the time being.";

                String message;
                if (event.getChannel().getName().contains("holograms")) message = fhMessage;
                else if (event.getChannel().getName().contains("npcs")) message = npcMessage;
                else message = fhMessage + "\n\n" + npcMessage;

                event.getHook().editOriginal(message).queue();
            }

            case "docs" -> event.getHook().editOriginal(
                "Here are the FancyPlugins docs: <https://fancyplugins.de/docs/welcome.html>").queue();

            case "fixed" -> event.getHook().editOriginal("""
                    To make a hologram not rotate, the billboarding must be set to FIXED.
                    Example: `/holo edit <hologram> billboard FIXED`
                    Once complete, you must set the hologram's rotation with the `rotate` and `rotatepitch` commands.""")
                .queue();

            case "geyser" -> event.getHook().editOriginal(geyserMsg).queue();

            case "manual-holo" -> event.getHook().editOriginal("""
                ### To manually edit a hologram:
                1. Run `/fancyholograms save`
                2. Back up the `holograms.yml` file in case something goes wrong
                3. Edit your `holograms.yml` file as desired
                4. Run `/fancyholograms reload` after saving the file""").queue();

            case "multiline" -> event.getHook().editOriginal(
                    "See [here](<https://fancyplugins.de/docs/fn-multiple-lines.html>) on how to make an NPC name have multiple lines.")
                .queue();

            case "noping" -> noPing(event);

            case "per-line" -> event.getHook().editOriginal(
                    "Per-line settings (such as scale or background) are not supported in FancyHolograms due to a limitation with display entities. A separate hologram will need to be created for each line.")
                .queue();

            case "versions" -> {
                JSONObject project = new Modrinth().getProjectInfo(event.getOption("plugin").getAsString());

                List<Object> versionList = project.getJSONArray("game_versions").toList();
                String versions = versionList.getFirst().toString() + " → " + versionList.getLast()
                    .toString();

                StringBuilder message = new StringBuilder("The **" + project.getString("title") + "** plugin supports Minecraft **" + versions + "** on the following software:");

                for (Object loader : project.getJSONArray("loaders").toList()) {
                    String name = loader.toString();
                    // Capitalize the first letter of the loader name
                    message.append("\n- ").append(name.substring(0, 1).toUpperCase())
                        .append(name.substring(1));
                }

                if (project.getString("status").equals("archived")) message.append(
                    "\n*Note: This plugin is no longer maintained.*");

                event.getHook().editOriginal(message.toString()).queue();
            }

            case "via" -> event.getHook().editOriginal(viaMsg).queue();
        }

        BotAnalytics.get().getClient().getEventService().createEvent(
            BotAnalytics.get().getProjectId(),
            new Event("CommandExecuted", Map.of("command", event.getName())));
    }

    private void noPing(SlashCommandInteractionEvent event) {
        if (!new Utils().isStaff(event.getMember())) {
            event.getHook().editOriginal("Invalid permissions!").queue();
            return;
        }

        try {
            JSONObject pingSettings = new JSONObject(Files.readString(pingFilePath));

            switch (event.getOption("option").getAsString()) {
                case "all" -> {
                    pingSettings.remove(String.valueOf(event.getMember().getIdLong()));
                    Files.writeString(pingFilePath, pingSettings.toString());
                    event.getHook().editOriginal("You are now protected from all pings.").queue();
                }

                case "explicit" -> {
                    pingSettings.put(String.valueOf(event.getMember().getIdLong()), 1);
                    Files.writeString(pingFilePath, pingSettings.toString());
                    event.getHook().editOriginal("You are now protected from explicit pings only.").queue();
                }

                case "off" -> {
                    pingSettings.put(String.valueOf(event.getMember().getIdLong()), 0);
                    Files.writeString(pingFilePath, pingSettings.toString());
                    event.getHook().editOriginal("You are no longer protected from pings.").queue();
                }
            }
        } catch (IOException e) {
            System.out.print(new Utils().getTime(Utils.LogColor.RED));
            e.printStackTrace();
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // Ignore bots
        if (event.getAuthor().isBot()) return;
        // Make sure the channel's in a guild
        if (!event.getMessage().getChannelType().isGuild()) return;
        // Ignore staff
        if (new Utils().isStaff(event.getMember())) return;

        Message message = event.getMessage();

        String strippedMsg = message.getContentStripped().toLowerCase();
        String spacelessMsg = strippedMsg.replace(" ", "");
        // If someone asks if a plugin works with Via or Geyser
        if (strippedMsg.contains("work") || strippedMsg.contains("support")) if (spacelessMsg.contains(
            "viaversion") || spacelessMsg.contains("viabackwards")) message.reply(viaMsg).queue();
        else if (strippedMsg.contains("geyser")) message.reply(geyserMsg).queue();

        // Ping check
        int count = 0;
        // Iterate through each pinged member
        for (Member member : message.getMentions().getMembers())
            if (new Utils().isStaff(member)) try {
                Long staffId = member.getIdLong();
                JSONObject pingSettings = new JSONObject(Files.readString(pingFilePath));
                // Default to -1 if the user doesn't have a setting
                if (pingSettings.has(String.valueOf(staffId))) {
                    // -1: all | 0: off | 1: explicit
                    int pingSetting = pingSettings.getInt(String.valueOf(staffId));
                    if (pingSetting == 0) continue;
                    if (pingSetting == 1 && message.getMessageReference() != null) continue;
                }
                count++;
            } catch (IOException e) {
                System.out.print(new Utils().getTime(Utils.LogColor.RED));
                e.printStackTrace();
            }

        Long authorId = event.getAuthor().getIdLong();
        if (count > 0) {
            new PingWarnings().warn(authorId, event, count);
            StringBuilder pingMessage = new StringBuilder("Hey " + event.getMember()
                .getEffectiveName() + ", please don't ping staff members!");

            boolean appendTicket = true;

            if (message.getChannelType() == ChannelType.TEXT)
                if (message.getCategory().getIdLong() == 1112487038160220180L) appendTicket = false;

            if (appendTicket) pingMessage.append(
                "\nIf there's something that must be addressed privately, please make a ticket with <#1112486757359960175>");

            if (PingWarnings.warnings.get(authorId) > 1 && message.getMessageReference() != null)
                pingMessage.append("\n-# Please [turn off](https://tenor.com/view/20411479) your reply pings.");

            // Save the message without the warning for later editing
            String warningRemoved = pingMessage.toString();

            if (PingWarnings.warnings.get(authorId) > 1) pingMessage.append("\n-# Warning ").append(
                PingWarnings.warnings.get(authorId)).append("/3");

            message.reply(pingMessage).queue(sentMsg -> sentMsg.editMessage(warningRemoved)
                .setSuppressEmbeds(true).queueAfter(
                    15, TimeUnit.MINUTES, null, new ErrorHandler().handle(
                        ErrorResponse.UNKNOWN_MESSAGE,
                        (e) -> System.out.println(new Utils().getTime(Utils.LogColor.RED) + "Unable to edit warning message."))));
        }
    }
}
