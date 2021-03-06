package me.jasonhorkles.quorum;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Modal;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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

    public CompletableFuture<List<Message>> getMessages(MessageChannel channel, int count) {
        return channel.getIterableHistory().takeAsync(count).thenApply(ArrayList::new);
    }

    public Modal createActivityModal(int activityCount) {
        TextInput activity = TextInput.create("activity" + activityCount, "Activity", TextInputStyle.SHORT)
            .setPlaceholder("Leave blank if no activity").setMaxLength(300).setRequired(false).build();

        TextInput date;
        TextInput time;

        if (activityCount == 5) {
            date = TextInput.create("date" + activityCount, "Date", TextInputStyle.SHORT)
                .setPlaceholder("Leave blank if there is no 5th Tuesday").setRequired(false).setMaxLength(2).build();

            time = TextInput.create("time" + activityCount, "Time", TextInputStyle.SHORT)
                .setPlaceholder("Leave blank if there is no 5th Tuesday").setRequired(false).setMaxLength(5).build();

        } else {
            date = TextInput.create("date" + activityCount, "Date", TextInputStyle.SHORT)
                .setPlaceholder("The number day of the month, e.g. 17").setMaxLength(2).build();

            time = TextInput.create("time" + activityCount, "Time", TextInputStyle.SHORT).setValue("7:00")
                .setPlaceholder("The time, will always be PM").setMinLength(4).setMaxLength(5).build();
        }

        return Modal.create("create-activity" + activityCount, "Create Activity " + activityCount + "/5")
            .addActionRows(ActionRow.of(activity), ActionRow.of(date), ActionRow.of(time)).build();
    }

    /*public Modal createEditActivityModal(Message message, int line) {
        TextInput activity = TextInput.create("activity" + activityCount, "Activity", TextInputStyle.SHORT)
            .setMaxLength(250).build();

        TextInput date = TextInput.create("date" + activityCount, "Date", TextInputStyle.SHORT)
            .setPlaceholder("The number day of the month, e.g. 17").setMaxLength(2).build();

        TextInput time = TextInput.create("time" + activityCount, "Time", TextInputStyle.SHORT).setValue("7:00")
            .setPlaceholder("The time, will always be PM").setMinLength(4).setMaxLength(5).build();

        return Modal.create("create-activity1", "Create Activity " + activityCount + "/5")
            .addActionRows(ActionRow.of(activity), ActionRow.of(date), ActionRow.of(time)).build();
    }*/
}
