package me.jasonhorkles.aircheck;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.json.JSONArray;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@SuppressWarnings("DataFlowIssue")
public class Forecasts {
    public void updateForecasts() throws IOException, URISyntaxException {
        System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + "Checking forecasts...");

        JSONArray input;
        if (AirCheck.testing) input = new JSONArray(Files.readString(Path.of("AirCheck/activities.json")));
        else {
            InputStream url = new URI("http://dataservice.accuweather.com/indices/v1/daily/1day/" + new Secrets().accuLocationCode() + "?apikey=" + new Secrets().accuApiKey())
                .toURL().openStream();
            input = new JSONArray(new String(url.readAllBytes(), StandardCharsets.UTF_8));
            url.close();
        }

        StringBuilder healthForecasts = new StringBuilder(120);
        String pollenForecasts = new Pollen().getPollen();
        StringBuilder transportForecasts = new StringBuilder(60);
        StringBuilder workForecasts = new StringBuilder(60);
        StringBuilder sportsForecasts = new StringBuilder(170);
        StringBuilder activityForecasts = new StringBuilder(120);

        for (int x = 0; x < input.length(); x++) {
            int id = input.getJSONObject(x).getInt("ID");
            int categoryValue = input.getJSONObject(x).getInt("CategoryValue");

            switch (id) {
                /* Health */

                // Common cold
                case 25 -> healthForecasts.append(getRisk(categoryValue, true)).append("**Common Cold**")
                    .append(getRisk(categoryValue, false)).append("\n");

                // Flu
                case 26 -> healthForecasts.append(getRisk(categoryValue, true)).append("**Flu**").append(
                    getRisk(categoryValue, false)).append("\n");

                // Migraines
                case 27 -> healthForecasts.append(getRisk(categoryValue, true)).append("**Migraines**")
                    .append(getRisk(categoryValue, false)).append("\n");

                // Mosquito activity
                case 17 -> healthForecasts.append(getExtremity(categoryValue, true)).append(
                    "**Mosquito Activity**").append(getExtremity(categoryValue, false)).append("\n");

                // Sinus pressure
                case 30 -> healthForecasts.append(getRisk(categoryValue, true)).append("**Sinus Pressure**")
                    .append(getRisk(categoryValue, false)).append("\n");


                /* Transportation */

                // Bicycling
                case 4 -> transportForecasts.append(getFairness(categoryValue, true)).append("**Bicycling**")
                    .append(getFairness(categoryValue, false)).append("\n");

                // Driving
                case 40 -> transportForecasts.append(getFairness(categoryValue, true)).append("**Driving**")
                    .append(getFairness(categoryValue, false)).append("\n");

                // Flight delays
                case -3 -> transportForecasts.append(getUnlikelihood(categoryValue, true)).append(
                    "**Flight Delays**").append(getUnlikelihood(categoryValue, false)).append("\n");


                /* Work */

                // Composting
                case 38 -> workForecasts.append(getFairness(categoryValue, true)).append("**Composting**")
                    .append(getFairness(categoryValue, false)).append("\n");

                // Construction
                case 14 -> workForecasts.append(getFairness(categoryValue, true)).append("**Construction**")
                    .append(getFairness(categoryValue, false)).append("\n");

                // Lawn mowing
                case 28 -> workForecasts.append(getFairness(categoryValue, true)).append("**Lawn Mowing**")
                    .append(getFairness(categoryValue, false)).append("\n");


                /* Sports */

                // Fishing
                case 13 -> sportsForecasts.append(getFairness(categoryValue, true)).append("**Fishing**")
                    .append(getFairness(categoryValue, false)).append("\n");

                // Golf
                case 5 -> sportsForecasts.append(getFairness(categoryValue, true)).append("**Golf**").append(
                    getFairness(categoryValue, false)).append("\n");

                // Hiking
                case 3 -> sportsForecasts.append(getFairness(categoryValue, true)).append("**Hiking**")
                    .append(getFairness(categoryValue, false)).append("\n");

                // Hunting
                case 20 -> sportsForecasts.append(getFairness(categoryValue, true)).append("**Hunting**")
                    .append(getFairness(categoryValue, false)).append("\n");

                // Jogging
                case 2 -> sportsForecasts.append(getFairness(categoryValue, true)).append("**Jogging**")
                    .append(getFairness(categoryValue, false)).append("\n");

                // Running
                case 1 -> sportsForecasts.append(getFairness(categoryValue, true)).append("**Running**")
                    .append(getFairness(categoryValue, false)).append("\n");

                // Skateboarding
                case 7 -> sportsForecasts.append(getFairness(categoryValue, true)).append("**Skateboarding**")
                    .append(getFairness(categoryValue, false)).append("\n");

                // Skiing
                case 15 -> sportsForecasts.append(getFairness(categoryValue, true)).append("**Skiing**")
                    .append(getFairness(categoryValue, false)).append("\n");

                // Swimming
                case 10 -> sportsForecasts.append(getFairness(categoryValue, true)).append("**Swimming**")
                    .append(getFairness(categoryValue, false)).append("\n");

                // Tennis
                case 6 -> sportsForecasts.append(getFairness(categoryValue, true)).append("**Tennis**")
                    .append(getFairness(categoryValue, false)).append("\n");


                /* Activities */

                // BBQ
                case 24 -> activityForecasts.append(getFairness(categoryValue, true)).append("**BBQ**")
                    .append(getFairness(categoryValue, false)).append("\n");

                // Dog walking
                case 43 -> activityForecasts.append(getFairness(categoryValue, true))
                    .append("**Dog Walking**").append(getFairness(categoryValue, false)).append("\n");

                // Outdoor concerts
                case 8 -> activityForecasts.append(getFairness(categoryValue, true)).append(
                    "**Outdoor Concerts**").append(getFairness(categoryValue, false)).append("\n");

                // Shopping
                case 39 -> activityForecasts.append(getFairness(categoryValue, true)).append("**Shopping**")
                    .append(getFairness(categoryValue, false)).append("\n");

                // Snow day
                case 19 -> activityForecasts.append(getLikelihood(categoryValue, true)).append("**Snow Day**")
                    .append(getLikelihood(categoryValue, false)).append("\n");

                // Stargazing
                case 12 -> activityForecasts.append(getFairness(categoryValue, true)).append("**Stargazing**")
                    .append(getFairness(categoryValue, false)).append("\n");
            }
        }

