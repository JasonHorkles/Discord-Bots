package me.jasonhorkles.booper.events;

import com.github.twitch4j.events.ChannelGoLiveEvent;
import com.github.twitch4j.events.ChannelGoOfflineEvent;
import com.github.twitch4j.helix.domain.Stream;
import me.jasonhorkles.booper.Booper;
import me.jasonhorkles.booper.Utils;
import net.dv8tion.jda.api.entities.Message;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LiveTwitch {
    // twitch username, message
    public static final Map<String, Message> liveUsers = new HashMap<>();

    // Ensure the user is still live on startup and cache them
    public void checkIfLive(String username, Message message) {
        String twitchId = Booper.twitch.getClientHelper().getTwitchHelix().getUsers(
            Booper.authToken,
            null,
            Collections.singletonList(username)).execute().getUsers().getFirst().getId();

        List<Stream> stream = Booper.twitch.getClientHelper().getTwitchHelix().getStreams(
            Booper.authToken,
            null,
            null,
            1,
            null,
            null,
            Collections.singletonList(twitchId),
            null).execute().getStreams();

        if (stream.isEmpty()) {
            //debug
            System.out.println("Twitch user " + username + " is not live, not caching");
            return;
        }

        liveUsers.put(username, message);
        System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + "Cached live Twitch user: " + username);
    }

    public void channelLiveEvent(ChannelGoLiveEvent event) {
        String username = event.getChannel().getName().toLowerCase();
        //debug
        System.out.println(event.getChannel().getName() + " is live!");
        if (liveUsers.containsKey(username)) return;

        new Thread(() -> {
            Message message = new Utils().sendLiveMessage(
                username,
                "https://twitch.tv/" + username,
                event.getStream().getTitle(),
                event.getStream().getGameName(),
                false);

            liveUsers.put(username, message);
        }).start();
    }

    public void channelOfflineEvent(ChannelGoOfflineEvent event) {
        //debug
        System.out.println(event.getChannel().getName() + " is no longer live");
        String username = event.getChannel().getName().toLowerCase();
        liveUsers.get(username).delete().queue();
        liveUsers.remove(username);
    }
}
