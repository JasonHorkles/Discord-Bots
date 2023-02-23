package me.jasonhorkles.quorum;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
        TextInput activity = TextInput.create("activity", "Activity", TextInputStyle.SHORT)
            .setPlaceholder("Leave blank if no activity").setMaxLength(300).setRequired(false).build();

        TextInput date;
        TextInput time;

        if (activityCount == 5) {
            date = TextInput.create("date", "Date", TextInputStyle.SHORT)
                .setPlaceholder("Leave blank if there is no 5th Tuesday").setRequired(false).setMaxLength(2)
                .build();

            time = TextInput.create("time", "Time", TextInputStyle.SHORT)
                .setPlaceholder("Leave blank if there is no 5th Tuesday").setRequired(false).setMaxLength(5)
                .build();

        } else {
            date = TextInput.create("date", "Date", TextInputStyle.SHORT)
                .setPlaceholder("The number day of the month, e.g. 17").setMaxLength(2).build();

            time = TextInput.create("time", "Time", TextInputStyle.SHORT).setValue("7:00 PM")
                .setPlaceholder("The time, defaults to PM if not specified").setMinLength(4).setMaxLength(8).build();
        }

        return Modal.create("create-activity" + activityCount, "Create Activity " + activityCount + "/5")
            .addActionRow(activity).addActionRow(date).addActionRow(time).build();
    }

    public Modal createEditActivityModal(Message message, int index) {
        try {
            MessageEmbed embed = message.getEmbeds().get(0);
            TextInput activity = TextInput.create("activity", "Activity", TextInputStyle.SHORT)
                .setPlaceholder("Leave blank if no activity")
                .setValue(embed.getFields().get(index).getValue()).setMaxLength(300).setRequired(false)
                .build();

            //noinspection ConstantConditions
            ZonedDateTime dateTime = Instant.ofEpochSecond(
                    Long.parseLong(embed.getFields().get(index).getName().replace("<t:", "").replace(":F>", "")))
                .atZone(ZoneId.of("America/Denver"));

            TextInput date = TextInput.create("date", "Date", TextInputStyle.SHORT)
                .setPlaceholder("The number day of the month, e.g. 17")
                .setValue(String.valueOf(dateTime.getDayOfMonth())).setMaxLength(2).build();

            TextInput time = TextInput.create("time", "Time", TextInputStyle.SHORT)
                .setPlaceholder("The time, defaults to PM if not specified")
                .setValue(dateTime.format(DateTimeFormatter.ofPattern("h:mm a"))).setMinLength(4)
                .setMaxLength(8).build();

            return Modal.create("edit-activity:" + message.getId() + ":" + index,
                    "Edit " + embed.getTitle() + " Activity " + (index + 1)).addActionRow(activity)
                .addActionRow(date).addActionRow(time).build();

        } catch (IndexOutOfBoundsException ignored) {
            return null;
        }
    }
}
