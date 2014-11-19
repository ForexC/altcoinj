package org.bitcoinj.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;

import static org.bitcoinj.core.Utils.doubleDigest;

/**
 * <p></p>
 */
public abstract class ProofOfWork implements Serializable {
    protected abstract Sha256Hash hash(byte[] header);

    public Sha256Hash getHash(Block block) {
        try {
            ByteArrayOutputStream bos = new UnsafeByteArrayOutputStream(Block.HEADER_SIZE);
            block.writeHeader(bos);
            return hash(bos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e); // Cannot happen.
        }
    }

    public boolean check(Block block, boolean throwException) throws VerificationException {
        BigInteger target = block.getDifficultyTargetAsInteger();

        BigInteger h = getHash(block).toBigInteger();
        if (h.compareTo(target) > 0) {
            // Proof of work check failed!
            if (throwException)
                throw new VerificationException("Sha256Hash is higher than target: " + block.getHashAsString() + " vs "
                        + target.toString(16));
            else
                return false;
        }
        return true;
    };
}
