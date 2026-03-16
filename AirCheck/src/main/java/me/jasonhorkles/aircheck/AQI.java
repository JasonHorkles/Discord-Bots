package me.jasonhorkles.aircheck;

import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class AQI {
    public AQI() {
        utils = new Utils();
    }

    public static final long CHANNEL_ID = 1261785476277338163L;
    public static int maxAqiLevel = -1;

    private final Utils utils;

    public void checkAir() throws IOException, URISyntaxException {
        System.out.println(utils.getTime(Utils.LogColor.YELLOW) + "Checking air quality...");

        JSONArray input;
        if (AirCheck.testing) input = new JSONArray(Files.readString(Path.of("AirCheck/Tests/air.json")));
        else {
            InputStream url = new URI(
                "https://www.airnowapi.org/aq/observation/zipCode/current/?format=application/json&zipCode=" + new Secrets().zip() + "&distance=25&API_KEY=" + new Secrets().aqiApiKey())
                .toURL().openStream();
            input = new JSONArray(new String(url.readAllBytes(), StandardCharsets.UTF_8));
            url.close();
        }

        if (input.isEmpty()) {
            System.out.println(utils.getTime(Utils.LogColor.RED) + "[ERROR] No air quality data found!");
            AirCheck.jda.getPresence().setStatus(OnlineStatus.DO_NOT_DISTURB);
            AirCheck.jda.getPresence().setActivity(Activity.customStatus("⚠ No air quality data found!"));
            return;
        }

        int highestAqiIndex = -1;
        int highestAqi = -1;
        for (int x = 0; x < input.length(); x++) {
            int aqi = input.getJSONObject(x).getInt("AQI");
            if (aqi > highestAqi) {
                highestAqiIndex = x;
                highestAqi = aqi;
            }
        }

        JSONObject topPollutantInfo = input.getJSONObject(highestAqiIndex);
        int catNumber = topPollutantInfo.getJSONObject("Category").getInt("Number");
        String topPollutant = topPollutantInfo.getString("ParameterName");
        if (topPollutant.equals("O3")) topPollutant = "Ozone";

        String airQualityName = switch (catNumber) {
            case 1 -> "Good 🟢";
            case 2 -> "Moderate 🟡";
            case 3 -> "Unhealty for sensitive groups 🟠";
            case 4 -> "Unhealthy 🔴";
            case 5 -> "Very unhealthy ⚫";
            case 6 -> "Hazardous ⚠️";
            case 7 -> "⚠️ Unavailable";
            default -> "⚠️ Error: " + catNumber;
        };

        AirCheck.jda.getPresence().setStatus(OnlineStatus.ONLINE);
        AirCheck.jda.getPresence()
            .setActivity(Activity.customStatus("AQI: " + airQualityName + " (" + highestAqi + ", " + topPollutant + ")"));

        System.out.println(utils.getTime(Utils.LogColor.GREEN) + "Got the air! (" + highestAqi + ", " + topPollutant + ")\n ");

        if ((catNumber >= 4 && catNumber < 7) && maxAqiLevel < catNumber) {
            maxAqiLevel = catNumber;

            TextChannel channel = AirCheck.jda.getTextChannelById(CHANNEL_ID);
            String ping = utils.shouldMsgPing(channel) ? "<@&1261811494191108156>\n" : "";

            //noinspection DataFlowIssue
            channel
                .sendMessage(ping + "### ⚠️  __Air Quality Alert__ ⚠️\nThe air quality is currently **" + airQualityName.toLowerCase() + "** with an AQI of **" + highestAqi + "**\nThe top pollutant is **" + topPollutant + "**")
                .queue();
        }
    }
}
