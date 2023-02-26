package me.jasonhorkles.thedawg;

import me.jasonhorkles.thedawg.lavaplayer.PlayerManager;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;

import java.util.Random;

public class Events extends ListenerAdapter {
    public static Guild currentVoiceChannel;

    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        if (event.getMember().getIdLong() != 840025878947692554L) return;
        if (event.getChannelJoined() == null) return;
        if (!event.getChannelJoined().getMembers()
            .contains(event.getGuild().getMemberById(277291758503723010L))) return;
        if (!isLive()) return;

        AudioManager audioManager = event.getGuild().getAudioManager();
        try {
            audioManager.openAudioConnection(event.getChannelJoined());
            audioManager.setSelfDeafened(true);
        } catch (InsufficientPermissionException ignored) {
            System.out.println(
                new Utils().getTime(Utils.LogColor.YELLOW) + "Couldn't join '" + event.getChannelJoined()
                    .getName() + "' due to insufficient permissions!");
        }

        String file = null;
        switch (new Random().nextInt(8)) {
            case 0 -> file = "TheDawg/Sounds/N1.mp3";
            case 1 -> file = "TheDawg/Sounds/SD1.mp3";
            case 2 -> file = "TheDawg/Sounds/SD2.mp3";
            case 3 -> file = "TheDawg/Sounds/SD3.mp3";
            case 4 -> file = "TheDawg/Sounds/SD4.mp3";
            case 5 -> file = "TheDawg/Sounds/SD5.mp3";
            case 6 -> file = "TheDawg/Sounds/SD6.mp3";
            case 7 -> file = "TheDawg/Sounds/SD7.mp3";
        }

        currentVoiceChannel = event.getGuild();
        PlayerManager.getInstance().getMusicManager(event.getGuild()).player.setVolume(100);
        PlayerManager.getInstance().loadAndPlay(file);
    }

    private boolean isLive() {
        //noinspection DataFlowIssue
        for (Activity activity : TheDawg.jda.getGuildById(335435349734064140L)
            .getMemberById(277291758503723010L).getActivities())
            if (activity.getName()
                .equalsIgnoreCase("Twitch") && activity.getType() == Activity.ActivityType.STREAMING)
                return true;
        return false;
    }
}
