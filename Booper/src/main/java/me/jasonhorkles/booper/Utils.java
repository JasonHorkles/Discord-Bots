package me.jasonhorkles.booper;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.awt.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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

    public CompletableFuture<List<Message>> getMessages(MessageChannel channel, int count) {
        return channel.getIterableHistory().takeAsync(count).thenApply(ArrayList::new);
    }

    public JSONObject getJsonFromFile(String fileName) {
        try {
            return new JSONObject(Files.readString(
                Path.of("Booper/Data/" + fileName),
                StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.out.print(getTime(LogColor.RED));
            e.printStackTrace();
            return new JSONObject();
        }
    }

    /**
     * @param usernameOrId Discord ID or Twitch username to fetch from custom messages
     */
    public Message sendLiveMessage(String usernameOrId, String twitchUrl, @Nullable String streamTitle, String gameName, boolean isDiscord) {
        String twitchUsername = twitchUrl.replaceFirst(".*twitch\\.tv/", "");

        String liveMessageText;
        try {
            JSONObject bothObjects = getJsonFromFile("live-msgs.json");
            JSONObject customMessages = isDiscord ? bothObjects.getJSONObject("discord") : bothObjects.getJSONObject(
                "twitch");

            // Get custom message if it exists, otherwise get a random default message
            String message = "";
            if (customMessages.has(usernameOrId)) message = customMessages.getString(usernameOrId);
            if (message.isBlank()) {
                List<String> defaultMessages = new ArrayList<>();

                try (Scanner fileScanner = new Scanner(
                    Path.of("Booper/Messages/default-live.txt"),
                    StandardCharsets.UTF_8)) {

                    while (fileScanner.hasNextLine()) {
                        String line = fileScanner.nextLine();
                        if (!line.isBlank()) defaultMessages.add(line);
                    }

                    Random r = new Random();
                    int random = r.nextInt(defaultMessages.size());
                    message = defaultMessages.get(random);
                }
            }
            liveMessageText = message.replace("{NAME}", "**" + twitchUsername + "**");

        } catch (IOException e) {
            System.out.print(getTime(LogColor.RED));
            e.printStackTrace();
            liveMessageText = "ERROR GRABBING MESSAGE";
        }

        if (streamTitle == null) {
            String twitchId = Booper.twitch.getClientHelper().getTwitchHelix().getUsers(
                Booper.authToken,
                null,
                Collections.singletonList(twitchUsername)).execute().getUsers().getFirst().getId();

            streamTitle = Booper.twitch.getClientHelper().getTwitchHelix()
                .getChannelInformation(Booper.authToken, Collections.singletonList(twitchId)).execute()
                .getChannels().getFirst().getTitle();
        }

        String imageUrl = "https://static-cdn.jtvnw.net/previews-ttv/live_user_" + twitchUsername + "-1920x1080.jpg?id=" + System.currentTimeMillis() / 1000;

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle(streamTitle);
        embed.setDescription(gameName);
        embed.setImage(imageUrl);
        embed.setUrl(twitchUrl);
        embed.setColor(new Color(36, 36, 41));

        //noinspection DataFlowIssue
        TextChannel channel = Booper.jda.getGuildById(1299547538445307986L)
            .getTextChannelById(Booper.TWITCH_CHANNEL_ID);
        //noinspection DataFlowIssue
        Message liveMessage = channel.sendMessage(liveMessageText).setEmbeds(embed.build()).complete();

        // Update image after 10 minutes
        liveMessage.editMessageEmbeds(embed.setImage(imageUrl + "1").build()).queueAfter(
            10, TimeUnit.MINUTES,
            null,
            new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));

        return liveMessage;
    }
}
