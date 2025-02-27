package io.lettuce.test.metrics;

import java.net.SocketAddress;
import java.util.Objects;

public class ConnectionKey {

    private final SocketAddress localAddress;

    private final SocketAddress remoteAddress;

    private final String epId;

    protected ConnectionKey(SocketAddress localPort, SocketAddress remotePort, String epId) {
        this.localAddress = localPort;
        this.remoteAddress = remotePort;
        this.epId = epId;
    }

    public SocketAddress getLocalAddress() {
        return localAddress;
    }

    public SocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    public String getEpId() {
        return epId;
    }

    @Override
    public String toString() {
        return "localPort=" + localAddress + ", remotePort=" + remoteAddress;
    }

    public static ConnectionKey create(SocketAddress localPort, SocketAddress remotePort, String epId) {
        return new ConnectionKey(localPort, remotePort, epId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ConnectionKey that))
            return false;
        return Objects.equals(localAddress, that.localAddress) && Objects.equals(remoteAddress, that.remoteAddress)
                && Objects.equals(epId, that.epId);
    }

    @Override
    public int hashCode() {

        int result = localAddress.hashCode();
        result = 31 * result + remoteAddress.hashCode();
        result = 31 * result + epId.hashCode();
        return result;
    }

}
