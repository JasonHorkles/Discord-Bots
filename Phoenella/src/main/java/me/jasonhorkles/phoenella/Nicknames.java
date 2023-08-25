package me.jasonhorkles.phoenella;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

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

        runNameCheckForUser(newNickname, member);
    }

    public void runNameCheckForUser(String newNickname, Member member) {
        if (hasGoodNickname(newNickname)) addRoleToMember(member);
        else removeRoleFromMember(member);
    }

    public void runNameCheckForGuild() {
        for (Member member : guild.getMembers()) {
            if (member.getUser().isBot()) continue;

            System.out.println(
                new Utils().getTime(Utils.LogColor.GREEN) + "Checking " + member.getEffectiveName() + "...");

            runNameCheckForUser(member.getEffectiveName(), member);
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
