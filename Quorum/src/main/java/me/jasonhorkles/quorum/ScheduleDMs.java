package me.jasonhorkles.quorum;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.*;

@SuppressWarnings("ConstantConditions")
public class ScheduleDMs {
    public static final ArrayList<ScheduledFuture<?>> schedules = new ArrayList<>();
    private final String firstAssistantId = "858844650580475954";

    public void scheduleDMs() throws ParseException, RuntimeException {
        File file = new File("Quorum/lessons.txt");
        if (!file.exists()) try {
            //noinspection ResultOfMethodCallIgnored
            file.createNewFile();
        } catch (IOException e) {
            System.out.print(new Utils().getTime(Utils.LogColor.RED));
            e.printStackTrace();
        }
        Scanner scanner = null;
        try {
            scanner = new Scanner(file);
        } catch (FileNotFoundException e) {
            System.out.print(new Utils().getTime(Utils.LogColor.RED));
            e.printStackTrace();
        }
        ArrayList<String> name = new ArrayList<>();
        ArrayList<String> date = new ArrayList<>();
        ArrayList<String> title = new ArrayList<>();
        ArrayList<String> scripture = new ArrayList<>();
        ArrayList<String> link = new ArrayList<>();

        while (scanner.hasNextLine()) {
            name.add(scanner.nextLine());
            date.add(scanner.nextLine());
            title.add(scanner.nextLine());
            scripture.add(scanner.nextLine());
            link.add(scanner.nextLine());
            try {
                scanner.nextLine();
            } catch (NoSuchElementException ignored) {
            }
        }

        int usersLeft = 0;
        for (String names : name) {
            int index = name.indexOf(names);

            DateFormat format = new SimpleDateFormat("M/d/yy h:mm a");
            Calendar future = Calendar.getInstance();
            Calendar futureReminder = Calendar.getInstance();
            future.setTime(format.parse(date.get(index) + " 1:00 PM"));
            futureReminder.setTime(format.parse(date.get(index) + " 7:00 PM"));
            future.add(Calendar.WEEK_OF_YEAR, -1);
            futureReminder.add(Calendar.DAY_OF_YEAR, -1);

            long delay = future.getTimeInMillis() - System.currentTimeMillis();
            if (delay > 0)
                scheduleInitial(name.get(index), date.get(index), title.get(index), scripture.get(index),
                    link.get(index), delay);

            long delayReminder = futureReminder.getTimeInMillis() - System.currentTimeMillis();
            if (delayReminder > 0) {
                usersLeft++;
                scheduleReminder(names, date.get(index), delayReminder);
            }
        }

        TextChannel messageChannel = Quorum.jda.getTextChannelById(869009573774761984L);
        if (usersLeft == 0) try {
            if (new Utils().getMessages(messageChannel, 1).get(30, TimeUnit.SECONDS).get(0).getTimeCreated()
                .isBefore(OffsetDateTime.now().minus(71, ChronoUnit.HOURS))) messageChannel.sendMessage(
                "<@" + firstAssistantId + ">, there are no more lessons scheduled for the quorum").queue();

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            System.out.print(new Utils().getTime(Utils.LogColor.RED));
            e.printStackTrace();
        }
    }

    public void scheduleInitial(String name, String date, String title, String scripture, String link, long delay) {
        schedules.add(Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            try {
                Quorum.jda.getGuildById(853775450680590387L).getMembersByEffectiveName(name, true).get(0)
                    .getUser().openPrivateChannel().flatMap(channel -> channel.sendMessage(
                            "Hey there! You have a lesson to give in Priest Quorum next Sunday: <t:" + parseDateToUnix(
                                date) + ">\n\nThe topic for the week is: **" + title + "**\n(" + scripture + ")\n\nIf you won't be able to give the lesson or have any questions, message us over in <#853775451708719125>")
                        .setActionRow(Button.link(link, "Open"))).queue();

                Quorum.jda.getTextChannelById(869009573774761984L)
                    .sendMessage("Messaged **" + name + "** to prepare his lesson for next Sunday: " + title)
                    .queue();
                System.out.println(new Utils().getTime(
                    Utils.LogColor.GREEN) + "Messaged " + name + " to prepare his lesson for next Sunday: " + title);
            } catch (NullPointerException e) {
                System.out.print(new Utils().getTime(Utils.LogColor.RED));
                e.printStackTrace();
                Quorum.jda.getTextChannelById(869009573774761984L).sendMessage(
                        ":warning: **ERROR:** Failed to message **" + name + "** to prepare their lesson for next Sunday! <@" + firstAssistantId + ">")
                    .queue();
            }
        }, delay, TimeUnit.MILLISECONDS));

        System.out.println(new Utils().getTime(
            Utils.LogColor.GREEN) + "Scheduled message to send to " + name + " in " + Math.round(
            delay / 86400000.0) + " days.");
    }

    public void scheduleReminder(String name, String date, long delay) {
        schedules.add(Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            try {
                Quorum.jda.getGuildById(853775450680590387L).getMembersByEffectiveName(name, true).get(0)
                    .getUser().openPrivateChannel().flatMap(channel -> channel.sendMessage(
                        "Don't forget to prepare your lesson for Priest Quorum this Sunday (<t:" + parseDateToUnix(
                            date) + ">) if you haven't already :arrow_up:")).queue();

                Quorum.jda.getTextChannelById(869009573774761984L)
                    .sendMessage("Reminded **" + name + "** to prepare his lesson for this Sunday!").queue();
                System.out.println(new Utils().getTime(
                    Utils.LogColor.GREEN) + "Reminded " + name + " to prepare his lesson for this Sunday!");
            } catch (NullPointerException e) {
                System.out.print(new Utils().getTime(Utils.LogColor.RED));
                e.printStackTrace();
                Quorum.jda.getTextChannelById(869009573774761984L).sendMessage(
                        ":warning: **ERROR:** Failed to remind **" + name + "** to prepare their lesson for this Sunday! <@" + firstAssistantId + ">")
                    .queue();
            }
        }, delay, TimeUnit.MILLISECONDS));

        System.out.println(new Utils().getTime(
            Utils.LogColor.GREEN) + "Scheduled reminder for " + name + " in " + Math.round(
            delay / 86400000.0) + " days.");
    }

    public long parseDateToUnix(String date) {
        DateFormat format = new SimpleDateFormat("M/d/yy h:mm a");
        Calendar future = Calendar.getInstance();
        try {
            future.setTime(format.parse(date + " 11:00 AM"));
        } catch (ParseException e) {
            System.out.print(new Utils().getTime(Utils.LogColor.RED));
            e.printStackTrace();
        }
        return future.getTimeInMillis() / 1000;
    }
}
