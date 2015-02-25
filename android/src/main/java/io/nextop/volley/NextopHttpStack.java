package io.nextop.volley;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.toolbox.HttpStack;
import io.nextop.Nextop;
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
        // FIXME
    }
}
