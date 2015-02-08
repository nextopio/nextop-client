package io.nextop;

import javax.annotation.Nullable;

// a url factored to work better with nextop concepts
// in nextop, Authority.local() is an alias for "$access-key.nextop.io",
// which the edge uses for control messages
public final class Route {
    public static final Via LOCAL = new Via(Scheme.NEXTOP, Authority.local());


    public static Route valueOf(String s) {
        // the string version is a mesh of the via and target
        // mirror the parsing rules in Target, but apply them in a custom way

        try {
            int n = s.length();
            if (n <= 0) {
                throw new IllegalArgumentException();
            }
            int i = s.indexOf(' ');
            if (i < 0) {
                throw new IllegalArgumentException();
            }
            String d = "://";
            int j = s.indexOf(d, i + 1);
            if (i < 0) {
                throw new IllegalArgumentException();
            }
            int k = s.indexOf('/', j + d.length());

            Method method = Method.valueOf(s.substring(0, i).toUpperCase());
            Via via;
            Path path;
            if (k < 0) {
                via = Via.valueOf(s.substring(i + 1, n));
                path = Path.empty();
            } else {
                via = Via.valueOf(s.substring(i + 1, k));
                path = Path.valueOf(s.substring(k + 1, n));
            }
            return new Route(new Target(method, path), via);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(s, e);
        }
    }



    public static Route create(Target target, Via via) {
        return new Route(target, via);
    }

    public static Route local(Target target) {
        return new Route(target, LOCAL);
    }

    public final Target target;
    public final Via via;


    private Route(Target target, Via via) {
        this.target = target;
        this.via = via;
    }


    @Nullable
    public Id getLocalId() {
        if (via.isLocal() && target.path.isFixed() && 1 <= target.path.segments.size()) {
            Path.Segment first = target.path.segments.get(0);
            assert Path.Segment.Type.FIXED.equals(first.type);
            try {
                return Id.valueOf(first.value);
            } catch (IllegalArgumentException e) {
                // FIXME log, strange ... could be a client-generated bad value
                return null;
            }
        }
        return null;
    }



    @Override
    public String toString() {
        return String.format("%s %s%s", target.method, via, target.path);
    }


    @Override
    public int hashCode() {
        int c = target.hashCode();
        c = 31 * c + via.hashCode();
        return c;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Route)) {
            return false;
        }
        Route b = (Route) obj;
        return target.equals(b.target) && via.equals(b.via);
    }



    public static enum Method {
        GET,
        HEAD,
        POST,
        PUT
        // FIXME expand HTTP methods
    }

    public static enum Scheme {
        HTTP,
        HTTPS,
        NEXTOP
    }


    public static class Target {
        public static Target valueOf(String s) {
            // e.g. "GET /"
            int n = s.length();
            int i = s.indexOf(' ');
            if (i < 0) {
                throw new IllegalArgumentException();
            }
            Method method = Method.valueOf(s.substring(0, i).toUpperCase());
            Path path = Path.valueOf(s.substring(i + 1, n));
            return new Target(method, path);
        }


        public static Target create(Method method, Path path) {
            return new Target(method, path);
        }


        public final Method method;
        public final Path path;

        private Target(Method method, Path path) {
            this.method = method;
            this.path = path;
        }

        @Override
        public String toString() {
            return String.format("%s %s", method, path);
        }

        @Override
        public int hashCode() {
            int c = method.hashCode();
            c = 31 * c + path.hashCode();
            return c;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Target)) {
                return false;
            }
            Target b = (Target) obj;
            return method.equals(b.method) && path.equals(b.path);
        }
    }

    public static class Via {
        public static Via valueOf(String s) {
            // e.g. "https://nextop.io"
            String d = "://";
            int n = s.length();
            int i = s.indexOf(d);
            if (i < 0) {
                throw new IllegalArgumentException();
            }
            Scheme scheme = Scheme.valueOf(s.substring(0, i).toUpperCase());
            Authority authority = Authority.valueOf(s.substring(i + d.length(), n));
            return new Via(scheme, authority);
        }


        public static Via create(Scheme scheme, Authority authority) {
            return new Via(scheme, authority);
        }


        public final Scheme scheme;
        public final Authority authority;


        private Via(Scheme scheme, Authority authority) {
            this.scheme = scheme;
            this.authority = authority;
        }


        public boolean isLocal() {
            return LOCAL.equals(this);
        }


        @Override
        public String toString() {
            return String.format("%s://%s", scheme.toString().toLowerCase(), authority);
        }

        @Override
        public int hashCode() {
            int c = scheme.hashCode();
            c = 31 * c + authority.hashCode();
            return c;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Via)) {
                return false;
            }
            Via b = (Via) obj;
            return scheme.equals(b.scheme) && authority.equals(b.authority);
        }
    }
}
