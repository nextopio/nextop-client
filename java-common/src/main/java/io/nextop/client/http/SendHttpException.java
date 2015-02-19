package io.nextop.client.http;

import io.nextop.org.apache.http.HttpException;

public class SendHttpException extends HttpException {

    public SendHttpException(HttpException cause) {
        super(null, cause);
    }
}
