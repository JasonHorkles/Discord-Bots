package me.jasonhorkles.aircheck;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;

public class Pollen {
    public void getPollen() throws IOException {
        Utils utils = new Utils();
        System.out.println(utils.getTime(Utils.LogColor.YELLOW) + "Checking pollen...");

        JSONObject rawInput;
        if (AirCheck.testing)
            rawInput = new JSONObject(Files.readString(Path.of("AirCheck/Tests/pollen.json")));
        else {
            InputStream url = new Secrets().pollenUrl().openStream();
            rawInput = new JSONObject(new String(url.readAllBytes(), StandardCharsets.UTF_8));
            url.close();
        }

        JSONArray input = new JSONArray();
        JSONArray dailyInfo = rawInput.getJSONArray("dailyInfo");
        if (!dailyInfo.isEmpty()) input = dailyInfo.getJSONObject(0).getJSONArray("pollenTypeInfo");

        if (input.isEmpty()) {
            System.out.println(utils.getTime(Utils.LogColor.RED) + "[ERROR] No pollen data found!");
            return;
        }

        EnumMap<PollenType, PollenData> pollenDataMap = new EnumMap<>(PollenType.class);
        // Add default values
        for (PollenType type : PollenType.values())
            pollenDataMap.put(type, new PollenData(-1, "ERROR"));

        for (int i = 0; i < input.length(); i++) {
            JSONObject pollenInfo = input.getJSONObject(i);
            String code = pollenInfo.getString("code");
            PollenType type;
            try {
                type = PollenType.valueOf(code);
            } catch (IllegalArgumentException e) {
                System.out.println(utils.getTime(Utils.LogColor.RED) + "[ERROR] Unknown pollen type: " + code);
                continue;
            }

            JSONObject indexInfo = pollenInfo.optJSONObject(
                "indexInfo",
                new JSONObject().put("value", 0).put("category", "None"));

            int level = indexInfo.getInt("value");
            String category = indexInfo.getString("category");

            pollenDataMap.put(type, new PollenData(level, category));
        }

        Map<PollenType, Long> channelMap = Map.of(
            PollenType.GRASS,
            1415457851849048094L,
            PollenType.TREE,
            1415457875098337380L,
            PollenType.WEED,
            1415453649479536751L);

        // Update each channel with the corresponding data
        if (!AirCheck.testing) for (Entry<PollenType, Long> entry : channelMap.entrySet()) {
            PollenData data = pollenDataMap.get(entry.getKey());
            utils.updateVoiceChannel(
                entry.getValue(),
                entry.getKey().getDisplayName() + " | " + getColor(data.level()) + " " + data.category());
        }

        PollenData grassLevel = pollenDataMap.get(PollenType.GRASS);
        PollenData treeLevel = pollenDataMap.get(PollenType.TREE);
        PollenData weedLevel = pollenDataMap.get(PollenType.WEED);

        System.out.println(utils.getTime(Utils.LogColor.GREEN) + "Pollen: G:" + grassLevel.category() + " T:" + treeLevel.category() + " W:" + weedLevel.category() + "\n ");
    }

    private enum PollenType {
        GRASS("Grass"),
        TREE("Tree"),
        WEED("Weed");

        private final String displayName;

        PollenType(String displayName) {
            this.displayName = displayName;
        }

        private String getDisplayName() {
            return displayName;
        }
    }

    private String getColor(int value) {
        // https://developers.google.com/maps/documentation/pollen/pollen-index
        return switch (value) {
            // Error
            case -1 -> "❌ ";

            // None
            case 0 -> "🔵 ";

            // Very Low
            case 1 -> "🟢 ";

            // Low
            case 2 -> "🟡 ";

            // Moderate
            case 3 -> "🟠 ";

            // High
            case 4 -> "🔴 ";

            // Very High
            case 5 -> "⚠️ ";

            default -> String.valueOf(value);
        };
    }

    private record PollenData(int level, String category) {}
}
