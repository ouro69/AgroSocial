package com.example.agrosocial;

import android.app.Application;
import okhttp3.OkHttpClient;
import com.yandex.mapkit.MapKitFactory;

public class MyApplication extends Application {

    private static MyApplication instance;
    private OkHttpClient httpClient;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;


        MapKitFactory.setApiKey("a3ba052f-c83c-45c8-822a-7531a7128541");
        MapKitFactory.initialize(this);

        // Инициализация глобального клиента OkHttp
        httpClient = new OkHttpClient();
    }

    public static MyApplication getInstance() {
        return instance;
    }

    public OkHttpClient getHttpClient() {
        return httpClient;
    }
}