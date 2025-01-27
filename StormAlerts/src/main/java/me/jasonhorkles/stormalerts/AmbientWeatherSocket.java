package me.jasonhorkles.stormalerts;

import io.socket.client.IO;
import io.socket.client.Socket;
import me.jasonhorkles.stormalerts.Utils.LogUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;

public class AmbientWeatherSocket {
    private final Socket socket;

    public AmbientWeatherSocket() {
        URI uri = URI.create("https://rt2.ambientweather.net/?api=1&applicationKey=" + new Secrets().awAppKey());
        IO.Options options = IO.Options.builder().setTransports(new String[]{"websocket"})
            .setReconnectionDelayMax(60000).build();
        socket = IO.socket(uri, options);

        socket.on(
            Socket.EVENT_CONNECT, args -> {
                JSONObject subscribeData = new JSONObject();
                JSONArray apiKeys = new JSONArray();
                apiKeys.put(new Secrets().awApiKey());
                subscribeData.put("apiKeys", apiKeys);

                socket.emit("subscribe", subscribeData);
            });

        socket.on("data", args -> processWeatherDataAsync(args[0].toString()));

        socket.on(
            "subscribed",
            args -> System.out.println(new LogUtils().getTime(LogUtils.LogColor.GREEN) + "Connected to Ambient Weather API."));

        socket.on(
            Socket.EVENT_DISCONNECT,
            args -> System.out.println(new LogUtils().getTime(LogUtils.LogColor.RED) + "Disconnected from Ambient Weather API."));

        socket.on(
            Socket.EVENT_CONNECT_ERROR,
            args -> System.out.println(new LogUtils().getTime(LogUtils.LogColor.RED) + "Connection error! " + args[0]));
    }

    private void processWeatherDataAsync(String data) {
        new Thread(
            () -> {
                try {
                    new AmbientWeatherProcessor().processWeatherData(data);
                } catch (Exception e) {
                    System.out.print(new LogUtils().getTime(LogUtils.LogColor.RED));
                    e.printStackTrace();
                    new LogUtils().logError(e);
                }
            }, "AmbientWeatherProcessor").start();
    }

    public void connect() {
        if (socket != null && !socket.connected()) {
            System.out.println(new LogUtils().getTime(LogUtils.LogColor.YELLOW) + "Attempting to connect to Ambient Weather...");
            socket.connect();
        }
    }

    public void disconnect() {
        if (socket != null && socket.connected()) {
            System.out.println(new LogUtils().getTime(LogUtils.LogColor.YELLOW) + "Disconnecting from Ambient Weather...");
            socket.disconnect();
        }
    }
}
