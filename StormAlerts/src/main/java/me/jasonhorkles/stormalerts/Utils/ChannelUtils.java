package me.jasonhorkles.stormalerts.Utils;

import me.jasonhorkles.stormalerts.StormAlerts;
import me.jasonhorkles.stormalerts.Weather;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;

public class ChannelUtils {
    public static TextChannel alertChannel;
    public static TextChannel aqiChannel;
    public static TextChannel earthquakeChannel;
    public static TextChannel hailChannel;
    public static TextChannel heavyRainChannel;
    public static TextChannel lightningChannel;
    public static TextChannel logChannel;
    public static TextChannel rainChannel;
    public static TextChannel rainConfirmationChannel;
    public static TextChannel recordsChannel;
    public static TextChannel snowChannel;
    public static TextChannel windChannel;

    public void cacheChannels(JDA jda) {
        alertChannel = jda.getTextChannelById(850442466775662613L);
        aqiChannel = jda.getTextChannelById(1261785476277338163L);
        earthquakeChannel = jda.getTextChannelById(1261771353002872924L);
        hailChannel = jda.getTextChannelById(845010798367473736L);
        heavyRainChannel = jda.getTextChannelById(843955756596461578L);
        lightningChannel = jda.getTextChannelById(899876734999089192L);
        logChannel = jda.getTextChannelById(1093060038265950238L);
        rainChannel = jda.getTextChannelById(900248256515285002L);
        rainConfirmationChannel = jda.getTextChannelById(921113488464695386L);
        recordsChannel = jda.getTextChannelById(1007060910050914304L);
        snowChannel = jda.getTextChannelById(845010495865618503L);
        windChannel = jda.getTextChannelById(1028358818050080768L);
    }

    @SuppressWarnings("DataFlowIssue")
    public void updateVoiceChannel(long id, String name) {
        VoiceChannel voiceChannel = StormAlerts.jda.getVoiceChannelById(id);
        if (!voiceChannel.getName().equals(name)) voiceChannel.getManager().setName(name).queue();
    }

    public TextChannel getWeatherChannel(Weather.WeatherType type) {
        switch (type) {
            case RAIN -> {
                return rainChannel;
            }

            case SNOW -> {
                return snowChannel;
            }

            case HAIL -> {
                return hailChannel;
            }

            default -> {
                return logChannel;
            }
        }
    }
}
