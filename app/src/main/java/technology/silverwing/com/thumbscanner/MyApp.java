package technology.silverwing.com.thumbscanner;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

public class MyApp extends Application {

    private static MyApp mInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;

    }

    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    public static synchronized MyApp getInstance() {
        return mInstance;
    }
}
