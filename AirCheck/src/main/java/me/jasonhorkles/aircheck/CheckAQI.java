package me.jasonhorkles.aircheck;

import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class CheckAQI {
    public void checkAir() throws IOException {
        JSONArray input;

        if (!AirCheck.testing) {
            String apiUrl = "https://www.airnowapi.org/aq/observation/zipCode/current/?format=application/json&zipCode=" + new Secrets().getZip() + "&distance=25&API_KEY=" + new Secrets().getAqiApiKey();

            InputStream stream = new URL(apiUrl).openStream();
            String out = new Scanner(stream, StandardCharsets.UTF_8).useDelimiter("\\A").nextLine();
            stream.close();

            input = new JSONArray(out);
        } else {
            File file = new File("AirCheck/air.json");
            Scanner fileScanner = new Scanner(file);

            input = new JSONArray(fileScanner.nextLine());
        }

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
