package me.jasonhorkles.fancyfriend.analytics;

import de.oliver.fancyanalytics.sdk.ApiClient;
import de.oliver.fancyanalytics.sdk.records.Record;
import me.jasonhorkles.fancyfriend.Secrets;

import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("Singleton")
public final class BotAnalytics {
    public final static String BASE_URL = "https://api.fancyanalytics.net";
    public final String projectId;

    private final ApiClient client;
    private final static BotAnalytics INSTANCE = new BotAnalytics();

    private BotAnalytics() {
        Secrets secrets = new Secrets();
        projectId = secrets.analyticsProjectId();

        client = new ApiClient(BASE_URL, "", secrets.analyticsApiToken());
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> new Thread(
            r,
            "FancyAnalytics"));

        executor.scheduleAtFixedRate(
            () -> client.getRecordService().createRecord(
                projectId,
                new Record("FancyFriend", projectId, System.currentTimeMillis(), new HashMap<>())),
            10,
            30,
            TimeUnit.SECONDS);
    }

    public static BotAnalytics get() {
        return INSTANCE;
    }

    public ApiClient getClient() {
        return client;
    }

    public String getProjectId() {
        return projectId;
    }
}
