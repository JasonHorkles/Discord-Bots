package me.jasonhorkles.musicdaddy;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Utils {
    public enum Color {
        RED("\u001B[31m"), YELLOW("\u001B[33m"), GREEN("\u001B[32m");

        private final String color;

        Color(String color) {
            this.color = color;
        }

        public String getColor() {
            return color;
        }
    }

    public String getTime(Color color) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("hh:mm:ss a");
        return color.getColor() + "[" + dtf.format(LocalDateTime.now()) + "] ";
    }
}
