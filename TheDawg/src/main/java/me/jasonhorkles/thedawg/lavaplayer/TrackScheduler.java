package me.jasonhorkles.thedawg.lavaplayer;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import me.jasonhorkles.thedawg.Events;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.managers.AudioManager;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TrackScheduler extends AudioEventAdapter {
    public final AudioPlayer player;
    public final BlockingQueue<AudioTrack> queue;

    public TrackScheduler(AudioPlayer player) {
        this.player = player;
        this.queue = new LinkedBlockingQueue<>();
    }

    public void queue(AudioTrack track) {
        player.startTrack(track, false);
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (endReason.mayStartNext) nextTrack();
    }

    public void nextTrack() {
        AudioTrack track = queue.poll();
        if (track != null) player.startTrack(track, false);
        else {
            Guild guild = Events.currentVoiceChannel;
            GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(guild);
            musicManager.player.destroy();

            AudioManager audioManager = guild.getAudioManager();
            audioManager.closeAudioConnection();
        }
    }
}
