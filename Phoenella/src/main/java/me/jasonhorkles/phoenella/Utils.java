package me.jasonhorkles.phoenella;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.annotation.Nullable;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;

@SuppressWarnings("ConstantConditions")
public class Utils {
    public static final ArrayList<ScheduledFuture<?>> schedules = new ArrayList<>();

    public enum LogColor {
        RED("\u001B[31m"), YELLOW("\u001B[33m"), GREEN("\u001B[32m");

        private final String logColor;

        LogColor(String logColor) {
            this.logColor = logColor;
        }

        public String getLogColor() {
            return logColor;
        }
    }

    public String getTime(LogColor logColor) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("hh:mm:ss a");
        return logColor.getLogColor() + "[" + dtf.format(LocalDateTime.now()) + "] ";
    }

    public CompletableFuture<List<Message>> getMessages(MessageChannel channel, int count) {
        return channel.getIterableHistory().takeAsync(count).thenApply(ArrayList::new);
    }

    public void runNameCheckForUser(String newNickname, Member member, Guild guild) {
        if (hasGoodNickname(newNickname)) if (newNickname.toLowerCase().contains("(parent)")) {
            addRoleToMember(member, guild, RoleType.PARENT);
            nonStudentNick(member, guild);
        } else {
            removeRoleFromMember(member, guild, RoleType.PARENT);
            addRoleToMember(member, guild, RoleType.STUDENT);
        }
        else {
            removeRoleFromMember(member, guild, RoleType.PARENT);
            nonStudentNick(member, guild);
        }
    }

    public void runNameCheckForGuild(Guild guild) {
        for (Member member : guild.getMembers()) {
            if (member.getUser().isBot()) continue;
            System.out.println(getTime(LogColor.GREEN) + "Checking " + member.getEffectiveName() + "...");

            if (hasGoodNickname(member.getEffectiveName()))
                if (member.getEffectiveName().toLowerCase().contains("(parent)")) {
                    addRoleToMember(member, guild, RoleType.PARENT);
                    nonStudentNick(member, guild);
                } else {
                    removeRoleFromMember(member, guild, RoleType.PARENT);
                    addRoleToMember(member, guild, RoleType.STUDENT);
                }
            else {
                removeRoleFromMember(member, guild, RoleType.PARENT);
                nonStudentNick(member, guild);
            }
        }
    }

    private void nonStudentNick(Member member, Guild guild) {
        // If nickname is invalid remove student role
        removeRoleFromMember(member, guild, RoleType.STUDENT);
        removeRoleFromMember(member, guild, RoleType.BUTTON);
    }

    public enum RoleType {
        STUDENT("892267754730709002"), BUTTON("892453842241859664"), PARENT("892853778829692968");

        private final String role;

        RoleType(String role) {
            this.role = role;
        }

        public String getRole() {
            return role;
        }
    }

    public void addRoleToMember(Member member, Guild guild, RoleType roleType) {
        Role role = guild.getRoleById(roleType.getRole());

        if (!member.getRoles().contains(role)) {
            System.out.println(getTime(LogColor.GREEN) + "Adding " + roleType.toString()
                .toLowerCase() + " role to '" + member.getEffectiveName() + "'");
            guild.addRoleToMember(member, role).queue();
        }
    }

    public void removeRoleFromMember(Member member, Guild guild, RoleType roleType) {
        Role role = guild.getRoleById(roleType.getRole());

        if (member.getRoles().contains(role)) {
            System.out.println(getTime(LogColor.GREEN) + "Removing " + roleType.toString()
                .toLowerCase() + " role from '" + member.getEffectiveName() + "'");
            guild.removeRoleFromMember(member, role).queue();
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

    public String getFirstName(@Nullable Member member) {
        if (member == null) return "null";
        return member.getEffectiveName().replaceAll(" .*", "").replaceAll("\\(.*", "").trim();
    }

    public String getFullName(@Nullable Member member) {
        if (member == null) return "null";
        return member.getEffectiveName().replaceAll("\\(.*", "").trim();
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

    public void updateDailyWordle() {
        try {
            Scanner words = new Scanner(new File("Phoenella/Wordle/words.txt"));
            ArrayList<String> wordList = new ArrayList<>();
            while (words.hasNext()) try {
                wordList.add(words.next());
            } catch (NoSuchElementException ignored) {
            }

            Random r = new Random();
            String word = wordList.get(r.nextInt(wordList.size()));

            FileWriter daily = new FileWriter("Phoenella/Wordle/daily.txt", false);
            daily.write(word);
            daily.close();

            FileWriter plays = new FileWriter("Phoenella/Wordle/played-daily.txt", false);
            plays.close();

            System.out.println(getTime(LogColor.GREEN) + "Updated the daily Wordle!");
        } catch (IOException e) {
            System.out.print(getTime(LogColor.RED));
            System.out.print(getTime(LogColor.RED));
            e.printStackTrace();
        }
    }

    public MessageEmbed defineWord(String word) {
        word = word.replaceAll(" .*", "");
        EmbedBuilder embed = new EmbedBuilder();

        try {
            InputStream url;

            try {
                url = new URL("https://api.dictionaryapi.dev/api/v2/entries/en/" + word).openStream();
            } catch (FileNotFoundException ignored) {
                embed.setColor(new Color(212, 43, 65));
                embed.setDescription("Couldn't find **" + word + "** in the dictionary!");
                return embed.build();
            }

            JSONObject obj = new JSONArray(
                new Scanner(url, StandardCharsets.UTF_8).useDelimiter("\\A").nextLine()).getJSONObject(0);
            url.close();

            String receivedWord = obj.getString("word");
            String phonetic = null;
            if (obj.has("phonetic")) phonetic = obj.getString("phonetic");
            StringBuilder definitions = new StringBuilder();

            JSONArray meanings = new JSONArray(obj.getJSONArray("meanings"));
            for (int x = 0; x < meanings.length() && x < 3; x++) {
                if (x > 0) definitions.append("\n\n");
                JSONObject info = meanings.getJSONObject(x);
                definitions.append("**").append(info.getString("partOfSpeech").toUpperCase()).append(":**");

                JSONArray rawDefinitions = info.getJSONArray("definitions");
                int iterations = 0;
                for (int y = 0; y < rawDefinitions.length() && iterations < 2; y++) {
                    String newDefinition = rawDefinitions.getJSONObject(y).getString("definition");
                    if (containsBadWord(newDefinition)) continue;

                    definitions.append("\n• ").append(newDefinition);
                    iterations++;
                }
            }

            embed.setColor(new Color(15, 157, 88));
            embed.setAuthor(receivedWord.toUpperCase() + " DEFINITION");
            if (phonetic != null) embed.setTitle(phonetic);
            embed.setDescription(definitions);

        } catch (IOException e) {
            System.out.print(getTime(LogColor.RED));
            e.printStackTrace();

            embed.setColor(new Color(212, 43, 65));
            embed.setDescription("Failed to search dictionary for word **" + word + "**! Please try again later.");
        }

        return embed.build();
    }

    /* BAD WORD LIST BELOW */

































    /* BAD WORD LIST BELOW */

    public boolean containsBadWord(String phrase) {
        String[] badWords = {"\\bsex\\b", "\\bass\\b", "bitch", "fuck", "breast"};

        for (String word : badWords) {
            String temp = phrase.replaceAll(word, "REDACTED");
            if (temp.contains("REDACTED")) return true;
        }

        return false;
    }
}
