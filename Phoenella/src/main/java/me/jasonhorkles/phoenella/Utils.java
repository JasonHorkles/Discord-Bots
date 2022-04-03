package me.jasonhorkles.phoenella;

import net.dv8tion.jda.api.entities.*;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.*;

@SuppressWarnings("ConstantConditions")
public class Utils {
    public static ArrayList<ScheduledFuture<?>> schedules = new ArrayList<>();

    public enum Color {
        RED("\u001B[31m"), YELLOW("\u001B[33m"), GREEN("\u001B[32m");

        private final String color;

        Color(String color) {
            this.color = color;
        }

        public String getColor() {
            return color;
        }
    }

    public String getTime(Color color) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("hh:mm:ss a");
        return color.getColor() + "[" + dtf.format(LocalDateTime.now()) + "] ";
    }

    public CompletableFuture<List<Message>> getMessages(MessageChannel channel, int count) {
        return channel.getIterableHistory().takeAsync(count).thenApply(ArrayList::new);
    }

    public void runNameCheckForUser(String newNickname, Member member, Guild guild) {
        if (hasGoodNickname(newNickname)) if (newNickname.toLowerCase().contains("(parent)")) {
            addRoleToMember(member, guild, "parent");
            nonStudentNick(member, guild);
        } else {
            removeRoleFromMember(member, guild, "parent");
            addRoleToMember(member, guild, "student");
        }
        else {
            removeRoleFromMember(member, guild, "parent");
            nonStudentNick(member, guild);
        }
    }

    public void runNameCheckForGuild(Guild guild) {
        for (Member member : guild.getMembers()) {
            System.out.println(
                new Utils().getTime(Utils.Color.GREEN) + "Checking " + member.getEffectiveName() + "...");

            // If not sushed
            if (!member.getRoles().toString().contains("842490529744945192"))
                if (hasGoodNickname(member.getEffectiveName()))
                    if (member.getEffectiveName().toLowerCase().contains("(parent)")) {
                        addRoleToMember(member, guild, "parent");
                        nonStudentNick(member, guild);
                    } else {
                        removeRoleFromMember(member, guild, "parent");
                        addRoleToMember(member, guild, "student");
                    }
                else {
                    removeRoleFromMember(member, guild, "parent");
                    nonStudentNick(member, guild);
                }
        }
    }

    private void nonStudentNick(Member member, Guild guild) {
        // If nickname is invalid, remove their reactions
        Thread removeReactions = new Thread(() -> {
            try {
                for (MessageReaction msgReactions : new Utils().getMessages(
                    guild.getTextChannelById(892104640567578674L), 1).get(60, TimeUnit.SECONDS).get(0).getReactions())
                    for (User reactionUsers : msgReactions.retrieveUsers().complete())
                        if (reactionUsers == member.getUser()) msgReactions.removeReaction(reactionUsers).queue();
            } catch (InterruptedException | ExecutionException | TimeoutException ignored) {
            }
        });
        removeReactions.start();

        removeRoleFromMember(member, guild, "button");
        removeRoleFromMember(member, guild, "student");
    }

    private void addRoleToMember(Member member, Guild guild, String role) {
        String roleName = role;

        switch (role) {
            case "student" -> role = "892267754730709002";
            case "button" -> role = "892453842241859664";
            case "parent" -> role = "892853778829692968";
        }

        if (!member.getRoles().toString().contains(role)) {
            System.out.println(new Utils().getTime(
                Utils.Color.GREEN) + "Adding role '" + roleName + "' to '" + member.getEffectiveName() + "'");
            guild.addRoleToMember(member, guild.getRoleById(role)).queue();
        }
    }

    private void removeRoleFromMember(Member member, Guild guild, String role) {
        String roleName = role;

        switch (role) {
            case "student" -> role = "892267754730709002";
            case "button" -> role = "892453842241859664";
            case "parent" -> role = "892853778829692968";
        }

        if (member.getRoles().toString().contains(role)) {
            System.out.println(new Utils().getTime(
                Utils.Color.GREEN) + "Removing role '" + roleName + "' from '" + member.getEffectiveName() + "'");
            guild.removeRoleFromMember(member, guild.getRoleById(role)).queue();
        }
    }

    private boolean hasGoodNickname(String nickname) {
        if (nickname == null) return false;
        return nickname.contains("(") && nickname.contains(")") && nickname.contains(" ");
    }

    public String lookUp(String message, String name) {
        try {
            String page = "https://api.wolframalpha.com/v1/result?i=" + URLEncoder.encode(message,
                StandardCharsets.UTF_8) + "&appid=2T7PHK-JPX3QAR42E";
            Connection conn = Jsoup.connect(page);
            Document doc = conn.get();
            message = doc.body().text().replace("Wolfram Alpha", "Phoenella").replace("Wolfram|Alpha", "")
                .replace("human", name);
            if (message.contains("69")) message = "nice";
            return message;
        } catch (IOException e) {
            if (e.getMessage().contains("Status=501")) return "501";
            else return "`ERROR: " + e.getMessage() + "`";
        }
    }

    public String getFirstName(Member member) {
        return member.getEffectiveName().replaceAll(" .*", "").replaceAll("\\(.*", "").trim();
    }

    public void sendMessage(@Nullable TextChannel channel, @Nullable Message replyTo, String message, Boolean allCaps) {
        if (allCaps) message = message.toUpperCase();
        else {
            Random r = new Random();
            if (r.nextBoolean()) message = message.toLowerCase();
        }

        if (channel != null) channel.sendMessage(message).queue();
        else if (replyTo != null) replyTo.reply(message).mentionRepliedUser(false).queue();
        else throw new NullPointerException("No TextChannel or Message defined");
    }

    public boolean shush(Member member, long duration) {
        File f = new File("Phoenella/Shush Data/" + member.getId() + ".txt");
        if (f.exists()) return false;
        else {
            Guild guild = member.getGuild();
            try {
                if (!f.createNewFile()) System.out.println(
                    new Utils().getTime(Utils.Color.RED) + "File '" + f.getName() + "' not created.");
                FileWriter fw = new FileWriter(f, false);
                fw.write(String.valueOf(System.currentTimeMillis() + duration));

                guild.addRoleToMember(member, guild.getRoleById(842490529744945192L)).queue();
                for (Role roles : member.getRoles()) {
                    fw.write("\n" + roles.getId());
                    guild.removeRoleFromMember(member, roles).complete();
                }
                fw.close();

                schedules.add(Executors.newSingleThreadScheduledExecutor()
                    .schedule(() -> unshush(member), duration, TimeUnit.MILLISECONDS));

                guild.getTextChannelById(893184802084225115L)
                    .sendMessage("Shushed " + member.getAsMention() + " for " + duration / 60000 + " minute(s)!")
                    .queue();
                return true;
            } catch (IOException e) {
                System.out.println(new Utils().getTime(Utils.Color.RED) + "[ERROR] Couldn't shush " + member.getUser()
                    .getAsTag() + "!");
                e.printStackTrace();
                guild.getTextChannelById(893184802084225115L)
                    .sendMessage("Couldn't shush " + member.getAsMention() + "!").queue();
                return false;
            }
        }
    }

    public void unshush(Member member) {
        File f = new File("Phoenella/Shush Data/" + member.getId() + ".txt");
        Guild guild = member.getGuild();
        if (f.exists()) try {
            Scanner scanner = new Scanner(f);
            scanner.nextLine();

            guild.removeRoleFromMember(member, guild.getRoleById(842490529744945192L)).queue();
            while (scanner.hasNextLine())
                guild.addRoleToMember(member, guild.getRoleById(scanner.nextLine())).complete();

            scanner.close();
            if (!f.delete())
                System.out.println(new Utils().getTime(Utils.Color.RED) + "File '" + f.getName() + "' not deleted.");

            guild.getTextChannelById(893184802084225115L).sendMessage("Un-shushed " + member.getAsMention() + "!")
                .queue();
        } catch (IOException e) {
            System.out.println(new Utils().getTime(Utils.Color.RED) + "[ERROR] Couldn't un-shush " + member.getUser()
                .getAsTag() + "!");
            e.printStackTrace();
            guild.getTextChannelById(893184802084225115L)
                .sendMessage("Couldn't un-shush " + member.getAsMention() + "!").queue();
        }

        runNameCheckForUser(member.getEffectiveName(), member, guild);
    }
}
