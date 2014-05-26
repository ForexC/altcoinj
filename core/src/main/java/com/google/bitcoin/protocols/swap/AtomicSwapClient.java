package com.google.bitcoin.protocols.swap;

import com.google.bitcoin.protocols.channels.IPaymentChannelClient;
import com.google.bitcoin.utils.Threading;
import net.jcip.annotations.GuardedBy;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.ReentrantLock;

public class AtomicSwapClient {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(AtomicSwapClient.class);

    protected final ReentrantLock lock = Threading.lock("swapclient");

    private enum Step {

    }

    protected AtomicSwapConnection connection;

    public AtomicSwapClient() {

    }
}
