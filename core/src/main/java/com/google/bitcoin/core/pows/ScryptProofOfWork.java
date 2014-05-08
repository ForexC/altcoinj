package com.google.bitcoin.core.pows;

import com.google.bitcoin.core.Hash;
import com.google.bitcoin.core.ProofOfWork;
import com.google.bitcoin.core.Utils;
import com.lambdaworks.crypto.SCrypt;

import java.security.GeneralSecurityException;
import java.util.Arrays;

public class ScryptProofOfWork extends ProofOfWork {
    protected int N, p, r, dkLen;

    public ScryptProofOfWork(int N, int p, int r, int dkLen) {
        super();
        this.N = N;
        this.p = p;
        this.r = r;
        this.dkLen = dkLen;
    }

    protected Hash hash(byte[] header) {
        try {
            byte[] h = Utils.reverseBytes(SCrypt.scrypt(header, header, N, r, p, dkLen));
            return new Hash(h);
        } catch(GeneralSecurityException ex) {
            return null;
        }
    }
}
