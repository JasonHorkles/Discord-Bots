package me.jasonhorkles.booper.events;

import me.jasonhorkles.booper.Utils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

@SuppressWarnings("DataFlowIssue")
public class SlashCommands extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + event.getMember()
            .getEffectiveName() + " used the /" + event.getName() + " command");

        switch (event.getName().toLowerCase()) {
            case "hug", "tackle", "pillow" -> {
                String fileName = event.getName().toLowerCase() + ".txt";
                List<String> messages = new ArrayList<>();

                try (Scanner fileScanner = new Scanner(
                    Path.of("Booper/Messages/" + fileName),
                    StandardCharsets.UTF_8)) {

                    while (fileScanner.hasNextLine()) {
                        String line = fileScanner.nextLine();
                        if (!line.isBlank()) messages.add(line);
                    }

                    Random r = new Random();
                    int random = r.nextInt(messages.size());
                    sendActionMessage(event, messages.get(random));

                } catch (IOException e) {
                    event.reply("Error with file: " + fileName).setEphemeral(true).queue();
                }
            }

            case "livemsg" -> {
                switch (event.getSubcommandName().toLowerCase()) {
                    case "set" -> event.reply(
                        "Which type of user would you like to **SET** a live message for?").addActionRow(
                        getLiveMessageButtons("set")).setEphemeral(true).queue();

                    case "reset" -> event.reply(
                            "Which type of user's live message would you like to **RESET**?\n-# Selecting Twitch will disable notifications for that user entirely.")
                        .addActionRow(getLiveMessageButtons("reset")).setEphemeral(true).queue();
                }
            }

            default -> event.reply("Unknown command (" + event.getName() + ")!").setEphemeral(true).queue();
        }
    }

    /// Sends a message with a mention to the user specified in the command options.
    ///
    /// @param event   The SlashCommandInteractionEvent containing the command details.
    /// @param message The message format to send, which should include placeholders for mentions. Use `{0}` for the sender and `{1}` for the receiver.
    private void sendActionMessage(SlashCommandInteractionEvent event, String message) {
        // Generic way to get the mentionable from the options
        IMentionable receiver = event.getOptionsByType(OptionType.MENTIONABLE).getFirst().getAsMentionable();

        // If it's a member, mention them regardless
        String mention = receiver.getAsMention();
        if (!(receiver instanceof Member)) {
            // If it's a user, send an ephemeral message
            if (receiver instanceof User) {
                event.reply("Sorry, but that user isn't in the server!").setEphemeral(true).queue();
                return;
            }

            // Never allow @everyone
            if (mention.equalsIgnoreCase("@everyone")) mention = mention.replaceFirst("@", "");
            else {
                // Otherwise, check if the sender is allowed to mention the role
                //noinspection BooleanVariableAlwaysNegated
                boolean canMention = event.getMember().hasPermission(
                    event.getGuildChannel(),
                    Permission.MESSAGE_MENTION_EVERYONE);

                // Fallback to just the name if they can't
                if (!canMention && receiver instanceof Role) mention = ((Role) receiver).getName();
            }
        }

        event.reply(MessageFormat.format(message, event.getMember().getAsMention(), "**" + mention + "**"))
            .queue();
    }

    private List<ItemComponent> getLiveMessageButtons(String type) {
        return List.of(
            Button.success("live-msg-discord-" + type, "Discord User"),
            Button.primary("live-msg-twitch-" + type, "Twitch User"));
    }
}
