package me.jasonhorkles.stormalerts;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class Visibility {
    public void checkConditions() throws IOException {
        System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Checking visibility...");

        JSONObject input;
        if (!StormAlerts.testing) {
            InputStream url = new URL("https://api.weather.gov/stations/" + new Secrets().getNwsStation() + "/observations/latest").openStream();
            input = new JSONObject(new String(url.readAllBytes(), StandardCharsets.UTF_8));
            url.close();

        } else input = new JSONObject(Files.readString(Path.of("StormAlerts/Tests/visibility.json")));

        String visibility = String.valueOf((int) Math.round(input.getJSONObject("properties")
            .getJSONObject("visibility").getInt("value") / 1609d));
        new Utils().updateVoiceChannel(899872710233051178L, "Visibility | " + visibility + " mi");
    }
}
