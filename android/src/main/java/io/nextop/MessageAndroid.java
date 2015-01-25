package io.nextop;

import android.net.Uri;

import javax.annotation.Nullable;
import java.net.URI;
import java.net.URISyntaxException;

public class MessageAndroid {

    public static Message valueOf(Nurl.Method method, Uri uri) {
        try {
            return Message.valueOf(method, NextopAndroid.toURI(uri));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
