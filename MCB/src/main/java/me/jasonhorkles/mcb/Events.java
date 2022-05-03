package me.jasonhorkles.mcb;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.ErrorResponse;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Events extends ListenerAdapter {
    private final Map<Long, Integer> warnings = new HashMap<>();

    private static final Long letsPlay = 688770749191815323L;
    private static final Long paid = 603191416801198080L;
    private static final Long free = 603191498837721098L;
    private static final Long hire = 625728304410001410L;
    private static final Long showServer = 603881733322047508L;
    private static final Long youtube = 692712729273696326L;
    private static final Long shopping = 615208624855449613L;
    //    private static final Long logs = 603585853444456449L;
    private static final Long[] channels = new Long[]{letsPlay, paid, free, hire, showServer, youtube, shopping};

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        // Give roles
        if (event.getMessage().getChannel().getIdLong() == 603190904303386635L || event.getMessage().getChannel()
            .getIdLong() == 603193297879433216L)
            if (!event.getMessage().getAttachments().isEmpty() || event.getMessage().getContentStripped().toLowerCase()
                .contains("http")) if (!event.getMember().getRoles().toString().contains("646293661729947658"))
                event.getGuild().addRoleToMember(event.getMember(), event.getGuild().getRoleById(646293661729947658L))
                    .queue();

        // Ping check
        if (event.getMember().getRoles().toString().contains("646291178144399371")) return;

        Long id = event.getAuthor().getIdLong();

        int count = 0;
        for (Member member : event.getMessage().getMentionedMembers())
            if (member.getRoles().toString().contains("646291178144399371")) count++;

        if (count > 0) {
            warn(id, event, count);
            StringBuilder message = new StringBuilder(
                "Hey " + event.getMember().getEffectiveName() + ", please don't ping staff members!");

            if (event.getTextChannel().getParentCategoryIdLong() != 720690619822899231L) message.append(
                "\nIf there's a problem, make a ticket by typing `-ticket open <ticket name>` in any channel.");

            if (warnings.get(id) > 1) message.append("\n\n*Warning ").append(warnings.get(id)).append("/3*");

            event.getMessage().reply(message).queue(message1 -> message1.delete().queueAfter(5, TimeUnit.MINUTES, null,
                new ErrorHandler().handle(ErrorResponse.UNKNOWN_MESSAGE, (e) -> System.out.println(
                    new Utils().getTime(Utils.Color.RED) + "Unable to delete warning message."))));
        }
    }

    @SuppressWarnings("ConstantConditions")
    public void warn(Long id, MessageReceivedEvent event, int count) {
        if (warnings.containsKey(id)) {
            warnings.put(id, warnings.get(id) + count);
            if (warnings.get(id) >= 3) {
                event.getMember().getUser().openPrivateChannel().flatMap(channel -> channel.sendMessage(
                        "Please reduce the amount of staff pings.\nYou may re-join the server @ https://discord.gg/GMEXvt9wVB"))
                    .queue(null, new ErrorHandler().handle(ErrorResponse.CANNOT_SEND_TO_USER, (e) -> System.out.println(
                        new Utils().getTime(Utils.Color.RED) + "Can't send message to " + event.getMember()
                            .getEffectiveName())));
                if (event.getMember().getRoles().toString().contains("646291178144399371"))
                    event.getChannel().sendMessage(event.getMember().getEffectiveName() + " would be kicked!")
                        .queueAfter(100, TimeUnit.MILLISECONDS);
                else event.getMember().kick("Ping spam").queueAfter(100, TimeUnit.MILLISECONDS);
            }
        } else warnings.put(id, count);

        System.out.println(new Utils().getTime(Utils.Color.YELLOW) + "Warned " + event.getMember()
            .getEffectiveName() + " for ping spam - " + warnings.get(id) + "/3");

        for (int x = count; x > 0; x--) {
            scheduleWarningRemoval(id, event.getMember().getEffectiveName());
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                System.out.print(new Utils().getTime(Utils.Color.RED));
                e.printStackTrace();
            }
        }
    }

    public void scheduleWarningRemoval(Long id, String name) {
        Executors.newSingleThreadScheduledExecutor().schedule(() -> takeWarning(id, name), 5, TimeUnit.MINUTES);
    }

    //todo
    /*@SuppressWarnings("ConstantConditions")
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName().toLowerCase()) {
        }
    }*/

    @SuppressWarnings("ConstantConditions")
    public void deleteMessages(JDA jda, int x) {
        int count = 0;

        try {
            for (Long id : channels) {
                System.out.println(
                    new Utils().getTime(Utils.Color.YELLOW) + "Deleting messages in #" + jda.getTextChannelById(id)
                        .getName() + "...");

                for (Message message : new Utils().getMessages(jda.getTextChannelById(id), x)
                    .get(45, TimeUnit.SECONDS)) {
                    Member member = jda.getGuildById(603190205393928193L)
                        .getMemberById(message.getAuthor().getIdLong());

                    if (message.getTimeCreated().isBefore(OffsetDateTime.now().minus(2, ChronoUnit.WEEKS))) continue;

                    if (member == null) {
                        if (count % 45 == 0) Thread.sleep(5000);

                        message.delete().queue();
                        count++;
                    }
                }
            }

            System.out.println(new Utils().getTime(Utils.Color.GREEN) + "Deleted " + count + " messages.\n");
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            System.out.print(new Utils().getTime(Utils.Color.RED));
            e.printStackTrace();
        }
    }

    private void takeWarning(Long id, String name) {
        if (warnings.get(id) <= 1) {
            warnings.remove(id);
            System.out.println(
                new Utils().getTime(Utils.Color.GREEN) + "Removed ping spam warning from " + name + " - 0/3");
        } else {
            warnings.put(id, warnings.get(id) - 1);
            System.out.println(new Utils().getTime(
                Utils.Color.GREEN) + "Removed ping spam warning from " + name + " - " + warnings.get(id) + "/3");
        }
    }
}
