package me.jasonhorkles.stormalerts;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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

    public CompletableFuture<List<Message>> getMessages(MessageChannel channel, int count) {
        return channel.getIterableHistory().takeAsync(count).thenApply(ArrayList::new);
    }

    public String checkIfNull(String string) {
        if (string.equalsIgnoreCase("null")) string = "ERROR";
        return string;
    }

    public boolean shouldIPing(TextChannel channel) {
        try {
            Message message = new Utils().getMessages(channel, 1).get(30, TimeUnit.SECONDS).get(0);
            if (message.isEdited()) //noinspection ConstantConditions
                return message.getTimeEdited().isBefore(OffsetDateTime.now().minus(1, ChronoUnit.HOURS));
            else return message.getTimeCreated().isBefore(OffsetDateTime.now().minus(1, ChronoUnit.HOURS));
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            System.out.print(new Utils().getTime(LogColor.RED));
            e.printStackTrace();
            return true;
        } catch (IndexOutOfBoundsException ignored) {
            return true;
        }
    }
}
