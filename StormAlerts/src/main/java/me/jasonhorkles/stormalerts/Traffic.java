package me.jasonhorkles.stormalerts;

import me.jasonhorkles.stormalerts.Utils.LogUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Traffic {
    public void checkTraffic(boolean north) {
        LogUtils logUtils = new LogUtils();
        try {
            Secrets secrets = new Secrets();
            JSONArray input;
            if (StormAlerts.testing) input = new JSONObject(Files.readString(Path.of(
                "StormAlerts/Tests/traffic.json"))).getJSONArray("results");
            else {
                InputStream url = new URI("https://data.traffic.hereapi.com/v7/flow?in=circle:" + secrets.trafficCoords() + ";r=10&locationReferencing=none&apiKey=" + secrets.trafficApiKey())
                    .toURL().openStream();
                JSONObject obj = new JSONObject(new String(url.readAllBytes(), StandardCharsets.UTF_8));
                url.close();

                input = obj.getJSONArray("results");
            }

            // Find the correct direction
            JSONObject traffic = new JSONObject();
            String direction = secrets.apiRoadName(north);
            for (int i = 0; i < input.length(); i++) {
                JSONObject obj = input.getJSONObject(i);
                if (obj.getJSONObject("location").getString("description").equals(direction)) {
                    traffic = obj;
                    break;
                }
            }

            // Get speed and jam factor
            int currentSpeed = Math.toIntExact(Math.round(traffic.getJSONObject("currentFlow")
                .getDouble("speedUncapped") * 2.23694));
            double jamFactor = traffic.getJSONObject("currentFlow").getDouble("jamFactor");

            System.out.println(logUtils.getTime(LogUtils.LogColor.GREEN) + secrets.roadName(north) + " is currently ~" + currentSpeed + " mph with a jam factor of " + jamFactor);

            if (currentSpeed <= 55 && currentSpeed >= 40) StormAlerts.jda.openPrivateChannelById(
                    277291758503723010L)
                .flatMap(channel -> channel.sendMessage("**" + secrets.roadName(north) + "** has a slowdown @ **" + currentSpeed + " mph**!\nJam factor: **" + jamFactor + "/10.0** :yellow_circle:"))
                .queue();

            else if (currentSpeed < 40) StormAlerts.jda.openPrivateChannelById(277291758503723010L).flatMap(
                    channel -> channel.sendMessage("**" + secrets.roadName(north) + "** has a slowdown @ **" + currentSpeed + " mph**!\nJam factor: **" + jamFactor + "/10.0** :red_circle:"))
                .queue();

        } catch (Exception e) {
            System.out.print(logUtils.getTime(LogUtils.LogColor.RED));
            e.printStackTrace();
            logUtils.logError(e);

            StormAlerts.jda.openPrivateChannelById(277291758503723010L)
                .flatMap(channel -> channel.sendMessage("**Failed to check traffic!** :warning:")).queue();
        }
    }

    public void scheduleTrafficCheck(String time, boolean toWork) throws ParseException {
        DayOfWeek day = LocalDate.now().getDayOfWeek();
        if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
            DateFormat format = new SimpleDateFormat("yyyy-MM-dd h:mm a", Locale.US);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(format.parse(LocalDate.now() + " " + time));

            long delay = calendar.getTimeInMillis() - System.currentTimeMillis();

            if (delay >= 0) new Thread(
                () -> {
                    StormAlerts.scheduledTimers.add(Executors.newSingleThreadScheduledExecutor()
                        .schedule(() -> new Traffic().checkTraffic(toWork), delay, TimeUnit.MILLISECONDS));
                    System.out.println(new LogUtils().getTime(LogUtils.LogColor.GREEN) + "Scheduled traffic check in " + Math.round(
                        delay / 3600000.0) + " hours.");
                }, "Traffic Check").start();
        }
    }
}
