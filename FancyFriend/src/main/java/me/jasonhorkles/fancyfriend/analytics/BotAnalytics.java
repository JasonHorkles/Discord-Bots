package me.jasonhorkles.fancyfriend.analytics;

import de.oliver.fancyanalytics.sdk.ApiClient;
import de.oliver.fancyanalytics.sdk.records.Record;
import me.jasonhorkles.fancyfriend.Secrets;

import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BotAnalytics {

    private final static BotAnalytics INSTANCE = new BotAnalytics();
    public final static String BASE_URL = "https://api.fancyanalytics.net";

    private final ApiClient client;
    public final String projectId;
    private final ScheduledExecutorService executor;

    private BotAnalytics() {
        Secrets secrets = new Secrets();
        this.projectId = secrets.analyticsProjectId();

        this.client = new ApiClient(BASE_URL, "", secrets.analyticsApiToken());
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "FancyAnalytics"));

        this.executor.scheduleAtFixedRate(() -> {
            client.getRecordService().createRecord(projectId, new Record("FancyFriend", projectId, System.currentTimeMillis(), new HashMap<>()));
        }, 5, 5, TimeUnit.SECONDS);
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

    public ScheduledExecutorService getExecutor() {
        return executor;
    }
}
