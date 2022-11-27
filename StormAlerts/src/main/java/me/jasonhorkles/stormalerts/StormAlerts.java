package me.jasonhorkles.stormalerts;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Scanner;
import java.util.concurrent.*;

@SuppressWarnings({"ConstantConditions"})
public class StormAlerts extends ListenerAdapter {
    public static JDA jda;
    public static final boolean testing = false;

    private static ScheduledFuture<?> alertTimer;
    private static ScheduledFuture<?> pwsTimer;
    private static ScheduledFuture<?> weatherTimer;
    private static final ArrayList<ScheduledFuture<?>> trafficTimers = new ArrayList<>();

    public static void main(String[] args) throws InterruptedException, ParseException {
        System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Starting...");

        JDABuilder builder = JDABuilder.createDefault(new Secrets().getBotToken());
        builder.disableCache(CacheFlag.ACTIVITY, CacheFlag.CLIENT_STATUS, CacheFlag.ONLINE_STATUS,
            CacheFlag.VOICE_STATE);
        builder.enableIntents(GatewayIntent.GUILD_MESSAGES);
        builder.setMemberCachePolicy(MemberCachePolicy.NONE);
        builder.setBulkDeleteSplittingEnabled(false);
        builder.setStatus(OnlineStatus.DO_NOT_DISTURB);
        builder.setEnableShutdownHook(false);
        builder.addEventListeners(new Events(), new Weather());
        jda = builder.build();

        jda.awaitReady();

        //noinspection ConstantConditions
        jda.getGuildById(843919716677582888L).updateCommands()
            .addCommands(Commands.slash("checknow", "Force all checks")).queue();


        // Alerts
        alertTimer = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                new Alerts().checkAlerts();
            } catch (Exception e) {
                System.out.println(new Utils().getTime(Utils.LogColor.RED) + "[ERROR] Couldn't get the alerts!");
                e.printStackTrace();
            }
        }, 1, 180, TimeUnit.SECONDS);


        // PWS / Rain / Lightning
        pwsTimer = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                new Pws().checkConditions();
            } catch (Exception e) {
                String reason = "";
                if (e.getMessage().contains("401")) reason = " (Unauthorized)";
                else if (e.getMessage().contains("500")) reason = " (Internal Server Error)";
                else if (e.getMessage().contains("502")) reason = " (Bad Gateway)";
                else if (e.getMessage().contains("503")) reason = " (Service Unavailable)";

                System.out.println(
                    new Utils().getTime(Utils.LogColor.RED) + "[ERROR] Couldn't get the PWS conditions!" + reason);
                if (reason.equals("")) {
                    System.out.print(new Utils().getTime(Utils.LogColor.RED));
                    e.printStackTrace();
                }
            }
        }, 3, 90, TimeUnit.SECONDS);


        // Weather
        weatherTimer = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                new Weather().checkConditions();
            } catch (Exception e) {
                System.out.println(
                    new Utils().getTime(Utils.LogColor.RED) + "[ERROR] Couldn't get the weather conditions!");
                e.printStackTrace();
                jda.getPresence().setStatus(OnlineStatus.DO_NOT_DISTURB);
                jda.getPresence().setActivity(Activity.playing("Error checking weather!"));
            }
        }, 5, 90, TimeUnit.SECONDS);

        // Schedule traffic checks
        new StormAlerts().scheduleTrafficCheck("2:40 PM", true);
        new StormAlerts().scheduleTrafficCheck("5:50 PM", false);
        new StormAlerts().scheduleTrafficCheck("6:00 PM", false);

        // Send select menu message if needed
        try {
            TextChannel channel = jda.getTextChannelById(843919716677582891L);
            ArrayList<SelectOption> selectOptions = new ArrayList<>();

            selectOptions.add(
                SelectOption.of("New NWS Alerts", "850471646191812700").withEmoji(Emoji.fromUnicode("⚠️")));
            selectOptions.add(
                SelectOption.of("NWS Alert Updates", "850471690093854810").withEmoji(Emoji.fromUnicode("📝")));
            selectOptions.add(
                SelectOption.of("BETA New Records (Coming Soon)", "1046149064519073813")
                    .withEmoji(Emoji.fromUnicode("📊")));
            selectOptions.add(SelectOption.of("Snow", "845055624165064734").withEmoji(Emoji.fromUnicode("🌨️")));
            selectOptions.add(SelectOption.of("Hail", "845055784156397608").withEmoji(Emoji.fromUnicode("🧊")));
            selectOptions.add(SelectOption.of("Rain", "843956362059841596").withEmoji(Emoji.fromUnicode("🌦️")));
            selectOptions.add(SelectOption.of("Heavy Rain", "843956325690900503").withEmoji(Emoji.fromUnicode("🌧️")));
            selectOptions.add(
                SelectOption.of("BETA High Wind (Coming Soon)", "1046148944108978227")
                    .withEmoji(Emoji.fromUnicode("🍃")));
            selectOptions.add(
                SelectOption.of("Lightning Info", "896877424824954881").withEmoji(Emoji.fromUnicode("⚡")));

            if (new Utils().getMessages(channel, 1).get(30, TimeUnit.SECONDS).isEmpty())
                channel.sendMessage("**Select your desired notifications below:**\n*Each selection acts as a toggle*")
                    .addActionRow(StringSelectMenu.create("role-select").addOptions(selectOptions).setMinValues(0)
                        .setMaxValues(selectOptions.size()).build())
                    .addActionRow(Button.secondary("viewroles", "Your Roles")).queue();
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            System.out.print(new Utils().getTime(Utils.LogColor.RED));
            e.printStackTrace();
        }

        jda.addEventListener(new Events());

        // Add shutdown hooks
        Runtime.getRuntime().addShutdownHook(new Thread(() -> new StormAlerts().shutdown(), "Shutdown Hook"));
        Thread input = new Thread(() -> {
            while (true) {
                Scanner in = new Scanner(System.in);
                String text = in.nextLine();
                if (text.equalsIgnoreCase("stop")) System.exit(0);
                if (text.equalsIgnoreCase("traffic n")) new Traffic().checkTraffic(true);
                if (text.equalsIgnoreCase("traffic s")) new Traffic().checkTraffic(false);
            }
        }, "Console Input");
        input.start();

        System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + "Done starting up!");
    }

    private void scheduleTrafficCheck(String time, boolean toWork) throws ParseException {
        DayOfWeek day = LocalDate.now().getDayOfWeek();
        if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
            DateFormat format = new SimpleDateFormat("yyyy-MM-dd h:mm a");
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(format.parse(LocalDate.now() + " " + time));

            long delay = calendar.getTimeInMillis() - System.currentTimeMillis();

            if (delay >= 0) {
                trafficTimers.add(Executors.newSingleThreadScheduledExecutor()
                    .schedule(() -> new Traffic().checkTraffic(toWork), delay, TimeUnit.MILLISECONDS));
                System.out.println(
                    new Utils().getTime(Utils.LogColor.GREEN) + "Scheduled traffic check in " + Math.round(
                        delay / 3600000.0) + " hours.");
            }
        }
    }

    public void shutdown() {
        System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Shutting down...");
        alertTimer.cancel(true);
        pwsTimer.cancel(true);
        weatherTimer.cancel(true);
        if (!trafficTimers.isEmpty()) for (ScheduledFuture<?> task : trafficTimers) task.cancel(true);
        if (Weather.previousTypeChannel != null) {
            Message message = null;
            try {
                message = new Utils().getMessages(Weather.previousTypeChannel, 1).get(1, TimeUnit.SECONDS).get(0);
                Thread.sleep(500);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                System.out.print(new Utils().getTime(Utils.LogColor.RED));
                e.printStackTrace();
            }
            if (!message.getContentRaw().contains("Ended") && !message.getContentRaw().contains("restarted"))
                message.editMessage(message.getContentRaw()
                    .replace("!", "! (Bot restarted at <t:" + System.currentTimeMillis() / 1000 + ":t>)")).complete();
            Weather.previousTypeChannel = null;
        }
        try {
            jda.shutdownNow();
        } catch (NoClassDefFoundError ignored) {
        }
    }
}
