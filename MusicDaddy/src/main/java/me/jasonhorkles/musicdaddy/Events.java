package me.jasonhorkles.musicdaddy;

import com.neovisionaries.i18n.CountryCode;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import me.jasonhorkles.musicdaddy.lavaplayer.GuildMusicManager;
import me.jasonhorkles.musicdaddy.lavaplayer.PlayerManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;
import org.apache.hc.core5.http.ParseException;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.ClientCredentials;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.PlaylistTrack;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.model_objects.specification.TrackSimplified;
import se.michaelthelin.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("ConstantConditions")
public class Events extends ListenerAdapter {
    public static final HashMap<Guild, AudioChannel> currentVoiceChannel = new HashMap<>();

    private static final SpotifyApi spotify = new SpotifyApi.Builder().setClientId(new Secrets().getSpotifyClientId())
        .setClientSecret(new Secrets().getSpotifyClientSecret()).build();
    private static final ClientCredentialsRequest ccr = spotify.clientCredentials().build();

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + event.getMember()
            .getEffectiveName() + " used the /" + event.getName() + " command");

        AudioChannel voiceChannel = event.getMember().getVoiceState().getChannel();
        GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(event.getGuild());
        AudioPlayer audioPlayer = musicManager.player;

        event.deferReply().setEphemeral(true).queue();

        switch (event.getName().toLowerCase()) {
            case "play", "search" -> {
                if (voiceChannel == null) {
                    event.getHook().editOriginal("You must be in a voice channel to do that!").queue();
                    return;
                }

                String search;
                if (event.getName().equalsIgnoreCase("play")) {
                    String url = event.getOption("url").getAsString();
                    if (url.contains("spotify")) try {
                        try {
                            ClientCredentials cc = ccr.execute();
                            spotify.setAccessToken(cc.getAccessToken());
                        } catch (IOException | SpotifyWebApiException | ParseException e) {
                            System.out.print(new Utils().getTime(Utils.LogColor.RED));
                            e.printStackTrace();
                        }

                        if (url.contains("track")) {
                            url = url.replaceFirst(".*track/", "").replaceAll("\\?si=.*", "");
                            Track track = spotify.getTrack(url).build().execute();

                            search = "ytsearch:" + track.getName() + " " + track.getArtists()[0].getName() + " audio";

                        } else if (url.contains("playlist")) {
                            url = url.replaceFirst(".*playlist/", "").replaceAll("\\?si=.*", "");
                            Paging<PlaylistTrack> playlist = spotify.getPlaylistsItems(url).build()
                                .execute();
                            List<PlaylistTrack> tracks = new ArrayList<>(Arrays.asList(playlist.getItems()));
                            boolean shuffle = false;
                            if (event.getOption("shuffle") != null) shuffle = event.getOption("shuffle").getAsBoolean();
                            if (shuffle) Collections.shuffle(tracks);

                            long maxSongs = 15;
                            if (event.getOption("maxsongs") != null) maxSongs = event.getOption("maxsongs").getAsLong();
                            if (maxSongs > 100) maxSongs = 100;
                            if (maxSongs == 0) maxSongs = 1;

                            int x = 1;
                            int additional = 0;
                            StringBuilder msg = new StringBuilder();
                            for (PlaylistTrack track : tracks) {
                                if (x > maxSongs) break;
                                PlayerManager.getInstance()
                                    .loadAndPlay(event, "ytsearch:" + track.getTrack().getName() + " song audio", true,
                                        false);

                                if (msg.length() < 1800)
                                    msg.append("\nSuccessfully added **").append(track.getTrack().getName())
                                        .append("** to the queue!");
                                else additional++;
                                x++;
                            }

                            if (additional > 0) msg.append("\n\n...and ").append(additional).append(" more...");
                            event.getHook().editOriginal(msg.toString()).queue();
                            currentVoiceChannel.put(event.getGuild(), voiceChannel);
                            return;

                        } else if (url.contains("album")) {
                            url = url.replaceFirst(".*album/", "").replaceAll("\\?si=.*", "");
                            Paging<TrackSimplified> album = spotify.getAlbumsTracks(url).build().execute();
                            List<TrackSimplified> tracks = new ArrayList<>(Arrays.asList(album.getItems()));
                            boolean shuffle = false;
                            if (event.getOption("shuffle") != null) shuffle = event.getOption("shuffle").getAsBoolean();
                            if (shuffle) Collections.shuffle(tracks);

                            long maxSongs = 15;
                            if (event.getOption("maxsongs") != null) maxSongs = event.getOption("maxsongs").getAsLong();
                            if (maxSongs > 100) maxSongs = 100;
                            if (maxSongs == 0) maxSongs = 1;

                            int x = 1;
                            int additional = 0;
                            StringBuilder msg = new StringBuilder();
                            for (TrackSimplified track : album.getItems()) {
                                if (x > maxSongs) break;
                                PlayerManager.getInstance().loadAndPlay(event,
                                    "ytsearch:" + track.getName() + " " + track.getArtists()[0].getName() + " audio",
                                    true, false);

                                if (msg.length() < 1800) msg.append("\nSuccessfully added **").append(track.getName())
                                    .append("** to the queue!");
                                else additional++;
                                x++;
                            }

                            if (additional > 0) msg.append("\n\n...and ").append(additional).append(" more...");
                            event.getHook().editOriginal(msg.toString()).queue();
                            currentVoiceChannel.put(event.getGuild(), voiceChannel);
                            return;

                        } else if (url.contains("artist")) {
                            url = url.replaceFirst(".*artist/", "").replaceAll("\\?si=.*", "");
                            Track[] artistTracks = spotify.getArtistsTopTracks(url, CountryCode.US).build()
                                .execute();
                            List<Track> tracks = new ArrayList<>(Arrays.asList(artistTracks));
                            boolean shuffle = false;
                            if (event.getOption("shuffle") != null) shuffle = event.getOption("shuffle").getAsBoolean();
                            if (shuffle) Collections.shuffle(tracks);

                            long maxSongs = 15;
                            if (event.getOption("maxsongs") != null) maxSongs = event.getOption("maxsongs").getAsLong();
                            if (maxSongs > 100) maxSongs = 100;
                            if (maxSongs == 0) maxSongs = 1;

                            int x = 1;
                            int additional = 0;
                            StringBuilder msg = new StringBuilder();
                            for (Track track : tracks) {
                                if (x > maxSongs) break;
                                PlayerManager.getInstance().loadAndPlay(event,
                                    "ytsearch:" + track.getName() + " " + track.getArtists()[0].getName() + " audio",
                                    true, false);

                                if (msg.length() < 1800) msg.append("\nSuccessfully added **").append(track.getName())
                                    .append("** to the queue!");
                                else additional++;
                                x++;
                            }

                            if (additional > 0) msg.append("\n\n...and ").append(additional).append(" more...");
                            event.getHook().editOriginal(msg.toString()).queue();
                            currentVoiceChannel.put(event.getGuild(), voiceChannel);
                            return;

                        } else search = url;
                    } catch (IOException | SpotifyWebApiException | ParseException e) {
                        System.out.print(new Utils().getTime(Utils.LogColor.RED));
                        e.printStackTrace();
                        search = url;
                    }
                    else search = url;
                } else search = "ytsearch:" + event.getOption("title").getAsString();

                PlayerManager.getInstance().loadAndPlay(event, search, false, false);
                currentVoiceChannel.put(event.getGuild(), voiceChannel);
            }

            case "skip" -> {
                if (audioPlayer.getPlayingTrack() == null) {
                    event.getHook().editOriginal("There are currently no tracks playing.").queue();
                    return;
                }

                musicManager.scheduler.nextTrack();

                event.getHook().editOriginal("Skipped the track!").queue();
            }

            case "stop" -> {
                musicManager.scheduler.queue.clear();
                audioPlayer.getPlayingTrack().stop();
                new MusicDaddy().leaveChannel(event.getGuild());
                event.getHook().editOriginal("Audio stopped!").queue();
            }

            case "pause" -> {
                boolean paused = audioPlayer.isPaused();
                AudioManager audioManager = event.getGuild().getAudioManager();

                if (paused) {
                    audioPlayer.setPaused(false);
                    audioManager.setSelfMuted(false);
                    event.getHook().editOriginal("Resuming audio!").queue();
                } else {
                    audioPlayer.setPaused(true);
                    audioManager.setSelfMuted(true);
                    event.getHook().editOriginal("Pausing audio!").queue();
                }
            }

            case "volume" -> {
                int volume = Math.toIntExact(event.getOption("volume").getAsLong());
                if (volume < 1 || volume > 150) {
                    event.getHook().editOriginal("Volume must be between 1-150! (100 is default)").queue();
                    return;
                }
                audioPlayer.setVolume(volume);
                event.getHook().editOriginal("Volume set to **" + volume + "**!").queue();
            }

            case "np" -> {
                if (audioPlayer.getPlayingTrack() == null) {
                    event.getHook().editOriginal("There are currently no tracks playing.").queue();
                    return;
                }

                AudioTrackInfo track = audioPlayer.getPlayingTrack().getInfo();
                long timeDifference = track.length / 1000;
                int h = (int) (timeDifference / (3600));
                int m = (int) ((timeDifference - (h * 3600)) / 60);
                int s = (int) (timeDifference - (h * 3600) - m * 60);
                event.getHook().editOriginal(
                        "Currently playing: **" + track.title + "** (" + h + "h " + m + "m " + s + "s)\n" + track.uri)
                    .queue();
            }

            case "queue" -> {
                BlockingQueue<AudioTrack> queue = musicManager.scheduler.queue;

                if (queue.isEmpty()) {
                    event.getHook().editOriginal("The queue is currently empty!").queue();
                    return;
                }

                List<AudioTrack> tracks = new ArrayList<>(queue.stream().toList());

                int additional = 0;
                StringBuilder msg = new StringBuilder("__**Queued songs:**__");
                for (AudioTrack track : tracks)
                    if (msg.length() < 1800) msg.append("\n").append(track.getInfo().title);
                    else additional++;

                if (additional > 0) msg.append("\n\n...and ").append(additional).append(" more...");
                event.getHook().editOriginal(msg.toString()).queue();
            }

            case "outro" -> {
                if (voiceChannel == null) {
                    event.getHook().editOriginal("You must be in a voice channel to do that!").queue();
                    return;
                }

                String file = "MusicDaddy/Outro Song.mp3";
                Random r = new Random();
                if (r.nextInt(1, 21) == 20) file = "MusicDaddy/Outro Song 2.mp3";
                else if (r.nextInt(1, 101) == 100) file = "MusicDaddy/Outro Song 3.mp3";

                PlayerManager.getInstance().loadAndPlay(event, file, false, true);
                currentVoiceChannel.put(event.getGuild(), voiceChannel);

                event.getHook().editOriginal("Remember to like and subscribe").queue();

                // Bass drop
                Executors.newSingleThreadScheduledExecutor().schedule(() -> {
                    if (event.getOption("disconnectall") == null || !event.getOption("disconnectall").getAsBoolean()) {
                        if (event.getMember().getVoiceState().getChannel() != null)
                            event.getGuild().kickVoiceMember(event.getMember()).queue();

                    } else for (Member member : voiceChannel.getMembers())
                        try {
                            event.getGuild().kickVoiceMember(member).queue();
                        } catch (IllegalStateException ignored) {
                        }
                }, 54250, TimeUnit.MILLISECONDS);
            }
        }
    }

    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        if (event.getChannelLeft() == null) return;
        if (event.getMember().getUser().isBot()) return;
        if (!Events.currentVoiceChannel.containsKey(event.getGuild())) return;

        int membersInChannel = currentVoiceChannel.get(event.getGuild()).getMembers().size();
        for (Member member : currentVoiceChannel.get(event.getGuild()).getMembers())
            if (member.getUser().isBot()) membersInChannel--;
        if (membersInChannel > 0) return;

        if (event.getChannelLeft() == currentVoiceChannel.get(event.getGuild()))
            new MusicDaddy().leaveChannel(event.getGuild());
    }
}
