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
    public void getPollen() throws IOException {
        Utils utils = new Utils();
        System.out.println(utils.getTime(Utils.LogColor.GREEN) + "Checking pollen...");

        String input;
        if (AirCheck.testing) input = Files.readString(Path.of("AirCheck/pollen.txt"));
        else {
            Connection conn = Jsoup
                .connect("https://weather.com/forecast/allergy/l/" + new Secrets().pollenLocationId())
                .timeout(30000).userAgent(
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0 Safari/537.36")
                .referrer("https://www.google.com");
            Document doc = conn.get();

            //noinspection DataFlowIssue
            input = doc.select("[class*=\"PollenBreakdown--body--\"]").first().text();
        }

        Map<String, String> pollenLevels = new HashMap<>();
        Pattern pattern = Pattern.compile("(\\w+?) Pollen (Today|Tonight): (\\w+)");
        Matcher matcher = pattern.matcher(input.replace("Very High", "VeryHigh")
            .replace("Very Low", "VeryLow"));

        while (matcher.find()) {
            String pollenType = matcher.group(1);
            String level = matcher.group(3);

            pollenLevels.put(
                pollenType,
                level.replace("VeryHigh", "Very High").replace("VeryLow", "Very Low"));
        }

        String grassLevel = pollenLevels.getOrDefault("Grass", "ERROR");
        String weedLevel = pollenLevels.getOrDefault("Ragweed", "ERROR");
        String treeLevel = pollenLevels.getOrDefault("Tree", "ERROR");

        utils.updateVoiceChannel(1415457851849048094L, "Grass | " + getColor(grassLevel) + " " + grassLevel);
        utils.updateVoiceChannel(1415453649479536751L, "Ragweed | " + getColor(weedLevel) + " " + weedLevel);
        utils.updateVoiceChannel(1415457875098337380L, "Tree | " + getColor(treeLevel) + " " + treeLevel);

        System.out.println(utils.getTime(Utils.LogColor.GREEN) + "Got the pollen! (G:" + grassLevel + " W:" + weedLevel + " T:" + treeLevel + ")");
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
