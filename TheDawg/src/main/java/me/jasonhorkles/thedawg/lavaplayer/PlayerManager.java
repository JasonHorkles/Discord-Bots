package me.jasonhorkles.thedawg.lavaplayer;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.jasonhorkles.thedawg.Events;
import me.jasonhorkles.thedawg.Utils;
import net.dv8tion.jda.api.entities.Guild;

import java.util.HashMap;
import java.util.Map;

public class PlayerManager {
    private final Map<Long, GuildMusicManager> musicManagers;
    private final AudioPlayerManager audioPlayerManager;

    public PlayerManager() {
        musicManagers = new HashMap<>();
        audioPlayerManager = new DefaultAudioPlayerManager();

        AudioSourceManagers.registerLocalSource(audioPlayerManager);
    }

    public GuildMusicManager getMusicManager(Guild guild) {
        return musicManagers.computeIfAbsent(guild.getIdLong(), (guildId) -> {
            GuildMusicManager guildMusicManager = new GuildMusicManager(audioPlayerManager);

            guild.getAudioManager().setSendingHandler(guildMusicManager.getSendHandler());

            return guildMusicManager;
        });
    }

    public void loadAndPlay(String file) {
        GuildMusicManager musicManager = getMusicManager(Events.currentVoiceChannel);

        audioPlayerManager.loadItemOrdered(musicManager, file, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                musicManager.scheduler.queue(track);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                System.out.println(new Utils().getTime(Utils.LogColor.RED) + "[ERROR] GOT A PLAYLIST");
            }

            @Override
            public void noMatches() {
                System.out.println(new Utils().getTime(Utils.LogColor.RED) + "[ERROR] NO MATCHES FOUND FOR \"" + file + "\"");
            }

            @Override
            public void loadFailed(FriendlyException e) {
                System.out.println(new Utils().getTime(Utils.LogColor.RED) + "[ERROR] FAILED TO LOAD");
                e.printStackTrace();
            }
        });
    }

    private static final class InstanceHolder {
        private static final PlayerManager instance = new PlayerManager();
    }

    public static PlayerManager getInstance() {
        return InstanceHolder.instance;
    }
}
