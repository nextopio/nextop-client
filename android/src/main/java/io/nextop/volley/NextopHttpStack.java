package io.nextop.volley;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.toolbox.HttpStack;
import io.nextop.Message;
import io.nextop.MessageAndroid;
import io.nextop.Nextop;
import io.nextop.httpclient.NextopHttpClient;
import org.apache.http.HttpResponse;

import java.io.IOException;
import java.util.Map;

public class NextopHttpStack implements HttpStack {
    private final Nextop nextop;

    public NextopHttpStack(Nextop nextop) {
        this.nextop = nextop;
    }

    @Override
    public HttpResponse performRequest(Request<?> request, Map<String, String> additionalHeaders) throws IOException, AuthFailureError {
        Message.Builder b = fromRequestBuilder(request);
        for (Map.Entry<String, String> e : additionalHeaders.entrySet()) {
            b.setHeader(e.getKey(), e.getValue());
        }
        Message message = b.build();

        return NextopHttpClient.execute(nextop, message);
    }


    public static Message.Builder fromRequestBuilder(Request<?> request) {
        // FIXME
        return null;
    }
}
