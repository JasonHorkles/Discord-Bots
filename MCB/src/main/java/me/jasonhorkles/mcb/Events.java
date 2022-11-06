package me.jasonhorkles.mcb;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.ErrorResponse;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public class Events extends ListenerAdapter {
    private final Map<Long, Integer> warnings = new HashMap<>();

    // Forums
    private static final Long showBuilds = 1023721665332523098L;
    private static final Long buildAdvice = 1023721017144786945L;
    private static final Long buildRequests = 1023709419017617438L;
    private static final Long[] forums = new Long[]{showBuilds, buildAdvice, buildRequests};

    // Channels
    private static final Long letsPlay = 688770749191815323L;
    private static final Long hire = 625728304410001410L;
    private static final Long showServer = 603881733322047508L;
    private static final Long youtube = 692712729273696326L;
    private static final Long shopping = 615208624855449613L;
    //    private static final Long logs = 603585853444456449L;
    private static final Long[] channels = new Long[]{letsPlay, hire, showServer, youtube, shopping};


    @SuppressWarnings("ConstantConditions")
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        // Give starter builder role
        if (event.getMessage().getChannel().getIdLong() == 1023721665332523098L)
            if (!event.getMessage().getAttachments().isEmpty() || event.getMessage().getContentStripped().toLowerCase()
                .contains("http")) if (!event.getMember().getRoles().toString().contains("646293661729947658"))
                event.getGuild().addRoleToMember(event.getMember(), event.getGuild().getRoleById(646293661729947658L))
                    .queue();

        // Ping check
        if (event.getMember().getRoles().toString().contains("646291178144399371")) return;
        if (!event.getMessage().getChannelType().isGuild()) return;

        Long id = event.getAuthor().getIdLong();

        int count = 0;
        for (Member member : event.getMessage().getMentions().getMembers())
            if (member.getRoles().toString().contains("646291178144399371")) count++;

        if (count > 0) {
            warn(id, event, count);
            StringBuilder message = new StringBuilder(
                "Hey " + event.getMember().getEffectiveName() + ", please don't ping staff members!");

            boolean appendTicket = true;

            if (event.getMessage().getChannelType() == ChannelType.TEXT)
                if (event.getMessage().getCategory().getIdLong() == 720690619822899231L) appendTicket = false;

            if (appendTicket) message.append(
                "\nIf there's a problem, make a ticket by typing `/tickets open <subject>` in any channel.");

            if (warnings.get(id) > 1) message.append("\n\n*Warning ").append(warnings.get(id)).append("/3*");

            event.getMessage().reply(message).queue(message1 -> message1.delete().queueAfter(15, TimeUnit.MINUTES, null,
                new ErrorHandler().handle(ErrorResponse.UNKNOWN_MESSAGE, (e) -> System.out.println(
                    new Utils().getTime(Utils.LogColor.RED) + "Unable to delete warning message."))));
        }
    }

    @SuppressWarnings("ConstantConditions")
    public void warn(Long id, MessageReceivedEvent event, int count) {
        if (warnings.containsKey(id)) {
            warnings.put(id, warnings.get(id) + count);
            if (warnings.get(id) >= 3) event.getMember().timeoutFor(12, TimeUnit.HOURS).queue(null,
                (na) -> event.getChannel().sendMessage("<@277291758503723010>").queue((del) -> del.delete()
                    .queueAfter(15, TimeUnit.SECONDS, null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE))));
        } else warnings.put(id, count);

        System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Warned " + event.getMember()
            .getEffectiveName() + " for ping spam - " + warnings.get(id) + "/3");

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
        Executors.newSingleThreadScheduledExecutor().schedule(() -> takeWarning(id, name), 15, TimeUnit.MINUTES);
    }

    private void takeWarning(Long id, String name) {
        if (warnings.get(id) <= 1) {
            warnings.remove(id);
            System.out.println(
                new Utils().getTime(Utils.LogColor.GREEN) + "Removed ping spam warning from " + name + " - 0/3");
        } else {
            warnings.put(id, warnings.get(id) - 1);
            System.out.println(new Utils().getTime(
                Utils.LogColor.GREEN) + "Removed ping spam warning from " + name + " - " + warnings.get(id) + "/3");
        }
    }

    // When recent chatter leaves
    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
        System.out.println("\n" + new Utils().getTime(Utils.LogColor.YELLOW) + event.getUser().getName() + " left!");

        int count = 0;

        for (Long channelId : forums)
            //noinspection ConstantConditions
            for (ThreadChannel thread : event.getJDA().getChannelById(ForumChannel.class, channelId)
                .getThreadChannels()) {
                if (thread.isArchived()) continue;

                System.out.println(
                    new Utils().getTime(Utils.LogColor.YELLOW) + "Checking post '" + thread.getName() + "'");

                if (thread.getOwnerIdLong() == event.getUser().getIdLong()) {
                    EmbedBuilder embed = new EmbedBuilder();
                    embed.setTitle("Original poster " + event.getUser().getAsTag() + " has left the server");
                    embed.setDescription(event.getUser().getAsMention());
                    embed.setFooter("This post will now be closed and locked");
                    embed.setThumbnail(event.getUser().getAvatarUrl());
                    embed.setColor(new Color(255, 100, 0));

                    thread.sendMessageEmbeds(embed.build()).queue(
                        (na) -> thread.getManager().setArchived(true).setLocked(true).queueAfter(1, TimeUnit.SECONDS));
                    count++;
                }
            }

        System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + "Closed and locked " + count + " posts!");

        deleteMessages(event.getUser());
    }

    @SuppressWarnings("ConstantConditions")
    public void deleteMessages(User author) {
        AtomicInteger count = new AtomicInteger();

        for (Long channelId : channels) {
            TextChannel channel = MCB.jda.getTextChannelById(channelId);
            System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Checking #" + channel.getName() + "...");

            try {
                for (Message message : new Utils().getMessages(channel, 25).get(45, TimeUnit.SECONDS)) {
                    if (message.getAuthor().getIdLong() != author.getIdLong()) continue;

                    message.delete().queue();
                    count.getAndIncrement();
                }

            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                System.out.print(new Utils().getTime(Utils.LogColor.RED));
                e.printStackTrace();
            }
        }

        System.out.println(new Utils().getTime(
            Utils.LogColor.GREEN) + "Deleted " + count + " messages from " + author.getAsTag() + "!\n");
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equalsIgnoreCase("enginehub")) {
            @SuppressWarnings("ConstantConditions") boolean ephemeral = !event.getMember().getRoles().toString()
                .contains("646291178144399371");

            event.reply("Join the EngineHub Discord for WorldEdit/WorldGuard support at https://discord.gg/enginehub")
                .setEphemeral(ephemeral).queue();
        }
    }
}
