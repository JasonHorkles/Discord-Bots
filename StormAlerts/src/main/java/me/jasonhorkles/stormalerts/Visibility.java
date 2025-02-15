package me.jasonhorkles.stormalerts;

import me.jasonhorkles.stormalerts.Utils.ChannelUtils;
import me.jasonhorkles.stormalerts.Utils.LogUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class Visibility {
    public void checkConditions() throws IOException, URISyntaxException {
        System.out.println(new LogUtils().getTime(LogUtils.LogColor.YELLOW) + "Checking visibility...");

        JSONObject input;
        if (StormAlerts.testing) input = new JSONObject(Files.readString(Path.of(
            "StormAlerts/Tests/visibility.json")));
        else {
            InputStream url = new URI("https://api.weather.gov/stations/" + new Secrets().nwsStation() + "/observations/latest")
                .toURL().openStream();
            input = new JSONObject(new String(url.readAllBytes(), StandardCharsets.UTF_8));
            url.close();
        }

        JSONObject rawVisibility = input.getJSONObject("properties").getJSONObject("visibility");
        String visibility = rawVisibility.isNull("value") ? "N/A" : String.valueOf((int) Math.round(
            rawVisibility.getInt("value") / 1609.0d));
        new ChannelUtils().updateVoiceChannel(899872710233051178L, "Visibility | " + visibility + " mi");
    }
}
