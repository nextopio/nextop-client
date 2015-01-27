package io.nextop;


import com.google.common.base.Objects;

import javax.annotation.Nullable;

public final class Authority {
    static enum Type {
        IP,
        NAMED,
        LOCAL
    }

    public static Authority valueOf(String s) {
        int n = s.length();

        if (n <= 0) {
            return local();
        }

        int i = n - 1;
        // find last : to be compatible with ipv6
        while (0 <= i && ':' != s.charAt(i)) {
            --i;
        }

        if (i < 0) {
            // no port - use the default
            try {
                return create(Ip.valueOf(s), 0);
            } catch (IllegalArgumentException e) {
                return create(DomainName.valueOf(s), 0);
            }
        } else {
            try {
                return create(Ip.valueOf(s.substring(0, i)), Integer.parseInt(s.substring(i + 1, n)));
            } catch (IllegalArgumentException e) {
                try {
                    return create(DomainName.valueOf(s.substring(0, i)), Integer.parseInt(s.substring(i + 1, n)));
                } catch (IllegalArgumentException e2) {
                    // could be an ipv6 with a default port
                    return create(DomainName.valueOf(s), 0);
                }
            }
        }
    }




    public static Authority create(Ip host, int port) {
        validatePort(port);
        return new Authority(Type.IP, host, port);
    }

    public static Authority create(DomainName host, int port) {
        validatePort(port);
        return new Authority(Type.NAMED, host, port);
    }

    public static Authority local() {
        return new Authority(Type.LOCAL, null, 0);
    }

    private static void validatePort(int port) {
        if ((port & 0xFFFF) != port) {
            throw new IllegalArgumentException();
        }
    }


    public final Type type;
    @Nullable
    private final Object host;
    /** <code>0</code> means default */
    public final int port;


    private Authority(Type type, @Nullable Object host, int port) {
        this.type = type;
        this.host = host;
        this.port = port;
    }


    public Ip getIp() {
        switch (type) {
            case IP:
                return (Ip) host;
            default:
                throw new IllegalStateException();
        }
    }

    public DomainName getDomainName() {
        switch (type) {
            case IP:
                return (DomainName) host;
            default:
                throw new IllegalStateException();
        }
    }

    public String getHost() {
        return host.toString();
    }




    @Override
    public String toString() {
        switch (type) {
            case IP:
            case NAMED:
                if (0 < port) {
                    return String.format("%s:%d", host, port);
                } else {
                    return String.format("%s", host);
                }
            case LOCAL:
                return "";
            default:
                throw new IllegalStateException();
        }
    }


    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Authority)) {
            return false;
        }
        Authority b = (Authority) o;

        return type.equals(b.type)
                && port == b.port
                && Objects.equal(host, b.host);
    }

    @Override
    public int hashCode() {
        int m = 31;
        int c = type.hashCode();
        c = m * c + Objects.hashCode(host);
        c = m * c + Integer.hashCode(port);
        return c;
    }

}
