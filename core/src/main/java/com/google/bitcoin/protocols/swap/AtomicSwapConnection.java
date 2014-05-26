package com.google.bitcoin.protocols.swap;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2Session;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionException;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;

public class AtomicSwapConnection {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(AtomicSwapConnection.class);

    public static int PROTOCOL_VERSION = 1;

    private int requests = 0;
    private JSONRPC2Session session;

    AtomicSwapConnection(JSONRPC2Session session) {
        this.session = session;
    }

    public JSONRPC2Response request(String method)
    throws JSONRPC2SessionException {
        JSONRPC2Request req = new JSONRPC2Request(method, requests++);
        return session.send(req);
    }

    public static AtomicSwapConnection connect(String server)
    throws MalformedURLException {
        URL url = new URL(server);
        return new AtomicSwapConnection(new JSONRPC2Session(url));
    }
}
