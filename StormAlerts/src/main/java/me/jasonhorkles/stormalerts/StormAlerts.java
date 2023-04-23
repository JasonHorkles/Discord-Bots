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
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.*;

@SuppressWarnings({"DataFlowIssue"})
public class StormAlerts extends ListenerAdapter {
    public static final ArrayList<ScheduledFuture<?>> scheduledTimers = new ArrayList<>();
    public static final boolean testing = false;
    public static JDA jda;


    public static void main(String[] args) throws InterruptedException, ParseException {
        System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Starting...");

        JDABuilder builder = JDABuilder.createDefault(new Secrets().getBotToken());
        builder.disableCache(CacheFlag.ACTIVITY, CacheFlag.CLIENT_STATUS, CacheFlag.ONLINE_STATUS,
            CacheFlag.VOICE_STATE);
        builder.enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS);
        builder.setMemberCachePolicy(MemberCachePolicy.ALL);
        builder.setBulkDeleteSplittingEnabled(false);
        builder.setStatus(OnlineStatus.DO_NOT_DISTURB);
        builder.setEnableShutdownHook(false);
        builder.addEventListeners(new Events(), new Weather());
        jda = builder.build();

        jda.awaitReady();

        //noinspection DataFlowIssue
        jda.getGuildById(843919716677582888L).updateCommands()
            .addCommands(Commands.slash("checknow", "Force all checks (except records)"),
                Commands.slash("updaterecords", "Force the record checks")).queue();


