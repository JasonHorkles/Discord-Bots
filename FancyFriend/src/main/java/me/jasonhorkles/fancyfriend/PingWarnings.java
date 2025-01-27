package me.jasonhorkles.fancyfriend;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PingWarnings {
    public static final Map<Long, Integer> warnings = new HashMap<>();

    @SuppressWarnings("DataFlowIssue")
    public void warn(Long id, MessageReceivedEvent event, int count) {
        if (warnings.containsKey(id)) {
            warnings.put(id, warnings.get(id) + count);

            if (warnings.get(id) >= 3) event.getMember().timeoutFor(3, TimeUnit.HOURS).queue(
                // Send a message to the staff channel
                na -> FancyFriend.jda.getGuildById(FancyFriend.GUILD_ID).getTextChannelById(
                        1195445607763030126L)
                    .sendMessage("<@" + id + "> has been timed out for 3 hours for pinging staff members.")
                    .queue(),
                // Ping me if the timeout fails to apply
                na -> event.getChannel().sendMessage("<@277291758503723010>").queue());

        } else warnings.put(id, count);

        System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Warned " + event.getMember()
            .getEffectiveName() + " for pinging - " + warnings.get(id) + "/3");

        for (int x = count; x > 0; x--) {
            scheduleWarningRemoval(id, event.getMember().getEffectiveName());
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                System.out.print(new Utils().getTime(Utils.LogColor.RED));
                e.printStackTrace();
            }
        }
    }

    public void scheduleWarningRemoval(Long id, String name) {
        new Thread(
            () -> {
                try (ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor()) {
                    executor.schedule(() -> takeWarning(id, name), 15, TimeUnit.MINUTES);
                }
            }, "Warning Removal").start();
    }

    private void takeWarning(Long id, String name) {
        if (warnings.get(id) <= 1) {
            warnings.remove(id);
            System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + "Removed ping warning from " + name + " - 0/3");
        } else {
            warnings.put(id, warnings.get(id) - 1);
            System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + "Removed ping warning from " + name + " - " + warnings.get(
                id) + "/3");
        }
    }
}
