package me.jasonhorkles.aircheck;

import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
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
    public void checkAir() throws IOException, URISyntaxException {
        System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + "Checking air quality...");

        JSONArray input;
        if (AirCheck.testing) input = new JSONArray(Files.readString(Path.of("AirCheck/air.json")));
        else {
            InputStream url = new URI(
                "https://www.airnowapi.org/aq/observation/zipCode/current/?format=application/json&zipCode=" + new Secrets().getZip() + "&distance=25&API_KEY=" + new Secrets().getAqiApiKey())
                .toURL().openStream();
            input = new JSONArray(new String(url.readAllBytes(), StandardCharsets.UTF_8));
            url.close();

        }

        if (input.isEmpty()) {
            System.out.println(new Utils().getTime(Utils.LogColor.RED) + "[ERROR] No air quality data found!");
            AirCheck.jda.getPresence().setStatus(OnlineStatus.DO_NOT_DISTURB);
            AirCheck.jda.getPresence().setActivity(Activity.playing("⚠ No air quality data found!"));
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
            // 🟢
            case 1 -> "Good \uD83D\uDFE2";
            // 🟡
            case 2 -> "Moderate \uD83D\uDFE1";
            // 🟠
            case 3 -> "Unhealty for sensitive groups \uD83D\uDFE0";
            // 🔴
            case 4 -> "Unhealthy \uD83D\uDD34";
            // ⚫
            case 5 -> "Very unhealthy ⚫";
            // ⚠️
            case 6 -> "Hazardous ⚠️";
            case 7 -> "⚠️ Unavailable";

            default -> "⚠️ Error: " + catNumber;
        };

        AirCheck.jda.getPresence().setStatus(OnlineStatus.ONLINE);
        AirCheck.jda.getPresence()
            .setActivity(Activity.customStatus(airQualityName + " (" + highestAqi + ", " + topPollutant + ")"));

        System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + "Got the air! (" + highestAqi + ", " + topPollutant + ")");
    }
}
