package io.nextop;

import java.util.LinkedList;
import java.util.List;

public class DomainName {

    public static DomainName valueOf(String s) {
        List<String> parts = new LinkedList<String>();
        int n = s.length();
        int j = n;
        for (int i = n - 1; 0 <= i; --i) {
            if (0 == i || '.' == i) {
                if (i + 1 == j) {
                    throw new IllegalArgumentException();
                }
                parts.add(s.substring(i + 1, j).toLowerCase());
            }
        }
        // TODO validate
        return new DomainName(parts.toArray(new String[parts.size()]));
    }


    // tld is [0]
    // top domain is [1]
    // 1st sub domain is [2]
    // ...
    // e.g. www.google.com -> ["com", "google", "www"]
    public final String[] parts;

    private DomainName(String ... parts) {
        this.parts = parts;
    }
}
