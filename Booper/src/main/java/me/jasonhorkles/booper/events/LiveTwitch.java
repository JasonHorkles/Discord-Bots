package me.jasonhorkles.booper.events;

import net.dv8tion.jda.api.entities.Message;

import java.util.HashMap;
import java.util.Map;

public class LiveTwitch {
    // twitch username, message
    public static final Map<String, Message> liveMembers = new HashMap<>();

    //todo create with Twitch API
    /*@Override
    public void onUserUpdateActivities(@NotNull UserUpdateActivitiesEvent event) {
        if (event.getGuild().getIdLong() != 1299547538445307986L) return;

        System.out.println("Activity update for " + event.getMember().getEffectiveName() + ": " + event
            .getMember().getActivities());
        checkIfLive(event.getMember());
    }

    public void checkIfLive(Member member) {
        Activity activity = getLiveActivity(member);

        // Member was live
        if (liveMembers.containsKey(member)) {
            if (activity != null) return;

            liveMembers.get(member).delete().queue();
            liveMembers.remove(member);
        }

        // Member is now live
        else if (activity != null) new Thread(() -> {
            Message message = new Utils().sendLiveMessage(
                member.getId(),
                Objects.requireNonNullElse(activity.getUrl(), "https://twitch.tv/thischanneldoesnotexist"),
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
    }*/
}
