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
import com.google.bitcoin.pows.ScryptProofOfWork;
import com.google.bitcoin.store.BlockStore;
import com.google.bitcoin.store.BlockStoreException;
import org.spongycastle.util.encoders.Hex;

import static com.google.common.base.Preconditions.checkState;

/**
 * Parameters for the main production network on which people trade goods and services.
 */
public class DogecoinMainNetParams extends NetworkParameters {
    public static final byte[] GENESIS_INPUT = Hex.decode("04ffff001d0104084e696e746f6e646f");
    public static final byte[] GENESIS_SCRIPTPUBKEY = Hex.decode("040184710fa689ad5023690c80f3a49c8f13f8d45b8c857fbcbc8bc4a8e4d3eb4b10f4d4604fa08dce601aaf0f470216fe1b51850b4acf21b179c45070ac7b03a9");

    public static final int TARGET_SPACING = 60;
    public static final int TARGET_DIFF = 145000;
    public static final int TARGET_INTERVAL = 4 * 60;

    public static final byte[] ALERT_KEY = Hex.decode("04d4da7a5dae4db797d9b0644d57a5cd50e05a70f36091cd62e2fc41c98ded06340be5a43a35e185690cd9cde5d72da8f6d065b499b06f51dcfba14aad859f443a");

    private static ScryptProofOfWork proofOfWorkInstance;

    public DogecoinMainNetParams() {
        maxMoney = null;
        alertSigningKey = ALERT_KEY;
        genesisBlock = createGenesis(this, GENESIS_INPUT, GENESIS_SCRIPTPUBKEY);
        if(proofOfWorkInstance == null)
            proofOfWork = new ScryptProofOfWork(1024, 1, 1, 32);
        maxTarget = Utils.decodeCompactBits(0x1e0fffffL);
        dumpedPrivateKeyHeader = 158;
        addressHeader = 30;
        p2shHeader = 22;
        acceptableAddressCodes = new int[] { addressHeader, p2shHeader };
        port = 22556;
        packetMagic = 0xc0c0c0c0;
        genesisBlock.setDifficultyTarget(0x1e0ffff0L);
        genesisBlock.setTime(1386325540L);
        genesisBlock.setNonce(99943L);
        genesisBlock.getTransactions().get(0).getOutput(0).setValue(Utils.toNanoCoins(88, 0));
        id = "org.dogecoin.production";
        subsidyDecreaseBlockCount = 100000;
        spendableCoinbaseDepth = 100;
        String genesisHash = genesisBlock.getHashAsString();
        checkState(genesisHash.equals("1a91e3dace36e2be3bf030a65679fe821aa1d6ef92e7c9902eb318182c355691"),
                genesisHash);

        dnsSeeds = new String[] {
                "seed.dogecoin.com",
                "seed.mophides.com",
                "seed.dglibrary.org",
                "seed.dogechain.info"
        };
    }

    @Override
    public int getInterval(int height) {
        if(height < TARGET_DIFF) return TARGET_INTERVAL;
        else return 1;
    }

    @Override
    protected int getTargetTimespan(int height) {
        return getInterval(height) * TARGET_SPACING;
    }

    // Mostly copied/pasted :/
    @Override
    protected int getTimespan(StoredBlock storedPrev, BlockStore blockStore)
            throws BlockStoreException, VerificationException {
        Block prev = storedPrev.getHeader();

        // We need to find a block far back in the chain. It's OK that this is expensive because it only occurs every
        // two weeks after the initial block chain download.
        long now = System.currentTimeMillis();
        int blockspan = getInterval(storedPrev.getHeight()) - (storedPrev.getHeight() + 1 == TARGET_INTERVAL ? 1 : 0);
        StoredBlock cursor = blockStore.get(prev.getHash());
        for (int i = 0; i < blockspan; i++) {
            if (cursor == null) {
                // This should never happen. If it does, it means we are following an incorrect or busted chain.
                throw new VerificationException(
                        "Difficulty transition point but we did not find a way back to the genesis block.");
            }
            cursor = blockStore.get(cursor.getHeader().getPrevBlockHash());
        }

        Block blockIntervalAgo = cursor.getHeader();
        int timespan = (int) (prev.getTimeSeconds() - blockIntervalAgo.getTimeSeconds());

        // Limit the adjustment step.
        int targetTimespan = getTargetTimespan(storedPrev.getHeight());

        if (storedPrev.getHeight() >= TARGET_DIFF) {
            timespan = targetTimespan + (timespan - targetTimespan) / 8;
            if (timespan < (targetTimespan - targetTimespan / 4))
                timespan = (targetTimespan - targetTimespan / 4);
            if (timespan > (targetTimespan + targetTimespan / 2))
                timespan = (targetTimespan + targetTimespan / 2);
        } else if (storedPrev.getHeight() >= 10000) {
            if (timespan < targetTimespan / 4)
                timespan = targetTimespan / 4;
            if (timespan > targetTimespan * 4)
                timespan = targetTimespan * 4;
        } else if (storedPrev.getHeight() >= 5000) {
            if (timespan < targetTimespan / 8)
                timespan = targetTimespan / 8;
            if (timespan > targetTimespan * 4)
                timespan = targetTimespan * 4;
        } else {
            if (timespan < targetTimespan / 16)
                timespan = targetTimespan / 16;
            if (timespan > targetTimespan * 4)
                timespan = targetTimespan * 4;
        }

        return timespan;
    }

    private static DogecoinMainNetParams instance;
    public static synchronized DogecoinMainNetParams get() {
        if (instance == null) {
            instance = new DogecoinMainNetParams();
        }
        return instance;
    }

    public String getPaymentProtocolId() {
        return PAYMENT_PROTOCOL_ID_MAINNET;
    }
}
