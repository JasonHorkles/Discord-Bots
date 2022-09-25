package me.jasonhorkles.stormalerts;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("ConstantConditions")
public class Alerts {
    private static final ArrayList<Message> dontDeleteMe = new ArrayList<>();
    private final String fa = Secrets.Area.FA.getArea();
    private final String ce = Secrets.Area.CE.getArea();
    private final String ka = Secrets.Area.KA.getArea();
    private final String nwf = Secrets.Area.NWF.getArea();
    private final String da = Secrets.Area.DA.getArea();

    public void checkAlerts() throws IOException {
        System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Checking alerts...");

        dontDeleteMe.clear();
        StringBuilder input = new StringBuilder();

        if (!StormAlerts.testing) {
            String apiUrl = "https://api.weather.gov/alerts/active?status=actual&message_type=alert,update&zone=" + new Secrets().getAlertZone();
            InputStream stream = new URL(apiUrl).openStream();
            Scanner scanner = new Scanner(stream, StandardCharsets.UTF_8).useDelimiter("\\A");
            while (scanner.hasNextLine()) input.append(scanner.nextLine());
            stream.close();

        } else {
            File file = new File("StormAlerts/Tests/alerts-empty.json");
            Scanner fileScanner = new Scanner(file);
            input.append(fileScanner.nextLine());
        }

        // Used to scan through every alert
        JSONArray alerts = new JSONArray();
        // Convert the input string to an object
        JSONObject inputObject = new JSONObject(input.toString());
        // Get what type the input is
        String type = inputObject.getString("type");

        // For each feature
        if (type.equals("Feature")) alerts.put(inputObject.get("properties"));
            // FeatureCollection
        else for (Object feature : inputObject.getJSONArray("features"))
            alerts.put(new JSONObject(feature.toString()).getJSONObject("properties"));

        TextChannel alertsChannel = StormAlerts.jda.getTextChannelById(850442466775662613L);

        if (alerts.isEmpty()) {
            alertsChannel.purgeMessages(alertsChannel.getIterableHistory().complete());
            return;
        }

        // For each alert
        for (Object objects : alerts) {
            JSONObject alert = new JSONObject(objects.toString());

            String description = boldAreas(
                new Utils().getJsonKey(alert, "description", true).replace("\\n", " ").replace("  ", "\n\n"));
            String id = new Utils().getJsonKey(alert, "id", true).replaceFirst("urn:oid:", "");
            String event = new Utils().getJsonKey(alert, "event", true);

            if (!description.toLowerCase().contains(fa.toLowerCase()) && !description.toLowerCase()
                .contains(ce.toLowerCase()) && !description.toLowerCase()
                .contains(ka.toLowerCase()) && !description.toLowerCase().contains(nwf.toLowerCase())) continue;

            Message alertMessage = null;
            boolean sameAlert = false;

            String alertType = new Utils().getJsonKey(alert, "messageType", true);
            if (!alertType.equals("Alert") && !alertType.equals("Update")) continue;

            List<Message> messages = alertsChannel.getIterableHistory().complete();

            // For each message in the channel
            for (Message message : messages) {
                // If the message has no embeds, delete it and continue
                if (message.getEmbeds().isEmpty()) {
                    message.delete().queue();
                    continue;
                }

                // If the ID is the same
                // And the description is the same, don't send an update
                if (message.getEmbeds().get(0).getFields().get(3).getValue().equals(id))
                    if (message.getEmbeds().get(0).getDescription().equals(description)) {
                        sameAlert = true;
                        dontDeleteMe.add(message);
                        break;
                    }
            }

            if (sameAlert) continue;

            // If the alert is an update
            if (alertType.equals("Update")) {
                boolean idFound = false;
                // Scan messages for the old ID(s)
                for (Message message : messages) {
                    if (idFound) break;
                    // Get the old ID(s)
                    for (Object references : alert.getJSONArray("references")) {
                        String identifier = new Utils().getJsonKey(new JSONObject(references.toString()), "identifier",
                            true).replaceFirst("urn:oid:", "");

                        // Compare if the footer has that ID
                        if (message.getEmbeds().get(0).getFields().get(3).getValue().equals(identifier)) {
                            alertMessage = message;
                            idFound = true;
                            break;
                        }
                    }
                }

                if (!idFound) alertType = "Alert";
            }

            String area = boldAreas(new Utils().getJsonKey(alert, "areaDesc", true));
            String severity = new Utils().getJsonKey(alert, "severity", true);
            String certainty = new Utils().getJsonKey(alert, "certainty", true);
            String urgency = new Utils().getJsonKey(alert, "urgency", true);
            String sender = new Utils().getJsonKey(alert, "senderName", true);
            String headline = new Utils().getJsonKey(alert, "headline", true);
            String instruction = new Utils().getJsonKey(alert, "instruction", true);
            String nwsHeadline = null;
            if (alert.toString().contains("NWSheadline")) {
                nwsHeadline = alert.toString().replaceFirst(".*\"NWSheadline\":\\[\"", "").replaceFirst("\"],\".*", "");
                if (nwsHeadline.length() >= 253)
                    nwsHeadline = nwsHeadline.substring(0, Math.min(nwsHeadline.length(), 253)) + "...";
            }

            if (alertMessage == null)
                System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + "Got an alert! " + event);
            else System.out.println(
                new Utils().getTime(Utils.LogColor.GREEN) + "Got an update for the \"" + event + "\" alert!");

            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle(headline).setDescription(description)
                .setFooter(sender, "https://pbs.twimg.com/profile_images/1076936762377814016/AOf7ktiH.jpg")
                .addField("Instruction", instruction.replace("\\n", " ").replace("  ", "\n\n"), false)
                .addField("Certainty", certainty, true).addField("Urgency", urgency, true).addField("ID", id, false);
            if (nwsHeadline != null) embed.setAuthor(nwsHeadline, null, null);

            switch (event) {
                case "911 Telephone Outage Emergency" ->
                    embed.setThumbnail("http://surefirecpr.com/wp-content/uploads/call-911-1024x1024.jpg");

                case "Administrative Message" -> embed.setThumbnail(
                    "https://www.lifewire.com/thmb/IS4gxtmhvYTokbYCcm4ygOUBX50=/1920x1200/filters:fill(auto,1)/how-to-fix-the-unknown-message-not-found-on-iphone-error-849e332f4e9241db9cb80ef9ddb63e01.jpg");

                case "Air Quality Alert" ->
                    embed.setThumbnail("https://breathepa.org/wp-content/uploads/2015/07/air-quality-alert-4x3-1.jpg");

                case "Blizzard Warning", "Blizzard Watch" ->
                    embed.setThumbnail("https://thestarryeye.typepad.com/.a/6a00d8341cdd0d53ef022ad3c2a6f5200d-pi");

                case "Brisk Wind Advisory", "High Wind Warning", "High Wind Watch", "Wind Advisory", "Extreme Wind Warning" ->
                    embed.setThumbnail(
                        "https://creazilla-store.fra1.digitaloceanspaces.com/cliparts/76394/wind-clipart-xl.png");

                case "Child Abduction Emergency" -> embed.setThumbnail(
                    "https://bloximages.chicago2.vip.townnews.com/tctimes.com/content/tncms/assets/v3/editorial/6/4e/64e135b2-043a-11e7-b2d1-d378823102d0/58c063da3ce0c.image.jpg?resize=400%2C328");

                case "Dense Fog Advisory", "Dense Smoke Advisory" -> embed.setThumbnail(
                    "https://www.seekpng.com/png/full/122-1220930_free-download-cloudy-weather-clipart-cloudy-day-clip.png");

                case "Earthquake Warning" -> embed.setThumbnail(
                    "https://media.istockphoto.com/vectors/earthquake-metaphor-vector-icon-vector-id1091823174?k=6&m=1091823174&s=612x612&w=0&h=GgBuA6w0ISsUpMzqlOjI4mOvugx5SuiQPIkjyloVX1k=");

                case "Excessive Heat Warning", "Excessive Heat Watch", "Heat Advisory" -> embed.setThumbnail(
                    "https://www.kindpng.com/picc/m/63-636901_heat-clipart-thermometer-thermometer-clipart-transparent-hd-png.png");

                case "Extreme Cold Warning", "Extreme Cold Watch" -> embed.setThumbnail(
                    "https://clipart.world/wp-content/uploads/2021/04/Cold-Thermometer-clipart-transparent.png");

                case "Extreme Fire Danger", "Fire Warning", "Fire Weather Watch", "Red Flag Warning" ->
                    embed.setThumbnail(
                        "http://dnrc.mt.gov/divisions/water/operations/images/floodplain/Fire_Icon.png/image");

                case "Flash Flood Statement", "Flash Flood Warning", "Flash Flood Watch", "Flood Advisory", "Flood Statement", "Flood Warning", "Flood Watch" ->
                    embed.setThumbnail(
                        "https://media.istockphoto.com/vectors/flood-disaster-home-vector-vector-id1038699624?k=6&m=1038699624&s=612x612&w=0&h=rUAO-3bCnkS67NhoBn_lKssFsfncWoSx0sTMJB6MbkE=");

                case "Snow Squall Warning", "Winter Storm Warning", "Winter Storm Watch", "Winter Weather Advisory" ->
                    embed.setThumbnail(
                        "https://cdn.discordapp.com/attachments/335445132520194058/918901071353614336/wintershovel.png");

                case "Freeze Warning", "Freeze Watch", "Frost Advisory", "Hard Freeze Warning", "Hard Freeze Watch" ->
                    embed.setThumbnail(
                        "https://thumbs.dreamstime.com/b/frost-texture-frozen-glass-surfaces-blue-ice-sheet-white-marks-frosty-crystal-winter-pattern-transparent-water-crystals-196937681.jpg");

                case "Severe Thunderstorm Warning", "Storm Watch", "Storm Warning", "Special Weather Statement", "Severe Weather Statement", "Severe Thunderstorm Watch" ->
                    embed.setThumbnail(
                        "https://cdn.discordapp.com/icons/843919716677582888/031643a212f5edbb8d153a5686796c0a.webp?size=128");

                default -> embed.setThumbnail(
                    "https://media.discordapp.net/attachments/421827334534856705/871617342210203689/Warning.png?width=714&height=676");
            }

            switch (severity) {
                case "Extreme" -> embed.setColor(new Color(212, 43, 65));
                case "Severe" -> embed.setColor(new Color(236, 143, 0));
                case "Moderate" -> embed.setColor(new Color(248, 202, 77));
                case "Minor" -> embed.setColor(new Color(126, 177, 84));
                default -> embed.setColor(new Color(50, 55, 61));
            }

            switch (alertType) {
                case "Alert" -> dontDeleteMe.add(alertsChannel.sendMessage(
                        "<@&850471646191812700>\n**[" + severity.toUpperCase() + "] " + event + "** for " + area)
                    .setEmbeds(embed.build()).complete());

                case "Update" -> {
                    dontDeleteMe.remove(alertMessage);
                    dontDeleteMe.add(alertMessage.editMessage(
                            "<@&850471690093854810>\n**[" + severity.toUpperCase() + "] " + event + "** for " + area)
                        .setEmbeds(embed.build()).complete());
                    alertsChannel.sendMessage("<@&850471690093854810>")
                        .queue((del) -> del.delete().queueAfter(250, TimeUnit.MILLISECONDS));
                }
            }
        }