        EmbedBuilder health = new EmbedBuilder();
        health.setColor(new Color(43, 45, 49));
        health.setTitle("Health");
        health.setDescription(healthForecasts.toString().strip());

        EmbedBuilder pollen = new EmbedBuilder();
        pollen.setColor(new Color(43, 45, 49));
        pollen.setTitle("Pollen");
        pollen.setDescription(pollenForecasts.strip());

        EmbedBuilder transportation = new EmbedBuilder();
        transportation.setColor(new Color(43, 45, 49));
        transportation.setTitle("Transportation");
        transportation.setDescription(transportForecasts.toString().strip());

        EmbedBuilder work = new EmbedBuilder();
        work.setColor(new Color(43, 45, 49));
        work.setTitle("Work");
        work.setDescription(workForecasts.toString().strip());

        EmbedBuilder sports = new EmbedBuilder();
        sports.setColor(new Color(43, 45, 49));
        sports.setTitle("Sports");
        sports.setDescription(sportsForecasts.toString().strip());

        EmbedBuilder activities = new EmbedBuilder();
        activities.setColor(new Color(43, 45, 49));
        activities.setTitle("Activities");
        activities.setDescription(activityForecasts.toString().strip());

        // Send message if needed - otherwise edit the existing one
        try {
            TextChannel channel = AirCheck.jda.getTextChannelById(1055226409267761214L);
            List<Message> latestMessages = new Utils().getMessages(channel, 6).get(30, TimeUnit.SECONDS);

            if (latestMessages.isEmpty()) {
                channel.sendMessageEmbeds(activities.build()).queue();
                channel.sendMessageEmbeds(sports.build()).queueAfter(1, TimeUnit.SECONDS);
                channel.sendMessageEmbeds(work.build()).queueAfter(2, TimeUnit.SECONDS);
                channel.sendMessageEmbeds(transportation.build()).queueAfter(3, TimeUnit.SECONDS);
                channel.sendMessageEmbeds(pollen.build()).queueAfter(4, TimeUnit.SECONDS);
                channel.sendMessageEmbeds(health.build()).queueAfter(5, TimeUnit.SECONDS);

            } else for (Message message : latestMessages)
                switch (message.getEmbeds().getFirst().getTitle()) {
                    case "Health" -> {
                        if (!message.getEmbeds().getFirst().getDescription().equals(healthForecasts.toString()
                            .strip())) message.editMessageEmbeds(health.build()).queue();
                    }

                    case "Pollen" -> {
                        if (!message.getEmbeds().getFirst().getDescription().equals(pollenForecasts.strip()))
                            message.editMessageEmbeds(pollen.build()).queueAfter(1, TimeUnit.SECONDS);
                    }

                    case "Transportation" -> {
                        if (!message.getEmbeds().getFirst().getDescription().equals(transportForecasts
                            .toString().strip()))
                            message.editMessageEmbeds(transportation.build()).queueAfter(2, TimeUnit.SECONDS);
                    }

                    case "Work" -> {
                        if (!message.getEmbeds().getFirst().getDescription().equals(workForecasts.toString()
                            .strip())) message.editMessageEmbeds(work.build()).queueAfter(3,
                            TimeUnit.SECONDS);
                    }

                    case "Sports" -> {
                        if (!message.getEmbeds().getFirst().getDescription().equals(sportsForecasts.toString()
                            .strip())) message.editMessageEmbeds(sports.build()).queueAfter(12,
                            TimeUnit.SECONDS);
                    }

                    case "Activities" -> {
                        if (!message.getEmbeds().getFirst().getDescription().equals(activityForecasts
                            .toString().strip())) message.editMessageEmbeds(activities.build()).queueAfter(17,
                            TimeUnit.SECONDS);
                    }
                }

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            System.out.print(new Utils().getTime(Utils.LogColor.RED));
            e.printStackTrace();
            new Utils().logError(e);
        }

