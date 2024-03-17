package me.jasonhorkles.aircheck;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Pollen {
    public String getPollen() throws IOException {
        System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + "Checking pollen...");

        String input;
        if (!AirCheck.testing) {
            Connection conn = Jsoup
                .connect("https://weather.com/forecast/allergy/l/" + new Secrets().getPollenLocationId())
                .timeout(30000);
            Document doc = conn.get();

            //noinspection DataFlowIssue
            input = doc.select("[class*=\"PollenBreakdown--body--\"]").first().text();

        } else input = Files.readString(Path.of("AirCheck/pollen.txt"));

        Map<String, String> pollenLevels = new HashMap<>();
        Pattern pattern = Pattern.compile("(\\w+?) Pollen (Today|Tonight): (\\w+)");
        Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {
            String pollenType = matcher.group(1);
            String level = matcher.group(3);

            pollenLevels.put(pollenType, level);
        }

        String grassLevel = pollenLevels.getOrDefault("Grass", "ERROR");
        String weedLevel = pollenLevels.getOrDefault("Ragweed", "ERROR");
        String treeLevel = pollenLevels.getOrDefault("Tree", "ERROR");

        String pollenForecasts = getColor(grassLevel) + "**Grass** → " + grassLevel + "\n" + getColor(
            weedLevel) + "**Ragweed** → " + weedLevel + "\n" + getColor(treeLevel) + "**Tree** → " + treeLevel + "\n";

        System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + "Got the pollen! (G:" + grassLevel + " W:" + weedLevel + " T:" + treeLevel + ")");

        return pollenForecasts;
    }

    private String getColor(String value) {
        return switch (value) {
            // ⭕
            case "ERROR" -> "⭕ ";
            // 🟢
            case "None" -> "\uD83D\uDFE2 ";
            // 🔵
            case "Very Low" -> "\uD83D\uDD35 ";
            // 🟡
            case "Low" -> "\uD83D\uDFE1 ";
            // 🟠
            case "Moderate" -> "\uD83D\uDFE0 ";
            // 🔴
            case "High" -> "\uD83D\uDD34 ";
            // ⚠️
            case "Very High" -> "⚠️ ";

            default -> value;
        };
    }
}
