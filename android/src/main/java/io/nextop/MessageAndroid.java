package io.nextop;

import android.net.Uri;

import java.net.URISyntaxException;

public class MessageAndroid {

    public static Message valueOf(Route.Method method, Uri uri) {
        try {
            return Message.valueOf(method, NextopAndroid.toURI(uri));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
