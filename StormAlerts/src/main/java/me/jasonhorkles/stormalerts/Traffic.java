package me.jasonhorkles.stormalerts;

import org.json.JSONObject;

public class Traffic {
    public void checkTraffic(boolean north) {
        int min = 1;
        int max = 7;

        Thread checks = new Thread(() -> {
            try {
                for (int section = min; section <= max; section++) {
                    JSONObject input = new Secrets().getTrafficAtCoords(section).getJSONObject("flowSegmentData");
                    if (input != null) {
                        // Check for closure
                        if (input.getBoolean("roadClosure")) {
                            System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + new Secrets().getRoadName(
                                north) + " Traffic section " + section + "/" + max + " is closed!");
                            StormAlerts.jda.openPrivateChannelById(277291758503723010L).flatMap(
                                channel -> channel.sendMessage(
                                    "**" + new Secrets().getRoadName(north) + "** is closed! :no_entry:")).queue();
                            continue;
                        }

                        // Get speed
                        int currentSpeed = input.getInt("currentSpeed");

                        System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + new Secrets().getRoadName(
                            north) + " Traffic section " + section + "/" + max + " is currently " + currentSpeed + " mph");

                        int finalSection = section;
                        if (currentSpeed <= 55 && currentSpeed >= 40)
                            StormAlerts.jda.openPrivateChannelById(277291758503723010L).flatMap(
                                    channel -> channel.sendMessage("**" + new Secrets().getRoadName(
                                        north) + " section " + finalSection + "/" + max + "** has a slowdown @ **" + currentSpeed + " mph**! :yellow_circle:"))
                                .queue();

                        else if (currentSpeed < 40) StormAlerts.jda.openPrivateChannelById(277291758503723010L).flatMap(
                                channel -> channel.sendMessage("**" + new Secrets().getRoadName(
                                    north) + " section " + finalSection + "/" + max + "** has a slowdown @ **" + currentSpeed + " mph**! :red_circle:"))
                            .queue();
                    }

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        System.out.print(new Utils().getTime(Utils.LogColor.RED));
                        e.printStackTrace();
                    }
                }

            } catch (Exception e) {
                System.out.print(new Utils().getTime(Utils.LogColor.RED));
                e.printStackTrace();

                StormAlerts.jda.openPrivateChannelById(277291758503723010L)
                    .flatMap(channel -> channel.sendMessage("**Failed to check traffic!** :warning:")).queue();
            }
        }, "Traffic Check");
        checks.start();
    }
}
