package com.google.bitcoin.pows;

import static com.google.bitcoin.core.Utils.reverseBytes;
import static com.google.bitcoin.core.Utils.doubleDigest;
import com.google.bitcoin.core.Hash;
import com.google.bitcoin.core.ProofOfWork;

public class Sha256ProofOfWork extends ProofOfWork {
    protected Hash hash(byte[] header) {
        return new Hash(reverseBytes(doubleDigest(header)));
    }

    private static Sha256ProofOfWork instance;
    public static Sha256ProofOfWork get() {
        if(instance == null)
            instance = new Sha256ProofOfWork();
        return instance;
    }
}
