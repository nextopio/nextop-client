package io.nextop;


import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

// paths can use "$var" segments
// optionally starts with a '/', see Urls.parseSegments
public class Path {

    public static Path valueOf(String s) {
        final int n = s.length();

        int j;
        if (0 < n && '/' == s.charAt(0)) {
            j = 1;
        } else {
            j = 0;
        }

        LinkedList<Segment> segments = new LinkedList<Segment>();
        for (int i = j; i <= n; ++i) {
            if (n == i || '/' == s.charAt(i)) {
                segments.add(Segment.valueOf(s.substring(j, i)));
                j = i + 1;
            }
        }

        return new Path(ImmutableList.copyOf(segments));
    }

    public static Path empty() {
        return new Path(Collections.<Segment>emptyList());
    }


    public final List<Segment> segments;


    private Path(List<Segment> segments) {
        this.segments = segments;
    }


    public boolean isFixed() {
        for (Segment segment : segments) {
            if (!Segment.Type.FIXED.equals(segment.type)) {
                return false;
            }
        }
        return true;
    }

    public List<String> getVariables() {
        List<String> variables = new LinkedList<String>();
        for (Segment segment : segments) {
            if (Segment.Type.VARIABLE.equals(segment.type)) {
                variables.add(segment.value);
            }
        }
        return variables;
    }


    @Override
    public String toString() {
        return "/" + Joiner.on("/").join(segments);
    }

    @Override
    public int hashCode() {
        return segments.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Path)) {
            return false;
        }
        Path b = (Path) obj;
        return segments.equals(b.segments);
    }




    public static final class Segment {
        public static enum Type {
            FIXED,
            VARIABLE
        }


        private static Segment valueOf(String s) {
            int n = s.length();
            if (n <= 0) {
                throw new IllegalArgumentException();
            }
            if ('$' == s.charAt(0)) {
                String value = s.substring(1, n);
                if (!M_VARIABLE.matcher(value).matches()) {
                    throw new IllegalArgumentException();
                }
                return new Segment(Type.VARIABLE, value);
            } else {
                String value = s;
                if (!M_FIXED.matcher(value).matches()) {
                    throw new IllegalArgumentException();
                }
                return new Segment(Type.FIXED, value);
            }
        }


        public final Type type;
        public final String value;

        private Segment(Type type, String value) {
            this.type = type;
            this.value = value;
        }

        @Override
        public String toString() {
            switch (type) {
                case FIXED:
                    return value;
                case VARIABLE:
                    return "$" + value;
                default:
                    throw new IllegalStateException();
            }
        }

        @Override
        public int hashCode() {
            int c = type.hashCode();
            c = 31 * c + value.hashCode();
            return c;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Segment)) {
                return false;
            }
            Segment b = (Segment) obj;
            return type.equals(b.type) && value.equals(b.value);
        }



        // FIXME ref spec
        private static final Pattern M_FIXED = Pattern.compile("[a-z0-9-_;]+");
        private static final Pattern M_VARIABLE = Pattern.compile("[a-z0-9-]+");
    }
}
