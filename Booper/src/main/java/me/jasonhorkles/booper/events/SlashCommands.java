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

import java.text.MessageFormat;
import java.util.Random;

@SuppressWarnings("DataFlowIssue")
public class SlashCommands extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + event.getMember()
            .getEffectiveName() + " used the /" + event.getName() + " command");

        Random r = new Random();
        switch (event.getName().toLowerCase()) {
            case "hug" -> {
                int random = r.nextInt(10);

                switch (random) {
                    case 0 -> sendActionMessage(
                        event,
                        "{0} wraps their arms around {1} and refuses to let go. You live here now. 💞");
                    case 1 -> sendActionMessage(
                        event,
                        "{0} hugs {1} like a cleric who just rolled a Nat 20 on *Cure Wounds*. That''s the good stuff. 🛐");
                    case 2 -> sendActionMessage(
                        event,
                        "{0} sneaks up behind {1} and hugs them like they mean it. No escape detected. 🤗");
                    case 3 -> sendActionMessage(
                        event, "{0} uses *Hug*. It''s super effective! {1} regains 1d8 Emotional HP. 🎯");
                    case 4 -> sendActionMessage(
                        event, "{0} gives {1} a hug so warm, it''s basically a Minecraft campfire buff. 🔥");
                    case 5 -> sendActionMessage(
                        event,
                        "{0} pulls {1} into a hug that lasts just long enough to make it awkward. You''re welcome. 😏");
                    case 6 -> sendActionMessage(
                        event,
                        "{0} glomps {1} and now they share a Mind Link. Too bad it''s 98% affection and memes. 📡");
                    case 7 -> sendActionMessage(
                        event,
                        "{0} hugs {1} with all the commitment of placing obsidian. You ain''t breaking this easy. 🧱");
                    case 8 -> sendActionMessage(
                        event,
                        "{0} snuggles into {1} like a blanket with abandonment issues. 🐾");
                    case 9 -> sendActionMessage(
                        event,
                        "{0} activates Hug Protocol: full embrace, gentle squeeze, +2 to Charisma checks with {1} for the next hour. 💫");
                }
            }

            case "tackle" -> {
                int random = r.nextInt(11);

                switch (random) {
                    case 0 -> sendActionMessage(
                        event,
                        "{0} charges at {1} like a linebacker made of fluff. They''ve been taken down. 💥");
                    case 1 -> sendActionMessage(
                        event,
                        "{0} tackles {1} straight to the ground. Cuddle combat has begun. 🛏");
                    case 2 -> sendActionMessage(
                        event,
                        "{0} pounces on {1} like a creeper that decided *exploding with love* is a vibe. 💚💣");
                    case 3 -> sendActionMessage(
                        event,
                        "{0} hits {1} like an affectionate freight train. No survivors. Only snuggles. 🚂");
                    case 4 -> sendActionMessage(
                        event,
                        "{0} does a Nat 20 dive onto {1}. The floor fails its Dex save. 🎲");
                    case 5 -> sendActionMessage(
                        event,
                        "{0} rolls across the server floor with {1} in tow. It''s not a fight. It''s a love story. \uD83C\uDFAC");
                    case 6 -> sendActionMessage(
                        event,
                        "{0} flattens {1} like pancake batter on a Saturday morning. Tasty. 🥞");
                    case 7 -> sendActionMessage(
                        event,
                        "{0} lands a flying tackle and yells, \"Bonus action: *Hold Person*!\" {1} is grappled... emotionally. 🔒");
                    case 8 -> sendActionMessage(
                        event,
                        "{0} suplexes {1} into the nearest hay bale. Minecraft physics not included. 🐄");
                    case 9 -> sendActionMessage(
                        event,
                        "{0} yeets themself into {1} like they tripped over a redstone tripwire. Hug trap activated. ⚡");
                    case 10 -> sendActionMessage(
                        event,
                        "{0} calls in an orbital tackle strike on {1}. Impact detected: maximum snuggle velocity achieved! 🚀💥");
                }
            }

            case "pillow" -> {
                int random = r.nextInt(12);

                switch (random) {
                    case 0 -> sendActionMessage(
                        event,
                        "{0} catapults a pillow across the server. It hits {1} and explodes in glitter. Fabulous. ✨");
                    case 1 -> sendActionMessage(
                        event,
                        "{0} bonks {1} right on the noggin. Pillow-based dominance established. 💢");
                    case 2 -> sendActionMessage(
                        event,
                        "{0} throws a pillow enchanted with *Featherfall*. Unfortunately, {1} failed the save. 🍃");
                    case 3 -> sendActionMessage(
                        event,
                        "{0} smirks and hurls a glitter-filled pillow at {1}. Fluff and sass collide. 😏");
                    case 4 -> sendActionMessage(
                        event,
                        "{0} whips out a Minecraft bed and slaps {1} with it. It''s bedtime. Fight it. 🛏");
                    case 5 -> sendActionMessage(
                        event,
                        "{0} sneak-attacks {1} with a pillow to the face. That''s what they get for being cute. 😼");
                    case 6 -> sendActionMessage(
                        event,
                        "{0} bonks {1} with a pillow that''s suspiciously filled with nether bricks. Oops. 🧱");
                    case 7 -> sendActionMessage(
                        event,
                        "{0} dual-wields pillows and goes full Whirlwind Attack on {1}. It''s a flurry of fluff and fury. 🌀");
                    case 8 -> sendActionMessage(
                        event,
                        "{0} tosses a pillow at {1} and shouts, \"FOR JUSTICE!\" No context given. ⚖️");
                    case 9 -> sendActionMessage(
                        event,
                        "{0} drops a pillow from the rafters onto {1}. Surprise snuggle-strike successful. 🎯");
                    case 10 -> sendActionMessage(
                        event,
                        "{0} sends a meteor shower of pillows at {1}. Pillow fight level: *Epic*. 🌠");
                    case 11 -> sendActionMessage(
                        event,
                        "{0} launches an orbital pillow strike on {1}. Pillows rain down from the sky, and there is no escape. 🌌🛏️");
                }
            }
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
}
