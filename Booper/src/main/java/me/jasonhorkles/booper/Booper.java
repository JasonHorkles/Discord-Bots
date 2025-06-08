package me.jasonhorkles.booper;

import me.jasonhorkles.booper.events.SlashCommands;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class Booper {
    public static JDA jda;

    public static void main(String[] args) throws InterruptedException {
        System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Starting...");

        JDABuilder builder = JDABuilder.createDefault(new Secrets().botToken());
        builder.disableCache(CacheFlag.ACTIVITY, CacheFlag.VOICE_STATE);
        builder.setEnableShutdownHook(false);
        builder.setStatus(OnlineStatus.ONLINE);
        builder.setActivity(Activity.customStatus("Booper Booping"));
        builder.addEventListeners(new SlashCommands());
        jda = builder.build();

        jda.awaitReady();

        Guild guild = jda.getGuildById(1299547538445307986L);
        if (guild == null) {
            System.out.println(new Utils().getTime(Utils.LogColor.RED) + "Guild not found! Exiting...");
            new Booper().shutdown();
            return;
        }

        guild.updateCommands().addCommands(
            Commands.slash("hug", "Give someone a hug!")
                .addOption(OptionType.MENTIONABLE, "hug", "Who do you want to hug?", true),
            Commands.slash("tackle", "Tackle someone!")
                .addOption(OptionType.MENTIONABLE, "tackle", "Who do you want to tackle?", true),
            Commands.slash("pillow", "Smack someone with a pillow!")
                .addOption(OptionType.MENTIONABLE, "pillow", "Who do you want to pillow?", true)).queue();

        // Add shutdown hooks
        Runtime.getRuntime().addShutdownHook(new Thread(() -> new Booper().shutdown(), "Shutdown Hook"));
        new Thread(
            () -> {
                Scanner in = new Scanner(System.in, StandardCharsets.UTF_8);
                while (true) {
                    String text = in.nextLine();
                    if (text.equalsIgnoreCase("stop")) {
                        in.close();
                        System.exit(0);
                    }
                }
            }, "Console Input").start();

        System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + "Done starting up!");
    }

    public void shutdown() {
        System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Shutting down...");
        try {
            // Initating the shutdown, this closes the gateway connection and subsequently closes the requester queue
            jda.shutdown();
            // Allow at most 10 seconds for remaining requests to finish
            if (!jda.awaitShutdown(
                10,
                TimeUnit.SECONDS)) { // returns true if shutdown is graceful, false if timeout exceeded
                jda.shutdownNow(); // Cancel all remaining requests, and stop thread-pools
                jda.awaitShutdown(); // Wait until shutdown is complete (indefinitely)
            }
        } catch (NoClassDefFoundError | InterruptedException ignored) {
        }
    }
}