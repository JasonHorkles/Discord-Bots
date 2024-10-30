package me.jasonhorkles.fancyfriend;

import net.dv8tion.jda.api.entities.Member;

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

    public String getTime(LogColor logColor) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("hh:mm:ss a", Locale.US);
        return logColor.getLogColor() + "[" + dtf.format(LocalDateTime.now()) + "] ";
    }

    public boolean isStaff(Member member) {
        // Moderator | Developer | Helpful | Contributor
        String roles = member.getRoles().toString();
        return roles.contains("1134906027142299749") || roles.contains("1092512242127339610") || roles.contains(
            "1198213765125128302") || roles.contains("1092512359886622881");
    }
}
