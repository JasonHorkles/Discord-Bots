package me.jasonhorkles.phoenella;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.awt.*;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;

//@SuppressWarnings("DataFlowIssue")
public class Utils {
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
            else return "`ERROR: " + e + "`";
        }
    }

    public String getFirstName(@Nullable Member member) {
        if (member == null) return "null";
        return member.getEffectiveName().replaceAll(" .*", "").replaceAll("\\(.*", "").strip();
    }

    public String getFullName(@Nullable Member member) {
        if (member == null) return "null";
        return member.getEffectiveName().replaceAll("\\(.*", "").strip();
    }

    public void sendMessage(@Nullable TextChannel channel, @Nullable Message replyTo, String message, Boolean allCaps) {
        if (!message.startsWith("http")) if (allCaps) message = message.toUpperCase();
        else {
            Random r = new SecureRandom();
            if (r.nextBoolean()) message = message.toLowerCase();
        }

        if (channel != null) channel.sendMessage(message).queue();
        else if (replyTo != null) replyTo.reply(message).mentionRepliedUser(false).queue();
        else throw new NullPointerException("No TextChannel or Message defined");
    }

    public void updateDailyWordle() {
        try {
            FileWriter daily = fileWriter();
            daily.close();

            FileWriter plays = new FileWriter("Phoenella/Wordle/played-daily.txt",
                StandardCharsets.UTF_8,
                false);
            plays.close();

            System.out.println(getTime(LogColor.GREEN) + "Updated the daily Wordle!");
        } catch (IOException e) {
            System.out.print(getTime(LogColor.RED));
            System.out.print(getTime(LogColor.RED));
            e.printStackTrace();
        }
    }

    @NotNull
    private static FileWriter fileWriter() throws IOException {
        Scanner words = new Scanner(new File("Phoenella/Wordle/words.txt"), StandardCharsets.UTF_8);
        ArrayList<String> wordList = new ArrayList<>();
        while (words.hasNext()) try {
            wordList.add(words.next());
        } catch (NoSuchElementException ignored) {
        }

        Random r = new SecureRandom();
        String word = wordList.get(r.nextInt(wordList.size()));

        FileWriter daily = new FileWriter("Phoenella/Wordle/daily.txt", StandardCharsets.UTF_8, false);
        daily.write(word);
        return daily;
    }

    public MessageEmbed defineWord(String word) {
        word = word.replaceAll(" .*", "");
        EmbedBuilder embed = new EmbedBuilder();

        try {
            InputStream url;

            try {
                url = new URI("https://api.dictionaryapi.dev/api/v2/entries/en/" + word).toURL().openStream();
            } catch (FileNotFoundException ignored) {
                embed.setColor(new Color(212, 43, 65));
                embed.setDescription("Couldn't find **" + word + "** in the dictionary!");
                return embed.build();
            }

            JSONObject obj = new JSONArray(new Scanner(url, StandardCharsets.UTF_8).useDelimiter("\\A")
                .nextLine()).getJSONObject(0);
            url.close();

            String receivedWord = obj.getString("word");
            String phonetic = null;
            if (obj.has("phonetic")) phonetic = obj.getString("phonetic");
            StringBuilder definitions = new StringBuilder();

            JSONArray meanings = new JSONArray(obj.getJSONArray("meanings"));
            for (int x = 0; x < meanings.length() && x < 3; x++) {
                if (x > 0) definitions.append("\n\n");
                JSONObject info = meanings.getJSONObject(x);

                List<String> definitionList = new ArrayList<>();
                JSONArray rawDefinitions = info.getJSONArray("definitions");
                int iterations = 0;
                for (int y = 0; y < rawDefinitions.length() && iterations < 2; y++) {
                    String newDefinition = rawDefinitions.getJSONObject(y).getString("definition");
                    if (containsBadWord(newDefinition)) continue;

                    definitionList.add(newDefinition);
                    iterations++;
                }

                if (!definitionList.isEmpty()) {
                    definitions.append("**").append(info.getString("partOfSpeech").toUpperCase())
                        .append(":**");
                    for (String definition : definitionList)
                        definitions.append("\n• ").append(definition);
                }
            }

            embed.setColor(new Color(15, 157, 88));
            embed.setAuthor(receivedWord.toUpperCase() + " DEFINITION");
            if (phonetic != null) embed.setTitle(phonetic);
            embed.setDescription(definitions);

        } catch (IOException | URISyntaxException e) {
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
        String[] badWords = {"\\bsex\\b", "\\bass\\b", "bitch", "fuck", "breast", "penis", "vagina", "dicks"};
        for (String word : badWords) if (phrase.matches("(?si).*" + word + ".*")) return true;
        return false;
    }
}
