package me.jasonhorkles.silverstone;

import net.dv8tion.jda.api.entities.VoiceChannel;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("ConstantConditions")
public class Time {
    public static ScheduledFuture<?> task;

    public void updateTime() {
        task = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            long time = System.currentTimeMillis() - 1511420400000L;
            long years = (long) Math.floor(TimeUnit.MILLISECONDS.toDays(time) / 365.24);
            time -= TimeUnit.DAYS.toMillis((long) (years * 365.24));
            long months = (long) Math.floor(TimeUnit.MILLISECONDS.toDays(time) / 30F);
            time -= TimeUnit.DAYS.toMillis(months * 30);
            long days = TimeUnit.MILLISECONDS.toDays(time);

            VoiceChannel channel = Silverstone.api.getVoiceChannelById(914918234518593546L);
            channel.getManager().setName(years + " years " + months + " months " + days + " days").queue();
        }, 0, 6, TimeUnit.HOURS);
    }
}
