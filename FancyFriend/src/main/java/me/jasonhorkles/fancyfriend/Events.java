package me.jasonhorkles.fancyfriend;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("DataFlowIssue")
public class Events extends ListenerAdapter {
    private final Map<Long, Integer> warnings = new HashMap<>();
    private final Path pingFilePath = Path.of("FancyFriend/ping-settings.json");

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + event.getMember()
            .getEffectiveName() + " used the /" + event.getName() + " command");

        if (event.getName().equalsIgnoreCase("noping")) {
            if (!new Utils().isStaff(event.getMember())) {
                event.reply("Invalid permissions!").setEphemeral(true).queue();
                return;
            }

            try {
                JSONObject pingSettings = new JSONObject(Files.readString(pingFilePath));

                switch (event.getOption("option").getAsString()) {
                    case "all" -> {
                        pingSettings.remove(String.valueOf(event.getMember().getIdLong()));
                        Files.writeString(pingFilePath, pingSettings.toString());
                        event.reply("You are now protected from all pings.").setEphemeral(true).queue();
                    }

                    case "explicit" -> {
                        pingSettings.put(String.valueOf(event.getMember().getIdLong()), 1);
                        Files.writeString(pingFilePath, pingSettings.toString());
                        event.reply("You are now protected from explicit pings only.").setEphemeral(true)
                            .queue();
                    }

                    case "off" -> {
                        pingSettings.put(String.valueOf(event.getMember().getIdLong()), 0);
                        Files.writeString(pingFilePath, pingSettings.toString());
                        event.reply("You are no longer protected from pings.").setEphemeral(true).queue();
                    }
                }
            } catch (IOException e) {
                System.out.print(new Utils().getTime(Utils.LogColor.RED));
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        // Ping check
        if (!event.getMessage().getChannelType().isGuild()) return;
        // Let staff ping other staff
        if (new Utils().isStaff(event.getMember())) return;

        Long id = event.getAuthor().getIdLong();

        int count = 0;
        // Iterate through each pinged member
        for (Member member : event.getMessage().getMentions().getMembers())
            if (new Utils().isStaff(member)) try {
                JSONObject pingSettings = new JSONObject(Files.readString(pingFilePath));
                // Default to -1 if the user doesn't have a setting
                if (pingSettings.has(String.valueOf(id))) {
                    // -1: all | 0: off | 1: explicit
                    int pingSetting = pingSettings.getInt(String.valueOf(id));
                    if (pingSetting == 0) continue;
                    if (pingSetting == 1 && event.getMessage().getMessageReference() != null) continue;
                }
                count++;
            } catch (IOException e) {
                System.out.print(new Utils().getTime(Utils.LogColor.RED));
                e.printStackTrace();
            }

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

            event.getMessage().reply(message).queue(sentMsg -> sentMsg.delete().queueAfter(
                15,
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
            if (warnings.get(id) >= 3) event.getMember().timeoutFor(3, TimeUnit.HOURS).queue(
                null,
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
}
