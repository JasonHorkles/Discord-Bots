package me.jasonhorkles.silverstone;

import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import org.joda.time.LocalDate;
import org.joda.time.Period;

@SuppressWarnings("DataFlowIssue")
public class Time {
    public void updateTime() {
        Period p = new Period(new LocalDate(1511420400000L), LocalDate.now());

        VoiceChannel channel = Silverstone.jda.getVoiceChannelById(914918234518593546L);
        channel.getManager()
            .setName(p.getYears() + "y " + p.getMonths() + "m " + (p.getDays() + (p.getWeeks() * 7)) + "d")
            .queue();

        if (p.getMonths() == 0 && p.getDays() + (p.getWeeks() * 7) == 0)
            new Secrets().yearlyMsg(p.getYears());
    }
}
