package me.jasonhorkles.quorum;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.*;

@SuppressWarnings("ConstantConditions")
public class ScheduleAnnouncements {
    public static final ArrayList<ScheduledFuture<?>> schedules = new ArrayList<>();

    public void scheduleAnnouncements() throws ExecutionException, InterruptedException, TimeoutException {
        for (Message message : new Utils().getMessages(Quorum.jda.getTextChannelById(Events.activitiesID), 10)
            .get(30, TimeUnit.SECONDS)) {
            // Check if it has embeds
            if (message.getEmbeds().isEmpty()) continue;

            MessageEmbed embed = message.getEmbeds().get(0);

            // Check if it's for this month
            if (!embed.getTitle().equalsIgnoreCase(LocalDate.now().getMonth().toString())) continue;

            for (MessageEmbed.Field activities : embed.getFields()) {
                String activity = activities.getValue();

                if (activity.endsWith("CANCELLED") || activity.equalsIgnoreCase("None")) continue;

                Calendar future = Calendar.getInstance();
                future.setTimeInMillis(
                    Long.parseLong(activities.getName().replace("<t:", "").replace(":F>", "")) * 1000);
                future.add(Calendar.MINUTE, -15);

                long delay = future.getTimeInMillis() - System.currentTimeMillis();
                if (delay > 0) {
                    schedules.add(Executors.newSingleThreadScheduledExecutor().schedule(
                        () -> Quorum.jda.getTextChannelById(Events.announcementsID).sendMessage(
                            "<@&858784990107140118>\nReminder: **" + activity + "** starts " + activities.getName()
                                .replace(":F", ":R") + "!").queue(), delay, TimeUnit.MILLISECONDS));

                    System.out.println(new Utils().getTime(
                        Utils.LogColor.GREEN) + "Scheduled an announcement for \"" + activity + "\" in " + Math.round(
                        delay / 86400000.0) + " days.");
                }
            }
        }
    }
}
