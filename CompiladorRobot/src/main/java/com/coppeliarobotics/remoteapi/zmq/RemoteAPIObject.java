package com.coppeliarobotics.remoteapi.zmq;

import co.nstant.in.cbor.CborException;
import java.util.Map;

public class RemoteAPIObject {
    protected final RemoteAPIClient client;
    protected final String name;

    public RemoteAPIObject(RemoteAPIClient client) {
        this.client = client;
        this.name = "";
    }

    public RemoteAPIObject(RemoteAPIClient client, String name, Map<String, Object> args) {
        this.client = client;
        this.name = name;
    }

    public Object[] call(String func, Object... args) throws CborException {
        return this.client.call(this.name + "." + func, args);
    }
}