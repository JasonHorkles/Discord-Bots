package me.jasonhorkles.aircheck;

import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class AQI {
    public void checkAir() throws IOException {
        System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + "Checking air quality...");

        JSONArray input;
        if (!AirCheck.testing) {
            InputStream url = new URL(
                "https://www.airnowapi.org/aq/observation/zipCode/current/?format=application/json&zipCode=" + new Secrets().getZip() + "&distance=25&API_KEY=" + new Secrets().getAqiApiKey()).openStream();
            input = new JSONArray(new String(url.readAllBytes(), StandardCharsets.UTF_8));
            url.close();

        } else input = new JSONArray(Files.readString(Path.of("AirCheck/air.json")));

        JSONObject pm25 = null;
        for (int x = 0; x < input.length(); x++) {
            if (!input.getJSONObject(x).getString("ParameterName").equals("PM2.5")) continue;
            pm25 = input.getJSONObject(x);
        }

        if (pm25 == null) {
            System.out.println(new Utils().getTime(Utils.LogColor.RED) + "[ERROR] Couldn't find the PM2.5!");
            AirCheck.jda.getPresence().setStatus(OnlineStatus.DO_NOT_DISTURB);
            AirCheck.jda.getPresence().setActivity(Activity.playing("⚠ Error"));
            return;
        }

        int catNumber = pm25.getJSONObject("Category").getInt("Number");
        int aqi = pm25.getInt("AQI");

        String airQualityName = switch (catNumber) {
            case 1 -> "Good \uD83D\uDFE2";
            case 2 -> "Moderate \uD83D\uDFE1";
            case 3 -> "Unhealty for sensitive groups \uD83D\uDFE0";
            case 4 -> "Unhealthy \uD83D\uDD34";
            case 5 -> "Very unhealthy ⚫";
            case 6 -> "Hazardous ⚠";
            case 7 -> "⚠ Unavailable";
            default -> "⚠ Error: " + catNumber;
        };

        AirCheck.jda.getPresence().setStatus(OnlineStatus.ONLINE);
        AirCheck.jda.getPresence().setActivity(Activity.playing(airQualityName + " (" + aqi + ")"));

        System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + "Got the air! (" + aqi + ")");
    }
}
