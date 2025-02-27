package io.lettuce.core.event.connection;

import io.lettuce.test.metrics.ConnectionKey;

public class ReconnectEventHelper {

    public static ConnectionKey connectionKey(ConnectionEventSupport event) {
        return ConnectionKey.create(event.localAddress(), event.remoteAddress(), event.getEpId());
    }

}
