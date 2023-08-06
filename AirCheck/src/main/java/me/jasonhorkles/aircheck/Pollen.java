package me.jasonhorkles.aircheck;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class Pollen {
    public String getPollen() throws IOException {
        System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + "Checking pollen...");

        JSONObject input;
        if (!AirCheck.testing) {
            //todo switch to a better api
            InputStream url = new URL(
                "http://dataservice.accuweather.com/forecasts/v1/daily/1day/" + new Secrets().getAccuLocationCode() + "?apikey=" + new Secrets().getAccuApiKey() + "&details=true").openStream();
            input = new JSONObject(new String(url.readAllBytes(), StandardCharsets.UTF_8));
            url.close();

        } else input = new JSONObject(Files.readString(Path.of("AirCheck/pollen.json")));

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

        StringBuilder pollenForecasts = new StringBuilder(getColor(grassIndex)).append("**Grass**")
            .append(getForecast(grassIndex)).append("\n");

        pollenForecasts.append(getColor(weedIndex)).append("**Ragweed**").append(getForecast(weedIndex))
            .append("\n");

        pollenForecasts.append(getColor(treeIndex)).append("**Tree**").append(getForecast(treeIndex))
            .append("\n");

        System.out.println(new Utils().getTime(
            Utils.LogColor.GREEN) + "Got the pollen! (G:" + grassIndex + " W:" + weedIndex + " T:" + treeIndex + ")");

        return pollenForecasts.toString();
    }

    private String getColor(int value) {
        return switch (value) {
            // 🟢
            case 1 -> "\uD83D\uDFE2 ";
            // 🟡
            case 2 -> "\uD83D\uDFE1 ";
            // 🟠
            case 3 -> "\uD83D\uDFE0 ";
            // 🔴
            case 4 -> "\uD83D\uDD34 ";
            // ⚠️
            case 5 -> "⚠️ ";

            default -> String.valueOf(value);
        };
    }

    private String getForecast(int value) {
        return switch (value) {
            // 🟢
            case 1 -> " → Low";
            // 🟡
            case 2 -> " → Moderate";
            // 🟠
            case 3 -> " → High";
            // 🔴
            case 4 -> " → Very High";
            // ⚠️
            case 5 -> " → Extreme";

            default -> String.valueOf(value);
        };
    }
}
