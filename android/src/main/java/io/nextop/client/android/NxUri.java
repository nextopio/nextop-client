package io.nextop.client.android;

import android.net.Uri;
import android.support.annotation.Nullable;

/** immutable URI+method
 * - supports $SYMBOL variables in the URI. These are replaced by bound parameters
 *   when converting {@link #toString} or {@link #toUri}.
 * - Data stored hierarchically allows common parts to be efficiently shared */
public abstract class NxUri {

    public static NxUri decode(String s) {
        // FIXME
        return null;
    }




    public static enum Method {
        POST,
        GET
    }
    public static enum Protocol {
        HTTP,
        HTTPS
    }


    // FIXME all these are final
    int noncommutativeHashCode;
    int commutativeHashCode;

    Protocol protocol;
    String host;
    int port;

    @Nullable Method method;

    @Nullable S pathSegments;
    @Nullable Q queryParams;
    @Nullable P params;


    public NxUri(String host) {

    }

    public NxUri(Protocol protocol, String host) {

    }

    public NxUri(Protocol protocol, String host, int port) {

        // FIXME non commutative
    }


    /////// TRANSFORMS ///////

    public NxUri method(@Nullable Method method) {
        // FIXME
        return null;
    }

    public NxUri appendPath(String path) {

        // FIXME non commutative
        // FIXME
        return null;
    }

    public NxUri appendQuery(String name, String value) {

        // FIXME commutative
        // FIXME
        return null;
    }

    public NxUri param(String name, Object value) {

        // FIXME commutative
        // FIXME
        return null;
    }



    @Override
    public String toString() {
        return null;
    }

    @Override
    public int hashCode() {
        final int prime = 37;
        return noncommutativeHashCode + prime * (commutativeHashCode + prime * method.hashCode());
    }

    @Override
    public boolean equals(Object b) {
        if (!(b instanceof NxUri)) {
            return false;
        }
        NxUri ub = (NxUri) b;
        if (noncommutativeHashCode != ub.noncommutativeHashCode ||
                commutativeHashCode != ub.commutativeHashCode) {
            return false;
        }
        // FIXME go through all fields
        return false;
    }


    /////// ANDROID ///////

    public static NxUri valueOf(Uri uri) {
        // FIXME
        return null;
    }

    public Uri toUri() {
        // FIXME throw exception if some params not bound
        return null;
    }


    private static final class S {
        static enum Type {
            STATIC,
            VARIABLE
        }

        @Nullable S parent;
        Type type;
        String value;

    }
    private static final class Q {
        static enum Type {
            STATIC,
            VARIABLE
        }

        @Nullable Q parent;
        Type type;
        String name;
        String value;

    }
    private static final class P {
        @Nullable P parent;
        String name;
        Object value;
    }
}