        System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + "Got the forecasts!");
    }

    private String getFairness(int value, boolean color) {
        switch (value) {
            // 🔴
            case 1 -> {
                return color ? "\uD83D\uDD34 " : " → Poor";
            }
            // 🟠
            case 2 -> {
                return color ? "\uD83D\uDFE0 " : " → Fair";
            }
            // 🟡
            case 3 -> {
                return color ? "\uD83D\uDFE1 " : " → Good";
            }
            // 🔵
            case 4 -> {
                return color ? "\uD83D\uDD35 " : " → Very Good";
            }
            // 🟢
            case 5 -> {
                return color ? "\uD83D\uDFE2 " : " → Excellent";
            }
        }

        return String.valueOf(value);
    }

    private String getExtremity(int value, boolean color) {
        switch (value) {
            // 🟢
            case 1 -> {
                return color ? "\uD83D\uDFE2 " : " → Low";
            }
            // 🔵
            case 2 -> {
                return color ? "\uD83D\uDD35 " : " → Moderate";
            }
            // 🟡
            case 3 -> {
                return color ? "\uD83D\uDFE1 " : " → High";
            }
            // 🟠
            case 4 -> {
                return color ? "\uD83D\uDFE0 " : " → Very High";
            }
            // 🔴
            case 5 -> {
                return color ? "\uD83D\uDD34 " : " → Extreme";
            }
        }

        return String.valueOf(value);
    }

    private String getLikelihood(int value, boolean color) {
        switch (value) {
            // 🔴
            case 1 -> {
                return color ? "\uD83D\uDD34 " : " → Very Unlikely";
            }
            // 🟠
            case 2 -> {
                return color ? "\uD83D\uDFE0 " : " → Unlikely";
            }
            // 🟡
            case 3 -> {
                return color ? "\uD83D\uDFE1 " : " → Possibly";
            }
            // 🔵
            case 4 -> {
                return color ? "\uD83D\uDD35 " : " → Likely";
            }
            // 🟢
            case 5 -> {
                return color ? "\uD83D\uDFE2 " : " → Very Likely";
            }
        }

        return String.valueOf(value);
    }

    private String getRisk(int value, boolean color) {
        switch (value) {
            // 🟢
            case 1 -> {
                return color ? "\uD83D\uDFE2 " : " → Beneficial";
            }
            // 🔵
            case 2 -> {
                return color ? "\uD83D\uDD35 " : " → Neutral";
            }
            // 🟡
            case 3 -> {
                return color ? "\uD83D\uDFE1 " : " → At Risk";
            }
            // 🟠
            case 4 -> {
                return color ? "\uD83D\uDFE0 " : " → At High Risk";
            }
            // 🔴
            case 5 -> {
                return color ? "\uD83D\uDD34 " : " → At Extreme Risk";
            }
        }

        return String.valueOf(value);
    }

    private String getUnlikelihood(int value, boolean color) {
        switch (value) {
            // 🔴
            case 1 -> {
                return color ? "\uD83D\uDD34 " : " → Very Likely";
            }
            // 🟠
            case 2 -> {
                return color ? "\uD83D\uDFE0 " : " → Likely";
            }
            // 🟡
            case 3 -> {
                return color ? "\uD83D\uDFE1 " : " → Possibly";
            }
            // 🔵
            case 4 -> {
                return color ? "\uD83D\uDD35 " : " → Unlikely";
            }
            // 🟢
            case 5 -> {
                return color ? "\uD83D\uDFE2 " : " → Very Unlikely";
            }
        }

        return String.valueOf(value);
    }
}
