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

import com.google.bitcoin.core.Coin;
import com.google.bitcoin.core.Hash;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.pows.ScryptProofOfWork;

import static com.google.common.base.Preconditions.checkState;

/**
 * Parameters for the main production network on which people trade goods and services.
 */
public class LitecoinMainNetParams extends NetworkParameters {
    public static final byte[] GENESIS_INPUT = Utils.HEX.decode("04b217bb4e022309");
    public static final byte[] GENESIS_SCRIPTPUBKEY = Utils.HEX.decode("41044870341873accab7600d65e204bb4ae47c43d20c562ebfbf70cbcb188da98dec8b5ccf0526c8e4d954c6b47b898cc30adf1ff77c2e518ddc9785b87ccb90b8cdac");
    public static final Hash GENESIS_ROOT = new Hash("97ddfbbae6be97fd6cdf3e7ca13232a3afff2353e29badfab7f73011edd4ced9");

    public static final int TARGET_TIMESPAN = (int)(3.5 * 24 * 60 * 60);
    public static final int TARGET_SPACING = (int)(2.5 * 60);
    public static final int INTERVAL = TARGET_TIMESPAN / TARGET_SPACING;

    public static final byte[] ALERT_KEY = Utils.HEX.decode("040184710fa689ad5023690c80f3a49c8f13f8d45b8c857fbcbc8bc4a8e4d3eb4b10f4d4604fa08dce601aaf0f470216fe1b51850b4acf21b179c45070ac7b03a9");

    private static ScryptProofOfWork proofOfWorkInstance;

    public LitecoinMainNetParams() {
        maxMoney = Coin.COIN.multiply(84000000);
        alertSigningKey = ALERT_KEY;
        genesisBlock = createGenesis(this, GENESIS_INPUT, GENESIS_SCRIPTPUBKEY, GENESIS_ROOT);
        interval = INTERVAL;
        intervalOffset = 1;
        targetTimespan = TARGET_TIMESPAN;
        targetSpacing = TARGET_SPACING;
        if(proofOfWorkInstance == null)
            proofOfWork = new ScryptProofOfWork(1024, 1, 1, 32);
        maxTarget = Utils.decodeCompactBits(0x1e0fffffL);
        dumpedPrivateKeyHeader = 128;
        addressHeader = 48;
        p2shHeader = 5;
        acceptableAddressCodes = new int[] { addressHeader, p2shHeader };
        port = 9333;
        packetMagic = 0xfbc0b6db;
        genesisBlock.setDifficultyTarget(0x1e0ffff0L);
        genesisBlock.setTime(1317972665L);
        genesisBlock.setNonce(2084524493L);
        id = "org.litecoin.production";
        subsidyDecreaseBlockCount = 840000;
        spendableCoinbaseDepth = 100;
        String genesisHash = genesisBlock.getHashAsString();
        checkState(genesisHash.equals("12a765e31ffd4059bada1e25190f6e98c99d9714d334efa41a195a7e7e04bfe2"),
                genesisHash);

        dnsSeeds = new String[] {
            "dnsseed.litecointools.com",
            "dnsseed.litecoinpool.org",
            "dnsseed.ltc.xurious.com",
            "dnsseed.koin-project.com",
            "dnsseed.weminemnc.com"
        };

        bloomFiltersEnabled = false;
    }

    private static LitecoinMainNetParams instance;
    public static synchronized LitecoinMainNetParams get() {
        if (instance == null) {
            instance = new LitecoinMainNetParams();
        }
        return instance;
    }

    public String getPaymentProtocolId() {
        return PAYMENT_PROTOCOL_ID_MAINNET;
    }
}
