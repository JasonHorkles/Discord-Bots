package me.jasonhorkles.stormalerts.Utils;

import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class LogUtils {
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

    public void logError(Exception e) {
        StringBuilder error = new StringBuilder("```accesslog\n");
        error.append(getTime(null)).append(e);
        for (StackTraceElement element : e.getStackTrace())
            if (element.toString().contains("jasonhorkles")) error.append("\n").append(element);
        error.append("```");

        ChannelUtils.logChannel.sendMessage(error).queue();
    }
}
