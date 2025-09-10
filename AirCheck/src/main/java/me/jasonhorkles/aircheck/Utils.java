package me.jasonhorkles.aircheck;

import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

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
}
