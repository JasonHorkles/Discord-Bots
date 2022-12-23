package me.jasonhorkles.aircheck;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.json.JSONArray;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@SuppressWarnings("DataFlowIssue")
public class Forecasts {
    public void updateForecasts() throws IOException {
        JSONArray input;

        if (!AirCheck.testing) {
            String apiUrl = "http://dataservice.accuweather.com/indices/v1/daily/1day/" + new Secrets().getAccuLocationCode() + "?apikey=" + new Secrets().getAccuApiKey();

            InputStream stream = new URL(apiUrl).openStream();
            String out = new Scanner(stream, StandardCharsets.UTF_8).useDelimiter("\\A").nextLine();
            stream.close();

            input = new JSONArray(out);
        } else {
            File activitiesFile = new File("AirCheck/activities.json");
            Scanner fileScanner = new Scanner(activitiesFile);

            input = new JSONArray(fileScanner.nextLine());
        }

        StringBuilder healthForecasts = new StringBuilder();
        String pollenForecasts = new Pollen().getPollen();
        StringBuilder transportationForecasts = new StringBuilder();
        StringBuilder workForecasts = new StringBuilder();
        StringBuilder sportsForecasts = new StringBuilder();
        StringBuilder activityForecasts = new StringBuilder();

        for (int x = 0; x < input.length(); x++) {
            int id = input.getJSONObject(x).getInt("ID");
            int categoryValue = input.getJSONObject(x).getInt("CategoryValue");

            switch (id) {
                /* Health */

                // Common cold
                case 25 -> healthForecasts.append(getColor(categoryValue, CategoryType.RISK))
                    .append("**Common Cold**").append(getForecast(categoryValue, CategoryType.RISK))
                    .append("\n");

                // Flu
                case 26 ->
                    healthForecasts.append(getColor(categoryValue, CategoryType.RISK)).append("**Flu**")
                        .append(getForecast(categoryValue, CategoryType.RISK)).append("\n");

                // Migraines
                case 27 ->
                    healthForecasts.append(getColor(categoryValue, CategoryType.RISK)).append("**Migraines**")
                        .append(getForecast(categoryValue, CategoryType.RISK)).append("\n");

                // Mosquito activity
                case 17 -> healthForecasts.append(getColor(categoryValue, CategoryType.EXTREMITY))
                    .append("**Mosquito Activity**")
                    .append(getForecast(categoryValue, CategoryType.EXTREMITY)).append("\n");

                // Sinus pressure
                case 30 -> healthForecasts.append(getColor(categoryValue, CategoryType.RISK))
                    .append("**Sinus Pressure**").append(getForecast(categoryValue, CategoryType.RISK))
                    .append("\n");


                /* Transportation */

                // Bicycling
                case 4 -> transportationForecasts.append(getColor(categoryValue, CategoryType.FAIRNESS))
                    .append("**Bicycling**").append(getForecast(categoryValue, CategoryType.FAIRNESS))
                    .append("\n");

                // Driving
                case 40 -> transportationForecasts.append(getColor(categoryValue, CategoryType.FAIRNESS))
                    .append("**Driving**").append(getForecast(categoryValue, CategoryType.FAIRNESS))
                    .append("\n");

                // Flight delays
                case -3 -> transportationForecasts.append(getColor(categoryValue, CategoryType.UNLIKELIHOOD))
                    .append("**Flight Delays**").append(getForecast(categoryValue, CategoryType.UNLIKELIHOOD))
                    .append("\n");


                /* Work */

                // Composting
                case 38 -> workForecasts.append(getColor(categoryValue, CategoryType.FAIRNESS))
                    .append("**Composting**").append(getForecast(categoryValue, CategoryType.FAIRNESS))
                    .append("\n");

                // Construction
                case 14 -> workForecasts.append(getColor(categoryValue, CategoryType.FAIRNESS))
                    .append("**Construction**").append(getForecast(categoryValue, CategoryType.FAIRNESS))
                    .append("\n");

                // Lawn mowing
                case 28 -> workForecasts.append(getColor(categoryValue, CategoryType.FAIRNESS))
                    .append("**Lawn Mowing**").append(getForecast(categoryValue, CategoryType.FAIRNESS))
                    .append("\n");


                /* Sports */

                // Fishing
                case 13 -> sportsForecasts.append(getColor(categoryValue, CategoryType.FAIRNESS))
                    .append("**Fishing**").append(getForecast(categoryValue, CategoryType.FAIRNESS))
                    .append("\n");

                // Golf
                case 5 ->
                    sportsForecasts.append(getColor(categoryValue, CategoryType.FAIRNESS)).append("**Golf**")
                        .append(getForecast(categoryValue, CategoryType.FAIRNESS)).append("\n");

                // Hiking
                case 3 -> sportsForecasts.append(getColor(categoryValue, CategoryType.FAIRNESS))
                    .append("**Hiking**").append(getForecast(categoryValue, CategoryType.FAIRNESS))
                    .append("\n");

                // Hunting
                case 20 -> sportsForecasts.append(getColor(categoryValue, CategoryType.FAIRNESS))
                    .append("**Hunting**").append(getForecast(categoryValue, CategoryType.FAIRNESS))
                    .append("\n");

                // Jogging
                case 2 -> sportsForecasts.append(getColor(categoryValue, CategoryType.FAIRNESS))
                    .append("**Jogging**").append(getForecast(categoryValue, CategoryType.FAIRNESS))
                    .append("\n");

                // Running
                case 1 -> sportsForecasts.append(getColor(categoryValue, CategoryType.FAIRNESS))
                    .append("**Running**").append(getForecast(categoryValue, CategoryType.FAIRNESS))
                    .append("\n");

                // Skateboarding
                case 7 -> sportsForecasts.append(getColor(categoryValue, CategoryType.FAIRNESS))
                    .append("**Skateboarding**").append(getForecast(categoryValue, CategoryType.FAIRNESS))
                    .append("\n");

                // Skiing
                case 15 -> sportsForecasts.append(getColor(categoryValue, CategoryType.FAIRNESS))
                    .append("**Skiing**").append(getForecast(categoryValue, CategoryType.FAIRNESS))
                    .append("\n");

                // Swimming
                case 10 -> sportsForecasts.append(getColor(categoryValue, CategoryType.FAIRNESS))
                    .append("**Swimming**").append(getForecast(categoryValue, CategoryType.FAIRNESS))
                    .append("\n");

                // Tennis
                case 6 -> sportsForecasts.append(getColor(categoryValue, CategoryType.FAIRNESS))
                    .append("**Tennis**").append(getForecast(categoryValue, CategoryType.FAIRNESS))
                    .append("\n");


                /* Activities */

                // BBQ
                case 24 ->
                    activityForecasts.append(getColor(categoryValue, CategoryType.FAIRNESS)).append("**BBQ**")
                        .append(getForecast(categoryValue, CategoryType.FAIRNESS)).append("\n");

                // Dog walking
                case 43 -> activityForecasts.append(getColor(categoryValue, CategoryType.FAIRNESS))
                    .append("**Dog Walking**").append(getForecast(categoryValue, CategoryType.FAIRNESS))
                    .append("\n");

                // Outdoor concerts
                case 8 -> activityForecasts.append(getColor(categoryValue, CategoryType.FAIRNESS))
                    .append("**Outdoor Concerts**").append(getForecast(categoryValue, CategoryType.FAIRNESS))
                    .append("\n");

                // Shopping
                case 39 -> activityForecasts.append(getColor(categoryValue, CategoryType.FAIRNESS))
                    .append("**Shopping**").append(getForecast(categoryValue, CategoryType.FAIRNESS))
                    .append("\n");

                // Snow day
                case 19 -> activityForecasts.append(getColor(categoryValue, CategoryType.LIKELIHOOD))
                    .append("**Snow Day**").append(getForecast(categoryValue, CategoryType.LIKELIHOOD))
                    .append("\n");

                // Stargazing
                case 12 -> activityForecasts.append(getColor(categoryValue, CategoryType.FAIRNESS))
                    .append("**Stargazing**").append(getForecast(categoryValue, CategoryType.FAIRNESS))
                    .append("\n");
            }
        }

        EmbedBuilder health = new EmbedBuilder();
        health.setColor(new Color(47, 49, 54));
        health.setTitle("Health");
        health.setDescription(healthForecasts.toString().strip());

        EmbedBuilder pollen = new EmbedBuilder();
        pollen.setColor(new Color(47, 49, 54));
        pollen.setTitle("Pollen");
        pollen.setDescription(pollenForecasts.strip());

        EmbedBuilder transportation = new EmbedBuilder();
        transportation.setColor(new Color(47, 49, 54));
        transportation.setTitle("Transportation");
        transportation.setDescription(transportationForecasts.toString().strip());

        EmbedBuilder work = new EmbedBuilder();
        work.setColor(new Color(47, 49, 54));
        work.setTitle("Work");
        work.setDescription(workForecasts.toString().strip());

        EmbedBuilder sports = new EmbedBuilder();
        sports.setColor(new Color(47, 49, 54));
        sports.setTitle("Sports");
        sports.setDescription(sportsForecasts.toString().strip());

        EmbedBuilder activities = new EmbedBuilder();
        activities.setColor(new Color(47, 49, 54));
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
                switch (message.getEmbeds().get(0).getTitle()) {
                    case "Health" -> {
                        if (!message.getEmbeds().get(0).getDescription()
                            .equals(healthForecasts.toString().strip()))
                            message.editMessageEmbeds(health.build()).queue();
                    }

                    case "Pollen" -> {
                        if (!message.getEmbeds().get(0).getDescription().equals(pollenForecasts.strip()))
                            message.editMessageEmbeds(pollen.build()).queueAfter(1, TimeUnit.SECONDS);
                    }

                    case "Transportation" -> {
                        if (!message.getEmbeds().get(0).getDescription()
                            .equals(transportationForecasts.toString().strip()))
                            message.editMessageEmbeds(transportation.build()).queueAfter(2, TimeUnit.SECONDS);
                    }

                    case "Work" -> {
                        if (!message.getEmbeds().get(0).getDescription()
                            .equals(workForecasts.toString().strip()))
                            message.editMessageEmbeds(work.build()).queueAfter(3, TimeUnit.SECONDS);
                    }

                    case "Sports" -> {
                        if (!message.getEmbeds().get(0).getDescription()
                            .equals(sportsForecasts.toString().strip()))
                            message.editMessageEmbeds(sports.build()).queueAfter(4, TimeUnit.SECONDS);
                    }

                    case "Activities" -> {
                        if (!message.getEmbeds().get(0).getDescription()
                            .equals(activityForecasts.toString().strip()))
                            message.editMessageEmbeds(activities.build()).queueAfter(5, TimeUnit.SECONDS);
                    }
                }

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            System.out.print(new Utils().getTime(Utils.LogColor.RED));
            e.printStackTrace();
        }

