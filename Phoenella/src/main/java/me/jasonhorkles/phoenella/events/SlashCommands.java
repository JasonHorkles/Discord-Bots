package me.jasonhorkles.phoenella.events;

import me.jasonhorkles.phoenella.Phoenella;
import me.jasonhorkles.phoenella.Utils;
import me.jasonhorkles.phoenella.games.Wordle;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@SuppressWarnings("DataFlowIssue")
public class SlashCommands extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + new Utils().getFullName(event.getMember()) + " used the /" + event.getName() + " command");

        //noinspection SwitchStatementWithTooFewBranches
        switch (event.getName().toLowerCase()) {
            case "wordle" -> {
                switch (event.getSubcommandName()) {
                    case "create" -> {
                        TextInput word = TextInput.create("word", "Word", TextInputStyle.SHORT)
                            .setPlaceholder("Standard words are 5 characters").setMinLength(4).setMaxLength(8)
                            .build();
                        TextInput tries = TextInput.create("tries", "Tries", TextInputStyle.SHORT)
                            .setPlaceholder("Must be between 4-8").setMinLength(1).setMaxLength(1).setValue(
                                String.valueOf(6)).build();

                        Modal modal = Modal.create("customwordle", "Create Custom Wordle").addActionRow(word)
                            .addActionRow(tries).build();

                        event.replyModal(modal).queue();
                    }

                    case "play" -> {
                        event.reply("Creating a game...").setEphemeral(true).queue();
                        try {
                            TextChannel gameChannel = new Wordle().startGame(event.getMember(),
                                null,
                                false,
                                false,
                                null);
                            if (gameChannel == null) event.getHook().editOriginal(
                                    "Either you already have an ongoing game with that word or you have too many games active at once!")
                                .queue();
                            else event.getHook().editOriginal("Game created in " + gameChannel.getAsMention())
                                .queue();
                        } catch (IOException e) {
                            event.getHook().editOriginal(
                                "Couldn't generate a random word! Please try again later.").queue();
                            System.out.print(new Utils().getTime(Utils.LogColor.RED));
                            e.printStackTrace();
                        }
                    }

                    case "daily" -> {
                        File daily = new File("Phoenella/Wordle/played-daily.txt");
                        try {
                            Scanner dailyPlays = new Scanner(daily, StandardCharsets.UTF_8);
                            ArrayList<String> plays = new ArrayList<>();
                            while (dailyPlays.hasNextLine()) plays.add(dailyPlays.nextLine());

                            if (plays.toString().contains(event.getMember().getId())) {
                                event.reply("You've already played today's Wordle!").setEphemeral(true)
                                    .queue();
                                return;
                            }
                        } catch (IOException e) {
                            System.out.print(new Utils().getTime(Utils.LogColor.RED));
                            e.printStackTrace();
                        }

                        event.reply("Creating a game...").setEphemeral(true).queue();
                        try {
                            File dailyWord = new File("Phoenella/Wordle/daily.txt");
                            String word = new Scanner(dailyWord, StandardCharsets.UTF_8).next();

                            FileWriter fw = new FileWriter(daily, StandardCharsets.UTF_8, true);
                            fw.write(event.getMember().getId() + "\n");
                            fw.close();

                            TextChannel gameChannel = new Wordle().startGame(event.getMember(),
                                word,
                                false,
                                true,
                                null);
                            if (gameChannel == null) event.getHook().editOriginal(
                                    "Either you already have an ongoing game with that word or you have too many games active at once!")
                                .queue();
                            else event.getHook().editOriginal("Game created in " + gameChannel.getAsMention())
                                .queue();
                        } catch (IOException e) {
                            event.getHook().editOriginal(
                                "Couldn't generate a random word! Please try again later.").queue();
                            System.out.print(new Utils().getTime(Utils.LogColor.RED));
                            e.printStackTrace();
                        }
                    }

                    case "leaderboard" -> {
                        if (Phoenella.localWordleBoard)
                            event.reply("The leaderboard is currently disabled!").setEphemeral(true).queue();
                        else {
                            boolean ephemeral = true;
                            if (event.getChannel().asTextChannel()
                                .getParentCategoryIdLong() != 900747596245639238L)
                                if (event.getOption("show") != null)
                                    ephemeral = !event.getOption("show").getAsBoolean();

                            event.deferReply(ephemeral).queue();

                            Scanner leaderboard = null;
                            try {
                                leaderboard = new Scanner(new File("Phoenella/Wordle/leaderboard.txt"),
                                    StandardCharsets.UTF_8);
                            } catch (IOException e) {
                                System.out.print(new Utils().getTime(Utils.LogColor.RED));
                                e.printStackTrace();
                            }
                            Map<Member, Integer> lines = new HashMap<>();

                            while (leaderboard.hasNextLine()) try {
                                String line = leaderboard.nextLine();
                                long id = Long.parseLong(line.replaceFirst(":.*", ""));
                                int score = Integer.parseInt(line.replaceFirst(".*:", ""));
                                Member member = event.getGuild().getMemberById(id);
                                lines.put(member, score);
                            } catch (NoSuchElementException ignored) {
                            }

                            LinkedHashMap<Member, Integer> sortedLeaderboard = new LinkedHashMap<>();
                            lines.entrySet().stream()
                                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).forEachOrdered(
                                    x -> sortedLeaderboard.put(x.getKey(), x.getValue()));

                            StringBuilder finalLeaderboard = new StringBuilder("```\n");
                            int index = 1;
                            for (Map.Entry<Member, Integer> entry : sortedLeaderboard.entrySet()) {
                                if (index > 10) break;
                                finalLeaderboard.append(index).append(". ").append(new Utils().getFullName(
                                    entry.getKey())).append(" | ").append(entry.getValue()).append("\n");
                                index++;
                            }
                            finalLeaderboard.append("```");

                            EmbedBuilder embed = new EmbedBuilder();
                            embed.setColor(new Color(43, 45, 49));
                            embed.setTitle("Wordle Leaderboard");
                            embed.setFooter("User-generated words are not counted");
                            embed.setDescription(finalLeaderboard);

                            event.getHook().editOriginalEmbeds(embed.build()).queue();
                        }
                    }
                }
            }
        }
    }
}
