package me.jasonhorkles.phoenella;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
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
import java.util.concurrent.*;

@SuppressWarnings("ConstantConditions")
public class Utils {
    public static ArrayList<ScheduledFuture<?>> schedules = new ArrayList<>();

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

            // If not sushed
            if (!member.getRoles().toString().contains("842490529744945192"))
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
        // If nickname is invalid, remove their reactions
        Thread removeReactions = new Thread(() -> {
            try {
                for (MessageReaction msgReactions : getMessages(guild.getTextChannelById(892104640567578674L), 1).get(
                    60, TimeUnit.SECONDS).get(0).getReactions())
                    for (User reactionUsers : msgReactions.retrieveUsers().complete())
                        if (reactionUsers == member.getUser()) msgReactions.removeReaction(reactionUsers).queue();
            } catch (InterruptedException | ExecutionException | TimeoutException ignored) {
            }
        }, "Remove Reactions - " + getFirstName(member));
        removeReactions.start();

        removeRoleFromMember(member, guild, RoleType.BUTTON);
        removeRoleFromMember(member, guild, RoleType.STUDENT);
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

    private void addRoleToMember(Member member, Guild guild, RoleType roleType) {
        if (!member.getRoles().toString().contains(roleType.getRole())) {
            System.out.println(
                getTime(LogColor.GREEN) + "Adding role '" + roleType + "' to '" + member.getEffectiveName() + "'");
            guild.addRoleToMember(member, guild.getRoleById(roleType.getRole())).queue();
        }
    }

    private void removeRoleFromMember(Member member, Guild guild, RoleType roleType) {
        if (member.getRoles().toString().contains(roleType.getRole())) {
            System.out.println(
                getTime(LogColor.GREEN) + "Removing role '" + roleType + "' from '" + member.getEffectiveName() + "'");
            guild.removeRoleFromMember(member, guild.getRoleById(roleType.getRole())).queue();
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

    public boolean shush(Member member, long duration) {
        File f = new File("Phoenella/Shush Data/" + member.getId() + ".txt");
        if (f.exists()) return false;
        else {
            Guild guild = member.getGuild();
            try {
                if (!f.createNewFile())
                    System.out.println(getTime(LogColor.RED) + "File '" + f.getName() + "' not created.");
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
                System.out.println(
                    getTime(LogColor.RED) + "[ERROR] Couldn't shush " + member.getUser().getAsTag() + "!");
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
            if (!f.delete()) System.out.println(getTime(LogColor.RED) + "File '" + f.getName() + "' not deleted.");

            guild.getTextChannelById(893184802084225115L).sendMessage("Un-shushed " + member.getAsMention() + "!")
                .queue();
        } catch (IOException e) {
            System.out.println(
                getTime(LogColor.RED) + "[ERROR] Couldn't un-shush " + member.getUser().getAsTag() + "!");
            e.printStackTrace();
            guild.getTextChannelById(893184802084225115L)
                .sendMessage("Couldn't un-shush " + member.getAsMention() + "!").queue();
        }

        runNameCheckForUser(member.getEffectiveName(), member, guild);
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

    String value;

    public String getJsonKey(JSONObject json, String key, boolean firstRun) {
        boolean exists = json.has(key);
        Iterator<?> keys;
        String nextKeys;
        if (firstRun) value = "null";

        if (!exists) {
            keys = json.keys();

            while (keys.hasNext()) {
                nextKeys = (String) keys.next();
                try {
                    if (json.get(nextKeys) instanceof JSONObject) getJsonKey(json.getJSONObject(nextKeys), key, false);
                    else if (json.get(nextKeys) instanceof JSONArray) {
                        JSONArray jsonArray = json.getJSONArray(nextKeys);

                        int x = 0;
                        if (x < jsonArray.length()) {
                            String jsonArrayString = jsonArray.get(x).toString();
                            JSONObject innerJSON = new JSONObject(jsonArrayString);

                            getJsonKey(innerJSON, key, false);
                        }
                    }
                } catch (Exception e) {
                    System.out.print(getTime(LogColor.RED));
                    e.printStackTrace();
                }
            }
        } else {
            value = json.get(key).toString();
            return value;
        }

        return value;
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

            String receivedWord = getJsonKey(obj, "word", true);
            String phonetic = getJsonKey(obj, "phonetic", true);
            StringBuilder definitions = new StringBuilder();

            JSONArray meanings = new JSONArray(obj.getJSONArray("meanings"));
            for (int x = 0; x < meanings.length() && x < 3; x++) {
                if (x > 0) definitions.append("\n\n");
                JSONObject info = meanings.getJSONObject(x);
                System.out.println(info);
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
            embed.setTitle(phonetic);
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
        String[] badWords = {"\\bsex\\b", "\\bass\\b", "bitch", "fuck"};

        for (String word : badWords) {
            String temp = phrase.replaceAll(word, "REDACTED");
            if (temp.contains("REDACTED")) return true;
        }

        return false;
    }
}
