package me.jasonhorkles.booper.events;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class Messages extends ListenerAdapter {
    // Roll call
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.isFromGuild()) return;
        if (event.getGuildChannel().getIdLong() != 1365398168937824256L) return;
        //        if (event.getAuthor().getIdLong() != 291471770140147712L) return;

        Message message = event.getMessage();
        String strippedMsg = message.getContentStripped().toLowerCase();
        if (strippedMsg.contains("role call") || strippedMsg.contains("roll call"))
            message.addReaction(Emoji.fromUnicode("👍")).queue(_ -> message.addReaction(Emoji.fromUnicode("👎"))
                .queue());
    }
}
