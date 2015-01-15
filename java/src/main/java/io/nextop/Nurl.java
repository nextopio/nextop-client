package io.nextop;

import io.nextop.Authority;

import javax.annotation.Nullable;

// a url factored to work better with nextop concepts
public class Nurl {

    Target target;
    @Nullable
    Via via;


    // target is a method+path
//    Target target;
    // via is a scheme+authority
    // if via is missing, assume it is in the implied via (e.g. for nextop, the access key domain)
//    Via via;



    public static enum Method {
        GET,
        POST,
        // FIXME expand HTTP methods
        SUBSCRIBE,
        UNSUBSCRIBE
    }

    public static class Target {
        Method method;
        // paths can use "$var" segments
        String path;
    }

    public static class Via {
        Scheme scheme;
        // FIXME the host can be named or ip
        Authority authority;
    }

    public static enum Scheme {
        HTTP,
        HTTPS
    }


}
