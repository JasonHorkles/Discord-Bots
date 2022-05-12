package me.jasonhorkles.musicdaddy;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.jasonhorkles.musicdaddy.lavaplayer.GuildMusicManager;
import me.jasonhorkles.musicdaddy.lavaplayer.PlayerManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.apache.hc.core5.http.ParseException;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.ClientCredentials;
import se.michaelthelin.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.Scanner;

@SuppressWarnings({"BusyWait"})
public class MusicDaddy extends ListenerAdapter {
    public static JDA api;
    public static final SpotifyApi spotify = new SpotifyApi.Builder().setClientId(new Secrets().getSpotifyClientId())
        .setClientSecret(new Secrets().getSpotifyClientSecret()).build();

    private static final ClientCredentialsRequest ccr = spotify.clientCredentials().build();

    public static void main(String[] args) throws LoginException, InterruptedException {
        System.out.println(new Utils().getTime(Utils.Color.YELLOW) + "Starting...");

        JDABuilder builder = JDABuilder.createDefault(new Secrets().getBotToken());
        builder.disableIntents(GatewayIntent.GUILD_MESSAGE_TYPING);
        builder.disableCache(CacheFlag.ACTIVITY);
        builder.enableCache(CacheFlag.VOICE_STATE);
        builder.enableIntents(GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.GUILD_MEMBERS);
        builder.setMemberCachePolicy(MemberCachePolicy.ALL);
        builder.setStatus(OnlineStatus.IDLE);
        builder.setActivity(Activity.listening("startup sounds"));
        builder.setEnableShutdownHook(false);
        api = builder.build();

        try {
            ClientCredentials cc = ccr.execute();
            spotify.setAccessToken(cc.getAccessToken());
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            System.out.print(new Utils().getTime(Utils.Color.RED));
            e.printStackTrace();
        }

        // Wait until the api works
        while (api.getGuilds().isEmpty()) Thread.sleep(100);

        api.addEventListener(new Events());

        for (Guild guilds : api.getGuilds()) {
            CommandListUpdateAction commands = guilds.updateCommands();

            commands.addCommands(Commands.slash("play", "Add a video / playlist to the queue")
                    .addOption(OptionType.STRING, "url", "Link to the video / playlist", true)
                    .addOption(OptionType.BOOLEAN, "shuffle",
                        "Whether or not the playlist should be shuffled (if applicable)", false)
                    .addOption(OptionType.INTEGER, "maxsongs",
                        "The maximum amount of songs to play from the playlist (if applicable) - limit 100", false),
                Commands.slash("search", "Search for a video on YouTube")
                    .addOption(OptionType.STRING, "title", "What to search for", true)
                    .addOption(OptionType.INTEGER, "result", "The nth result to use", false),
                Commands.slash("skip", "Skip the current video"), Commands.slash("stop", "Stop the audio"),
                Commands.slash("pause", "Toggle the paused state of the audio"),
                Commands.slash("volume", "Change the volume")
                    .addOption(OptionType.INTEGER, "volume", "The volume level (default 100, allows 1-150)", true),
                Commands.slash("np", "Displays the currently playing track"),
                Commands.slash("queue", "Displays the current queue")).queue();
        }

        // Add shutdown hooks
        Runtime.getRuntime().addShutdownHook(new Thread(() -> new MusicDaddy().shutdown(), "Shutdown Hook"));
        Thread input = new Thread(() -> {
            while (true) {
                Scanner in = new Scanner(System.in);
                String text = in.nextLine();
                if (text.equalsIgnoreCase("stop")) System.exit(0);
            }
        }, "Console Input");
        input.start();

        api.getPresence().setStatus(OnlineStatus.ONLINE);
        api.getPresence().setActivity(Activity.listening("dope tunes"));

        System.out.println(new Utils().getTime(Utils.Color.GREEN) + "Done starting up!");
    }

    private GuildMusicManager getMusicManager(Guild guild) {
        return PlayerManager.musicManagers.computeIfAbsent(guild.getIdLong(), (guildId) -> {
            GuildMusicManager guildMusicManager = new GuildMusicManager(PlayerManager.audioPlayerManager);
            guild.getAudioManager().setSendingHandler(guildMusicManager.getSendHandler());
            return guildMusicManager;
        });
    }

    public void leaveChannel(Guild guild) {
        GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(guild);
        musicManager.scheduler.player.stopTrack();
        musicManager.scheduler.queue.clear();
        musicManager.player.destroy();

        AudioManager audioManager = guild.getAudioManager();
        audioManager.closeAudioConnection();

        Events.currentVoiceChannel.remove(guild);

        System.gc();

        System.out.println(new Utils().getTime(Utils.Color.GREEN) + "Leaving voice channel in " + guild.getName());
    }

    public void shutdown() {
        System.out.println(new Utils().getTime(Utils.Color.YELLOW) + "Shutting down...");
        try {
            AudioSourceManagers.registerLocalSource(PlayerManager.audioPlayerManager);
            final boolean[] inChannels = {false};
            for (Guild guild : api.getGuilds()) {
                GuildMusicManager musicManager = getMusicManager(guild);

                PlayerManager.audioPlayerManager.loadItemOrdered(musicManager, "MusicDaddy/Shutting Down.mp3",
                    new AudioLoadResultHandler() {
                        @Override
                        public void trackLoaded(AudioTrack track) {
                            if (!guild.getAudioManager().isConnected()) return;
                            inChannels[0] = true;

                            musicManager.scheduler.queue.clear();
                            musicManager.scheduler.queue(track);
                            if (musicManager.scheduler.player.getPlayingTrack() != null)
                                musicManager.scheduler.nextTrack();
                        }

                        @Override
                        public void playlistLoaded(AudioPlaylist playlist) {
                            System.out.println(new Utils().getTime(Utils.Color.RED) + "Playlist");
                        }

                        @Override
                        public void noMatches() {
                            System.out.println(new Utils().getTime(Utils.Color.RED) + "No file matches");
                        }

                        @Override
                        public void loadFailed(FriendlyException e) {
                            System.out.print(new Utils().getTime(Utils.Color.RED));
                            e.printStackTrace();
                        }
                    });
            }
            if (inChannels[0]) Thread.sleep(3000);
        } catch (InterruptedException | NoClassDefFoundError ignored) {
        }

        try {
            for (Guild guild : api.getGuilds())
                if (guild.getAudioManager().isConnected()) {
                    GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(guild);
                    musicManager.scheduler.player.stopTrack();
                    musicManager.scheduler.queue.clear();
                    musicManager.player.destroy();

                    guild.getAudioManager().closeAudioConnection();
                }
            api.shutdownNow();
        } catch (NoClassDefFoundError ignored) {
        }
    }
}
