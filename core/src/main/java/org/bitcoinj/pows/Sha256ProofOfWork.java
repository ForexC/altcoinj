package org.bitcoinj.pows;

import static org.bitcoinj.core.Utils.reverseBytes;
import static org.bitcoinj.core.Utils.doubleDigest;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.ProofOfWork;

public class Sha256ProofOfWork extends ProofOfWork {
    protected Sha256Hash hash(byte[] header) {
        return new Sha256Hash(reverseBytes(doubleDigest(header)));
    }

    private static Sha256ProofOfWork instance;
    public static Sha256ProofOfWork get() {
        if(instance == null)
            instance = new Sha256ProofOfWork();
        return instance;
    }
}
