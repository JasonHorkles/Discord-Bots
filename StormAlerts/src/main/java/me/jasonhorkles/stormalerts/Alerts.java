package me.jasonhorkles.stormalerts;

import com.github.difflib.text.DiffRow;
import com.github.difflib.text.DiffRowGenerator;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@SuppressWarnings("DataFlowIssue")
public class Alerts {
    private static final List<Long> dontDeleteMe = new ArrayList<>();
    private final String fa = Secrets.Area.FA.getArea();
    private final String ce = Secrets.Area.CE.getArea();
    private final String ka = Secrets.Area.KA.getArea();
    private final String nwf = Secrets.Area.NWF.getArea();
    private final String da = Secrets.Area.DA.getArea();

    public void checkAlerts() throws IOException, URISyntaxException {
        System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Checking alerts...");

        dontDeleteMe.clear();

        JSONObject input;
        if (StormAlerts.testing) input = new JSONObject(Files.readString(Path.of(
            "StormAlerts/Tests/alerts-empty.json")));
        else {
            InputStream url = new URI(
                "https://api.weather.gov/alerts/active?status=actual&message_type=alert,update&zone=" + new Secrets().getAlertZone())
                .toURL().openStream();
            input = new JSONObject(new String(url.readAllBytes(), StandardCharsets.UTF_8));
            url.close();

        }

        JSONArray alerts = new JSONArray();
        String type = input.getString("type");
        // Feature (1 alert)
        if (type.equals("Feature")) alerts.put(input.get("properties"));
            // FeatureCollection (2+ alerts)
        else for (Object feature : input.getJSONArray("features"))
            alerts.put(new JSONObject(feature.toString()).getJSONObject("properties"));

        TextChannel alertsChannel = StormAlerts.jda.getTextChannelById(850442466775662613L);

        if (alerts.isEmpty()) {
            alertsChannel.purgeMessages(alertsChannel.getIterableHistory().complete());
            return;
        }

        boolean hasUpdated = false;
        // For each alert
        for (Object object : alerts) {
            JSONObject alert = new JSONObject(object.toString());

            String description = boldAreas(alert.getString("description").replace("\n", "§").replaceAll(" {2,}",
                    " ").replace("§§", "\n").replace("§", " ").replaceAll("\\s{2}(?=\\b[A-Z]{2,}\\.\\.\\.)",
                    "\n### ").replace("* ", "### ").replaceAll("- ### .*\\.\\.\\.", "")
                .replaceAll("(?<=[A-Z])\\.{3}", "\n").replace(" - ", "\n- "));

            String id = alert.getString("id").replaceFirst("urn:oid:", "");

            if (!description.toLowerCase().contains(fa.toLowerCase()) && !description.toLowerCase().contains(
                ce.toLowerCase()) && !description.toLowerCase().contains(ka.toLowerCase()) && !description
                .toLowerCase().contains(nwf.toLowerCase())) continue;

            String alertType = alert.getString("messageType");
            if (!alertType.equals("Alert") && !alertType.equals("Update")) continue;

            // For each message in the channel
            List<Message> messages = alertsChannel.getIterableHistory().complete();
            boolean sameAlert = false;
            for (Message message : messages) {
                // If the message has no embeds, delete it and continue
                if (message.getEmbeds().isEmpty()) {
                    message.delete().queue();
                    continue;
                }

                // If the ID is the same, don't send an update
                if (message.getEmbeds().getFirst().getFields().get(3).getValue().equals(id)) {
                    sameAlert = true;
                    dontDeleteMe.add(message.getIdLong());
                    break;
                }
            }
            if (sameAlert) continue;

            // If the alert is an update
            Message alertMessage = null;
            if (alertType.equals("Update")) {
                boolean idFound = false;
                // Scan messages for the old ID(s)
                for (Message message : messages) {
                    if (idFound) break;
                    // Get the old ID(s)
                    for (Object references : alert.getJSONArray("references")) {
                        String identifier = new JSONObject(references.toString()).getString("identifier")
                            .replaceFirst("urn:oid:", "");

                        // Compare if the footer has that ID
                        if (message.getEmbeds().getFirst().getFields().get(3).getValue().equals(identifier)) {
                            alertMessage = message;
                            idFound = true;
                            break;
                        }
                    }
                }

                if (!idFound) alertType = "Alert";
            }

            long ends;
            if (alert.isNull("ends")) ends = Instant.from(DateTimeFormatter.ISO_INSTANT.parse(alert.getString(
                "expires"))).toEpochMilli() / 1000;
            else ends = Instant.from(DateTimeFormatter.ISO_INSTANT.parse(alert.getString("ends")))
                .toEpochMilli() / 1000;

            long sent = Instant.from(DateTimeFormatter.ISO_INSTANT.parse(alert.getString("sent")))
                .toEpochMilli() / 1000;
            String area = boldAreas(alert.getString("areaDesc"));
            String certainty = alert.getString("certainty");
            String event = alert.getString("event");
            String instruction;
            if (alert.isNull("instruction")) instruction = "None";
            else instruction = "- " + alert.getString("instruction").replace("\n", " ").replace("  ", "\n- ");
            String sender = alert.getString("senderName");
            String severity = alert.getString("severity");
            String urgency = alert.getString("urgency");

            if (alertMessage == null)
                System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + "Got an alert! " + event);
            else
                System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + "Got an update for the \"" + event + "\" alert!");

