package me.jasonhorkles.phoenella.events;

import me.jasonhorkles.phoenella.Phoenella;
import me.jasonhorkles.phoenella.Utils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("DataFlowIssue")
public class Reactions extends ListenerAdapter {
    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        if (event.getUser().isBot()) return;

        // kek
        if (event.getReaction().getEmoji().getName().equalsIgnoreCase("kek")) {
            event.retrieveMessage().queue(msg -> msg
                .addReaction(event.getGuild().getEmojiById("841681203278774322")).queue());
            return;
        }

        // Shush users
        if (event.getReaction().getEmoji().getName().equals("\uD83E\uDD2B")) {
            // Verify if mod or coach
            if (event.getMember().getRoles().toString().contains("751166721624375435") || event.getMember()
                .getRoles().toString().contains("729108220479537202"))
                event.retrieveMessage().queue(message -> {
                    Member member = message.getMember();

                    if (member.isTimedOut()) {
                        event.getChannel().sendMessage(event.getMember()
                            .getAsMention() + ", that person is already shushed!").queue((m) -> m.delete()
                            .queueAfter(5,
                                TimeUnit.SECONDS,
                                null,
                                new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE)));
                        return;
                    }

                    member.timeoutFor(10, TimeUnit.MINUTES).queue((na) -> event.getChannel()
                            .sendMessage(new Utils().getFirstName(member) + " just got shushed!").queue(del -> {
                                del.delete().queueAfter(10,
                                    TimeUnit.MINUTES,
                                    null,
                                    new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
                                event.getChannel().sendMessage("https://tenor.com/vfW7.gif").queue(del2 -> del2
                                    .delete().queueAfter(10,
                                        TimeUnit.MINUTES,
                                        null,
                                        new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE)));
                            }),
                        (na1) -> event.getChannel()
                            .sendMessage(event.getMember().getAsMention() + ", I can't shush that person!")
                            .queue((del) -> del.delete().queueAfter(5,
                                TimeUnit.SECONDS,
                                null,
                                new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE))));
                });
            return;
        }

        // Add / remove word from dictionary
        if (event.getReaction().getEmoji().getName().equals("✅")) {
            if (event.getChannel().getIdLong() == 960213547944661042L) {
                if (event.getUserIdLong() != 277291758503723010L) {
                    System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + event.getUser()
                        .getName() + " just tried to accept a Wordle word!");
                    return;
                }

                Message message = event.retrieveMessage().complete();
                File file = new File("Phoenella/Wordle/words.txt");

                try {
                    if (message.getContentStripped().toLowerCase().contains("word request")) {
                        FileWriter fileWriter = new FileWriter(file, StandardCharsets.UTF_8, true);
                        String word = message.getContentStripped().replaceAll(".*: ", "").toUpperCase();
                        fileWriter.write(word + "\n");
                        fileWriter.close();

                    } else if (message.getContentStripped().contains("Word report")) {
                        FileWriter fileWriter = fileWriter(file, message);
                        fileWriter.close();
                    }

                    message.delete().queue();

                } catch (IOException e) {
                    message.reply("Failed to write word! See console for details.").queue(msg -> msg.delete()
                        .queueAfter(5, TimeUnit.SECONDS));
                    System.out.print(new Utils().getTime(Utils.LogColor.RED));
                    e.printStackTrace();
                }
            }
            return;
        }

        // Delete message
        if (event.getReaction().getEmoji().getName().equals("❌") && event.getChannel().asTextChannel()
            .getParentCategoryIdLong() != 900747596245639238L) {
            event.retrieveMessage().queue(message -> {
                if (message.getAuthor().equals(Phoenella.jda.getSelfUser())) message.delete().queue();
            });
            return;
        }

        // Prevent future requests for that word
        if (event.getEmoji().getName().equals("⛔"))
            if (event.getChannel().getIdLong() == 960213547944661042L) {
                if (event.getUserIdLong() != 277291758503723010L) {
                    System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + event.getUser()
                        .getName() + " just tried to decline a Wordle word!");
                    return;
                }

                Message message = event.retrieveMessage().complete();
                File file = new File("Phoenella/Wordle/banned-requests.txt");

                try {
                    if (message.getContentStripped().contains("Auto word request")) {
                        FileWriter fileWriter = new FileWriter(file, StandardCharsets.UTF_8, true);
                        fileWriter.write(message.getContentStripped().replaceAll(".*: ", "")
                            .toUpperCase() + "\n");
                        fileWriter.close();
                    }
                    message.delete().queue();

                } catch (IOException e) {
                    message.reply("Failed to write word! See console for details.").queue(msg -> msg.delete()
                        .queueAfter(5, TimeUnit.SECONDS));
                    System.out.print(new Utils().getTime(Utils.LogColor.RED));
                    e.printStackTrace();
                }
            }
    }

    @NotNull
    private FileWriter fileWriter(File file, Message message) throws IOException {
        Scanner words = new Scanner(file, StandardCharsets.UTF_8);
        List<String> wordList = new ArrayList<>();

        while (words.hasNext()) try {
            String next = words.next();
            if (!next.equalsIgnoreCase(message.getContentStripped().replaceAll(".*: ", "")))
                wordList.add(next.toUpperCase());
        } catch (NoSuchElementException ignored) {
        }
        words.close();

        FileWriter fileWriter = new FileWriter(file, StandardCharsets.UTF_8, false);
        for (String word : wordList) fileWriter.write(word + "\n");
        return fileWriter;
    }

    @Override
    public void onMessageReactionRemove(MessageReactionRemoveEvent event) {
        // Remove kek if only bot is left
        if (event.getReaction().getEmoji().getName().equalsIgnoreCase("kek")) event.retrieveMessage().queue(
            msg -> event.getReaction().retrieveUsers().queue(users -> {
                if (users.size() == 1 && users.getFirst().equals(event.getJDA().getSelfUser()))
                    msg.removeReaction(event.getGuild().getEmojiById("841681203278774322"),
                        event.getJDA().getSelfUser()).queue();
            }));
    }
}
