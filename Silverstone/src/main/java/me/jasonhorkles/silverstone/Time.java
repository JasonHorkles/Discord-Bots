package me.jasonhorkles.silverstone;

import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import org.joda.time.LocalDate;
import org.joda.time.Period;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("ConstantConditions")
public class Time {
    public static ScheduledFuture<?> task;

    public void updateTime() {
        task = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            Period p = new Period(new LocalDate(1511420400000L), LocalDate.now());

            VoiceChannel channel = Silverstone.jda.getVoiceChannelById(914918234518593546L);
            channel.getManager().setName(
                    p.getYears() + " years " + p.getMonths() + " months " + (p.getDays() + (p.getWeeks() * 7)) + " days")
                .queue();
        }, 0, 6, TimeUnit.HOURS);
    }
}
