package me.jasonhorkles.phoenella;

import me.jasonhorkles.phoenella.games.RPS;
import me.jasonhorkles.phoenella.games.Wordle;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import javax.annotation.Nullable;
import java.util.ArrayList;

@SuppressWarnings("ConstantConditions")
public class GameManager extends ListenerAdapter {
    public TextChannel createGame(Game game, @Nullable Message message, Member member, boolean isMultiplayer, @Nullable String wordleWord) {
        if (message != null) if ((message.getMentionedMembers().isEmpty() || message.getMentionedMembers()
            .get(0) == member || message.getMentionedMembers().get(0).getUser().isBot()) && isMultiplayer) return null;

        ArrayList<Member> players = new ArrayList<>();
        ArrayList<String> playerIDs = new ArrayList<>();
        players.add(member);
        playerIDs.add(member.getId());
        if (isMultiplayer) {
            players.add(message.getMentionedMembers().get(0));
            playerIDs.add(message.getMentionedMembers().get(0).getId());
        }

        TextChannel gameChannel = Phoenella.api.getCategoryById(900747596245639238L)
            .createTextChannel(new Utils().getFirstName(member) + "-" + game).setTopic(playerIDs.toString()).complete();

        for (Member player : players)
            gameChannel.putPermissionOverride(player).setAllow(Permission.VIEW_CHANNEL).setDeny(Permission.MESSAGE_SEND)
                .queue();

        switch (game) {
            case RPS -> new RPS().startGame(players, gameChannel);
            case WORDLE -> new Wordle().startGame(players.get(0), gameChannel, wordleWord);
        }

        return gameChannel;
    }

    public enum Game {
        RPS, WORDLE
    }

    public void sendEndGameMessage(TextChannel channel, String game) {
        channel.sendMessage("**Click the button below to end the game.**")
            .setActionRow(Button.danger("endgame:" + game, "End game").withEmoji(Emoji.fromUnicode("🗑️"))).complete();
    }

    public void deleteGame(TextChannel channel) {
        if (channel.getParentCategory() != null)
            if (channel.getParentCategoryIdLong() == 900747596245639238L) channel.delete().queue();
    }
}