        System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + "Got the forecasts!");
    }

    private enum CategoryType {
        FAIRNESS, EXTREMITY, LIKELIHOOD, RISK, UNLIKELIHOOD
    }

    private String getColor(int value, CategoryType categoryType) {
        switch (categoryType) {
            case FAIRNESS -> {
                switch (value) {
                    // 🔴
                    case 1 -> {
                        return "\uD83D\uDD34 ";
                    }
                    // 🟠
                    case 2 -> {
                        return "\uD83D\uDFE0 ";
                    }
                    // 🟡
                    case 3 -> {
                        return "\uD83D\uDFE1 ";
                    }
                    // 🔵
                    case 4 -> {
                        return "\uD83D\uDD35 ";
                    }
                    // 🟢
                    case 5 -> {
                        return "\uD83D\uDFE2 ";
                    }
                }
            }

            case EXTREMITY -> {
                switch (value) {
                    // 🟢
                    case 1 -> {
                        return "\uD83D\uDFE2 ";
                    }
                    // 🔵
                    case 2 -> {
                        return "\uD83D\uDD35 ";
                    }
                    // 🟡
                    case 3 -> {
                        return "\uD83D\uDFE1 ";
                    }
                    // 🟠
                    case 4 -> {
                        return "\uD83D\uDFE0 ";
                    }
                    // 🔴
                    case 5 -> {
                        return "\uD83D\uDD34 ";
                    }
                }
            }

            case LIKELIHOOD -> {
                switch (value) {
                    // 🔴
                    case 1 -> {
                        return "\uD83D\uDD34 ";
                    }
                    // 🟠
                    case 2 -> {
                        return "\uD83D\uDFE0 ";
                    }
                    // 🟡
                    case 3 -> {
                        return "\uD83D\uDFE1 ";
                    }
                    // 🔵
                    case 4 -> {
                        return "\uD83D\uDD35 ";
                    }
                    // 🟢
                    case 5 -> {
                        return "\uD83D\uDFE2 ";
                    }
                }
            }

            case RISK -> {
                switch (value) {
                    // 🟢
                    case 1 -> {
                        return "\uD83D\uDFE2 ";
                    }
                    // 🔵
                    case 2 -> {
                        return "\uD83D\uDD35 ";
                    }
                    // 🟡
                    case 3 -> {
                        return "\uD83D\uDFE1 ";
                    }
                    // 🟠
                    case 4 -> {
                        return "\uD83D\uDFE0 ";
                    }
                    // 🔴
                    case 5 -> {
                        return "\uD83D\uDD34 ";
                    }
                }
            }

            case UNLIKELIHOOD -> {
                switch (value) {
                    // 🔴
                    case 1 -> {
                        return "\uD83D\uDD34 ";
                    }
                    // 🟠
                    case 2 -> {
                        return "\uD83D\uDFE0 ";
                    }
                    // 🟡
                    case 3 -> {
                        return "\uD83D\uDFE1 ";
                    }
                    // 🔵
                    case 4 -> {
                        return "\uD83D\uDD35 ";
                    }
                    // 🟢
                    case 5 -> {
                        return "\uD83D\uDFE2 ";
                    }
                }
            }
        }

        return categoryType.toString();
    }

    private String getForecast(int value, CategoryType categoryType) {
        switch (categoryType) {
            case FAIRNESS -> {
                switch (value) {
                    // 🔴
                    case 1 -> {
                        return " → Poor";
                    }
                    // 🟠
                    case 2 -> {
                        return " → Fair";
                    }
                    // 🟡
                    case 3 -> {
                        return " → Good";
                    }
                    // 🔵
                    case 4 -> {
                        return " → Very Good";
                    }
                    // 🟢
                    case 5 -> {
                        return " → Excellent";
                    }
                }
            }

            case EXTREMITY -> {
                switch (value) {
                    // 🟢
                    case 1 -> {
                        return " → Low";
                    }
                    // 🔵
                    case 2 -> {
                        return " → Moderate";
                    }
                    // 🟡
                    case 3 -> {
                        return " → High";
                    }
                    // 🟠
                    case 4 -> {
                        return " → Very High";
                    }
                    // 🔴
                    case 5 -> {
                        return " → Extreme";
                    }
                }
            }

            case LIKELIHOOD -> {
                switch (value) {
                    // 🔴
                    case 1 -> {
                        return " → Very Unlikely";
                    }
                    // 🟠
                    case 2 -> {
                        return " → Unlikely";
                    }
                    // 🟡
                    case 3 -> {
                        return " → Possibly";
                    }
                    // 🔵
                    case 4 -> {
                        return " → Likely";
                    }
                    // 🟢
                    case 5 -> {
                        return " → Very Likely";
                    }
                }
            }

            case RISK -> {
                switch (value) {
                    // 🟢
                    case 1 -> {
                        return " → Beneficial";
                    }
                    // 🔵
                    case 2 -> {
                        return " → Neutral";
                    }
                    // 🟡
                    case 3 -> {
                        return " → At Risk";
                    }
                    // 🟠
                    case 4 -> {
                        return " → At High Risk";
                    }
                    // 🔴
                    case 5 -> {
                        return " → At Extreme Risk";
                    }
                }
            }

            case UNLIKELIHOOD -> {
                switch (value) {
                    // 🔴
                    case 1 -> {
                        return " → Very Likely";
                    }
                    // 🟠
                    case 2 -> {
                        return " → Likely";
                    }
                    // 🟡
                    case 3 -> {
                        return " → Possibly";
                    }
                    // 🔵
                    case 4 -> {
                        return " → Unlikely";
                    }
                    // 🟢
                    case 5 -> {
                        return " → Very Unlikely";
                    }
                }
            }
        }

        return categoryType.toString();
    }
}
