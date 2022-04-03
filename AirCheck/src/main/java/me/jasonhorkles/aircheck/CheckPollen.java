package me.jasonhorkles.aircheck;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

@SuppressWarnings("ConstantConditions")
public class CheckPollen {
    public void checkConditions() throws IOException {
        JSONObject input;

        if (!AirCheck.testing) {
            String apiUrl = "https://api.tomorrow.io/v4/timelines?apikey=" + new Secrets().getApiKey() + "&location=" + new Secrets().getLocation() + "&units=imperial&timesteps=current&timezone=America/Denver&fields=treeIndex,grassIndex,weedIndex";
            String out = new Scanner(new URL(apiUrl).openStream(), StandardCharsets.UTF_8).useDelimiter("\\A").next();

            input = new JSONObject(out);
        } else {
            File pollenFile = new File("AirCheck/pollen.json");
            Scanner fileScanner = new Scanner(pollenFile);

            input = new JSONObject(fileScanner.nextLine());
        }

        int grassIndex = Integer.parseInt(new Utils().getJsonKey(input, "grassIndex", true));
        int ragweedIndex = Integer.parseInt(new Utils().getJsonKey(input, "weedIndex", true));
        int treeIndex = Integer.parseInt(new Utils().getJsonKey(input, "treeIndex", true));

        String treeIndexName = switch (treeIndex) {
            case 0 -> "None \uD83D\uDFE2";
            case 1 -> "Very low \uD83D\uDFE1";
            case 2 -> "Low \uD83D\uDFE0";
            case 3 -> "Medium \uD83D\uDD34";
            case 4 -> "High ⚫";
            case 5 -> "Very high ⚠";
            default -> String.valueOf(treeIndex);
        };

        String grassIndexName = switch (grassIndex) {
            case 0 -> "None \uD83D\uDFE2";
            case 1 -> "Very low \uD83D\uDFE1";
            case 2 -> "Low \uD83D\uDFE0";
            case 3 -> "Medium \uD83D\uDD34";
            case 4 -> "High ⚫";
            case 5 -> "Very high ⚠";
            default -> String.valueOf(grassIndex);
        };

        String ragweedIndexName = switch (ragweedIndex) {
            case 0 -> "None \uD83D\uDFE2";
            case 1 -> "Very low \uD83D\uDFE1";
            case 2 -> "Low \uD83D\uDFE0";
            case 3 -> "Medium \uD83D\uDD34";
            case 4 -> "High ⚫";
            case 5 -> "Very high ⚠";
            default -> String.valueOf(ragweedIndex);
        };

        long grassPollenChannel = 877269665578115092L;
        if (!AirCheck.api.getVoiceChannelById(grassPollenChannel).getName().equals("Grass | " + grassIndexName))
            AirCheck.api.getVoiceChannelById(grassPollenChannel).getManager().setName("Grass | " + grassIndexName)
                .queue();

        long ragweedPollenChannel = 877269703180054577L;
        if (!AirCheck.api.getVoiceChannelById(ragweedPollenChannel).getName().equals("Ragweed | " + ragweedIndexName))
            AirCheck.api.getVoiceChannelById(ragweedPollenChannel).getManager().setName("Ragweed | " + ragweedIndexName)
                .queue();

        long treePollenChannel = 877269444160815104L;
        if (!AirCheck.api.getVoiceChannelById(treePollenChannel).getName().equals("Tree | " + treeIndexName))
            AirCheck.api.getVoiceChannelById(treePollenChannel).getManager().setName("Tree | " + treeIndexName).queue();
    }
}
