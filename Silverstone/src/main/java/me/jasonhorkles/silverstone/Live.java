package me.jasonhorkles.silverstone;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.NewsChannel;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.time.OffsetDateTime;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Live {
    private static Message liveMessage;

    public void checkIfLive() {
        if (liveMessage != null) {
            if (getLiveActivity() == null) {
                liveMessage.editMessage(liveMessage.getContentRaw().replace("is now live", "was live")
                    .replace("Visit the stream", "Streamed")).queue();
                liveMessage = null;
            }
        } else {
            Activity activity = getLiveActivity();
            if (activity != null) sendLiveAlert(activity);
        }
    }

    @Nullable
    private Activity getLiveActivity() {
        Activity activity = null;
        //noinspection DataFlowIssue
        for (Activity activities : Silverstone.jda.getGuildById(455919765999976461L).getMemberById(
            277291758503723010L).getActivities())
            if (activities.getName().equalsIgnoreCase("Twitch")) activity = activities;

        if (activity == null) return null;
        if (activity.getType() != Activity.ActivityType.STREAMING) return null;
        return activity;
    }

    @SuppressWarnings("DataFlowIssue")
    private void sendLiveAlert(Activity activity) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setImage("https://static-cdn.jtvnw.net/previews-ttv/live_user_jasonhorkles-1920x1080.jpg?id=" + System.currentTimeMillis() / 1000);
        embed.setColor(new Color(43, 45, 49));

        NewsChannel channel = Silverstone.jda.getGuildById(455919765999976461L).getNewsChannelById(
            847942135402594314L);
        String ping = "\n<@&1196208254804566027>";

        try {
            if (new Utils().getMessages(channel, 1).get(30, TimeUnit.SECONDS).getFirst().getTimeCreated()
                .isAfter(OffsetDateTime.now().minusHours(1))) ping = "";

            liveMessage = channel
                .sendMessage("Jason is now live in **" + activity.getState() + "**!\nVisit the stream at [**twitch.tv/jasonhorkles**](https://twitch.tv/jasonhorkles)" + ping)
                .setSuppressedNotifications(ping.isEmpty()).complete();

            liveMessage.editMessageEmbeds(embed.build()).queueAfter(10, TimeUnit.MINUTES);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }
}
