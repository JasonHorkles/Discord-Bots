package me.jasonhorkles.phoenella;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.ErrorResponse;

import java.util.ArrayList;

@SuppressWarnings("ConstantConditions")
public class GameManager extends ListenerAdapter {
    public TextChannel createChannel(Game game, ArrayList<Member> players) {
        TextChannel channel = Phoenella.api.getCategoryById(900747596245639238L)
            .createTextChannel(new Utils().getFirstName(players.get(0)) + "-" + game.toString().toLowerCase())
            .complete();

        for (Member player : players)
            channel.putPermissionOverride(player).setAllow(Permission.VIEW_CHANNEL).setDeny(Permission.MESSAGE_SEND)
                .queue();

        sendEndGameMessage(channel, game);

        return channel;
    }

    public enum Game {
        RPS, WORDLE
    }

    public void sendEndGameMessage(TextChannel channel, Game game) {
        channel.sendMessage("**Click the button below to end the game.**").setActionRow(
                Button.danger("endgame:" + game.toString().toLowerCase(), "End game").withEmoji(Emoji.fromUnicode("🗑️")))
            .complete();
    }

    public void deleteGame(TextChannel channel) {
        if (channel.getParentCategory() != null) if (channel.getParentCategoryIdLong() == 900747596245639238L)
            channel.delete().queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_CHANNEL));
    }
}
