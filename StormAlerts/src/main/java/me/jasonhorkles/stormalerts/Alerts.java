package me.jasonhorkles.stormalerts;

import com.github.difflib.text.DiffRow;
import com.github.difflib.text.DiffRowGenerator;
import me.jasonhorkles.stormalerts.Utils.LogUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.io.File;
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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static me.jasonhorkles.stormalerts.Utils.ChannelUtils.alertChannel;

public class Alerts {
    public static final String historyDir = "StormAlerts/Alert History";

    private static final List<Long> dontDeleteMe = new ArrayList<>();
    private final String fa = Secrets.Area.FA.area();
    private final String ce = Secrets.Area.CE.area();
    private final String ka = Secrets.Area.KA.area();
    private final String nwf = Secrets.Area.NWF.area();
    private final String da = Secrets.Area.DA.area();

    public void checkAlerts() throws IOException, URISyntaxException {
        LogUtils logUtils = new LogUtils();
        System.out.println(logUtils.getTime(LogUtils.LogColor.YELLOW) + "Checking alerts...");

        dontDeleteMe.clear();

        JSONObject input;
        if (StormAlerts.testing) input = new JSONObject(Files.readString(Path.of(
            "StormAlerts/Tests/alerts-empty.json")));
        else {
            InputStream url = new URI(
                "https://api.weather.gov/alerts/active?status=actual&message_type=alert,update&zone=" + new Secrets().alertZone())
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

        if (alerts.isEmpty()) {
            alertChannel.purgeMessages(alertChannel.getIterableHistory().complete());

            // Delete the alert file history
            //noinspection DataFlowIssue - It should exist
            List<File> files = Arrays.stream(new File(historyDir).listFiles()).toList();
            for (File file : files) //noinspection ResultOfMethodCallIgnored
                file.delete();
            return;
        }

        boolean hasUpdated = false;
        boolean isDifferentDesc = false;
        // For each alert
        for (Object object : alerts) {
            JSONObject alert = new JSONObject(object.toString());
            String description = alert.getString("description");

            // Replace extra spaces with a single space
            description = description.replaceAll(" {2,}", " ");

            // Format newlines
            // Step 1: Replace double newlines with a temporary placeholder (§)
            description = description.replace("\n\n", "§");
            // Step 2: Replace newlines with a space
            description = description.replace("\n", " ");
            // Step 3: Replace the temporary placeholder with a single newline
            description = description.replace("§", "\n");

            // Ignore alerts for (irrelevant) places outside of the region before we format everything else
            String area = alert.getString("areaDesc");
            String[] locations = {
                fa,
                ce,
                ka,
                nwf,
                da
            };
            boolean irrelevantLoc = true;
            for (String location : locations)
                if (description.toLowerCase().contains(location.toLowerCase())) {
                    irrelevantLoc = false;
                    break;
                }
            if (irrelevantLoc) continue;

            // Format small headers
            // Seen NWS formats for headers:
            // * HEADER A...
            // HEADER B...
            Pattern pattern = Pattern.compile("(^|\\n)\\*? ?([A-Z ]+\\.\\.\\.)");
            Matcher matcher = pattern.matcher(description);

            // Replace all occurrences of the above pattern
            StringBuilder result = new StringBuilder();
            // Append the matched group with the new format, removing the '* ' if it exists
            while (matcher.find()) matcher.appendReplacement(
                result,
                matcher.group(1) + "## " + matcher.group(2));
            matcher.appendTail(result);
            description = result.toString();

            // Format "* Locations impacted include"
            description = description.replaceAll(
                "(\\* )?Locations impacted include",
                "### Locations impacted include");

            // Format "This includes the following highways"
            description = description.replace(
                "This includes the following highways",
                "### This includes the following highways");

            // Replace ... with a newline
            description = description.replace("...", "\n");

            // Format bullet points
            // ' - ' -> '\n- '
            description = description.replace(" - ", "\n- ");

            // Format indentations
            description = description.replace("\n ", "\n- ");

            // Finally, bold the relevant areas
            description = boldAreas(description);

            String alertType = alert.getString("messageType");
            if (!alertType.equals("Alert") && !alertType.equals("Update")) continue;

            String id = alert.getString("id").replaceFirst("urn:oid:", "");
            // For each message in the channel
            List<Message> messages = alertChannel.getIterableHistory().complete();
            boolean sameAlert = false;
            for (Message message : messages) {
                // If the message has no embeds, delete it and continue
                if (message.getEmbeds().isEmpty()) {
                    message.delete().queue();
                    continue;
                }

                // If the ID is the same, don't send an update
                //noinspection DataFlowIssue - Every embed will have fields
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
                        //noinspection DataFlowIssue - Every embed will have fields
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
            String boldArea = boldAreas(area);
            String certainty = alert.getString("certainty");
            String event = alert.getString("event");
            String instruction;
            if (alert.isNull("instruction")) instruction = "None";
            else instruction = "- " + alert.getString("instruction").replace("\n", " ").replace("  ", "\n- ");
            String sender = alert.getString("senderName");
            String severity = alert.getString("severity");
            String urgency = alert.getString("urgency");

            if (alertMessage == null)
                System.out.println(logUtils.getTime(LogUtils.LogColor.GREEN) + "Got an alert! " + event);
            else
                System.out.println(logUtils.getTime(LogUtils.LogColor.GREEN) + "Got an update for the \"" + event + "\" alert!");

            EmbedBuilder embed = new EmbedBuilder();
            embed.setAuthor(
                sender,
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

            embed.setDescription(description);
            for (MessageEmbed.Field field : fields) embed.addField(field);

            switch (alertType) {
                case "Alert" -> {
                    String message = "<@&850471646191812700>\n**[" + severity.toUpperCase() + "] " + event + "** for " + boldArea;
                    if (severity.equalsIgnoreCase("Extreme")) message = message.replaceFirst(
                        "<@&850471646191812700>\n",
                        "<@&850471646191812700>\n<a:weewoo:1083615022455992382> ");

                    dontDeleteMe.add(alertChannel.sendMessage(message).setEmbeds(embed.build()).complete()
                        .getIdLong());
                }

                case "Update" -> {
                    // Check if the description has actually changed
                    String oldDescription = alertMessage.getEmbeds().getFirst().getDescription();
                    //noinspection DataFlowIssue - Every embed will have a description
                    if (!oldDescription.equalsIgnoreCase(description)) isDifferentDesc = true;

                    if (isDifferentDesc) {
                        // Create a diff checker
                        DiffRowGenerator generator = DiffRowGenerator.create().showInlineDiffs(true)
                            .mergeOriginalRevised(true).inlineDiffByWord(true)
                            .replaceOriginalLinefeedInChangesWithSpaces(false).oldTag(f -> "~~")
                            .newTag(f -> "__").build();

                        // Generate diffs
                        List<DiffRow> diff = generator.generateDiffRows(
                            List.of(oldDescription),
                            List.of(description));

                        // Get the updated text with the diff formatting applied
                        Stream<DiffRow> diffStream = diff.stream();
                        // Add a space after strikethrough for better readability
                        String diffText = diffStream.map(DiffRow::getOldLine)
                            .collect(Collectors.joining("\n")).replace("~~__", "~~ __");
                        diffStream.close();

                        // Save diffs in a file
                        String fileName = alertMessage.getId() + ".txt";
                        Path path = Path.of(historyDir, fileName);
                        Files.writeString(path, diffText);
                    }

                    String message = "<@&850471690093854810>\n**[" + severity.toUpperCase() + "] " + event + "** for " + boldArea;
                    if (severity.equalsIgnoreCase("Extreme")) message = message.replaceFirst(
                        "<@&850471690093854810>\n",
                        "<@&850471690093854810>\n<a:weewoo:1083615022455992382> ");
                    embed.setFooter("Updated");
                    embed.setTimestamp(Instant.now());

                    dontDeleteMe.add(alertMessage.editMessage(message).setEmbeds(embed.build()).setComponents(
                            ActionRow.of(Button.secondary("viewchanges", "View changes"))).complete()
                        .getIdLong());
                    hasUpdated = true;
                }
            }
        }

        // Delete inactive alerts
        Stream<Message> history = alertChannel.getIterableHistory().complete().stream();
        List<Long> deleteTheseMessages = history.map(Message::getIdLong).collect(Collectors.toCollection(
            ArrayList::new));
        history.close();
        // Remove all the saved messages from the to-delete list
        deleteTheseMessages.removeAll(dontDeleteMe);

        // Delete the remaining to-delete messages
        for (Long id : deleteTheseMessages)
            alertChannel.retrieveMessageById(id).queue(msg -> {
                System.out.println(logUtils.getTime(LogUtils.LogColor.GREEN) + "Deleted \"" + msg
                    .getContentStripped().replaceFirst(".*\n.*] ", "") + "\" alert as it no longer exists.");
                msg.delete().queue();

                // Delete the alert file
                File file = new File(historyDir + "/" + id + ".txt");
                if (file.exists()) //noinspection ResultOfMethodCallIgnored
                    file.delete();
            });

        // Alert if any alert has been updated and the description actually changed
        if (hasUpdated && isDifferentDesc) alertChannel.sendMessage("<@&850471690093854810>").queue(del -> del
            .delete().queueAfter(250, TimeUnit.MILLISECONDS));
    }

    private String boldAreas(String input) {
        List<String> areas = List.of(fa, ce, ka, nwf, da);
        // Replace all areas with bolded areas, including the uppercase version
        return areas.stream().flatMap(area -> Stream.of(area, area.toUpperCase())).reduce(
            input,
            (result, area) -> result.replace(area, "**" + area + "**"));
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
