package io.nextop.client.http;

import io.nextop.org.apache.http.HttpException;

public class ReceiveHttpException extends HttpException {

    public ReceiveHttpException(HttpException cause) {
        super(null, cause);
    }
}
