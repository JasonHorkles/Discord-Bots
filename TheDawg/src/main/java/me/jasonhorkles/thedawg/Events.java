package me.jasonhorkles.thedawg;

import me.jasonhorkles.thedawg.lavaplayer.PlayerManager;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;

import java.security.SecureRandom;

public class Events extends ListenerAdapter {
    public static Guild currentVoiceChannel;

    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        if (event.getChannelJoined() == null) return;
        if (event.getMember().getIdLong() != 840025878947692554L && event.getMember()
                                                                        .getIdLong() != 277291758503723010L)
            return;
        if (!event.getChannelJoined().getMembers().contains(event.getGuild()
            .getMemberById(840025878947692554L)) || !event.getChannelJoined().getMembers().contains(event
            .getGuild().getMemberById(277291758503723010L))) return;
        if (!event.getChannelJoined().getMembers().contains(event.getGuild()
            .getMemberById(277291758503723010L))) return;
        if (!isLive()) return;

        AudioManager audioManager = event.getGuild().getAudioManager();
        try {
            audioManager.openAudioConnection(event.getChannelJoined());
            audioManager.setSelfDeafened(true);
        } catch (InsufficientPermissionException ignored) {
            System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Couldn't join '" + event
                .getChannelJoined().getName() + "' due to insufficient permissions!");
        }

        String file = null;
        switch (new SecureRandom().nextInt(8)) {
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

    @SuppressWarnings("DataFlowIssue")
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + event.getMember()
            .getEffectiveName() + " used the /" + event.getName() + " command");

        //noinspection SwitchStatementWithTooFewBranches
        switch (event.getName().toLowerCase()) {
            case "zebra" -> {
                if (event.getMember().getIdLong() == 277291758503723010L) {
                    if (event.getOption("zebra") == null) {
                        event.reply("You didn't provide anything to say!").setEphemeral(true).queue();
                        return;
                    }

                    event.getChannel().sendMessage(event.getOption("zebra").getAsString()).queue();
                    event.reply("Tada!").setEphemeral(true).queue();
                } else {
                    String image = "You broke it!";
                    switch (new SecureRandom().nextInt(9)) {
                        case 0 ->
                            image = "https://media.discordapp.net/attachments/335445132520194058/1098675297873838161/Plains_Zebra_Equus_quagga.png?width=447&height=671";
                        case 1 ->
                            image = "https://media.discordapp.net/attachments/335445132520194058/1098675298469412875/hero_zebra_animals.png?width=1193&height=671";
                        case 2 ->
                            image = "https://media.discordapp.net/attachments/335445132520194058/1098675299090178048/HjFE8NKWuCmgfHCcndJ3rK-1200-80.png";
                        case 3 ->
                            image = "https://media.discordapp.net/attachments/335445132520194058/1098675299383787602/GettyImages-520064858-79cc024.png";
                        case 4 ->
                            image = "https://media.discordapp.net/attachments/335445132520194058/1098675299681566812/maxresdefault.png?width=1193&height=671";
                        case 5 ->
                            image = "https://media.discordapp.net/attachments/335445132520194058/1098675299954204854/zebra-laugh.png";
                        case 6 ->
                            image = "https://media.discordapp.net/attachments/335445132520194058/1098675300289757245/zebra-portrait-adorable-animal-face-looking-to-the-camera.png";
                        case 7 ->
                            image = "https://media.discordapp.net/attachments/335445132520194058/1098675300549787708/360_F_216467464_75xb1uxe025pyWMAkMxlUPRHzS8zETDB.png";
                        case 8 ->
                            image = "https://media.discordapp.net/attachments/335445132520194058/1098675300801454190/zebraclimb.png";
                    }
                    event.reply(image).setEphemeral(true).queue();

                }
            }
        }
    }

    private boolean isLive() {
        //noinspection DataFlowIssue
        for (Activity activity : TheDawg.jda.getGuildById(335435349734064140L).getMemberById(
            277291758503723010L).getActivities())
            if (activity.getName()
                    .equalsIgnoreCase("Twitch") && activity.getType() == Activity.ActivityType.STREAMING)
                return true;
        return false;
    }
}
