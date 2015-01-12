package io.nextop.util;

import java.util.Arrays;
import java.util.List;

public class Urls {

    /** @param path leading '/' may be omitted */
    public static List<String> parseSegments(String path) {
        int j;
        if ('/' == path.charAt(0)) {
            j = 1;
        } else {
            j = 0;
        }

        final int length = path.length();
        int n = 0;

        for (int i = j; i <= length; ++i) {
            if (length == i || '/' == path.charAt(i)) {
                ++n;
            }
        }
        String[] segments = new String[n];
        n = 0;
        for (int i = j; i <= length; ++i) {
            if (length == i || '/' == path.charAt(i)) {
                segments[n++] = path.substring(j, i);
                j = i + 1;
            }
        }
        assert n == segments.length;
        return Arrays.asList(segments);
    }
}
