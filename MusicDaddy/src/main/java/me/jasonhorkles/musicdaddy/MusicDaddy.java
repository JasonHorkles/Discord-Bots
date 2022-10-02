package me.jasonhorkles.musicdaddy;

import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import me.jasonhorkles.musicdaddy.lavaplayer.GuildMusicManager;
import me.jasonhorkles.musicdaddy.lavaplayer.PlayerManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.apache.hc.core5.http.ParseException;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.ClientCredentials;
import se.michaelthelin.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;

import java.io.IOException;
import java.util.Scanner;

public class MusicDaddy {
    public static JDA jda;
    public static final SpotifyApi spotify = new SpotifyApi.Builder().setClientId(new Secrets().getSpotifyClientId())
        .setClientSecret(new Secrets().getSpotifyClientSecret()).build();

    private static final ClientCredentialsRequest ccr = spotify.clientCredentials().build();

    public static void main(String[] args) throws InterruptedException {
        System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Starting...");

        JDABuilder builder = JDABuilder.createDefault(new Secrets().getBotToken());
        builder.disableCache(CacheFlag.ACTIVITY);
        builder.enableCache(CacheFlag.VOICE_STATE);
        builder.enableIntents(GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.GUILD_MEMBERS);
        builder.setMemberCachePolicy(MemberCachePolicy.ALL);
        builder.setStatus(OnlineStatus.IDLE);
        builder.setActivity(Activity.listening("startup sounds"));
        builder.setEnableShutdownHook(false);
        builder.addEventListeners(new Events());
        jda = builder.build();

        try {
            ClientCredentials cc = ccr.execute();
            spotify.setAccessToken(cc.getAccessToken());
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            System.out.print(new Utils().getTime(Utils.LogColor.RED));
            e.printStackTrace();
        }

        jda.awaitReady();

        jda.updateCommands().addCommands(Commands.slash("play", "Add a video / playlist to the queue")
                .addOption(OptionType.STRING, "url", "Link to the video / playlist", true)
                .addOption(OptionType.BOOLEAN, "shuffle", "Whether or not the playlist should be shuffled (if applicable)",
                    false).addOption(OptionType.INTEGER, "maxsongs",
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

        //noinspection ConstantConditions
        jda.getGuildById(605786572519899206L).updateCommands()
            .addCommands(Commands.slash("outro", "Play the outro song")).queue();

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

        jda.getPresence().setStatus(OnlineStatus.ONLINE);
        jda.getPresence().setActivity(Activity.listening("dope tunes"));

        System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + "Done starting up!");
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

        System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + "Leaving voice channel in " + guild.getName());
    }

    public void shutdown() {
        System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Shutting down...");
        try {
            AudioSourceManagers.registerLocalSource(PlayerManager.audioPlayerManager);

            for (Guild guild : Events.currentVoiceChannel.keySet()) {
                if (!guild.getAudioManager().isConnected()) continue;
                new Utils().playFile(guild, "MusicDaddy/Shutting Down.mp3");
            }

            if (!Events.currentVoiceChannel.isEmpty()) Thread.sleep(3000);

        } catch (InterruptedException | NoClassDefFoundError ignored) {
        }

        try {
            for (Guild guild : jda.getGuilds())
                if (guild.getAudioManager().isConnected()) {
                    GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(guild);
                    musicManager.scheduler.player.stopTrack();
                    musicManager.scheduler.queue.clear();
                    musicManager.player.destroy();

                    guild.getAudioManager().closeAudioConnection();
                }
            jda.shutdownNow();
        } catch (NoClassDefFoundError ignored) {
        }
    }
}
