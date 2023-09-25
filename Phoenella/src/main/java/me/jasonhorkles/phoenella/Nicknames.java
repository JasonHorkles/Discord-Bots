package me.jasonhorkles.phoenella;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public class Nicknames extends ListenerAdapter {
    private final Guild guild;
    private final Role role;

    public Nicknames() {
        this.guild = Phoenella.jda.getGuildById(729083627308056597L);
        this.role = Phoenella.jda.getRoleById(1144676839588106360L);
    }

    @Override
    public void onGuildMemberUpdateNickname(GuildMemberUpdateNicknameEvent event) {
        if (event.getUser().isBot()) return;

        Member member = event.getMember();
        String newNickname = event.getNewNickname();

        System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + member.getUser()
            .getName() + " changed their nickname from '" + event.getOldNickname() + "' to '" + newNickname + "'");

        runNameCheckForUser(newNickname, member, false);
    }

    public void runNameCheckForUser(String newNickname, Member member, boolean isStartup) {
        Role coach = guild.getRoleById(729108220479537202L);
        Role mod = guild.getRoleById(751166721624375435L);
        if (member.getRoles().contains(coach) || member.getRoles().contains(mod)) {
            System.out.println(new Utils().getTime(
                Utils.LogColor.YELLOW) + "Skipping checking " + member.getEffectiveName() + "...");
            return;
        }

        User user = member.getUser();
        try {
            Path dataPath = Path.of("Phoenella/nickname-warnings.json");
            JSONArray usersData = new JSONArray(Files.readString(dataPath));

            JSONObject userData = null;
            int index = 0;
            for (int x = 0; x < usersData.length(); x++) {
                JSONObject userJSON = usersData.getJSONObject(x);
                if (userJSON.getLong("id") == user.getIdLong()) {
                    userData = userJSON;
                    index = x;
                    break;
                }
            }

            if (hasGoodNickname(newNickname)) {
                addRoleToMember(member);

                // Remove user from data if they have a good nickname
                if (userData != null) {
                    usersData.remove(index);
                    Files.writeString(dataPath, usersData.toString());
                }

            } else {
                removeRoleFromMember(member);

                if (isStartup) {
                    int daysLeft = 3;
                    long cooldownTill = 0;
                    if (userData != null) {
                        daysLeft = userData.getInt("daysLeft") - 1;
                        cooldownTill = userData.getLong("cooldownTill");
                    }

                    if (System.currentTimeMillis() < cooldownTill) {
                        System.out.println(new Utils().getTime(
                            Utils.LogColor.YELLOW) + "Skipping messaging " + member.getEffectiveName() + " because they are on cooldown!");
                        return;
                    }

                    // Kick user if noncompliant
                    if (daysLeft == 0) {
                        user.openPrivateChannel().flatMap(channel -> channel.sendMessage(
                                "You have been kicked from the Phoenix Gaming server for not changing your nickname in time."))
                            .queue(null,
                                new ErrorHandler().handle(ErrorResponse.CANNOT_SEND_TO_USER, (na) -> {
                                    //noinspection DataFlowIssue
                                    guild.getTextChannelById(893184802084225115L).sendMessage(
                                            ":warning: Couldn't message " + user.getAsMention() + " that they were kicked! Are their DMs off?")
                                        .queue();
                                }));
                        member.kick().queueAfter(1, TimeUnit.SECONDS);

                        System.out.println(new Utils().getTime(
                            Utils.LogColor.YELLOW) + "Kicked " + user.getName() + " for not changing their nickname!");

                        EmbedBuilder embed = new EmbedBuilder();
                        embed.setTitle(
                            user.getName() + " was kicked for not changing their nickname in time!");
                        embed.setDescription(user.getAsMention());
                        embed.setThumbnail(user.getAvatarUrl());
                        embed.setColor(new Color(255, 200, 0));

                        //noinspection DataFlowIssue
                        guild.getTextChannelById(893184802084225115L).sendMessageEmbeds(embed.build())
                            .queue();

                        // Remove user data
                        usersData.remove(index);
                        Files.writeString(dataPath, usersData.toString());
                        return;
                    }

                    // Save user data
                    cooldownTill = System.currentTimeMillis() + 82800000L;
                    if (userData == null) {
                        userData = new JSONObject();
                        userData.put("id", user.getIdLong());
                        userData.put("daysLeft", daysLeft);
                        userData.put("cooldownTill", cooldownTill);
                        usersData.put(userData);
                    } else {
                        userData.put("daysLeft", daysLeft);
                        userData.put("cooldownTill", cooldownTill);
                    }
                    Files.writeString(dataPath, usersData.toString());

                    String s = daysLeft == 1 ? "" : "s";
                    String message = "Your nickname is not in the correct format in the Phoenix Gaming server! Please see https://discord.com/channels/729083627308056597/741088695498571786/1144702255996612628 and change it as soon as you can. If you're stuck, please DM <@277291758503723010>.\n\n*You will be kicked in " + daysLeft + " day" + s + " if you do not change your nickname.*";
                    user.openPrivateChannel().flatMap(channel -> channel.sendMessage(message))
                        .queue(null, new ErrorHandler().handle(ErrorResponse.CANNOT_SEND_TO_USER, (na) -> {
                            //noinspection DataFlowIssue
                            guild.getTextChannelById(893184802084225115L).sendMessage(
                                    ":warning: Couldn't message " + member.getAsMention() + " to change their nickname! Are their DMs off?")
                                .queue();
                        }));
                    System.out.println(new Utils().getTime(
                        Utils.LogColor.YELLOW) + "Messaged " + member.getEffectiveName() + " to change their nickname! (" + daysLeft + " days left)");
                }
            }
        } catch (IOException e) {
            System.out.print(new Utils().getTime(Utils.LogColor.RED));
            e.printStackTrace();
            //noinspection DataFlowIssue
            guild.getTextChannelById(893184802084225115L).sendMessage(
                    ":warning: **ERROR:** Failed to get user data for " + member.getAsMention() + "'s nickname! <@277291758503723010>")
                .queue();
        }
    }

    public void runNameCheckForGuild(boolean isStartup) {
        if (!isStartup)
            // Reload members
            //noinspection DataFlowIssue
            Phoenella.jda.getGuildById(729083627308056597L).loadMembers().get();

        for (Member member : guild.getMembers()) {
            if (member.getUser().isBot()) continue;

            System.out.println(
                new Utils().getTime(Utils.LogColor.GREEN) + "Checking " + member.getEffectiveName() + "...");

            runNameCheckForUser(member.getEffectiveName(), member, true);
        }
    }

    public void addRoleToMember(Member member) {
        if (!member.getRoles().contains(role)) {
            System.out.println(new Utils().getTime(
                Utils.LogColor.YELLOW) + "Adding " + role.getName() + " role to '" + member.getEffectiveName() + "'");
            guild.addRoleToMember(member, role).queue();
        }
    }

    public void removeRoleFromMember(Member member) {
        if (member.getRoles().contains(role)) {
            System.out.println(new Utils().getTime(
                Utils.LogColor.YELLOW) + "Removing " + role.getName() + " role from '" + member.getEffectiveName() + "'");
            guild.removeRoleFromMember(member, role).queue();
        }
    }

    private boolean hasGoodNickname(String nickname) {
        if (nickname == null) return false;
        return nickname.contains("(") && nickname.contains(")") && nickname.contains(" ");
    }
}
