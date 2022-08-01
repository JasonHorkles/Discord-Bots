package me.jasonhorkles.phoenella.games;

import me.jasonhorkles.phoenella.GameManager;
import me.jasonhorkles.phoenella.Utils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.ErrorResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RPS extends ListenerAdapter {
    private static final HashMap<TextChannel, Integer> round = new HashMap<>();
    private static final HashMap<Member, Integer> points = new HashMap<>();
    private static final HashMap<TextChannel, Member> player1 = new HashMap<>();
    private static final HashMap<TextChannel, Member> player2 = new HashMap<>();
    private static final HashMap<Member, String> player1Selection = new HashMap<>();
    private static final HashMap<Member, String> player2Selection = new HashMap<>();

    public TextChannel startGame(ArrayList<Member> playerList) {
        TextChannel channel = new GameManager().createChannel(GameManager.Game.RPS, playerList);

        player1.put(channel, playerList.get(0));
        player2.put(channel, playerList.get(1));
        round.put(channel, 0);
        points.put(playerList.get(0), 0);
        points.put(playerList.get(1), 0);

        channel.sendMessage(
                "__**How to play:**__\nBeat your opponent, win 2/3 times\n\n*The game will start in 5 seconds...*")
            .queue((del) -> del.delete().queueAfter(5, TimeUnit.SECONDS));

        channel.sendMessage("__**Round 0/3**__\n\n**" + new Utils().getFirstName(
                playerList.get(0)) + ":** 0\n**" + new Utils().getFirstName(playerList.get(1)) + ":** 0")
            .setActionRow(Button.primary("rps:rock", Emoji.fromUnicode("\uD83E\uDEA8")),
                Button.primary("rps:paper", Emoji.fromUnicode("\uD83D\uDCDD")),
                Button.primary("rps:scissors", Emoji.fromUnicode("✂️"))).queueAfter(5, TimeUnit.SECONDS);

        StringBuilder mentions = new StringBuilder();
        for (Member player : playerList) mentions.append(player.getAsMention());
        channel.sendMessage(mentions).queue((del) -> del.delete()
            .queueAfter(100, TimeUnit.MILLISECONDS, null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE)));

        return channel;
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        TextChannel channel = event.getGuildChannel().asTextChannel();
        Member member = event.getMember();

        if (event.getComponentId().startsWith("rps:")) {
            Member player1Local = player1.get(channel);
            Member player2Local = player2.get(channel);
            String selectionFiltered = event.getComponentId().replace("rps:", "");

            if (member == player1.get(channel)) player1Selection.put(member, selectionFiltered);
            else if (member == player2.get(channel)) player2Selection.put(member, selectionFiltered);
            else return;

            event.deferEdit().queue();

            if (player1Selection.containsKey(player1Local) && player2Selection.containsKey(player2Local)) {
                event.getMessage().editMessageComponents(
                    ActionRow.of(Button.primary("rps:rock", Emoji.fromUnicode("\uD83E\uDEA8")).asDisabled(),
                        Button.primary("rps:paper", Emoji.fromUnicode("\uD83D\uDCDD")).asDisabled(),
                        Button.primary("rps:scissors", Emoji.fromUnicode("✂️")).asDisabled())).complete();

                if (player1Selection.get(player1Local).equals(player2Selection.get(player2Local)))
                    channel.sendMessage("You both chose **" + player1Selection.get(player1Local) + "**!").complete()
                        .delete().queueAfter(3, TimeUnit.SECONDS);
                else if ((player1Selection.get(player1Local).equals("rock") && player2Selection.get(player2Local)
                    .equals("scissors")) || (player1Selection.get(player1Local)
                    .equals("scissors") && player2Selection.get(player2Local).equals("paper")) || (player1Selection.get(
                    player1Local).equals("paper") && player2Selection.get(player2Local).equals("rock"))) {

                    points.put(player1Local, points.get(player1Local) + 1);
                    channel.sendMessage(player1Local.getAsMention() + " chose **" + player1Selection.get(
                            player1Local) + "**! They win this round!")
                        .queue((del) -> del.delete().queueAfter(3, TimeUnit.SECONDS));
                } else {
                    points.put(player2Local, points.get(player2Local) + 1);
                    channel.sendMessage(player2Local.getAsMention() + " chose **" + player2Selection.get(
                            player2Local) + "**! They win this round!")
                        .queue((del) -> del.delete().queueAfter(3, TimeUnit.SECONDS));
                }

                round.put(channel, round.get(channel) + 1);
                player1Selection.remove(player1Local);
                player2Selection.remove(player2Local);

                event.getMessage().editMessage(
                    "__**Round " + round.get(channel) + "/3**__\n\n**" + new Utils().getFirstName(
                        player1Local) + ":** " + points.get(player1Local) + "\n**" + new Utils().getFirstName(
                        player2Local) + ":** " + points.get(player2Local)).queue();

                if ((points.get(player1Local) >= 2 && points.get(player2Local) <= 1) || (points.get(
                    player2Local) >= 2 && points.get(player1Local) <= 1))

                    if (points.get(player1Local) - points.get(player2Local) >= 1 || points.get(
                        player1Local) - points.get(player2Local) <= -1) {

                        if (points.get(player1Local) > points.get(player2Local))
                            channel.sendMessage(player1Local.getAsMention() + " wins!").queue();
                        else channel.sendMessage(player2Local.getAsMention() + " wins!").queue();

                        Executors.newSingleThreadScheduledExecutor()
                            .schedule(() -> endGame(channel), 15, TimeUnit.SECONDS);

                        return;
                    }

                event.getMessage().editMessageComponents(
                    ActionRow.of(Button.primary("rps:rock", Emoji.fromUnicode("\uD83E\uDEA8")),
                        Button.primary("rps:paper", Emoji.fromUnicode("\uD83D\uDCDD")),
                        Button.primary("rps:scissors", Emoji.fromUnicode("✂️")))).queueAfter(1, TimeUnit.SECONDS);
            }
        }

        if (event.getComponentId().equals("endgame:rps")) {
            event.deferEdit().queue();
            endGame(channel);
        }
    }

    private void endGame(TextChannel channel) {
        player1Selection.remove(player1.get(channel));
        player2Selection.remove(player2.get(channel));
        points.remove(player1.get(channel));
        points.remove(player2.get(channel));
        player1.remove(channel);
        player2.remove(channel);
        round.remove(channel);
        new GameManager().deleteGame(channel);
    }
}
