package me.jasonhorkles.musicdaddy;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.jasonhorkles.musicdaddy.lavaplayer.GuildMusicManager;
import me.jasonhorkles.musicdaddy.lavaplayer.PlayerManager;
import net.dv8tion.jda.api.entities.Guild;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Utils {
    public enum LogColor {
        RED("\u001B[31m"), YELLOW("\u001B[33m"), GREEN("\u001B[32m");

        private final String logColor;

        LogColor(String logColor) {
            this.logColor = logColor;
        }

        public String getLogColor() {
            return logColor;
        }
    }

    public String getTime(LogColor logColor) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("hh:mm:ss a");
        return logColor.getLogColor() + "[" + dtf.format(LocalDateTime.now()) + "] ";
    }

    public void playFile(Guild guild, String file) {
        GuildMusicManager musicManager = getMusicManager(guild);

        PlayerManager.audioPlayerManager.loadItemOrdered(musicManager, file, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                musicManager.scheduler.queue.clear();
                musicManager.scheduler.queue(track);
                if (musicManager.scheduler.player.getPlayingTrack() != null) musicManager.scheduler.nextTrack();
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                System.out.println(new Utils().getTime(Utils.LogColor.RED) + "Playlist");
            }

            @Override
            public void noMatches() {
                System.out.println(new Utils().getTime(Utils.LogColor.RED) + "No file matches");
            }

            @Override
            public void loadFailed(FriendlyException e) {
                System.out.print(new Utils().getTime(Utils.LogColor.RED));
                e.printStackTrace();
            }
        });
    }

    private GuildMusicManager getMusicManager(Guild guild) {
        return PlayerManager.musicManagers.computeIfAbsent(guild.getIdLong(), (guildId) -> {
            GuildMusicManager guildMusicManager = new GuildMusicManager(PlayerManager.audioPlayerManager);
            guild.getAudioManager().setSendingHandler(guildMusicManager.getSendHandler());
            return guildMusicManager;
        });
    }
}
