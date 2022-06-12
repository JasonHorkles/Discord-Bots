package me.jasonhorkles.quorum;

import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("ConstantConditions")
public class ScheduleAnnouncements {
    public static final ArrayList<ScheduledFuture<?>> schedules = new ArrayList<>();

    public void scheduleAnnouncements() {
        MessageEmbed message;
        try {
            message = new Utils().getMessages(Quorum.api.getTextChannelById(Events.activitiesID), 1)
                .get(30, TimeUnit.SECONDS).get(0).getEmbeds().get(0);
        } catch (Exception e) {
            System.out.println(new Utils().getTime(Utils.LogColor.RED) + "[ERROR] Couldn't get an activity message!");
            e.printStackTrace();
            return;
        }

        for (MessageEmbed.Field activities : message.getFields()) {
            String activity = activities.getValue();

            if (activity.endsWith("CANCELLED") || activity.equalsIgnoreCase("None")) continue;

            Calendar future = Calendar.getInstance();
            future.setTimeInMillis(Long.parseLong(activities.getName().replace("<t:", "").replace(":F>", "")) * 1000);
            future.add(Calendar.MINUTE, -15);

            long delay = future.getTimeInMillis() - System.currentTimeMillis();
            if (delay > 0) {
                schedules.add(Executors.newSingleThreadScheduledExecutor().schedule(
                    () -> Quorum.api.getTextChannelById(Events.announcementsID).sendMessage(
                        "<@&858784990107140118>\nReminder: **" + activity + "** starts " + activities.getName()
                            .replace(":F", ":R") + "!").queue(), delay, TimeUnit.MILLISECONDS));

                System.out.println(new Utils().getTime(
                    Utils.LogColor.GREEN) + "Scheduled an announcement for \"" + activity + "\" in " + Math.round(
                    delay / 86400000.0) + " days.");
            }
        }
    }
}
