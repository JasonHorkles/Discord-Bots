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

import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

//@SuppressWarnings("DataFlowIssue")
public class Utils {
    public enum LogColor {
        RED("\u001B[31m"),
        YELLOW("\u001B[33m"),
        GREEN("\u001B[32m");

        private final String logColor;

        LogColor(String logColor) {
            this.logColor = logColor;
        }

        public String getLogColor() {
            return logColor;
        }
    }

    public String getTime(LogColor logColor) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("hh:mm:ss a", Locale.US);
        return logColor.getLogColor() + "[" + dtf.format(LocalDateTime.now()) + "] ";
    }

    public String lookUp(String message, String name) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URI(
                "https://api.wolframalpha.com/v1/result?i=" + URLEncoder.encode(
                    message,
                    StandardCharsets.UTF_8) + "&appid=" + new Secrets().wolframAppId()).toURL()
                .openConnection();

            int responseCode = connection.getResponseCode();
            if (responseCode == 501) {
                connection.disconnect();
                return "501";
            }

            InputStream url = connection.getInputStream();
            String result = new String(url.readAllBytes(), StandardCharsets.UTF_8);
            url.close();
            connection.disconnect();

            message = result.replace("Wolfram Alpha", "Phoenella").replace("Wolfram|Alpha", "").replace("human",
                name);
            return message;

        } catch (IOException | URISyntaxException e) {
            System.out.print(new Utils().getTime(LogColor.RED));
            e.printStackTrace();
            return "`ERROR: " + e.toString().replace(new Secrets().wolframAppId(), "REDACTED") + "`";
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
            Random r = new Random();
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

            FileWriter plays = new FileWriter(
                "Phoenella/Wordle/played-daily.txt",
                StandardCharsets.UTF_8,
                false);
            plays.close();

            System.out.println(getTime(LogColor.GREEN) + "Updated the daily Wordle!");
        } catch (IOException e) {
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
        words.close();

        Random r = new Random();
        String word = wordList.get(r.nextInt(wordList.size()));

        FileWriter daily = new FileWriter("Phoenella/Wordle/daily.txt", StandardCharsets.UTF_8, false);
        daily.write(word);
        return daily;
    }

    public record DefinitionResult(MessageEmbed embed, boolean isSuccessful) {}

    public DefinitionResult defineWord(String word) {
        word = word.replaceAll(" .*", "");
        EmbedBuilder embed = new EmbedBuilder();

        try {
            InputStream url;

            try {
                url = new URI("https://api.dictionaryapi.dev/api/v2/entries/en/" + word).toURL().openStream();
            } catch (FileNotFoundException ignored) {
                embed.setColor(new Color(212, 43, 65));
                embed.setDescription("Couldn't find **" + word + "** in the dictionary!");

                return new DefinitionResult(embed.build(), false);
            }

            JSONObject obj = new JSONArray(new String(
                url.readAllBytes(),
                StandardCharsets.UTF_8)).getJSONObject(0);
            url.close();

            String receivedWord = obj.getString("word");
            String phonetic = null;
            if (obj.has("phonetic")) phonetic = obj.getString("phonetic");
            StringBuilder definitions = new StringBuilder(250);

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

            return new DefinitionResult(embed.build(), false);
        }

        return new DefinitionResult(embed.build(), true);
    }

    /* BAD WORD LIST BELOW */

































    /* BAD WORD LIST BELOW */

    public boolean containsBadWord(String phrase) {
        String[] badWords = {
            "\\bsex\\b",
            "\\bass\\b",
            "bitch",
            "fuck",
            "breast",
            "penis",
            "vagina",
            "dicks",
            "ejaculate",
            "sperm",
            "sexual",
            "seduce",
            "seduct",
            "slur"
        };
        for (String word : badWords) if (phrase.matches("(?si).*" + word + ".*")) return true;
        return false;
    }
}
