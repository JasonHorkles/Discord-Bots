package me.jasonhorkles.polytrichopsida;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.ThreadMember;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class Utils {
    public enum LogColor {
        RED("\u001B[31m"),
        YELLOW("\u001B[33m"),
        GREEN("\u001B[32m");

        private final String logColor;

        LogColor(String logColor) {
            this.logColor = logColor;
        }

        public String getLogColor() {
            return logColor;
        }
    }

    public String getTime(LogColor logColor) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("hh:mm:ss a", Locale.US);
        return logColor.getLogColor() + "[" + dtf.format(LocalDateTime.now()) + "] ";
    }

    public CompletableFuture<List<Message>> getMessages(MessageChannel channel, int count) {
        return channel.getIterableHistory().takeAsync(count).thenApply(ArrayList::new);
    }

    @Nullable
    public ThreadMember getThreadOP(ThreadChannel channel) {
        try {
            return channel.retrieveThreadMemberById(channel.getOwnerIdLong()).complete();
        } catch (ErrorResponseException ignored) {
            return null;
        }
    }

    // EntityClearer, ExpensiveDeaths, FileCleaner
    private final long[] supportChannels = {
        1226927981977403452L,
        1264700031819059340L,
        1264699977293107242L
    };

    public long[] getSupportIds() {
        return supportChannels;
    }
}
