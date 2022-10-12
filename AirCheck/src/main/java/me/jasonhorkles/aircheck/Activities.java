package me.jasonhorkles.aircheck;

import org.json.JSONArray;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

@SuppressWarnings("ConstantConditions")
public class Activities {
    public void checkConditions() throws IOException {
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

        for (int x = 0; x < input.length(); x++) {
            int id = input.getJSONObject(x).getInt("ID");
            int categoryValue = input.getJSONObject(x).getInt("CategoryValue");

            switch (id) {
                /* Health */

                // Common cold
                case 25 -> {
                    String categoryName = convertToForecast(categoryValue, CategoryType.RISK);
                    long channelId = 1028367213851332638L;
                    if (!AirCheck.jda.getVoiceChannelById(channelId).getName().equals("└─ " + categoryName))
                        AirCheck.jda.getVoiceChannelById(channelId).getManager().setName("└─ " + categoryName)
                            .queue();
                }
                // Flu
                case 26 -> {
                    String categoryName = convertToForecast(categoryValue, CategoryType.RISK);
                    long channelId = 1028367389320028230L;
                    if (!AirCheck.jda.getVoiceChannelById(channelId).getName().equals("Flu | " + categoryName))
                        AirCheck.jda.getVoiceChannelById(channelId).getManager().setName("Flu | " + categoryName)
                            .queue();
                }
                // Migraines
                case 27 -> {
                    String categoryName = convertToForecast(categoryValue, CategoryType.RISK);
                    long channelId = 1028367636792365126L;
                    if (!AirCheck.jda.getVoiceChannelById(channelId).getName().equals("└─ " + categoryName))
                        AirCheck.jda.getVoiceChannelById(channelId).getManager().setName("└─ " + categoryName)
                            .queue();
                }
                // Mosquito activity
                case 17 -> {
                    String categoryName = convertToForecast(categoryValue, CategoryType.EXTREMITY);
                    long channelId = 1028366585003524116L;
                    if (!AirCheck.jda.getVoiceChannelById(channelId).getName().equals("└─ " + categoryName))
                        AirCheck.jda.getVoiceChannelById(channelId).getManager().setName("└─ " + categoryName)
                            .queue();
                }
                // Sinus pressure
                case 30 -> {
                    String categoryName = convertToForecast(categoryValue, CategoryType.RISK);
                    long channelId = 1028367936739614901L;
                    if (!AirCheck.jda.getVoiceChannelById(channelId).getName().equals("└─ " + categoryName))
                        AirCheck.jda.getVoiceChannelById(channelId).getManager().setName("└─ " + categoryName)
                            .queue();
                }

                /* Transportation */

                // Bicycling
                case 4 -> {
                    String categoryName = convertToForecast(categoryValue, CategoryType.FAIRNESS);
                    long channelId = 1028364022967435385L;
                    if (!AirCheck.jda.getVoiceChannelById(channelId).getName().equals("Bicycling | " + categoryName))
                        AirCheck.jda.getVoiceChannelById(channelId).getManager().setName("Bicycling | " + categoryName)
                            .queue();
                }
                // Driving
                case 40 -> {
                    String categoryName = convertToForecast(categoryValue, CategoryType.FAIRNESS);
                    long channelId = 1028368672772849754L;
                    if (!AirCheck.jda.getVoiceChannelById(channelId).getName().equals("Driving | " + categoryName))
                        AirCheck.jda.getVoiceChannelById(channelId).getManager().setName("Driving | " + categoryName)
                            .queue();
                }
                // Flight delays
                case -3 -> {
                    String categoryName = convertToForecast(categoryValue, CategoryType.UNLIKELIHOOD);
                    long channelId = 1028363222442578054L;
                    if (!AirCheck.jda.getVoiceChannelById(channelId).getName().equals("└─ " + categoryName))
                        AirCheck.jda.getVoiceChannelById(channelId).getManager().setName("└─ " + categoryName)
                            .queue();
                }

                /* Work */

                // Composting
                case 38 -> {
                    String categoryName = convertToForecast(categoryValue, CategoryType.FAIRNESS);
                    long channelId = 1028368464244645939L;
                    if (!AirCheck.jda.getVoiceChannelById(channelId).getName().equals("└─ " + categoryName))
                        AirCheck.jda.getVoiceChannelById(channelId).getManager().setName("└─ " + categoryName)
                            .queue();
                }
                // Construction
                case 14 -> {
                    String categoryName = convertToForecast(categoryValue, CategoryType.FAIRNESS);
                    long channelId = 1028366293205782638L;
                    if (!AirCheck.jda.getVoiceChannelById(channelId).getName().equals("└─ " + categoryName))
                        AirCheck.jda.getVoiceChannelById(channelId).getManager().setName("└─ " + categoryName)
                            .queue();
                }
                // Lawn mowing
                case 28 -> {
                    String categoryName = convertToForecast(categoryValue, CategoryType.FAIRNESS);
                    long channelId = 1028367726596599928L;
                    if (!AirCheck.jda.getVoiceChannelById(channelId).getName().equals("└─ " + categoryName))
                        AirCheck.jda.getVoiceChannelById(channelId).getManager().setName("└─ " + categoryName)
                            .queue();
                }

                /* Sports */

                // Fishing
                case 13 -> {
                    String categoryName = convertToForecast(categoryValue, CategoryType.FAIRNESS);
                    long channelId = 1028366148904955904L;
                    if (!AirCheck.jda.getVoiceChannelById(channelId).getName().equals("Fishing | " + categoryName))
                        AirCheck.jda.getVoiceChannelById(channelId).getManager().setName("Fishing | " + categoryName)
                            .queue();
                }
                // Golf
                case 5 -> {
                    String categoryName = convertToForecast(categoryValue, CategoryType.FAIRNESS);
                    long channelId = 1028364219785150484L;
                    if (!AirCheck.jda.getVoiceChannelById(channelId).getName().equals("Golf | " + categoryName))
                        AirCheck.jda.getVoiceChannelById(channelId).getManager().setName("Golf | " + categoryName)
                            .queue();
                }
                // Hiking
                case 3 -> {
                    String categoryName = convertToForecast(categoryValue, CategoryType.FAIRNESS);
                    long channelId = 1028363937332338728L;
                    if (!AirCheck.jda.getVoiceChannelById(channelId).getName().equals("Hiking | " + categoryName))
                        AirCheck.jda.getVoiceChannelById(channelId).getManager().setName("Hiking | " + categoryName)
                            .queue();
                }
                // Hunting
                case 20 -> {
                    String categoryName = convertToForecast(categoryValue, CategoryType.FAIRNESS);
                    long channelId = 1028366927514566706L;
                    if (!AirCheck.jda.getVoiceChannelById(channelId).getName().equals("Hunting | " + categoryName))
                        AirCheck.jda.getVoiceChannelById(channelId).getManager().setName("Hunting | " + categoryName)
                            .queue();
                }
                // Jogging
                case 2 -> {
                    String categoryName = convertToForecast(categoryValue, CategoryType.FAIRNESS);
                    long channelId = 1028363881946550315L;
                    if (!AirCheck.jda.getVoiceChannelById(channelId).getName().equals("Jogging | " + categoryName))
                        AirCheck.jda.getVoiceChannelById(channelId).getManager().setName("Jogging | " + categoryName)
                            .queue();
                }
                // Running
                case 1 -> {
                    String categoryName = convertToForecast(categoryValue, CategoryType.FAIRNESS);
                    long channelId = 1028363741106024499L;
                    if (!AirCheck.jda.getVoiceChannelById(channelId).getName().equals("Running | " + categoryName))
                        AirCheck.jda.getVoiceChannelById(channelId).getManager().setName("Running | " + categoryName)
                            .queue();
                }
                // Skateboarding
                case 7 -> {
                    String categoryName = convertToForecast(categoryValue, CategoryType.FAIRNESS);
                    long channelId = 1028364390619160687L;
                    if (!AirCheck.jda.getVoiceChannelById(channelId).getName().equals("└─ " + categoryName))
                        AirCheck.jda.getVoiceChannelById(channelId).getManager().setName("└─ " + categoryName)
                            .queue();
                }
                // Skiing
                case 15 -> {
                    String categoryName = convertToForecast(categoryValue, CategoryType.FAIRNESS);
                    long channelId = 1028366361816211586L;
                    if (!AirCheck.jda.getVoiceChannelById(channelId).getName().equals("Skiing | " + categoryName))
                        AirCheck.jda.getVoiceChannelById(channelId).getManager().setName("Skiing | " + categoryName)
                            .queue();
                }
                // Swimming
                case 10 -> {
                    String categoryName = convertToForecast(categoryValue, CategoryType.FAIRNESS);
                    long channelId = 1028365906851659896L;
                    if (!AirCheck.jda.getVoiceChannelById(channelId).getName().equals("└─ " + categoryName))
                        AirCheck.jda.getVoiceChannelById(channelId).getManager().setName("└─ " + categoryName)
                            .queue();
                }
                // Tennis
                case 6 -> {
                    String categoryName = convertToForecast(categoryValue, CategoryType.FAIRNESS);
                    long channelId = 1028364243642368040L;
                    if (!AirCheck.jda.getVoiceChannelById(channelId).getName().equals("Tennis | " + categoryName))
                        AirCheck.jda.getVoiceChannelById(channelId).getManager().setName("Tennis | " + categoryName)
                            .queue();
                }

                /* Activities */

                // BBQ
                case 24 -> {
                    String categoryName = convertToForecast(categoryValue, CategoryType.FAIRNESS);
                    long channelId = 1028367037833154631L;
                    if (!AirCheck.jda.getVoiceChannelById(channelId).getName().equals("BBQ | " + categoryName))
                        AirCheck.jda.getVoiceChannelById(channelId).getManager().setName("BBQ | " + categoryName)
                            .queue();
                }
                // Dog walking
                case 43 -> {
                    String categoryName = convertToForecast(categoryValue, CategoryType.FAIRNESS);
                    long channelId = 1028368761914408990L;
                    if (!AirCheck.jda.getVoiceChannelById(channelId).getName().equals("└─ " + categoryName))
                        AirCheck.jda.getVoiceChannelById(channelId).getManager().setName("└─ " + categoryName)
                            .queue();
                }
                // Kite flying
                case 9 -> {
                    String categoryName = convertToForecast(categoryValue, CategoryType.FAIRNESS);
                    long channelId = 1028364913535631380L;
                    if (!AirCheck.jda.getVoiceChannelById(channelId).getName().equals("└─ " + categoryName))
                        AirCheck.jda.getVoiceChannelById(channelId).getManager().setName("└─ " + categoryName)
                            .queue();
                }
                // Outdoor concerts
                case 8 -> {
                    String categoryName = convertToForecast(categoryValue, CategoryType.FAIRNESS);
                    long channelId = 1028364677853495386L;
                    if (!AirCheck.jda.getVoiceChannelById(channelId).getName().equals("└─ " + categoryName))
                        AirCheck.jda.getVoiceChannelById(channelId).getManager().setName("└─ " + categoryName)
                            .queue();
                }
                // Shopping
                case 39 -> {
                    String categoryName = convertToForecast(categoryValue, CategoryType.FAIRNESS);
                    long channelId = 1028368578417803325L;
                    if (!AirCheck.jda.getVoiceChannelById(channelId).getName().equals("└─ " + categoryName))
                        AirCheck.jda.getVoiceChannelById(channelId).getManager().setName("└─ " + categoryName)
                            .queue();
                }
                // Snow day
                case 19 -> {
                    String categoryName = convertToForecast(categoryValue, CategoryType.LIKELIHOOD);
                    long channelId = 1028366819511242873L;
                    if (!AirCheck.jda.getVoiceChannelById(channelId).getName().equals("└─ " + categoryName))
                        AirCheck.jda.getVoiceChannelById(channelId).getManager().setName("└─ " + categoryName)
                            .queue();
                }
                // Stargazing
                case 12 -> {
                    String categoryName = convertToForecast(categoryValue, CategoryType.FAIRNESS);
                    long channelId = 1028366085109592235L;
                    if (!AirCheck.jda.getVoiceChannelById(channelId).getName().equals("└─ " + categoryName))
                        AirCheck.jda.getVoiceChannelById(channelId).getManager().setName("└─ " + categoryName)
                            .queue();
                }
            }
        }

        System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + "Got the activity forecasts!");
    }

    private enum CategoryType {
        FAIRNESS, EXTREMITY, LIKELIHOOD, RISK, UNLIKELIHOOD
    }

    private String convertToForecast(int value, CategoryType categoryType) {
        switch (categoryType) {
            case FAIRNESS -> {
                switch (value) {
                    // 🔴
                    case 1 -> {
                        return "Poor \uD83D\uDD34";
                    }
                    // 🟠
                    case 2 -> {
                        return "Fair \uD83D\uDFE0";
                    }
                    // 🟡
                    case 3 -> {
                        return "Good \uD83D\uDFE1";
                    }
                    // 🔵
                    case 4 -> {
                        return "Very good \uD83D\uDD35";
                    }
                    // 🟢
                    case 5 -> {
                        return "Excellent \uD83D\uDFE2";
                    }
                }
            }

            case EXTREMITY -> {
                switch (value) {
                    // 🟢
                    case 1 -> {
                        return "Low \uD83D\uDFE2";
                    }
                    // 🔵
                    case 2 -> {
                        return "Moderate \uD83D\uDD35";
                    }
                    // 🟡
                    case 3 -> {
                        return "High \uD83D\uDFE1";
                    }
                    // 🟠
                    case 4 -> {
                        return "Very high \uD83D\uDFE0";
                    }
                    // 🔴
                    case 5 -> {
                        return "Extreme \uD83D\uDD34";
                    }
                }
            }

            case LIKELIHOOD -> {
                switch (value) {
                    // 🔴
                    case 1 -> {
                        return "Very unlikely \uD83D\uDD34";
                    }
                    // 🟠
                    case 2 -> {
                        return "Unlikely \uD83D\uDFE0";
                    }
                    // 🟡
                    case 3 -> {
                        return "Possibly \uD83D\uDFE1";
                    }
                    // 🔵
                    case 4 -> {
                        return "Likely \uD83D\uDD35";
                    }
                    // 🟢
                    case 5 -> {
                        return "Very likely \uD83D\uDFE2";
                    }
                }
            }

            case RISK -> {
                switch (value) {
                    // 🟢
                    case 1 -> {
                        return "Beneficial \uD83D\uDFE2";
                    }
                    // 🔵
                    case 2 -> {
                        return "Neutral \uD83D\uDD35";
                    }
                    // 🟡
                    case 3 -> {
                        return "At risk \uD83D\uDFE1";
                    }
                    // 🟠
                    case 4 -> {
                        return "At high risk \uD83D\uDFE0";
                    }
                    // 🔴
                    case 5 -> {
                        return "At extreme risk \uD83D\uDD34";
                    }
                }
            }

            case UNLIKELIHOOD -> {
                switch (value) {
                    // 🔴
                    case 1 -> {
                        return "Very likely \uD83D\uDD34";
                    }
                    // 🟠
                    case 2 -> {
                        return "Likely \uD83D\uDFE0";
                    }
                    // 🟡
                    case 3 -> {
                        return "Possibly \uD83D\uDFE1";
                    }
                    // 🔵
                    case 4 -> {
                        return "Unlikely \uD83D\uDD35";
                    }
                    // 🟢
                    case 5 -> {
                        return "Very unlikely \uD83D\uDFE2";
                    }
                }
            }
        }

        return categoryType.toString();
    }
}
