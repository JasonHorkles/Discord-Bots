package me.jasonhorkles.phoenella;

import me.jasonhorkles.phoenella.lavaplayer.GuildMusicManager;
import me.jasonhorkles.phoenella.lavaplayer.PlayerManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.managers.AudioManager;

@SuppressWarnings("DataFlowIssue")
public class Soundboard extends ListenerAdapter {
    private static AudioChannel currentVoiceChannel = null;

    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        if (event.getMember().getUser().isBot()) return;

        // Join
        if (event.getChannelJoined() != null && event.getChannelLeft() == null) {
            if (currentVoiceChannel == null) joinVC(event.getGuild(), event.getChannelJoined());
        }

        // Leave
        else if (event.getChannelJoined() == null && event.getChannelLeft() != null) {
            if (currentVoiceChannel == null) return;

            int membersInChannel = currentVoiceChannel.getMembers().size();
            for (Member member : currentVoiceChannel.getMembers()) if (member.getUser().isBot()) membersInChannel--;

            if (membersInChannel == 0) {
                GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(event.getGuild());
                musicManager.scheduler.player.stopTrack();
                musicManager.scheduler.queue.clear();
                musicManager.player.destroy();

                for (VoiceChannel voiceChannels : event.getGuild().getVoiceChannels())
                    if (!voiceChannels.getMembers().isEmpty())
                        if (!voiceChannels.getMembers().contains(event.getGuild().getMemberById(892263254825500692L))) {
                            joinVC(event.getGuild(), voiceChannels);
                            return;
                        }
                AudioManager audioManager = event.getGuild().getAudioManager();
                audioManager.closeAudioConnection();
                currentVoiceChannel = null;
            }
        }

        // Move
        else if (event.getChannelJoined() != currentVoiceChannel && event.getChannelLeft() == currentVoiceChannel) {

            int membersInChannel = currentVoiceChannel.getMembers().size();
            for (Member member : currentVoiceChannel.getMembers())
                if (member.getUser().isBot()) membersInChannel--;

            if (membersInChannel == 0) {
                GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(event.getGuild());
                musicManager.scheduler.player.stopTrack();
                musicManager.scheduler.queue.clear();
                musicManager.player.destroy();

                joinVC(event.getGuild(), event.getChannelJoined());
            }
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (event.getComponentId().startsWith("sound:")) {
            System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + event.getUser()
                .getAsTag() + " clicked the '" + event.getComponentId().replace("sound:", "") + "' button");

            if (event.getComponentId().equals("sound:forceswitch")) {
                if (event.getMember().getVoiceState().inAudioChannel()) {
                    joinVC(event.getGuild(), event.getMember().getVoiceState().getChannel());
                    event.deferEdit().queue();
                } else event.reply("You must be in a voice channel!").setEphemeral(true).queue();
                return;
            }

            if (event.getMember().getVoiceState().inAudioChannel()) {
                if (event.getMember().getVoiceState().getChannel() != currentVoiceChannel) {
                    event.reply("You must be in the same voice channel as me!")
                        .addActionRow(Button.danger("sound:forceswitch", "Move Phoenella to current voice channel"))
                        .setEphemeral(true).queue();
                    return;
                }
            } else {
                event.reply("You must be in a voice channel!").setEphemeral(true).queue();
                return;
            }

            String file = "null";
            int volume = 100;
            switch (event.getComponentId().replace("sound:", "")) {
                case "benny" -> {
                    file = "Phoenella/Sounds/Benny Hill Theme.mp3";
                    volume = 50;
                }
                case "bfg" -> {
                    file = "Phoenella/Sounds/BFG Division.mp3";
                    volume = 17;
                }
                case "careless" -> {
                    file = "Phoenella/Sounds/Careless Whisper.mp3";
                    volume = 45;
                }
                case "crickets" -> file = "Phoenella/Sounds/Crickets Chirping.mp3";
                case "discord" -> {
                    file = "Phoenella/Sounds/Discord.ogg";
                    volume = 75;
                }
                case "dramatic" -> {
                    file = "Phoenella/Sounds/Dramatic Sound Effect.mp3";
                    volume = 45;
                }
                case "drumroll" -> {
                    file = "Phoenella/Sounds/Drumroll.mp3";
                    volume = 45;
                }
                case "flysave" -> {
                    file = "Phoenella/Sounds/Flying Save.mp3";
                    volume = 75;
                }
                case "honk" -> {
                    file = "Phoenella/Sounds/Honk.mp3";
                    volume = 30;
                }
                case "laugh" -> {
                    file = "Phoenella/Sounds/Laugh Track.mp3";
                    volume = 50;
                }
                case "maya" -> {
                    file = "Phoenella/Sounds/Maya Hee.mp3";
                    volume = 75;
                }
                case "metalgear" -> {
                    file = "Phoenella/Sounds/Metal Gear Alert.mp3";
                    volume = 40;
                }
                case "oof" -> file = "Phoenella/Sounds/Oof.mp3";
                case "party" -> file = "Phoenella/Sounds/Party Horn.mp3";
                case "phasmophobia" -> {
                    file = "Phoenella/Sounds/Phasmophobia Ghost Attack 2.mp3";
                    volume = 50;
                }
                case "sad" -> {
                    file = "Phoenella/Sounds/Sad Violin.mp3";
                    volume = 50;
                }
                case "skibidi" -> {
                    file = "Phoenella/Sounds/SKIBIDI BOP MM DADA.mp3";
                    volume = 20;
                }
                case "suspense1" -> file = "Phoenella/Sounds/Suspense 1.mp3";
                case "suspense2" -> {
                    file = "Phoenella/Sounds/Suspense 2.mp3";
                    volume = 45;
                }
                case "tech" -> {
                    file = "Phoenella/Sounds/Technical Difficulties Elevator Sound.mp3";
                    volume = 75;
                }
                case "thunk" -> file = "Phoenella/Sounds/Thunk.mp3";
                case "yeet" -> {
                    file = "Phoenella/Sounds/Yeet.mp3";
                    volume = 20;
                }
                case "stop" -> {
                    GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(event.getGuild());
                    musicManager.scheduler.player.stopTrack();
                    musicManager.scheduler.queue.clear();
                    musicManager.player.destroy();
                    event.deferEdit().queue();
                    return;
                }
            }

            PlayerManager.getInstance().getMusicManager(event.getGuild()).player.setVolume(volume);
            PlayerManager.getInstance().loadAndPlay(file);

            event.deferEdit().queue();
        }
    }

    private void joinVC(Guild guild, AudioChannel channel) {
        AudioManager audioManager = guild.getAudioManager();
        currentVoiceChannel = channel;
        try {
            audioManager.openAudioConnection(channel);
            audioManager.setSelfDeafened(true);
        } catch (InsufficientPermissionException ignored) {
            System.out.println(new Utils().getTime(
                Utils.LogColor.YELLOW) + "Couldn't join '" + channel.getName() + "' due to insufficient permissions!");
        }
    }
}
