package me.jasonhorkles.aircheck;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;

public class Utils {
    public enum LogColor {
        RED("\u001B[31m"), YELLOW("\u001B[33m"), GREEN("\u001B[32m");

        private final String logColor;

        LogColor(String logColor) {
            this.logColor = logColor;
        }

        public String getLogColor() {
            return logColor;
        }
    }

    public String getTime(LogColor logColor) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("hh:mm:ss a");
        return logColor.getLogColor() + "[" + dtf.format(LocalDateTime.now()) + "] ";
    }

    String value;

    public String getJsonKey(JSONObject json, String key, boolean firstRun) {
        boolean exists = json.has(key);
        Iterator<?> keys;
        String nextKeys;
        if (firstRun) value = "null";

        if (!exists) {
            keys = json.keys();

            while (keys.hasNext()) {
                nextKeys = (String) keys.next();
                try {
                    if (json.get(nextKeys) instanceof JSONObject) getJsonKey(json.getJSONObject(nextKeys), key, false);
                    else if (json.get(nextKeys) instanceof JSONArray) {
                        JSONArray jsonArray = json.getJSONArray(nextKeys);

                        int x = 0;
                        if (x < jsonArray.length()) {
                            String jsonArrayString = jsonArray.get(x).toString();
                            JSONObject innerJSON = new JSONObject(jsonArrayString);

                            getJsonKey(innerJSON, key, false);
                        }
                    }
                } catch (Exception e) {
                    System.out.print(new Utils().getTime(LogColor.RED));
                    e.printStackTrace();
                }
            }
        } else {
            value = json.get(key).toString();
            return value;
        }

        return value;
    }
}