        // Delete inactive alerts
        // Add all messages to the arraylist
        ArrayList<Message> deleteTheseMessages = new ArrayList<>(alertsChannel.getIterableHistory().complete());
        // For every message in the channel
        // Remove all the saved messages from the to-delete arraylist
        for (Message message : dontDeleteMe) deleteTheseMessages.remove(message);
        // For all the remaining to-delete messages
        for (Message message : deleteTheseMessages) {
            System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + "Deleted \"" + message.getContentStripped()
                .replaceFirst(".*\n.*] ", "") + "\" alert as it no longer exists.");
            // Delete them
            message.delete().queue();
        }
    }

    private String boldAreas(String input) {
        return input.replace(fa, "**" + fa + "**").replace(ce, "**" + ce + "**").replace(ka, "**" + ka + "**")
            .replace(nwf, "**" + nwf + "**").replace(da, "**" + da + "**")

            .replace(fa.toUpperCase(), "**" + fa.toUpperCase() + "**")
            .replace(ce.toUpperCase(), "**" + ce.toUpperCase() + "**")
            .replace(ka.toUpperCase(), "**" + ka.toUpperCase() + "**")
            .replace(nwf.toUpperCase(), "**" + nwf.toUpperCase() + "**")
            .replace(da.toUpperCase(), "**" + da.toUpperCase() + "**");
    }
}
