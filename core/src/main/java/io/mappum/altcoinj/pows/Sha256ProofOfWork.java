package io.mappum.altcoinj.pows;

import static io.mappum.altcoinj.core.Utils.reverseBytes;
import static io.mappum.altcoinj.core.Utils.doubleDigest;
import io.mappum.altcoinj.core.Sha256Hash;
import io.mappum.altcoinj.core.ProofOfWork;

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
