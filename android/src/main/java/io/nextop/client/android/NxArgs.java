package io.nextop.client.android;

import android.support.annotation.Nullable;

public interface NxArgs {
    /** HTTP body value is translated to this arg key. */
    String BODY_KEY = "$body$";


    Iterable<String> keys();

    @Nullable NxByteString getBytes(String key);
    @Nullable String getString(String key);
    int getInt(String key, int defaultValue);
    long getLong(String key, long defaultValue);
    float getFloat(String key, float defaultValue);
    double getDouble(String key, double defaultValue);


    /////// ANDROID ///////

    @Nullable <T> T get(String key);
}
