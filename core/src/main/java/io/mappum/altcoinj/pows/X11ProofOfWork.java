package io.mappum.altcoinj.pows;

import io.mappum.altcoinj.core.ProofOfWork;
import io.mappum.altcoinj.core.Sha256Hash;
import io.mappum.altcoinj.core.Utils;
import fr.cryptohash.*;

import java.util.Arrays;

public class X11ProofOfWork extends ProofOfWork {
    public Sha256Hash hash(byte[] header) {
        byte[] digest;

        BLAKE512 blake512 = new BLAKE512();
        digest = blake512.digest(header);

        BMW512 bmw = new BMW512();
        digest = bmw.digest(digest);

        Groestl512 groestl = new Groestl512();
        digest = groestl.digest(digest);

        Skein512 skein = new Skein512();
        digest = skein.digest(digest);

        JH512 jh = new JH512();
        digest = jh.digest(digest);

        Keccak512 keccak = new Keccak512();
        digest = keccak.digest(digest);

        Luffa512 luffa = new Luffa512();
        digest = luffa.digest(digest);

        CubeHash512 cubehash = new CubeHash512();
        digest = cubehash.digest(digest);

        SHAvite512 shavite = new SHAvite512();
        digest = shavite.digest(digest);

        SIMD512 simd = new SIMD512();
        digest = simd.digest(digest);

        ECHO512 echo = new ECHO512();
        digest = echo.digest(digest);

        return new Sha256Hash(Utils.reverseBytes(Arrays.copyOfRange(digest, 0, 32)));
    }

    private static X11ProofOfWork instance;
    public static X11ProofOfWork get() {
        if(instance == null)
            instance = new X11ProofOfWork();
        return instance;
    }
}
