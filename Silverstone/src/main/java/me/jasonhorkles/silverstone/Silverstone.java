package me.jasonhorkles.silverstone;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import javax.security.auth.login.LoginException;
import java.util.Scanner;

@SuppressWarnings("BusyWait")
public class Silverstone {
    public static JDA api;

    public static void main(String[] args) throws LoginException, InterruptedException {
        System.out.println(new Utils().getTime(Utils.Color.YELLOW) + "Starting...");

        JDABuilder builder = JDABuilder.createDefault(new Secrets().getBotToken());
        builder.disableIntents(GatewayIntent.GUILD_MESSAGE_TYPING);
        builder.disableCache(CacheFlag.ACTIVITY);
        builder.enableIntents(GatewayIntent.GUILD_PRESENCES, GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGES,
            GatewayIntent.GUILD_VOICE_STATES);
        builder.enableCache(CacheFlag.ONLINE_STATUS, CacheFlag.VOICE_STATE);
        builder.setMemberCachePolicy(MemberCachePolicy.ALL);
        builder.setBulkDeleteSplittingEnabled(false);
        builder.setStatus(OnlineStatus.ONLINE);
        builder.setActivity(Activity.watching("Dave"));
        builder.setEnableShutdownHook(false);
        api = builder.build();

        // Wait until the api works
        while (api.getGuildById(455919765999976461L) == null) Thread.sleep(100);

        api.addEventListener(new AntiScam(), new Events(), new Testing());

        //noinspection ConstantConditions
        CommandListUpdateAction commands = api.getGuildById(455919765999976461L).updateCommands();

        commands.addCommands(Commands.slash("ecdebug", "EntityClearer debug"),
                Commands.slash("paste", "Get a link to paste text to")
                    .addOption(OptionType.STRING, "what", "What should be pasted", true),
                Commands.slash("plgh", "Links to the plugins on GitHub"),
                Commands.slash("plugins", "Get a list of Jason's plugins"),
                Commands.slash("tutorials", "Link to the tutorial channel"),
                Commands.slash("moss", "M.O.S.S. Discord invite"), Commands.slash("lp", "LuckPerms Discord invite"))
            .queue();

        new Time().updateTime();

        // Add shutdown hooks
        Runtime.getRuntime().addShutdownHook(new Thread(() -> new Silverstone().shutdown(), "Shutdown Hook"));
        Thread input = new Thread(() -> {
            while (true) {
                Scanner in = new Scanner(System.in);
                String text = in.nextLine();
                if (text.equalsIgnoreCase("stop")) System.exit(0);
            }
        }, "Console Input");
        input.start();

        System.out.println(new Utils().getTime(Utils.Color.GREEN) + "Done starting up!");
    }

    public void shutdown() {
        System.out.println(new Utils().getTime(Utils.Color.YELLOW) + "Shutting down...");
        try {
            api.shutdownNow();
        } catch (NoClassDefFoundError ignored) {
        }
    }
}
