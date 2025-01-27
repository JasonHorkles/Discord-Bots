package me.jasonhorkles.stormalerts.Utils;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MessageUtils {
    public CompletableFuture<List<Message>> getMessages(MessageChannel channel, int count) {
        return channel.getIterableHistory().takeAsync(count).thenApply(ArrayList::new);
    }

    public boolean shouldIPing(TextChannel channel) {
        try {
            Message message = getMessages(channel, 1).get(30, TimeUnit.SECONDS).getFirst();

            if (message.isEdited()) //noinspection DataFlowIssue
                return message.getTimeEdited().isBefore(OffsetDateTime.now().minusHours(12));
            else return message.getTimeCreated().isBefore(OffsetDateTime.now().minusHours(12));

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LogUtils logUtils = new LogUtils();

            System.out.print(logUtils.getTime(LogUtils.LogColor.RED));
            e.printStackTrace();
            logUtils.logError(e);
            return true;

        } catch (NoSuchElementException ignored) {
            return true;
        }
    }

    public boolean shouldIBeSilent(TextChannel channel) {
        // Set whether or not the message should be silent (e.g. right after a restart)
        try {
            Message message = getMessages(channel, 1).get(30, TimeUnit.SECONDS).getFirst();

            // If edited/sent within the last hour, send silently
            if (message.isEdited()) //noinspection DataFlowIssue
                return message.getTimeEdited().isAfter(OffsetDateTime.now().minusHours(1));
            else return message.getTimeCreated().isAfter(OffsetDateTime.now().minusHours(1));

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LogUtils logUtils = new LogUtils();

            System.out.print(logUtils.getTime(LogUtils.LogColor.RED));
            e.printStackTrace();
            logUtils.logError(e);
            return false;

        } catch (NoSuchElementException ignored) {
            return false;
        }
    }
}
