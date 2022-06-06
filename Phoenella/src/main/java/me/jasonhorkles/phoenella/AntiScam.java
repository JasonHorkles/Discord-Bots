package me.jasonhorkles.phoenella;

import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("ConstantConditions")
public class AntiScam extends ListenerAdapter {
    private final Map<String, Integer> attempts = new HashMap<>();

    private static final String guild = "729083627308056597";
    private static final String modRole = "751166721624375435";
    private static final Long modChannel = 893184802084225115L;

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        String rawMessage = event.getMessage().getContentRaw();
        if (!rawMessage.toLowerCase().contains("free") || !rawMessage.toLowerCase().contains("nitro")) return;

        String id = event.getAuthor().getId();
        if (event.isFromType(ChannelType.PRIVATE) && event.getAuthor().getMutualGuilds().toString().contains(guild)) {
            event.getJDA().getTextChannelById(modChannel).sendMessage(
                    "<@&" + modRole + "> **Potential scam from " + event.getAuthor()
                        .getAsMention() + " in DMs detected!**\n```\n" + rawMessage + "```")
                .setActionRow(Button.danger("kick:" + id, "Kick " + event.getAuthor().getName())).queue();
            return;
        }

        if (!event.isFromGuild()) return;
        if (!event.getGuild().getId().equals(guild)) return;
        Member member = event.getMember();
        if (member.getUser().isBot()) return;

        String name = event.getMember().getEffectiveName();
        if (attempts.containsKey(id)) {
            attempts.put(id, attempts.get(id) + 1);
            if (attempts.get(id) >= 3) {
                event.getMember().timeoutFor(24, TimeUnit.HOURS).queue();
                event.getMessage().reply("Potential scam detected. Please wait for the mods to decide your fate...")
                    .mentionRepliedUser(true).queue();
                event.getJDA().getTextChannelById(modChannel).sendMessage(
                        "<@&" + modRole + "> **Potential scam from " + event.getAuthor()
                            .getAsMention() + " detected!**\n```\n" + rawMessage + "```")
                    .setActionRow(Button.danger("softban:" + id, "Softban " + name),
                        Button.success("pardon:" + id, "False positive")).queue();
            }
        } else attempts.put(id, 1);

        System.out.println(
            new Utils().getTime(Utils.LogColor.YELLOW) + "Potential Nitro scam from " + name + " - " + attempts.get(
                id) + "/3");

        Executors.newSingleThreadScheduledExecutor().schedule(() -> takeWarning(id, name), 10, TimeUnit.MINUTES);
    }

    private void takeWarning(String id, String name) {
        if (attempts.get(id) <= 1) {
            attempts.remove(id);
            System.out.println(new Utils().getTime(
                Utils.LogColor.GREEN) + "Potential Nitro scam attempt from " + name + " removed - 0/3");
        } else {
            attempts.put(id, attempts.get(id) - 1);
            System.out.println(new Utils().getTime(
                Utils.LogColor.GREEN) + "Potential Nitro scam attempt from " + name + " removed - " + attempts.get(
                id) + "/3");
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (event.getComponentId().startsWith("softban:")) try {
            Message message = event.getMessage();
            message.editMessageComponents(
                    ActionRow.of(message.getButtons().get(0).asDisabled(), message.getButtons().get(1).asDisabled()))
                .queue();

            Member member = event.getGuild().getMemberById(event.getComponentId().replace("softban:", ""));
            User user = member.getUser();
            event.getGuild().removeTimeout(member).queue();
            event.getGuild().ban(user, 1, "Discord Nitro scam").queue();
            event.getGuild().unban(user).queueAfter(500, TimeUnit.MILLISECONDS);
            event.reply(event.getMember().getAsMention() + " successfully softbanned **" + user.getAsTag() + "**")
                .setEphemeral(false).mentionRepliedUser(false).queue();
        } catch (NullPointerException ignored) {
            event.reply("Unable to softban that person! Are they still in the server?").setEphemeral(true).queue();
        }

        else if (event.getComponentId().startsWith("pardon:")) try {
            Message message = event.getMessage();
            message.editMessageComponents(
                    ActionRow.of(message.getButtons().get(0).asDisabled(), message.getButtons().get(1).asDisabled()))
                .queue();

            Member member = event.getGuild().getMemberById(event.getComponentId().replace("pardon:", ""));
            User user = member.getUser();
            event.getGuild().removeTimeout(member).queue();
            event.reply(event.getMember().getAsMention() + " successfully pardoned **" + user.getAsTag() + "**")
                .setEphemeral(false).mentionRepliedUser(false).queue();
        } catch (NullPointerException ignored) {
            event.reply("Unable to pardon that person! Are they still in the server?").setEphemeral(true).queue();
        }

        else if (event.getComponentId().startsWith("kick:")) try {
            Message message = event.getMessage();
            message.editMessageComponents(ActionRow.of(message.getButtons().get(0).asDisabled())).queue();

            Member member = event.getGuild().getMemberById(event.getComponentId().replace("kick:", ""));
            User user = member.getUser();
            event.getGuild().kick(member, "Discord Nitro scam").queue();
            event.reply(event.getMember().getAsMention() + " successfully kicked **" + user.getAsTag() + "**")
                .setEphemeral(false).mentionRepliedUser(false).queue();
        } catch (NullPointerException ignored) {
            event.reply("Unable to kick that person! Are they still in the server?").setEphemeral(true).queue();
        }
    }
}

