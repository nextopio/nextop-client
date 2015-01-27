package io.nextop;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Pattern;

public final class DomainName {

    public static DomainName valueOf(String s) {
        int n = s.length();

        if (n <= 0) {
            throw new IllegalArgumentException();
        }

        List<String> labels = new LinkedList<String>();
        int j = n;
        for (int i = n - 1; 0 <= i; --i) {
            if ('.' == i) {
                if (i + 1 == j) {
                    throw new IllegalArgumentException();
                }
                // normalize to lower case (domain names are case-insensitive)
                labels.add(s.substring(i + 1, j).toLowerCase());
                j = i;
            }
        }
        if (1 == j) {
            throw new IllegalArgumentException();
        }
        // normalize to lower case (domain names are case-insensitive)
        labels.add(s.substring(0, j).toLowerCase());

        for (String label : labels) {
            if (!M_LABEL.matcher(label).matches()) {
                throw new IllegalArgumentException();
            }
        }
        // this does not validate tlds or public suffixes
        // see https://code.google.com/p/guava-libraries/wiki/InternetDomainNameExplained

        return new DomainName(ImmutableList.copyOf(labels));
    }


    // tld is [0]
    // top domain is [1]
    // 1st sub domain is [2]
    // ...
    // e.g. www.google.com -> ["com", "google", "www"]
    public final List<String> labels;

    private DomainName(List<String> labels) {
        this.labels = labels;
    }


    @Override
    public String toString() {
        return Joiner.on('.').join(labels);
    }

    @Override
    public int hashCode() {
        return labels.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DomainName)) {
            return false;
        }
        DomainName b = (DomainName) obj;
        return labels.equals(b.labels);
    }


    /** @see http://en.wikipedia.org/wiki/Domain_name */
    private static final Pattern M_LABEL = Pattern.compile("[a-z0-9]+(?:[a-z0-9-]+[a-z0-9]+)?");
}
