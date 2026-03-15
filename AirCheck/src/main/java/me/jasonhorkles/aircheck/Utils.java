package me.jasonhorkles.aircheck;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

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
        RED("\u001B[31m"),
        YELLOW("\u001B[33m"),
        GREEN("\u001B[32m");

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

    @SuppressWarnings("DataFlowIssue")
    public void updateVoiceChannel(long id, String name) {
        VoiceChannel voiceChannel = AirCheck.jda.getVoiceChannelById(id);
        if (!voiceChannel.getName().equals(name)) voiceChannel.getManager().setName(name).queue();
    }

    public void logError(Exception e) {
        StringBuilder error = new StringBuilder("```accesslog\n");
        error.append(getTime(null)).append(e);
        for (StackTraceElement element : e.getStackTrace())
            if (element.toString().contains("jasonhorkles")) error.append("\n").append(element);
        error.append("```");

        //noinspection DataFlowIssue
        AirCheck.jda.getTextChannelById(1093060038265950238L).sendMessage(error).queue();
    }

    public CompletableFuture<List<Message>> getMessages(MessageChannel channel, int count) {
        return channel.getIterableHistory().takeAsync(count).thenApply(ArrayList::new);
    }

    public boolean shouldMsgPing(TextChannel channel) {
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
}
