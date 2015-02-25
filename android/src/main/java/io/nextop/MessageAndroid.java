package io.nextop;

import android.net.Uri;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;

import javax.annotation.Nullable;
import java.net.URISyntaxException;

public class MessageAndroid {

    public static Message valueOf(Route.Method method, Uri uri) {
        try {
            return Message.valueOf(method, NextopAndroid.toURI(uri));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }


    /** @param nextop if not null, enables abort */
    public static Message fromHttpRequest(HttpHost httpHost, HttpRequest httpRequest, @Nullable Nextop nextop) {
        // FIXME support request.abort

        // FIXME adapt org.apache.* to io.nextop.org.apache.* then use Message.fromHttpRequest,
        // FIXME and adapt back with abort behavior mixed in

        return null;
    }

    public static HttpResponse toHttpResponse(Message message) {
        // FIXME use Message.toHttpResponse and adapt back with abort behavior mixed in

        return null;
    }
}