        // Alerts
        scheduledTimers.add(Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                new Alerts().checkAlerts();
            } catch (Exception e) {
                System.out.println(
                    new Utils().getTime(Utils.LogColor.RED) + "[ERROR] Couldn't get the alerts!");
                e.printStackTrace();
            }
        }, 1, 180, TimeUnit.SECONDS));


        // PWS / Rain / Lightning
        scheduledTimers.add(Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                new Pws().checkConditions();
            } catch (Exception e) {
                String reason = "";
                if (e.getMessage().contains("401")) reason = " (Unauthorized)";
                else if (e.getMessage().contains("500")) reason = " (Internal Server Error)";
                else if (e.getMessage().contains("502")) reason = " (Bad Gateway)";
                else if (e.getMessage().contains("503")) reason = " (Service Unavailable)";

                System.out.println(new Utils().getTime(
                    Utils.LogColor.RED) + "[ERROR] Couldn't get the PWS conditions!" + reason);
                if (reason.equals("")) {
                    System.out.print(new Utils().getTime(Utils.LogColor.RED));
                    e.printStackTrace();
                }
            }
        }, 3, 90, TimeUnit.SECONDS));


        // Weather
        scheduledTimers.add(Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                new Weather().checkConditions();
            } catch (Exception e) {
                System.out.println(
                    new Utils().getTime(Utils.LogColor.RED) + "[ERROR] Couldn't get the weather conditions!");
                e.printStackTrace();
                jda.getPresence().setStatus(OnlineStatus.DO_NOT_DISTURB);
                jda.getPresence().setActivity(Activity.playing("Error checking weather!"));
            }
        }, 5, 90, TimeUnit.SECONDS));

        // Schedule records announcement
        try {
            new Records().scheduleRecordCheck();
        } catch (Exception e) {
            System.out.println(new Utils().getTime(Utils.LogColor.RED) + "Error grabbing the records!");
            e.printStackTrace();
        }

        // Schedule traffic checks
        new Traffic().scheduleTrafficCheck("2:40 PM", true);
        new Traffic().scheduleTrafficCheck("5:50 PM", false);
        new Traffic().scheduleTrafficCheck("6:00 PM", false);

        // Send select menu message if needed
        try {
            TextChannel channel = jda.getTextChannelById(843919716677582891L);
            ArrayList<SelectOption> selectOptions = new ArrayList<>();

            selectOptions.add(
                SelectOption.of("New NWS Alerts", "850471646191812700").withEmoji(Emoji.fromUnicode("⚠️")));
            selectOptions.add(
                SelectOption.of("NWS Alert Updates", "850471690093854810").withEmoji(Emoji.fromUnicode("📝")));
            selectOptions.add(
                SelectOption.of("New Records", "1046149064519073813").withEmoji(Emoji.fromUnicode("📊")));
            selectOptions.add(
                SelectOption.of("Snow", "845055624165064734").withEmoji(Emoji.fromUnicode("🌨️")));
            selectOptions.add(
                SelectOption.of("Hail", "845055784156397608").withEmoji(Emoji.fromUnicode("🧊")));
            selectOptions.add(
                SelectOption.of("Rain", "843956362059841596").withEmoji(Emoji.fromUnicode("🌦️")));
            selectOptions.add(
                SelectOption.of("Heavy Rain", "843956325690900503").withEmoji(Emoji.fromUnicode("🌧️")));
            selectOptions.add(
                SelectOption.of("High Wind", "1046148944108978227").withEmoji(Emoji.fromUnicode("🍃")));
            selectOptions.add(
                SelectOption.of("Lightning Info", "896877424824954881").withEmoji(Emoji.fromUnicode("⚡")));

            if (new Utils().getMessages(channel, 1).get(30, TimeUnit.SECONDS).isEmpty()) channel.sendMessage(
                    "**Select your desired notifications below:**\n*Each selection acts as a toggle*")
                .addActionRow(StringSelectMenu.create("role-select").addOptions(selectOptions).setMinValues(0)
                    .setMaxValues(selectOptions.size()).build())
                .addActionRow(Button.secondary("viewroles", "Your Roles")).queue();
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            System.out.print(new Utils().getTime(Utils.LogColor.RED));
            e.printStackTrace();
        }

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

    public void shutdown() {
        System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Shutting down...");

        System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + "Dumping record data...");
        try {
            String filePath = "StormAlerts/records-today.json";

            JSONObject allRecords = new JSONObject();
            allRecords.put("highLightningRate", Records.highestLightningRateToday);
            allRecords.put("highTemp", Records.highestTempToday);
            allRecords.put("highUv", Records.highestUvToday);
            allRecords.put("lowTemp", Records.lowestTempToday);
            allRecords.put("maxLightning", Records.maxLightningToday);
            allRecords.put("maxRainAmount", Records.maxRainAmountToday);
            allRecords.put("maxRainRate", Records.maxRainRateToday);
            allRecords.put("maxWind", Records.maxWindToday);

            allRecords.put("highLightningRateTime", Records.highestLightningRateTime);
            allRecords.put("highTempTime", Records.highestTempTime);
            allRecords.put("highUvTime", Records.highestUvTime);
            allRecords.put("lowTempTime", Records.lowestTempTime);
            allRecords.put("maxLightningTime", Records.maxLightningTime);
            allRecords.put("maxRainAmountTime", Records.maxRainAmountTime);
            allRecords.put("maxRainRateTime", Records.maxRainRateTime);
            allRecords.put("maxWindTime", Records.maxWindTime);

            FileWriter recordsToday = new FileWriter(filePath, false);
            recordsToday.write(allRecords.toString());
            recordsToday.close();

        } catch (IOException e) {
            System.out.println(new Utils().getTime(
                Utils.LogColor.RED) + "Unable to write to records file! Dumping to DMs...");

            System.out.print(new Utils().getTime(Utils.LogColor.RED));
            e.printStackTrace();

            jda.openPrivateChannelById(277291758503723010L).flatMap(channel -> channel.sendMessage(
                MessageFormat.format(
                    "Error saving records file!\nLightning rate: {0}:{8}\nTemp high: {1}:{9}\nUV high: {2}:{10}\nTemp low: {3}:{11}\nLightning today: {4}:{12}\nRain today: {5}:{13}\nRain rate: {6}:{14}\nWind: {7}:{15}",
                    Records.highestLightningRateToday, Records.highestTempToday, Records.highestUvToday,
                    Records.lowestTempToday, Records.maxLightningToday, Records.maxRainAmountToday,
                    Records.maxRainRateToday, Records.maxWindToday, Records.highestLightningRateTime,
                    Records.highestTempTime, Records.highestUvTime, Records.lowestTempTime,
                    Records.maxLightningTime, Records.maxRainAmountTime, Records.maxRainRateTime,
                    Records.maxWindTime))).complete();
        }

        if (!scheduledTimers.isEmpty()) for (ScheduledFuture<?> task : scheduledTimers) task.cancel(true);

        if (Weather.previousTypeChannel != null) {
            Message message = null;
            try {
                message = new Utils().getMessages(Weather.previousTypeChannel, 1).get(1, TimeUnit.SECONDS)
                    .get(0);
                Thread.sleep(500);

            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                System.out.print(new Utils().getTime(Utils.LogColor.RED));
                e.printStackTrace();
            }

            if (!message.getContentRaw().contains("Ended") && !message.getContentRaw().contains("restarted"))
                message.editMessage(message.getContentRaw()
                        .replace("!", "! (Bot restarted at <t:" + System.currentTimeMillis() / 1000 + ":t>)"))
                    .complete();
            Weather.previousTypeChannel = null;
        }

        try {
            // Initating the shutdown, this closes the gateway connection and subsequently closes the requester queue
            jda.shutdown();
            // Allow at most 10 seconds for remaining requests to finish
            if (!jda.awaitShutdown(10,
                TimeUnit.SECONDS)) { // returns true if shutdown is graceful, false if timeout exceeded
                jda.shutdownNow(); // Cancel all remaining requests, and stop thread-pools
                jda.awaitShutdown(); // Wait until shutdown is complete (indefinitely)
            }
        } catch (NoClassDefFoundError | InterruptedException ignored) {
        }
    }
}
