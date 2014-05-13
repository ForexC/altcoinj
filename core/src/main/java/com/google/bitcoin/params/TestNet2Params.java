/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.bitcoin.params;

import com.google.bitcoin.core.*;
import com.google.bitcoin.pows.Sha256ProofOfWork;
import com.google.bitcoin.store.BlockStore;
import com.google.bitcoin.store.BlockStoreException;

import java.math.BigInteger;
import java.util.Date;

import static com.google.bitcoin.core.Utils.COIN;
import static com.google.common.base.Preconditions.checkState;

/**
 * Parameters for the old version 2 testnet. This is not useful to you - it exists only because some unit tests are
 * based on it.
 */
public class TestNet2Params extends NetworkParameters {

    // February 16th 2012
    private static final Date DIFF_DATE = new Date(1329264000000L);

    protected Date diffDate;

    public TestNet2Params() {
        maxMoney = new BigInteger("21000000", 10).multiply(COIN);
        alertSigningKey = SATOSHI_KEY;
        genesisBlock = createGenesis(this, MainNetParams.GENESIS_INPUT, MainNetParams.GENESIS_SCRIPTPUBKEY);
        id = ID_TESTNET;
        packetMagic = 0xfabfb5daL;
        port = 18333;
        addressHeader = 111;
        p2shHeader = 196;
        acceptableAddressCodes = new int[] { addressHeader, p2shHeader };
        interval = MainNetParams.INTERVAL;
        targetTimespan = MainNetParams.TARGET_TIMESPAN;
        targetSpacing = MainNetParams.TARGET_SPACING;
        proofOfWork = Sha256ProofOfWork.get();
        proofOfWorkLimit = Utils.decodeCompactBits(0x1d0fffffL);
        dumpedPrivateKeyHeader = 239;
        genesisBlock.setTime(1296688602L);
        genesisBlock.setDifficultyTarget(0x1d07fff8L);
        genesisBlock.setNonce(384568319);
        spendableCoinbaseDepth = 100;
        subsidyDecreaseBlockCount = 210000;
        String genesisHash = genesisBlock.getHashAsString();
        checkState(genesisHash.equals("00000007199508e34a9ff81e6ec0c477a4cccff2a4767a8eee39c11db367b008"));
        dnsSeeds = null;
        bloomFiltersEnabled = true;
        diffDate = DIFF_DATE;
    }

    @Override
    public void checkDifficulty(StoredBlock storedPrev, Block nextBlock, BlockStore blockStore)
    throws BlockStoreException, VerificationException {
        if(!shouldRetarget(storedPrev) && nextBlock.getTime().after(diffDate)) {
            // After 15th February 2012 the rules on the testnet change to avoid people running up the difficulty
            // and then leaving, making it too hard to mine a block. On non-difficulty transition points, easy
            // blocks are allowed if there has been a span of 20 minutes without one.
            Block prev = storedPrev.getHeader();
            final long timeDelta = nextBlock.getTimeSeconds() - prev.getTimeSeconds();
            // There is an integer underflow bug in bitcoin-qt that means mindiff blocks are accepted when time
            // goes backwards.
            if (timeDelta >= 0 && timeDelta <= targetSpacing * 2) {
                // Walk backwards until we find a block that doesn't have the easiest proof of work, then check
                // that difficulty is equal to that one.
                StoredBlock cursor = storedPrev;
                while (!cursor.getHeader().equals(genesisBlock) &&
                        cursor.getHeight() % getInterval(storedPrev.getHeight()) != 0 &&
                        cursor.getHeader().getDifficultyTargetAsInteger().equals(proofOfWorkLimit))
                    cursor = cursor.getPrev(blockStore);
                BigInteger cursorDifficulty = cursor.getHeader().getDifficultyTargetAsInteger();
                BigInteger newDifficulty = nextBlock.getDifficultyTargetAsInteger();
                if (!cursorDifficulty.equals(newDifficulty))
                    throw new VerificationException("Testnet block transition that is not allowed: " +
                            Long.toHexString(cursor.getHeader().getDifficultyTarget()) + " vs " +
                            Long.toHexString(nextBlock.getDifficultyTarget()));
            }

        // If we are at a retarget interval or before Feb 15 2012, do a normal difficulty check
        } else {
            super.checkDifficulty(storedPrev, nextBlock, blockStore);
        }
    }

    private static TestNet2Params instance;
    public static synchronized TestNet2Params get() {
        if (instance == null) {
            instance = new TestNet2Params();
        }
        return instance;
    }

    public String getPaymentProtocolId() {
        return null;
    }
}
