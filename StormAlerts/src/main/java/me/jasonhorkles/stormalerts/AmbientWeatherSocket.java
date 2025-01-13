package me.jasonhorkles.stormalerts;

import io.socket.client.IO;
import io.socket.client.Socket;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;

public class AmbientWeatherSocket {
    private final Socket socket;

    public AmbientWeatherSocket() {
        URI uri = URI.create("https://rt2.ambientweather.net/?api=1&applicationKey=" + new Secrets().awAppKey());
        IO.Options options = IO.Options.builder().setTransports(new String[]{"websocket"}).setReconnection(
            false).setReconnectionDelayMax(60000).build();
        socket = IO.socket(uri, options);

        socket.on(
            Socket.EVENT_CONNECT, args -> {
                JSONObject subscribeData = new JSONObject();
                JSONArray apiKeys = new JSONArray();
                apiKeys.put(new Secrets().awApiKey());
                subscribeData.put("apiKeys", apiKeys);

                socket.emit("subscribe", subscribeData);
            });

        socket.on(
            "data", args -> {
                try {
                    new AmbientWeatherProcessor().processWeatherData(args[0].toString());
                } catch (Exception e) {
                    String reason = "";
                    /*if (e.getMessage().contains("401")) reason = " (Unauthorized)";
                    else if (e.getMessage().contains("500")) reason = " (Internal Server Error)";
                    else if (e.getMessage().contains("502")) reason = " (Bad Gateway)";
                    else if (e.getMessage().contains("503")) reason = " (Service Unavailable)";
                    else if (e.getMessage().contains("504")) reason = " (Gateway Timeout)";
                    else if (e.getMessage().contains("520")) reason = " (Catch-all error)";
                    else if (e.getMessage().contains("524")) reason = " (Timeout)";*/

                    System.out.println(new Utils().getTime(Utils.LogColor.RED) + "[ERROR] Couldn't get the PWS conditions!" + reason);
                    if (reason.isBlank()) {
                        System.out.print(new Utils().getTime(Utils.LogColor.RED));
                        e.printStackTrace();
                        new Utils().logError(e);
                    }
                }
            });

        socket.on(
            "subscribed",
            args -> System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + "Connected to Ambient Weather API."));

        socket.on(
            Socket.EVENT_DISCONNECT,
            args -> System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + "Disconnected from Ambient Weather API."));

        socket.on(
            Socket.EVENT_CONNECT_ERROR,
            args -> System.out.println(new Utils().getTime(Utils.LogColor.RED) + "Connection error! " + args[0]));
    }

    public void connect() {
        if (socket != null && !socket.connected()) {
            System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Attempting to connect to Ambient Weather...");
            socket.connect();
        }
    }

    public void disconnect() {
        if (socket != null && socket.connected()) {
            System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Disconnecting from Ambient Weather...");
            socket.disconnect();
        }
    }
}
