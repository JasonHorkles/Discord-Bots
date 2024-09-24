package me.jasonhorkles.fancyfriend;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.ErrorResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("DataFlowIssue")
public class Events extends ListenerAdapter {
    private final Map<Long, Integer> warnings = new HashMap<>();

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + event.getMember()
            .getEffectiveName() + " used the /" + event.getName() + " command");

        //noinspection SwitchStatementWithTooFewBranches
        switch (event.getName().toLowerCase()) {
            case "noping" -> {
                //todo: implement
            }
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        // Ping check
        if (!event.getMessage().getChannelType().isGuild()) return;
        // Let staff ping other staff
        if (isStaff(event.getMember())) return;

        Long id = event.getAuthor().getIdLong();

        int count = 0;
        for (Member member : event.getMessage().getMentions().getMembers())
            if (isStaff(member)) count++;

        if (count > 0) {
            warn(id, event, count);
            StringBuilder message = new StringBuilder("Hey " + event.getMember()
                .getEffectiveName() + ", please don't ping staff members!");

            boolean appendTicket = true;

            if (event.getMessage().getChannelType() == ChannelType.TEXT)
                if (event.getMessage().getCategory().getIdLong() == 1112487038160220180L)
                    appendTicket = false;

            if (appendTicket) message.append(
                "\nIf there's a problem, please make a ticket with <#1112486757359960175>");

            if (warnings.get(id) > 1) message.append("-# Warning ").append(warnings.get(id)).append("/3");

            event.getMessage().reply(message).queue(message1 -> message1.delete().queueAfter(15,
                TimeUnit.MINUTES,
                null,
                new ErrorHandler().handle(ErrorResponse.UNKNOWN_MESSAGE,
                    (e) -> System.out.println(new Utils().getTime(Utils.LogColor.RED) + "Unable to delete warning message."))));
        }
    }

    @SuppressWarnings("DataFlowIssue")
    public void warn(Long id, MessageReceivedEvent event, int count) {
        if (warnings.containsKey(id)) {
            warnings.put(id, warnings.get(id) + count);
            if (warnings.get(id) >= 3) event.getMember().timeoutFor(12, TimeUnit.HOURS).queue(null,
                (na) -> event.getChannel().sendMessage("<@277291758503723010>")
                    .queue(del -> del.delete().queueAfter(15,
                        TimeUnit.SECONDS,
                        null,
                        new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE))));
        } else warnings.put(id, count);

        System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Warned " + event.getMember()
            .getEffectiveName() + " for pinging - " + warnings.get(id) + "/3");

        for (int x = count; x > 0; x--) {
            scheduleWarningRemoval(id, event.getMember().getEffectiveName());
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                System.out.print(new Utils().getTime(Utils.LogColor.RED));
                e.printStackTrace();
            }
        }
    }

    public void scheduleWarningRemoval(Long id, String name) {
        new Thread(() -> {
            try (ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor()) {
                executor.schedule(() -> takeWarning(id, name), 15, TimeUnit.MINUTES);
            }
        }, "Warning Removal").start();
    }

    private void takeWarning(Long id, String name) {
        if (warnings.get(id) <= 1) {
            warnings.remove(id);
            System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + "Removed ping warning from " + name + " - 0/3");
        } else {
            warnings.put(id, warnings.get(id) - 1);
            System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + "Removed ping warning from " + name + " - " + warnings.get(
                id) + "/3");
        }
    }

    private boolean isStaff(Member member) {
        // Moderator | Developer | Helpful
        String roles = member.getRoles().toString();
        return roles.contains("1134906027142299749") || roles.contains("1092512242127339610") || roles.contains(
            "1198213765125128302");
    }
}
