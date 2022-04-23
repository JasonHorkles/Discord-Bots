package me.jasonhorkles.stormalerts;

import org.json.JSONObject;

public class Traffic {
    public void checkTraffic(boolean north) {
        // 1-7 = north
        // 8-14 = south
        int min;
        int max;
        if (north) {
            min = 1;
            max = 7;
        } else {
            min = 8;
            max = 14;
        }

        Thread checks = new Thread(() -> {
            boolean closed = false;
            int minSpeed = 100;

            for (int section = min; section <= max; section++) {
                JSONObject input = new Secrets().getTrafficAtCoords(section);
                if (input != null) {
                    // Check for closure
                    if (input.getBoolean("roadClosure")) {
                        closed = true;
                        System.out.println(
                            new Utils().getTime(Utils.Color.YELLOW) + "Traffic section " + section + " is closed!");
                        break;
                    }

                    // Get speed
                    int currentSpeed = input.getInt("currentSpeed");
                    if (currentSpeed < minSpeed) minSpeed = currentSpeed;
                    System.out.println(new Utils().getTime(
                        Utils.Color.GREEN) + "Traffic section " + section + " is currently " + currentSpeed + " mph");
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            if (closed) {
                if (max == 7) {
                    StormAlerts.api.openPrivateChannelById(277291758503723010L)
                        .flatMap(channel -> channel.sendMessage(
                            ":no_entry: " + new Secrets().getRoadName(true) + " is closed!"))
                        .queue();
                } else {
                    // send message S
                }
                return;
            }

            if (minSpeed <= 65 && minSpeed > 40) {

            } else if (minSpeed <= 40) {

            }
        });
        checks.start();
    }
}
