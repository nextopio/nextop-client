package io.nextop;


public final class Authority {
    public static Authority valueOf(String s) {
        int n = s.length();
        int i = n - 1;
        // find last : to be compatible with ipv6
        while (0 <= i && ':' != s.charAt(i)) {
            --i;
        }
        if (i < 0) {
            throw new IllegalArgumentException();
        }

        Ip host = Ip.valueOf(s.substring(0, i));
        int port = Integer.parseInt(s.substring(i + 1, n));
        return new Authority(host, port);
    }




    public static Authority create(Ip host, int port) {
        if (port < 0 || Short.MAX_VALUE < port) {
            throw new IllegalArgumentException();
        }
        return new Authority(host, port);
    }


    public final Ip host;
    public final int port;


    private Authority(Ip host, int port) {
        this.host = host;
        this.port = port;
    }


    @Override
    public String toString() {
        return String.format("%s:%d", host, port);
    }


    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Authority)) {
            return false;
        }
        Authority b = (Authority) o;
        return port == b.port && host.equals(b.host);
    }

    @Override
    public int hashCode() {
        int m = 31;
        int c = host.hashCode();
        c = m * c + Integer.hashCode(port);
        return c;
    }

}
