package me.jasonhorkles.polytrichopsida;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;

import java.time.OffsetDateTime;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class CloseStale {
    // Close stale posts
    public void closeStale() throws ExecutionException, InterruptedException, TimeoutException {
        System.out.println("\n" + new Utils().getTime(Utils.LogColor.YELLOW) + "Checking for stale posts...");

        OffsetDateTime twoWeeksAgo = OffsetDateTime.now().minusWeeks(2);

        //noinspection DataFlowIssue
        for (ThreadChannel thread : Polytrichopsida.jda.getGuildById(390942438061113344L).getChannelById(ForumChannel.class,
            1226927981977403452L).getThreadChannels()) {
            if (thread.isArchived()) continue;

            System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Checking post '" + thread.getName() + "'");

            Message latestMessage = new Utils().getMessages(thread, 1).get(30, TimeUnit.SECONDS).getFirst();
            if (latestMessage.getTimeCreated().isBefore(twoWeeksAgo)) {
                System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Closing stale post '" + thread.getName() + "'");

                String ping = new Utils().getThreadOP(thread).getAsMention();
                thread
                    .sendMessage(ping + ", this thread has been archived due to inactivity. If you still need help, send a message to re-open it.")
                    .queue(na -> thread.getManager().setArchived(true).queue());
            }
        }
    }
}
