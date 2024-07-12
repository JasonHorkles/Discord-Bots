package me.jasonhorkles.stormalerts;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("DataFlowIssue")
public class Events extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        System.out.println(Utils.getTime(Utils.LogColor.GREEN) + event.getMember()
            .getEffectiveName() + " used the /" + event.getName() + " command");

        switch (event.getName().toLowerCase()) {
            case "checknow" -> Utils.updateNow(event);
            case "updaterecords" -> {
                event.reply("Updating records...").setEphemeral(true).queue();
                new Records().checkRecords();
            }
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        switch (event.getComponentId()) {
            case "viewroles" -> {
                event.deferReply(true).queue();

                StringBuilder roleList = new StringBuilder();
                StringBuilder notRoleList = new StringBuilder();
                for (SelectOption option : ((StringSelectMenu) event.getMessage().getActionRows().getFirst()
                    .getComponents().getFirst()).getOptions()) {
                    Role role = event.getGuild().getRoleById(option.getValue());

                    if (event.getMember().getRoles().contains(role))
                        roleList.append(role.getAsMention()).append("\n");
                    else notRoleList.append(role.getAsMention()).append("\n");
                }

                if (roleList.isEmpty()) roleList.append("None");
                if (notRoleList.isEmpty()) notRoleList.append("None");

                EmbedBuilder embed = new EmbedBuilder();
                embed.addField("You have", roleList.toString(), true);
                embed.addBlankField(true);
                embed.addField("You don't have", notRoleList.toString(), true);
                embed.setColor(new Color(43, 45, 49));

                event.getHook().editOriginalEmbeds(embed.build()).queue();
            }

            case "viewchanges" -> {
                event.deferReply(true).queue();

                Message message = event.getMessage();
                if (message.getEmbeds().isEmpty()) {
                    event.getHook().editOriginal("There are no changes to view!").queue();
                    return;
                }

                MessageEmbed oldEmbed = message.getEmbeds().getFirst();
                EmbedBuilder embed = new EmbedBuilder(message.getEmbeds().getFirst());
                embed.setDescription(oldEmbed.getDescription().replace("||", "~~"));

                List<MessageEmbed.Field> fields = new ArrayList<>(oldEmbed.getFields());
                fields.set(0, new MessageEmbed.Field(
                    oldEmbed.getFields().getFirst().getName(),
                    oldEmbed.getFields().getFirst().getValue().replace("||", "~~"),
                    false));

                embed.clearFields();
                for (MessageEmbed.Field field : fields) embed.addField(field);

                event.getHook().editOriginalEmbeds(embed.build()).queue();
            }
        }
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (event.getComponentId()) {
            case "role-select" -> {
                if (event.getSelectedOptions().isEmpty()) {
                    event.deferEdit().queue();
                    return;
                }

                event.deferReply(true).queue();
                Guild guild = event.getGuild();
                Member member = event.getMember();

                new Thread(() -> {
                    for (SelectOption option : event.getSelectedOptions()) {
                        Role role = guild.getRoleById(option.getValue());
                        if (member.getRoles().contains(role)) {
                            System.out.println(Utils.getTime(Utils.LogColor.YELLOW) + "Removing " + role
                                .getName().toLowerCase() + " role from '" + member.getEffectiveName() + "'");
                            guild.removeRoleFromMember(member, role).complete();

                        } else {
                            System.out.println(Utils.getTime(Utils.LogColor.YELLOW) + "Adding " + role
                                .getName().toLowerCase() + " role to '" + member.getEffectiveName() + "'");
                            guild.addRoleToMember(member, role).complete();
                        }
                    }

                    // Show user's roles
                    StringBuilder roleList = new StringBuilder();
                    StringBuilder notRoleList = new StringBuilder();
                    for (SelectOption option : ((StringSelectMenu) event.getMessage().getActionRows()
                        .getFirst().getComponents().getFirst()).getOptions()) {
                        Role role = event.getGuild().getRoleById(option.getValue());

                        if (event.getMember().getRoles().contains(role))
                            roleList.append(role.getAsMention()).append("\n");
                        else notRoleList.append(role.getAsMention()).append("\n");
                    }

                    if (roleList.isEmpty()) roleList.append("None");
                    if (notRoleList.isEmpty()) notRoleList.append("None");

                    EmbedBuilder embed = new EmbedBuilder();
                    embed.addField("You have", roleList.toString(), true);
                    embed.addBlankField(true);
                    embed.addField("You don't have", notRoleList.toString(), true);
                    embed.setColor(new Color(43, 45, 49));

                    event.getHook().editOriginalEmbeds(embed.build()).queue();
                }, "Add Roles - " + member.getEffectiveName()).start();
            }
        }
    }

    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
        if (event.getGuild().getIdLong() != 843919716677582888L) return;

        User user = event.getUser();
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle(user.getName() + " has left the server");
        embed.setDescription(user.getAsMention());
        embed.setThumbnail(user.getAvatarUrl());
        embed.setColor(new Color(255, 200, 0));

        event.getJDA().getTextChannelById(1093060038265950238L).sendMessageEmbeds(embed.build()).queue();
    }
}
