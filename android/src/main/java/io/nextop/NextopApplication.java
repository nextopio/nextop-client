package io.nextop;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import com.google.common.annotations.Beta;

@Beta
public class NextopApplication extends Application implements NextopContext {
    private Nextop nextop;



    public NextopApplication() {
    }


    @Override
    public void onCreate() {
        super.onCreate();

        nextop = Nextop.create(this).start();
    }


    @Override
    public Nextop getNextop() {
        return nextop;
    }

}
