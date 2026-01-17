package me.jasonhorkles.booper.events;

import me.jasonhorkles.booper.Utils;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.user.update.UserUpdateActivitiesEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class LiveDiscord extends ListenerAdapter {
    public static final Map<Member, Message> liveMembers = new HashMap<>();
    public static boolean teddyIsLive;

    @Override
    public void onUserUpdateActivities(@NotNull UserUpdateActivitiesEvent event) {
        if (event.getGuild().getIdLong() != 1299547538445307986L) return;
        if (event.getUser().getIdLong() == 314933806173913089L) return;
        checkIfLive(event.getMember());
    }

    public void checkIfLive(Member member) {
        Activity activity = getLiveActivity(member);

        // Member was live
        if (liveMembers.containsKey(member)) {
            if (activity != null) return;

            if (member.getUser().getIdLong() == 291471770140147712L) teddyIsLive = false;
            liveMembers.get(member).delete().queue();
            liveMembers.remove(member);
        }

        // Member is now live
        else if (activity != null) new Thread(() -> {
            // Stop sending live messages while Teddy is live
            if (teddyIsLive) return;
            if (member.getUser().getIdLong() == 291471770140147712L) teddyIsLive = true;

            Message message = new Utils().sendLiveMessage(
                member.getId(),
                Objects.requireNonNullElse(activity.getUrl(), "https://twitch.tv/thischanneldoesnotexist"),
                null,
                activity.getState(),
                true);
            liveMembers.put(member, message);
        }).start();
    }

    @Nullable
    private Activity getLiveActivity(Member member) {
        Activity activity = null;
        for (Activity activities : member.getActivities())
            if (activities.getName().equalsIgnoreCase("Twitch")) {
                activity = activities;
                break;
            }

        if (activity == null) return null;
        if (activity.getType() != Activity.ActivityType.STREAMING) return null;
        return activity;
    }
}