            EmbedBuilder embed = new EmbedBuilder();
            embed.setAuthor(sender,
                null,
                "https://pbs.twimg.com/profile_images/1076936762377814016/AOf7ktiH.jpg");
            embed.setTitle("Issued <t:" + sent + ":F>\nEnds <t:" + ends + ":F>");
            embed.setThumbnail(getThumbnailImage(event));

            List<MessageEmbed.Field> fields = new ArrayList<>();
            fields.add(new MessageEmbed.Field("Instruction", instruction, false));
            fields.add(new MessageEmbed.Field("Certainty", certainty, true));
            fields.add(new MessageEmbed.Field("Urgency", urgency, true));
            fields.add(new MessageEmbed.Field("UID", id, false));

            switch (severity) {
                case "Extreme" -> embed.setColor(new Color(212, 43, 65));
                case "Severe" -> embed.setColor(new Color(236, 143, 0));
                case "Moderate" -> embed.setColor(new Color(248, 202, 77));
                case "Minor" -> embed.setColor(new Color(126, 177, 84));
                default -> embed.setColor(new Color(50, 55, 61));
            }

            switch (alertType) {
                case "Alert" -> {
                    String message = "<@&850471646191812700>\n**[" + severity.toUpperCase() + "] " + event + "** for " + area;
                    if (severity.equalsIgnoreCase("Extreme")) message = message.replaceFirst(
                        "<@&850471646191812700>\n",
                        "<@&850471646191812700>\n<a:weewoo:1083615022455992382> ");

                    embed.setDescription(description);
                    for (MessageEmbed.Field field : fields) embed.addField(field);

                    dontDeleteMe.add(alertsChannel.sendMessage(message).setEmbeds(embed.build()).complete()
                        .getIdLong());
                }

                case "Update" -> {
                    // Create a diff checker
                    DiffRowGenerator generator = DiffRowGenerator.create().showInlineDiffs(true)
                        .mergeOriginalRevised(true).inlineDiffByWord(true)
                        .replaceOriginalLinefeedInChangesWithSpaces(true).oldTag(f -> "||").newTag(f -> "__")
                        .build();

                    // Calculate diffs for description
                    String newDescription = applyDiffs(generator,
                        alertMessage.getEmbeds().getFirst().getDescription(),
                        description);
                    embed.setDescription(newDescription);

                    // Calculate diffs for instruction field
                    String newInstruction = applyDiffs(generator,
                        alertMessage.getEmbeds().getFirst().getFields().getFirst().getValue(),
                        instruction);
                    fields.set(0, new MessageEmbed.Field("Instruction", newInstruction, false));
                    for (MessageEmbed.Field field : fields) embed.addField(field);

                    String message = "<@&850471690093854810>\n**[" + severity.toUpperCase() + "] " + event + "** for " + area;
                    if (severity.equalsIgnoreCase("Extreme")) message = message.replaceFirst(
                        "<@&850471690093854810>\n",
                        "<@&850471690093854810>\n<a:weewoo:1083615022455992382> ");
                    embed.setFooter("Updated");
                    embed.setTimestamp(Instant.now());

                    dontDeleteMe.add(alertMessage.editMessage(message).setEmbeds(embed.build()).setActionRow(
                        Button.secondary("viewchanges", "View all changes")).complete().getIdLong());
                    hasUpdated = true;
                }
            }
        }

        // Delete inactive alerts
        List<Long> deleteTheseMessages = alertsChannel.getIterableHistory().complete().stream()
            .map(Message::getIdLong).collect(Collectors.toCollection(ArrayList::new));
        // Remove all the saved messages from the to-delete list
        deleteTheseMessages.removeAll(dontDeleteMe);

        // Delete the remaining to-delete messages
        for (Long id : deleteTheseMessages)
            alertsChannel.retrieveMessageById(id).queue(msg -> {
                System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + "Deleted \"" + msg
                    .getContentStripped().replaceFirst(".*\n.*] ", "") + "\" alert as it no longer exists.");
                msg.delete().queue();
            });

        if (hasUpdated) alertsChannel.sendMessage("<@&850471690093854810>").queue(del -> del.delete()
            .queueAfter(250, TimeUnit.MILLISECONDS));
    }

    private String boldAreas(String input) {
        return input.replace(fa, "**" + fa + "**").replace(ce, "**" + ce + "**").replace(ka, "**" + ka + "**")
            .replace(nwf, "**" + nwf + "**").replace(da, "**" + da + "**")

            .replace(fa.toUpperCase(), "**" + fa.toUpperCase() + "**").replace(ce.toUpperCase(),
                "**" + ce.toUpperCase() + "**").replace(ka.toUpperCase(), "**" + ka.toUpperCase() + "**")
            .replace(
                nwf.toUpperCase(),
                "**" + nwf.toUpperCase() + "**").replace(da.toUpperCase(), "**" + da.toUpperCase() + "**");
    }

    private String applyDiffs(DiffRowGenerator generator, String originalText, String newText) {
        // Remove previous text and strip updated text formatting, then generate diffs
        List<DiffRow> diff = generator.generateDiffRows(List.of(originalText
                .replaceAll("\\|\\|\\s*(.*?)\\s*\\|\\| ?", "").replace("__", "")),
            List.of(newText));

        // Return the updated text with the diff formatting applied
        return diff.stream().map(DiffRow::getOldLine).collect(Collectors.joining("\n")).replaceAll("\\|\\|_",
            "|| _");
    }

    private String getThumbnailImage(String event) {
        switch (event) {
            case "911 Telephone Outage Emergency" -> {
                return "https://cdn.discordapp.com/attachments/335445132520194058/1049516136171044864/call-911-1024x1024.png";
            }

            case "Administrative Message" -> {
                return "https://cdn.discordapp.com/attachments/335445132520194058/1049516136594673694/how-to-fix-the-unknown-message-not-found-on-iphone-error-849e332f4e9241db9cb80ef9ddb63e01.png";
            }

            case "Air Quality Alert" -> {
                return "https://cdn.discordapp.com/attachments/335445132520194058/1049516137085403176/Power-Plant-Clip-Art.png";
            }

            case "Blizzard Warning", "Blizzard Watch" -> {
                return "https://cdn.discordapp.com/attachments/335445132520194058/1049516137605501039/6a00d8341cdd0d53ef022ad3c2a6f5200d-pi.png";
            }

            case "Brisk Wind Advisory", "High Wind Warning", "High Wind Watch", "Wind Advisory",
                 "Extreme Wind Warning" -> {
                return "https://cdn.discordapp.com/attachments/335445132520194058/1049516138079453235/wind-clipart-xl.png";
            }

            case "Child Abduction Emergency" -> {
                return "https://cdn.discordapp.com/attachments/335445132520194058/1049517612880625714/58c063da3ce0c.png";
            }

            case "Dense Fog Advisory", "Dense Smoke Advisory" -> {
                return "https://cdn.discordapp.com/attachments/335445132520194058/1049516436789403658/122-1220930_free-download-cloudy-weather-clipart-cloudy-day-clip.png";
            }

            case "Earthquake Warning" -> {
                return "https://cdn.discordapp.com/attachments/335445132520194058/1049517612566073425/earthquake-metaphor-vector-icon-vec.png";
            }

            case "Excessive Heat Warning", "Excessive Heat Watch", "Heat Advisory" -> {
                return "https://cdn.discordapp.com/attachments/335445132520194058/1049516438030913616/kindpng_636901.png";
            }

            case "Extreme Cold Warning", "Extreme Cold Watch", "Wind Chill Watch", "Wind Chill Warning" -> {
                return "https://cdn.discordapp.com/attachments/335445132520194058/1049516437619867668/Cold-Thermometer-clipart-transparent.png";
            }

            case "Extreme Fire Danger", "Fire Warning", "Fire Weather Watch", "Red Flag Warning" -> {
                return "https://cdn.discordapp.com/attachments/335445132520194058/1049516774737059881/image.png";
            }

            case "Flash Flood Statement", "Flash Flood Warning", "Flash Flood Watch", "Flood Advisory",
                 "Flood Statement", "Flood Warning", "Flood Watch" -> {
                return "https://cdn.discordapp.com/attachments/335445132520194058/1049516775118745640/flood-disaster-home-vector-vector-id1038699624.png";
            }

            case "Snow Squall Warning", "Winter Storm Warning", "Winter Storm Watch",
                 "Winter Weather Advisory" -> {
                return "https://cdn.discordapp.com/attachments/335445132520194058/1049516775492042794/wintershovel.png";
            }

            case "Freeze Warning", "Freeze Watch", "Frost Advisory", "Hard Freeze Warning",
                 "Hard Freeze Watch" -> {
                return "https://cdn.discordapp.com/attachments/335445132520194058/1049516775810801664/frost-texture-frozen-glass-surfaces-blue-ice-sheet-white-marks-frosty-crystal-winter-pattern-transparent-water-crystals-196937681.png";
            }

            case "Severe Thunderstorm Warning", "Storm Watch", "Storm Warning", "Special Weather Statement",
                 "Severe Weather Statement", "Severe Thunderstorm Watch" -> {
                return "https://cdn.discordapp.com/attachments/335445132520194058/1049519190614228992/StormAlerts.png";
            }

            default -> {
                return "https://media.discordapp.net/attachments/421827334534856705/871617342210203689/Warning.png?width=714&height=676";
            }
        }
    }
}
