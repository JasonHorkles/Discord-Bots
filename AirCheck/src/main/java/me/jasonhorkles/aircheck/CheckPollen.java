package me.jasonhorkles.aircheck;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

@SuppressWarnings("ConstantConditions")
public class CheckPollen {
    public void checkConditions() throws IOException {
        JSONObject input;

        if (!AirCheck.testing) {
            String apiUrl = "http://dataservice.accuweather.com/forecasts/v1/daily/1day/" + new Secrets().getLocationCode() + "?apikey=" + new Secrets().getAccuApiKey() + "&details=true";

            InputStream stream = new URL(apiUrl).openStream();
            String out = new Scanner(stream, StandardCharsets.UTF_8).useDelimiter("\\A").nextLine();
            stream.close();

            input = new JSONObject(out);
        } else {
            File pollenFile = new File("AirCheck/pollen.json");
            Scanner fileScanner = new Scanner(pollenFile);

            input = new JSONObject(fileScanner.nextLine());
        }

        JSONArray pollen = input.getJSONArray("DailyForecasts").getJSONObject(0).getJSONArray("AirAndPollen");
        int grassIndex = -1;
        int weedIndex = -1;
        int treeIndex = -1;

        for (int x = 0; x < pollen.length(); x++) {
            JSONObject obj = pollen.getJSONObject(x);

            switch (obj.getString("Name")) {
                case "Grass" -> grassIndex = obj.getInt("CategoryValue");
                case "Ragweed" -> weedIndex = obj.getInt("CategoryValue");
                case "Tree" -> treeIndex = obj.getInt("CategoryValue");
            }
        }

        String treeIndexName = switch (treeIndex) {
            case 1 -> "Low \uD83D\uDFE2";
            case 2 -> "Moderate \uD83D\uDFE1";
            case 3 -> "High \uD83D\uDFE0";
            case 4 -> "Very high \uD83D\uDD34";
            case 5 -> "Extreme ⚠";
            default -> String.valueOf(treeIndex);
        };

        String grassIndexName = switch (grassIndex) {
            case 1 -> "Low \uD83D\uDFE2";
            case 2 -> "Moderate \uD83D\uDFE1";
            case 3 -> "High \uD83D\uDFE0";
            case 4 -> "Very high \uD83D\uDD34";
            case 5 -> "Extreme ⚠";
            default -> String.valueOf(grassIndex);
        };

        String ragweedIndexName = switch (weedIndex) {
            case 1 -> "Low \uD83D\uDFE2";
            case 2 -> "Moderate \uD83D\uDFE1";
            case 3 -> "High \uD83D\uDFE0";
            case 4 -> "Very high \uD83D\uDD34";
            case 5 -> "Extreme ⚠";
            default -> String.valueOf(weedIndex);
        };

        long grassPollenChannel = 877269665578115092L;
        if (!AirCheck.jda.getVoiceChannelById(grassPollenChannel).getName().equals("Grass | " + grassIndexName))
            AirCheck.jda.getVoiceChannelById(grassPollenChannel).getManager().setName("Grass | " + grassIndexName)
                .queue();

        long ragweedPollenChannel = 877269703180054577L;
        if (!AirCheck.jda.getVoiceChannelById(ragweedPollenChannel).getName().equals("Ragweed | " + ragweedIndexName))
            AirCheck.jda.getVoiceChannelById(ragweedPollenChannel).getManager().setName("Ragweed | " + ragweedIndexName)
                .queue();

        long treePollenChannel = 877269444160815104L;
        if (!AirCheck.jda.getVoiceChannelById(treePollenChannel).getName().equals("Tree | " + treeIndexName))
            AirCheck.jda.getVoiceChannelById(treePollenChannel).getManager().setName("Tree | " + treeIndexName).queue();

        System.out.println(new Utils().getTime(
            Utils.LogColor.GREEN) + "Got the pollen! (G:" + grassIndex + " W:" + weedIndex + " T:" + treeIndex + ")");
    }
}
