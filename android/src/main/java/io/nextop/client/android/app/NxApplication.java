package io.nextop.client.android.app;

import android.app.Application;
import android.support.annotation.Nullable;
import io.nextop.client.android.NxClient;

public class NxApplication extends Application implements NxContext {
    @Nullable
    final NxConfig overConfig;

    @Nullable
    private _NxContext _nxContext = null;






    /////// NxContext IMPLEMENTATION ///////

    @Override
    public NxClient getClient() {
        return nxContext.getClient();
    }
}
