package me.jasonhorkles.musicdaddy.lavaplayer;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.jasonhorkles.musicdaddy.Utils;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.managers.AudioManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("ConstantConditions")
public class PlayerManager {
    private static PlayerManager instance;
    public static final Map<Long, GuildMusicManager> musicManagers = new HashMap<>();
    public static final AudioPlayerManager audioPlayerManager = new DefaultAudioPlayerManager();

    public PlayerManager() {
        AudioSourceManagers.registerRemoteSources(audioPlayerManager);
    }

    public GuildMusicManager getMusicManager(Guild guild) {
        return musicManagers.computeIfAbsent(guild.getIdLong(), (guildId) -> {
            final GuildMusicManager guildMusicManager = new GuildMusicManager(audioPlayerManager);

            guild.getAudioManager().setSendingHandler(guildMusicManager.getSendHandler());

            return guildMusicManager;
        });
    }

    public void loadAndPlay(SlashCommandInteractionEvent event, String search, boolean isSpotifyList) {
        final GuildMusicManager musicManager = getMusicManager(event.getGuild());

        audioPlayerManager.loadItemOrdered(musicManager, search, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                joinVC(event.getGuild(), event.getMember().getVoiceState().getChannel());

                musicManager.scheduler.queue(track);

                event.getHook().editOriginal("Successfully added " + track.getInfo().uri + " to the queue!").queue();
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                if (playlist.isSearchResult()) {
                    int x = 0;
                    if (event.getOption("result") != null) {
                        x = (int) event.getOption("result").getAsLong() - 1;

                        if (x + 1 > playlist.getTracks().size() || x + 1 < 1) {
                            event.getHook()
                                .editOriginal("Invalid result number! Range: `1-" + playlist.getTracks().size() + "`")
                                .queue();
                            return;
                        }
                    }

                    joinVC(event.getGuild(), event.getMember().getVoiceState().getChannel());

                    musicManager.scheduler.queue(playlist.getTracks().get(x));

                    if (!isSpotifyList) event.getHook().editOriginal(
                        "Successfully added " + playlist.getTracks().get(x).getInfo().uri + " to the queue!").queue();
                    return;
                }

                joinVC(event.getGuild(), event.getMember().getVoiceState().getChannel());

                List<AudioTrack> tracks = playlist.getTracks();
                boolean shuffle = false;
                if (event.getOption("shuffle") != null) shuffle = event.getOption("shuffle").getAsBoolean();
                if (shuffle) Collections.shuffle(tracks);

                long maxSongs = 25;
                if (event.getOption("maxsongs") != null) maxSongs = event.getOption("maxsongs").getAsLong();
                if (maxSongs > 100) maxSongs = 100;
                if (maxSongs == 0) maxSongs = 1;

                int x = 1;
                int additional = 0;
                StringBuilder msg = new StringBuilder();
                for (AudioTrack track : tracks) {
                    if (x > maxSongs) break;
                    musicManager.scheduler.queue(track);

                    if (msg.length() < 1800)
                        msg.append("\nSuccessfully added **").append(track.getInfo().title).append("** to the queue!");
                    else additional++;
                    x++;
                }

                if (additional > 0) msg.append("\n\n...and ").append(additional).append(" more...");
                event.getHook().editOriginal(msg.toString()).queue();
            }

            @Override
            public void noMatches() {
                event.getHook().editOriginal("Could not find a video with the link `" + event.getOption("url")
                    .getAsString() + "`\nUse `/search` instead if searching for a title.").queue();
            }

            @Override
            public void loadFailed(FriendlyException e) {
                event.getChannel().sendMessage(
                        ":warning: **Error:** `" + e.getMessage() + "`\nPlease report the error to <@277291758503723010>")
                    .queue();
                System.out.println(
                    new Utils().getTime(Utils.LogColor.RED) + "Error in Guild " + event.getGuild()
                        .getName() + ":");
                e.printStackTrace();
            }
        });
    }

    public static PlayerManager getInstance() {
        if (instance == null) instance = new PlayerManager();

        return instance;
    }

    public void joinVC(Guild guild, AudioChannel channel) {
        GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(guild);
        AudioPlayer audioPlayer = musicManager.player;
        AudioManager audioManager = guild.getAudioManager();
        if (!audioManager.isConnected()) {
            audioPlayer.setVolume(100);
            audioManager.openAudioConnection(channel);
            audioManager.setSelfDeafened(true);
            System.out.println(
                new Utils().getTime(Utils.LogColor.GREEN) + "Joining voice channel in " + guild.getName());
        }
    }
}
