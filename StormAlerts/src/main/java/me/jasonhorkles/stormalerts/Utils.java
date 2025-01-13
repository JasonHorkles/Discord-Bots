package me.jasonhorkles.stormalerts;

import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
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
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("hh:mm:ss a", Locale.US);
        String time = "[" + dtf.format(LocalDateTime.now()) + "] ";

        if (logColor == null) return time;
        return logColor.getLogColor() + time;
    }

    public CompletableFuture<List<Message>> getMessages(MessageChannel channel, int count) {
        return channel.getIterableHistory().takeAsync(count).thenApply(ArrayList::new);
    }

    public boolean shouldIPing(TextChannel channel) {
        if (StormAlerts.testing) return false;

        try {
            Message message = getMessages(channel, 1).get(30, TimeUnit.SECONDS).getFirst();

            if (message.isEdited()) //noinspection DataFlowIssue
                return message.getTimeEdited().isBefore(OffsetDateTime.now().minusHours(12));
            else return message.getTimeCreated().isBefore(OffsetDateTime.now().minusHours(12));

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            System.out.print(getTime(LogColor.RED));
            e.printStackTrace();
            logError(e);
            return true;

        } catch (NoSuchElementException ignored) {
            return true;
        }
    }

    public boolean shouldIBeSilent(TextChannel channel) {
        if (StormAlerts.testing) return true;

        // Set whether or not the message should be silent (e.g. right after a restart)
        try {
            Message message = getMessages(channel, 1).get(30, TimeUnit.SECONDS).getFirst();

            // If edited/sent within the last hour, send silently
            if (message.isEdited()) //noinspection DataFlowIssue
                return message.getTimeEdited().isAfter(OffsetDateTime.now().minusHours(1));
            else return message.getTimeCreated().isAfter(OffsetDateTime.now().minusHours(1));

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            System.out.print(getTime(LogColor.RED));
            e.printStackTrace();
            logError(e);
            return false;

        } catch (NoSuchElementException ignored) {
            return false;
        }
    }

    @SuppressWarnings("DataFlowIssue")
    public void updateVoiceChannel(long id, String name) {
        VoiceChannel voiceChannel = StormAlerts.jda.getVoiceChannelById(id);
        if (!voiceChannel.getName().equals(name)) voiceChannel.getManager().setName(name).queue();
    }

    public void updateNow(SlashCommandInteractionEvent event) {
        String error = "Done!";

        event.deferReply(true).complete();

        // Alerts
        System.out.println(getTime(Utils.LogColor.YELLOW) + "Force checking alerts...");
        event.getHook().editOriginal("Checking alerts...").complete();
        try {
            new Alerts().checkAlerts();
        } catch (Exception e) {
            System.out.println(getTime(Utils.LogColor.RED) + "[ERROR] Couldn't get the alerts!");
            e.printStackTrace();
            logError(e);
            error = "Couldn't get the alerts!";
        }

        // Weather
        System.out.println(getTime(Utils.LogColor.YELLOW) + "Force checking weather conditions...");
        event.getHook().editOriginal("Checking weather conditions...").complete();
        try {
            new Weather().checkConditions();
        } catch (Exception e) {
            System.out.println(getTime(Utils.LogColor.RED) + "[ERROR] Couldn't get the weather conditions!");
            e.printStackTrace();
            logError(e);
            error = "Couldn't get the weather conditions!";
            StormAlerts.jda.getPresence().setStatus(OnlineStatus.DO_NOT_DISTURB);
            StormAlerts.jda.getPresence().setActivity(Activity.playing("Error checking weather!"));
        }

        event.getHook().editOriginal(error).complete();
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
