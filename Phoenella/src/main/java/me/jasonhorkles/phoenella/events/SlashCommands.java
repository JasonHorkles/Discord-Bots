package me.jasonhorkles.phoenella.events;

import me.jasonhorkles.phoenella.Phoenella;
import me.jasonhorkles.phoenella.Utils;
import me.jasonhorkles.phoenella.games.RPS;
import me.jasonhorkles.phoenella.games.Wordle;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

@SuppressWarnings("DataFlowIssue")
public class SlashCommands extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + new Utils().getFullName(event.getMember()) + " used the /" + event.getName() + " command");

        switch (event.getName().toLowerCase()) {
            case "wordle" -> wordleCommand(event);
            case "rps" -> {
                event.deferReply(true).queue();

                Member player2 = event.getOption("player").getAsMember();
                if (player2 == event.getMember() || player2.getUser().isBot()) {
                    event.getHook().editOriginal("Invalid opponent!").queue();
                    return;
                }

                TextChannel gameChannel = new RPS().startGame(new ArrayList<>(Arrays.asList(
                    event.getMember(),
                    player2)));
                event.getHook().editOriginal("Game created in " + gameChannel.getAsMention()).queue();
            }
        }
    }

    private void wordleCommand(SlashCommandInteractionEvent event) {
        switch (event.getSubcommandName()) {
            case "create" -> {
                TextInput word = TextInput.create("word", "Word", TextInputStyle.SHORT).setPlaceholder(
                    "Standard words are 5 characters").setMinLength(4).setMaxLength(8).build();
                TextInput tries = TextInput.create("tries", "Tries", TextInputStyle.SHORT).setPlaceholder(
                        "Must be between 4-8").setMinLength(1).setMaxLength(1).setValue(String.valueOf(6))
                    .build();

                Modal modal = Modal.create("customwordle", "Create Custom Wordle").addActionRow(word)
                    .addActionRow(tries).build();

                event.replyModal(modal).queue();
            }

            case "play" -> {
                event.reply("Creating a game...").setEphemeral(true).queue();
                try {
                    TextChannel gameChannel = new Wordle().startGame(
                        event.getMember(),
                        null,
                        false,
                        false,
                        null);
                    if (gameChannel == null) event.getHook().editOriginal(
                            "Either you already have an ongoing game with that word or you have too many games active at once!")
                        .queue();
                    else
                        event.getHook().editOriginal("Game created in " + gameChannel.getAsMention()).queue();
                } catch (IOException e) {
                    event.getHook().editOriginal("Couldn't generate a random word! Please try again later.")
                        .queue();
                    System.out.print(new Utils().getTime(Utils.LogColor.RED));
                    e.printStackTrace();
                }
            }

            case "daily" -> {
                event.deferReply(true).queue();
                event.getHook().editOriginal(new Wordle().startDailyWordle(event.getMember())).queue();
            }

            case "leaderboard" -> {
                if (Phoenella.localWordleBoard)
                    event.reply("The leaderboard is currently disabled!").setEphemeral(true).queue();
                else {
                    boolean ephemeral = true;
                    if (event.getChannel().asTextChannel().getParentCategoryIdLong() != 900747596245639238L)
                        if (event.getOption("show") != null)
                            ephemeral = !event.getOption("show").getAsBoolean();

                    event.deferReply(ephemeral).queue();
                    event.getHook().editOriginalEmbeds(new Wordle().getLeaderboard(event.getGuild())).queue();
                }
            }
        }
    }
}
