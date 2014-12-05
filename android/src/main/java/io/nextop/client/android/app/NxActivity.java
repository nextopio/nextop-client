package io.nextop.client.android.app;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.support.annotation.Nullable;
import io.nextop.client.android.NxClient;
import io.nextop.client.android.NxContext;

public class NxActivity extends Activity implements NxContext {

    @Nullable
    final NxConfig overConfig;

    @Nullable
    private _NxContext _nxContext = null;
    private NxContext nxContext = null;


    public NxActivity() {
        this(null);
    }

    /** @param overConfig may be useful for demos or testing,
     *                    but use null for optimal performance (shares the client with the {@link NxApplication}) */
    public NxActivity(@Nullable NxConfig overConfig) {
        this.overConfig = overConfig;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Application a = getApplication();
        // assume the base configuration is the same across application and activity (same resources)
        if (null == overConfig && (a instanceof NxContext) ||
                null != overConfig && (a instanceof NxApplication) &&
                        (null == ((NxApplication) a).overConfig || ((NxApplication) a).overConfig.isCompatible(overConfig))) {
            // the application context is compatible
            nxContext = (NxContext) a;
        } else {
            _nxContext = new _NxContext();
            NxConfig config = NxConfig.create(getResources());
            if (null != overConfig) {
                config = config.replace(overConfig);
            }
            _nxContext.setConfig(config);
            nxContext = _nxContext;

            _nxContext.onCreate(savedInstanceState);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (null != _nxContext) {
            _nxContext.onSaveInstanceState(outState);
        }
    }

    @Override
    protected void onDestroy() {
        if (null != _nxContext) {
            _nxContext.onDestroy();
        }

        super.onDestroy();
    }


    /////// NxContext IMPLEMENTATION ///////

    @Override
    public NxClient getClient() {
        return nxContext.getClient();
    }
}
