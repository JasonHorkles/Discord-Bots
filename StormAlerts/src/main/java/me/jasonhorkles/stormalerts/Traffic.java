package me.jasonhorkles.stormalerts;

import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Traffic {
    public void checkTraffic(boolean north) {
        int min = 1;
        int max = 7;

        Thread checks = new Thread(() -> {
            try {
                for (int section = min; section <= max; section++) {
                    JSONObject input = new Secrets().getTrafficAtCoords(section)
                        .getJSONObject("flowSegmentData");
                    if (input != null) {
                        // Check for closure
                        if (input.getBoolean("roadClosure")) {
                            System.out.println(
                                new Utils().getTime(Utils.LogColor.YELLOW) + new Secrets().getRoadName(
                                    north) + " Traffic section " + section + "/" + max + " is closed!");
                            StormAlerts.jda.openPrivateChannelById(277291758503723010L).flatMap(
                                    channel -> channel.sendMessage(
                                        "**" + new Secrets().getRoadName(north) + "** is closed! :no_entry:"))
                                .queue();
                            continue;
                        }

                        // Get speed
                        int currentSpeed = input.getInt("currentSpeed");

                        System.out.println(
                            new Utils().getTime(Utils.LogColor.GREEN) + new Secrets().getRoadName(
                                north) + " Traffic section " + section + "/" + max + " is currently " + currentSpeed + " mph");

                        int finalSection = section;
                        if (currentSpeed <= 55 && currentSpeed >= 40)
                            StormAlerts.jda.openPrivateChannelById(277291758503723010L).flatMap(
                                    channel -> channel.sendMessage("**" + new Secrets().getRoadName(
                                        north) + " section " + finalSection + "/" + max + "** has a slowdown @ **" + currentSpeed + " mph**! :yellow_circle:"))
                                .queue();

                        else if (currentSpeed < 40)
                            StormAlerts.jda.openPrivateChannelById(277291758503723010L).flatMap(
                                    channel -> channel.sendMessage("**" + new Secrets().getRoadName(
                                        north) + " section " + finalSection + "/" + max + "** has a slowdown @ **" + currentSpeed + " mph**! :red_circle:"))
                                .queue();
                    }

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        System.out.print(new Utils().getTime(Utils.LogColor.RED));
                        e.printStackTrace();
                        new Utils().logError(e);
                    }
                }

            } catch (Exception e) {
                System.out.print(new Utils().getTime(Utils.LogColor.RED));
                e.printStackTrace();
                new Utils().logError(e);

                StormAlerts.jda.openPrivateChannelById(277291758503723010L)
                    .flatMap(channel -> channel.sendMessage("**Failed to check traffic!** :warning:"))
                    .queue();
            }
        }, "Traffic Check");
        checks.start();
    }

    public void scheduleTrafficCheck(String time, boolean toWork) throws ParseException {
        DayOfWeek day = LocalDate.now().getDayOfWeek();
        if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
            DateFormat format = new SimpleDateFormat("yyyy-MM-dd h:mm a");
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(format.parse(LocalDate.now() + " " + time));

            long delay = calendar.getTimeInMillis() - System.currentTimeMillis();

            if (delay >= 0) {
                StormAlerts.scheduledTimers.add(Executors.newSingleThreadScheduledExecutor()
                    .schedule(() -> new Traffic().checkTraffic(toWork), delay, TimeUnit.MILLISECONDS));
                System.out.println(
                    new Utils().getTime(Utils.LogColor.GREEN) + "Scheduled traffic check in " + Math.round(
                        delay / 3600000.0) + " hours.");
            }
        }
    }
}
