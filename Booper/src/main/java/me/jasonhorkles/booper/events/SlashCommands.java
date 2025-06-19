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
                String[] messages = {
                    "{0} wraps their arms around {1} and refuses to let go. You live here now. 💞",
                    "{0} hugs {1} like a cleric who just rolled a Nat 20 on *Cure Wounds*. That''s the good stuff. 🛐",
                    "{0} sneaks up behind {1} and hugs them like they mean it. No escape detected. 🤗",
                    "{0} uses *Hug*. It''s super effective! {1} regains 1d8 Emotional HP. 🎯",
                    "{0} gives {1} a hug so warm, it''s basically a Minecraft campfire buff. 🔥",
                    "{0} pulls {1} into a hug that lasts just long enough to make it awkward. You''re welcome. 😏",
                    "{0} glomps {1} and now they share a Mind Link. Too bad it''s 98% affection and memes. 📡",
                    "{0} hugs {1} with all the commitment of placing obsidian. You ain''t breaking this easy. 🧱",
                    "{0} snuggles into {1} like a blanket with abandonment issues. 🐾",
                    "{0} activates Hug Protocol: full embrace, gentle squeeze, +2 to Charisma checks with {1} for the next hour. 💫",
                    "{0} wraps their arms around {1} and refuses to let go. You live here now. 💞",
                    "{0} gives {1} a Nat 20 hug. It''s super effective! 🎯",
                    "{0} hugs {1} like a clingy anime sidekick in episode 2. 🤗",
                    "{0} activates *Hug Protocol*. {1} has been emotionally compromised. 🧠💘",
                    "{0} sneaks up behind {1} like a Goose Goose Duck Assassin—except it''s just a hug. 🦆💨",
                    "{0} uses *Snuggle Beam*! {1} is now paralyzed by affection. ⚡",
                    "{0} drops a fluffy Cuddle Crit on {1}. Resistance was futile. 💥",
                    "{0} hugs {1} like their anime OP just started playing. 🎶",
                    "{0} buries their face into {1} like a cat to a warm laptop. 🐱🔥",
                    "{0} initiates full-body cling. {1} is now backpack-certified. 🎒",
                    "{0} hugs {1} with the urgency of a character at the end of a JRPG. 🕊",
                    "{0} uses *Hug* — a critical hit! {1} feels oddly flustered. 🥴",
                    "{0} hugs {1} like a Pokéfan hugs their first shiny. ✨",
                    "{0} gently tackles {1} into a hug pile and yells, \"Group anime moment!\" 🫂",
                    "{0} glomps {1}. You hear a soft \"nya~\" in the distance. 😽",
                    "{0} gives {1} a hug so powerful, it disrupts the multiverse. 🌌",
                    "{0} hugs {1} like they''re the impostor—but with *emotional honesty*. 👀",
                    "{0} casts *Mass Snuggle*. {1} is the epicenter. 💫",
                    "{0} whispers \"get loved fool\" while hugging {1} from behind. 🐾",
                    "{0} hugs {1} so hard, their hitpoints went up. IRL potion vibes. 🧪"
                };

                int random = r.nextInt(messages.length);
                sendActionMessage(event, messages[random]);
            }

            case "tackle" -> {
                String[] messages = {
                    "{0} charges at {1} like a linebacker made of fluff. They''ve been taken down. 💥",
                    "{0} tackles {1} straight to the ground. Cuddle combat has begun. 🛏",
                    "{0} pounces on {1} like a creeper that decided *exploding with love* is a vibe. 💚💣",
                    "{0} hits {1} like an affectionate freight train. No survivors. Only snuggles. 🚂",
                    "{0} does a Nat 20 dive onto {1}. The floor fails its Dex save. 🎲",
                    "{0} rolls across the server floor with {1} in tow. It''s not a fight. It''s a love story. 🎬",
                    "{0} flattens {1} like pancake batter on a Saturday morning. Tasty. 🥞",
                    "{0} lands a flying tackle and yells, \"Bonus action: *Hold Person*!\" {1} is grappled... emotionally. 🔒",
                    "{0} suplexes {1} into the nearest hay bale. Minecraft physics not included. 🐄",
                    "{0} yeets themself into {1} like they tripped over a redstone tripwire. Hug trap activated. ⚡",
                    "{0} calls in an orbital tackle strike on {1}. Impact detected: maximum snuggle velocity achieved! 🚀💥",
                    "{0} tackles {1} like a rogue with unresolved feelings. 🖤",
                    "{0} charges at {1} like a paladin in fuzzy slippers. 🧷",
                    "{0} pounces on {1} like a Goose after you ate bread meant for another. 🦆💢",
                    "{0} hits {1} with the force of 1,000 Poké Balls. 🎯",
                    "{0} delivers a sneak tackle to {1}, then plays it off as an anime meet-cute. 💥💘",
                    "{0} yeets into {1} like a creeper with cuddle damage. 💚💣",
                    "{0} rolls a 20 for Snuggle Slam. {1} fails the Dex save. 🎲",
                    "{0} uses *Tackle*! {1} took 12 points of blush damage. 🌡",
                    "{0} jumps on {1} like a surprise filler arc. No warning. 🌀",
                    "{0} flattens {1} with a hug lariat. Stream ends in 3...2... 💫",
                    "{0} suplexes {1} into the cuddle plane. Wobbuffet approves. 🤼",
                    "{0} tackles {1} with anime sound effects \"Wooow\" and glitter. 💫✨",
                    "{0} bonks {1} like they''re the Dodo Bird. Goose Goose Duck vengeance! 🦆🔪",
                    "{0} tackles {1} through a Minecraft wall. You hear a faint \"oof.\" ⛏️",
                    "{0} grabs {1} mid-jump. They spin. You hear the Kingdom Hearts theme. 🌌🎵",
                    "{0} goes full Tsundere and tackles {1} while yelling \"i-it''s not like I LIKE you!\" 😤",
                    "{0} performs a *tackle-teleport* straight into {1}''s heart. 💘",
                    "{0} runs at {1} screaming \"Senpai noticed meeeee—!!\" and tackles. 🥹",
                    "{0} dive rolls into {1} like a Pokémon with max speed EVs. 🌀",
                    "{0} launches a wrestling move on {1} and whispers, \"you''re valid\" during impact. 💪💙"
                };

                int random = r.nextInt(messages.length);
                sendActionMessage(event, messages[random]);
            }

            case "pillow" -> {
                String[] messages = {
                    "{0} catapults a pillow across the server. It hits {1} and explodes in glitter. Fabulous. ✨",
                    "{0} bonks {1} right on the noggin. Pillow-based dominance established. 💢",
                    "{0} throws a pillow enchanted with *Featherfall*. Unfortunately, {1} failed the save. 🍃",
                    "{0} smirks and hurls a glitter-filled pillow at {1}. Fluff and sass collide. 😏",
                    "{0} whips out a Minecraft bed and slaps {1} with it. It''s bedtime. Fight it. 🛏",
                    "{0} sneak-attacks {1} with a pillow to the face. That''s what they get for being cute. 😼",
                    "{0} bonks {1} with a pillow that''s suspiciously filled with nether bricks. Oops. 🧱",
                    "{0} dual-wields pillows and goes full Whirlwind Attack on {1}. It''s a flurry of fluff and fury. 🌀",
                    "{0} tosses a pillow at {1} and shouts, \"FOR JUSTICE!\" No context given. ⚖️",
                    "{0} drops a pillow from the rafters onto {1}. Surprise snuggle-strike successful. 🎯",
                    "{0} sends a meteor shower of pillows at {1}. Pillow fight level: *Epic*. 🌠",
                    "{0} launches an orbital pillow strike on {1}. Pillows rain down from the sky, and there is no escape. 🌌🛏️",
                    "{0} bonks {1} with a heart-shaped pillow full of glitter. ✨",
                    "{0} throws a pillow at {1} like it''s a Poké Ball. Roll for catch rate. 🔴",
                    "{0} sneak-attacks {1} with a goose-feather pillow. Suspiciously thematic. 🦆",
                    "{0} yeets a pillow at {1} while yelling \"PIKAAA...no wait just BONK!\" ⚡",
                    "{0} uses *Pillow Bonk*. {1} took +5 embarrassment damage. 💢",
                    "{0} hurls a pillow at {1} and shouts \"FOR HONOR AND FLUFF!\" ⚔️",
                    "{0} surprise pillow ambushes {1} like a rogue in a onesie. 😈",
                    "{0} flings a pillow at {1} so fast, anime wind lines appear. 🌀",
                    "{0} smacks {1} with a pillow and whispers \"baka~\" for no reason. 😏",
                    "{0} throws a pillow at {1} then hides behind a Minecraft block. Cowardice level: adorable. ⛏️",
                    "{0} channels a bard''s vengeance into a pillow slap. It rhymed. 🎶",
                    "{0} bonks {1} with a pillow full of Pokémon cards. Illegal? Should be. Effective? Absolutely. 🃏",
                    "{0} summons a Featherstorm. {1} now looks like a plucked Torchic. 🐥",
                    "{0} tosses a magical girl pillow. {1} is now sparkly and confused. 💖💫",
                    "{0} uses *Fluff Missile* on {1}. No cooldown. No regrets. 🚀",
                    "{0} fires a pillow at {1} like a Pelican spitting up a corpse. GGD chaos achieved. 🦆💀",
                    "{0} bonks {1} with a D20 pillow. It rolled a 1, but you both laughed. 🎲",
                    "{0} throws a pillow at {1} and shouts \"BOOP.EXE HAS NO MERCY.\" 💢🐾",
                    "{0} slaps {1} with a body pillow featuring their anime fave. Emotional damage: maxed. 🖼",
                    "{0} dives into a pillow fight with {1}, yells \"FILLER EPISODE,\" and vanishes. 🍿"
                };

                int random = r.nextInt(messages.length);
                sendActionMessage(event, messages[random]);
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
