package me.jasonhorkles.aircheck;

import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Scanner;

public class CheckAQI {
    public void checkAir() throws IOException {
        JSONObject input;

        if (!AirCheck.testing) {
            String apiUrl = "https://api.tomorrow.io/v4/timelines?apikey=" + new Secrets().getApiKey() + "&location=" + new Secrets().getLocation() + "&units=imperial&timesteps=current&timezone=America/Denver&fields=epaHealthConcern,epaIndex";

            InputStream stream = new URL(apiUrl).openStream();
            String out = new Scanner(stream, StandardCharsets.UTF_8).useDelimiter("\\A").next();
            stream.close();

            input = new JSONObject(out);
        } else {
            File file = new File("AirCheck/air.json");
            Scanner fileScanner = new Scanner(file);

            input = new JSONObject(fileScanner.nextLine());
        }

        ArrayList<String> air = new ArrayList<>();
        air.add(new Utils().getJsonKey(input, "epaHealthConcern", true));
        air.add(new Utils().getJsonKey(input, "epaIndex", true));

        doAirStuff(air);
    }

    public void doAirStuff(ArrayList<String> air) {
        System.out.println(new Utils().getTime(Utils.Color.GREEN) + "Got the air!");

        int epaStatus = Integer.parseInt(air.get(0));
        int aqi = Integer.parseInt(air.get(1));

        String airQualityName = switch (epaStatus) {
            case 0 -> "Good \uD83D\uDFE2";
            case 1 -> "Moderate \uD83D\uDFE1";
            case 2 -> "Unhealty for sensitive groups \uD83D\uDFE0";
            case 3 -> "Unhealthy \uD83D\uDD34";
            case 4 -> "Very unhealthy ⚫";
            case 5 -> "Hazardous ⚠";
            default -> String.valueOf(epaStatus);
        };

        AirCheck.api.getPresence().setStatus(OnlineStatus.ONLINE);
        AirCheck.api.getPresence().setActivity(Activity.playing(airQualityName + " (" + aqi + ")"));
    }
}
