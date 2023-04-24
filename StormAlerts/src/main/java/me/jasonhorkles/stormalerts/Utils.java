package me.jasonhorkles.stormalerts;

import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.Nullable;
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

    public String getTime(@Nullable LogColor logColor) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("hh:mm:ss a");
        String time = "[" + dtf.format(LocalDateTime.now()) + "] ";

        if (logColor == null) return time;
        return logColor.getLogColor() + time;
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
                    if (json.get(nextKeys) instanceof JSONObject)
                        getJsonKey(json.getJSONObject(nextKeys), key, false);
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
                    new Utils().logError(e);
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

    public boolean shouldIPing(TextChannel channel) {
        try {
            Message message = new Utils().getMessages(channel, 1).get(30, TimeUnit.SECONDS).get(0);

            if (message.isEdited()) //noinspection DataFlowIssue
                return message.getTimeEdited().isBefore(OffsetDateTime.now().minus(2, ChronoUnit.HOURS));
            else return message.getTimeCreated().isBefore(OffsetDateTime.now().minus(2, ChronoUnit.HOURS));

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            System.out.print(new Utils().getTime(LogColor.RED));
            e.printStackTrace();
            new Utils().logError(e);
            return true;

        } catch (IndexOutOfBoundsException ignored) {
            return true;
        }
    }

    public boolean shouldIBeSilent(TextChannel channel) {
        // Set whether or not the message should be silent (e.g. right after a restart)
        try {
            Message message = new Utils().getMessages(channel, 1).get(30, TimeUnit.SECONDS).get(0);

            // If edited or sent within the last 5 minutes, send silently
            if (message.isEdited()) //noinspection DataFlowIssue
                return message.getTimeEdited().isAfter(OffsetDateTime.now().minus(5, ChronoUnit.MINUTES));
            else return message.getTimeCreated().isAfter(OffsetDateTime.now().minus(5, ChronoUnit.MINUTES));
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            System.out.print(new Utils().getTime(LogColor.RED));
            e.printStackTrace();
            new Utils().logError(e);
            return false;

        } catch (IndexOutOfBoundsException ignored) {
            return false;
        }
    }

    public void updateNow(@Nullable SlashCommandInteractionEvent event) {
        String error = "Done!";
        boolean isSlash = event != null;

        if (isSlash) event.deferReply(true).complete();

        // Alerts
        System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Force checking alerts...");
        if (isSlash) event.getHook().editOriginal("Checking alerts...").complete();
        try {
            new Alerts().checkAlerts();
        } catch (Exception e) {
            System.out.println(new Utils().getTime(Utils.LogColor.RED) + "[ERROR] Couldn't get the alerts!");
            e.printStackTrace();
            new Utils().logError(e);
            error = "Couldn't get the alerts!";
        }

        // PWS / Rain
        System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Force checking PWS conditions...");
        if (isSlash) event.getHook().editOriginal("Checking PWS conditions...").complete();
        try {
            new Pws().checkConditions();
        } catch (Exception e) {
            System.out.println(
                new Utils().getTime(Utils.LogColor.RED) + "[ERROR] Couldn't get the PWS conditions!");
            e.printStackTrace();
            new Utils().logError(e);
            error = "Couldn't get the PWS conditions!";
        }

        // Weather
        System.out.println(
            new Utils().getTime(Utils.LogColor.YELLOW) + "Force checking weather conditions...");
        if (isSlash) event.getHook().editOriginal("Checking weather conditions...").complete();
        try {
            new Weather().checkConditions();
        } catch (Exception e) {
            System.out.println(
                new Utils().getTime(Utils.LogColor.RED) + "[ERROR] Couldn't get the weather conditions!");
            e.printStackTrace();
            new Utils().logError(e);
            error = "Couldn't get the weather conditions!";
            StormAlerts.jda.getPresence().setStatus(OnlineStatus.DO_NOT_DISTURB);
            StormAlerts.jda.getPresence().setActivity(Activity.playing("Error checking weather!"));
        }

        if (isSlash) event.getHook().editOriginal(error).complete();
    }

    public void logError(Exception e) {
        StringBuilder error = new StringBuilder("```accesslog\n");
        error.append(getTime(null)).append(e);
        for (StackTraceElement element : e.getStackTrace())
            if (element.toString().contains("jasonhorkles")) error.append("\n").append(element);
        error.append("```");

        //noinspection DataFlowIssue
        StormAlerts.jda.getTextChannelById(1093060038265950238L).sendMessage(error).queue();
    }
}
