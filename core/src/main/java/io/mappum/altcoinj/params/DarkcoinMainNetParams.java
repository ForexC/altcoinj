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

package io.mappum.altcoinj.params;

import io.mappum.altcoinj.core.*;
import io.mappum.altcoinj.pows.ScryptProofOfWork;
import io.mappum.altcoinj.pows.X11ProofOfWork;
import io.mappum.altcoinj.store.BlockStore;
import io.mappum.altcoinj.store.BlockStoreException;

import java.math.BigInteger;

import static com.google.common.base.Preconditions.checkState;

/**
 * Parameters for the main production network on which people trade goods and services.
 */
public class DarkcoinMainNetParams extends NetworkParameters {
    public static final byte[] GENESIS_INPUT = Utils.HEX.decode("04ffff001d01044c5957697265642030392f4a616e2f3230313420546865204772616e64204578706572696d656e7420476f6573204c6976653a204f76657273746f636b2e636f6d204973204e6f7720416363657074696e6720426974636f696e73");
    public static final byte[] GENESIS_SCRIPTPUBKEY = Utils.HEX.decode("41040184710fa689ad5023690c80f3a49c8f13f8d45b8c857fbcbc8bc4a8e4d3eb4b10f4d4604fa08dce601aaf0f470216fe1b51850b4acf21b179c45070ac7b03a9ac");
    public static final Sha256Hash GENESIS_ROOT = new Sha256Hash("e0028eb9648db56b1ac77cf090b99048a8007e2bb64b68f092c03c7f56a662c7");

    public static final int TARGET_TIMESPAN = (int)(24 * 60 * 60);
    public static final int TARGET_SPACING = (int)(2.5 * 60);
    public static final int INTERVAL = TARGET_TIMESPAN / TARGET_SPACING;
    public static final BigInteger MAX_TARGET = Utils.decodeCompactBits(0x1e0fffffL);

    public static final byte[] ALERT_KEY = Utils.HEX.decode("048240a8748a80a286b270ba126705ced4f2ce5a7847b3610ea3c06513150dade2a8512ed5ea86320824683fc0818f0ac019214973e677acd1244f6d0571fc5103");

    public DarkcoinMainNetParams() {
        maxMoney = Coin.COIN.multiply(22000000);
        minFee = Coin.valueOf(10000);
        alertSigningKey = ALERT_KEY;
        genesisBlock = createGenesis(this, GENESIS_INPUT, GENESIS_SCRIPTPUBKEY, GENESIS_ROOT);
        interval = INTERVAL;
        targetTimespan = TARGET_TIMESPAN;
        targetSpacing = TARGET_SPACING;
        proofOfWork = hashFunction = X11ProofOfWork.get();
        maxTarget = MAX_TARGET;
        dumpedPrivateKeyHeader = 128;
        addressHeader = 76;
        p2shHeader = 16;
        acceptableAddressCodes = new int[] { addressHeader, p2shHeader };
        port = 9999;
        packetMagic = 0xbf0c6bbd;
        protocolVersion = 70054;
        genesisBlock.setDifficultyTarget(0x1e0ffff0L);
        genesisBlock.setTime(1390095618L);
        genesisBlock.setNonce(28917698);
        id = "org.darkcoin.production";
        subsidyDecreaseBlockCount = 4730400;
        spendableCoinbaseDepth = 100;
        String genesisHash = genesisBlock.getHashAsString();
        checkState(genesisHash.equals("00000ffd590b1485b3caadc19b22e6379c733355108f107a430458cdf3407ab6"),
                genesisHash);

        dnsSeeds = new String[] {
                "dnsseed.darkcoin.io",
                "dnsseed.darkcoin.qa",
                "dnsseed.masternode.io"
        };
    }

    private static double convertBitsToDouble(long nBits){
        long nShift = (nBits >> 24) & 0xff;

        double dDiff =
                (double)0x0000ffff / (double)(nBits & 0x00ffffff);

        while (nShift < 29)
        {
            dDiff *= 256.0;
            nShift++;
        }
        while (nShift > 29)
        {
            dDiff /= 256.0;
            nShift--;
        }

        return dDiff;
    }

    @Override
    public void checkDifficulty(StoredBlock storedPrev, Block nextBlock, BlockStore blockStore)
            throws BlockStoreException, VerificationException {
        Block prev = storedPrev.getHeader();

        // Is this supposed to be a difficulty transition point?
        if (!shouldRetarget(storedPrev)) {
            // No ... so check the difficulty didn't actually change.
            if (nextBlock.getDifficultyTarget() != prev.getDifficultyTarget())
                throw new VerificationException("Unexpected change in difficulty at height " + storedPrev.getHeight() +
                        ": " + Long.toHexString(nextBlock.getDifficultyTarget()) + " vs " +
                        Long.toHexString(prev.getDifficultyTarget()));
            return;
        }

        int timespan = getTimespan(storedPrev, blockStore);

        BigInteger newTarget;
        int height = storedPrev.getHeight() + 1;

        if(height < 15200) {
            newTarget = Utils.decodeCompactBits(prev.getDifficultyTarget());
            newTarget = newTarget.multiply(BigInteger.valueOf(timespan));
            newTarget = newTarget.divide(BigInteger.valueOf(getTargetTimespan(storedPrev.getHeight())));
        } else if(height < 34140) {
            int secondsPerDay = 60 * 60 * 24;
            int pastBlocksMin = secondsPerDay / 40 / targetSpacing;
            int pastBlocksMax = secondsPerDay * 7 / targetSpacing;
            newTarget = kimotoGravityWell(storedPrev, nextBlock, blockStore, targetSpacing, pastBlocksMin, pastBlocksMax);
        } else if(height < 68589) {
            newTarget = darkGravityWave(storedPrev, nextBlock, blockStore);
        } else {
            newTarget = darkGravityWave3(storedPrev, nextBlock, blockStore);
        }

        if (newTarget.compareTo(maxTarget) > 0) {
            //log.info("Difficulty hit proof of work limit: {}", newTarget.toString(16));
            newTarget = maxTarget;
        }

        int accuracyBytes = (int) (nextBlock.getDifficultyTarget() >>> 24) - 3;
        long receivedTargetCompact = nextBlock.getDifficultyTarget();

        // The calculated difficulty is to a higher precision than received, so reduce here.
        BigInteger mask = BigInteger.valueOf(0xFFFFFFL).shiftLeft(accuracyBytes * 8);
        newTarget = newTarget.and(mask);
        long newTargetCompact = Utils.encodeCompactBits(newTarget);

        if(height < 68590) {
            double n1 = convertBitsToDouble(newTargetCompact);
            double n2 = convertBitsToDouble(receivedTargetCompact);

            if(Math.abs(n1 - n2) > n1 * 0.2)
                throw new VerificationException("Network provided difficulty bits do not match what was calculated: " +
                        newTargetCompact + " vs " + receivedTargetCompact);

        } else {
            if (newTargetCompact != receivedTargetCompact)
                throw new VerificationException("Network provided difficulty bits do not match what was calculated: " +
                        newTargetCompact + " vs " + receivedTargetCompact);
        }
    }

    private static BigInteger kimotoGravityWell(StoredBlock storedPrev, Block nextBlock, BlockStore blockStore,
                                         long targetSpacing, long pastBlocksMin, long pastBlocksMax)
                                         throws BlockStoreException {
        StoredBlock         BlockLastSolved             = storedPrev;
        StoredBlock         BlockReading                = storedPrev;
        Block               BlockCreating               = nextBlock;

        long				PastBlocksMass				= 0;
        long				PastRateActualSeconds		= 0;
        long				PastRateTargetSeconds		= 0;
        double				PastRateAdjustmentRatio		= 1f;
        BigInteger			PastDifficultyAverage = BigInteger.valueOf(0);
        BigInteger			PastDifficultyAveragePrev = BigInteger.valueOf(0);;
        double				EventHorizonDeviation;
        double				EventHorizonDeviationFast;
        double				EventHorizonDeviationSlow;

        long start = System.currentTimeMillis();

        if (BlockLastSolved == null || BlockLastSolved.getHeight() == 0 || (long)BlockLastSolved.getHeight() < pastBlocksMin)
            return MAX_TARGET;

        int i = 0;
        long LatestBlockTime = BlockLastSolved.getHeader().getTimeSeconds();

        for (i = 1; BlockReading != null && BlockReading.getHeight() > 0; i++) {
            if (pastBlocksMax > 0 && i > pastBlocksMax) { break; }
            PastBlocksMass++;

            if (i == 1)	{ PastDifficultyAverage = BlockReading.getHeader().getDifficultyTargetAsInteger(); }
            else		{ PastDifficultyAverage = ((BlockReading.getHeader().getDifficultyTargetAsInteger().subtract(PastDifficultyAveragePrev)).divide(BigInteger.valueOf(i)).add(PastDifficultyAveragePrev)); }
            PastDifficultyAveragePrev = PastDifficultyAverage;


            if (BlockReading.getHeight() > 646120 && LatestBlockTime < BlockReading.getHeader().getTimeSeconds()) {
                //eliminates the ability to go back in time
                LatestBlockTime = BlockReading.getHeader().getTimeSeconds();
            }

            PastRateActualSeconds			= BlockLastSolved.getHeader().getTimeSeconds() - BlockReading.getHeader().getTimeSeconds();
            PastRateTargetSeconds			= targetSpacing * PastBlocksMass;
            PastRateAdjustmentRatio			= 1.0f;
            if (BlockReading.getHeight() > 646120){
                //this should slow down the upward difficulty change
                if (PastRateActualSeconds < 5) { PastRateActualSeconds = 5; }
            }
            else {
                if (PastRateActualSeconds < 0) { PastRateActualSeconds = 0; }
            }
            if (PastRateActualSeconds != 0 && PastRateTargetSeconds != 0) {
                PastRateAdjustmentRatio			= (double)PastRateTargetSeconds / PastRateActualSeconds;
            }
            EventHorizonDeviation			= 1 + (0.7084 * java.lang.Math.pow((Double.valueOf(PastBlocksMass)/Double.valueOf(28.2)), -1.228));
            EventHorizonDeviationFast		= EventHorizonDeviation;
            EventHorizonDeviationSlow		= 1 / EventHorizonDeviation;

            if (PastBlocksMass >= pastBlocksMin) {
                if ((PastRateAdjustmentRatio <= EventHorizonDeviationSlow) || (PastRateAdjustmentRatio >= EventHorizonDeviationFast)) {
                    /*assert(BlockReading)*/
                    break;
                }
            }
            StoredBlock BlockReadingPrev = blockStore.get(BlockReading.getHeader().getPrevBlockHash());
            if (BlockReadingPrev == null) {
                //Since we are using the checkpoint system, there may not be enough blocks to do this diff adjust, so skip until we do
                continue;
            }
            BlockReading = BlockReadingPrev;
        }

        BigInteger newDifficulty = PastDifficultyAverage;
        if (PastRateActualSeconds != 0 && PastRateTargetSeconds != 0) {
            newDifficulty = newDifficulty.multiply(BigInteger.valueOf(PastRateActualSeconds));
            newDifficulty = newDifficulty.divide(BigInteger.valueOf(PastRateTargetSeconds));
        }

        return newDifficulty;
    }

    private BigInteger darkGravityWave(StoredBlock storedPrev, Block nextBlock, BlockStore blockStore) throws BlockStoreException {
    /* current difficulty formula, limecoin - DarkGravity, written by Evan Duffield - evan@limecoin.io */
        StoredBlock BlockLastSolved = storedPrev;
        StoredBlock BlockReading = storedPrev;
        Block BlockCreating = nextBlock;
        //BlockCreating = BlockCreating;
        long nBlockTimeAverage = 0;
        long nBlockTimeAveragePrev = 0;
        long nBlockTimeCount = 0;
        long nBlockTimeSum2 = 0;
        long nBlockTimeCount2 = 0;
        long LastBlockTime = 0;
        long PastBlocksMin = 14;
        long PastBlocksMax = 140;
        long CountBlocks = 0;
        BigInteger PastDifficultyAverage = BigInteger.valueOf(0);
        BigInteger PastDifficultyAveragePrev = BigInteger.valueOf(0);

        //if (BlockLastSolved == NULL || BlockLastSolved->nHeight == 0 || BlockLastSolved->nHeight < PastBlocksMin) { return bnProofOfWorkLimit.GetCompact(); }
        if (BlockLastSolved == null || BlockLastSolved.getHeight() == 0 || (long)BlockLastSolved.getHeight() < PastBlocksMin)
            return MAX_TARGET;

        for (int i = 1; BlockReading != null && BlockReading.getHeight() > 0; i++) {
            if (PastBlocksMax > 0 && i > PastBlocksMax)
            {
                break;
            }
            CountBlocks++;

            if(CountBlocks <= PastBlocksMin) {
                if (CountBlocks == 1) { PastDifficultyAverage = BlockReading.getHeader().getDifficultyTargetAsInteger(); }
                else
                {
                    //PastDifficultyAverage = ((CBigNum().SetCompact(BlockReading->nBits) - PastDifficultyAveragePrev) / CountBlocks) + PastDifficultyAveragePrev;
                    PastDifficultyAverage = BlockReading.getHeader().getDifficultyTargetAsInteger().subtract(PastDifficultyAveragePrev).divide(BigInteger.valueOf(CountBlocks)).add(PastDifficultyAveragePrev);

                }
                PastDifficultyAveragePrev = PastDifficultyAverage;
            }

            if(LastBlockTime > 0){
                long Diff = (LastBlockTime - BlockReading.getHeader().getTimeSeconds());
                //if(Diff < 0)
                //   Diff = 0;
                if(nBlockTimeCount <= PastBlocksMin) {
                    nBlockTimeCount++;

                    if (nBlockTimeCount == 1) { nBlockTimeAverage = Diff; }
                    else { nBlockTimeAverage = ((Diff - nBlockTimeAveragePrev) / nBlockTimeCount) + nBlockTimeAveragePrev; }
                    nBlockTimeAveragePrev = nBlockTimeAverage;
                }
                nBlockTimeCount2++;
                nBlockTimeSum2 += Diff;
            }
            LastBlockTime = BlockReading.getHeader().getTimeSeconds();

            StoredBlock BlockReadingPrev = blockStore.get(BlockReading.getHeader().getPrevBlockHash());
            if (BlockReadingPrev == null)
            {
                //assert(BlockReading); break;
                continue;
            }
            BlockReading = BlockReadingPrev;
        }

        BigInteger bnNew = PastDifficultyAverage;
        if (nBlockTimeCount != 0 && nBlockTimeCount2 != 0) {
            double SmartAverage = ((((double)nBlockTimeAverage)*0.7)+(((double)nBlockTimeSum2 / (double)nBlockTimeCount2)*0.3));
            if(SmartAverage < 1) SmartAverage = 1;
            double Shift = targetSpacing/SmartAverage;

            double fActualTimespan = (((double)CountBlocks*(double)targetSpacing)/Shift);
            double fTargetTimespan = ((double)CountBlocks*targetSpacing);
            if (fActualTimespan < fTargetTimespan/3)
                fActualTimespan = fTargetTimespan/3;
            if (fActualTimespan > fTargetTimespan*3)
                fActualTimespan = fTargetTimespan*3;

            long nActualTimespan = (long)fActualTimespan;
            long nTargetTimespan = (long)fTargetTimespan;

            // Retarget
            bnNew = bnNew.multiply(BigInteger.valueOf(nActualTimespan));
            bnNew = bnNew.divide(BigInteger.valueOf(nTargetTimespan));
        }
        return bnNew;
    }

    private BigInteger darkGravityWave3(StoredBlock storedPrev, Block nextBlock, BlockStore blockStore) throws BlockStoreException {
        /* current difficulty formula, darkcoin - DarkGravity v3, written by Evan Duffield - evan@darkcoin.io */
        StoredBlock BlockLastSolved = storedPrev;
        StoredBlock BlockReading = storedPrev;
        Block BlockCreating = nextBlock;
        BlockCreating = BlockCreating;
        long nActualTimespan = 0;
        long LastBlockTime = 0;
        long PastBlocksMin = 24;
        long PastBlocksMax = 24;
        long CountBlocks = 0;
        BigInteger PastDifficultyAverage = BigInteger.ZERO;
        BigInteger PastDifficultyAveragePrev = BigInteger.ZERO;

        if (BlockLastSolved == null || BlockLastSolved.getHeight() == 0 || BlockLastSolved.getHeight() < PastBlocksMin)
            return MAX_TARGET;

        for (int i = 1; BlockReading != null && BlockReading.getHeight() > 0; i++) {
            if (PastBlocksMax > 0 && i > PastBlocksMax) { break; }
            CountBlocks++;

            if(CountBlocks <= PastBlocksMin) {
                if (CountBlocks == 1) { PastDifficultyAverage = BlockReading.getHeader().getDifficultyTargetAsInteger(); }
                else { PastDifficultyAverage = ((PastDifficultyAveragePrev.multiply(BigInteger.valueOf(CountBlocks)).add(BlockReading.getHeader().getDifficultyTargetAsInteger()).divide(BigInteger.valueOf(CountBlocks + 1)))); }
                PastDifficultyAveragePrev = PastDifficultyAverage;
            }

            if(LastBlockTime > 0){
                long Diff = (LastBlockTime - BlockReading.getHeader().getTimeSeconds());
                nActualTimespan += Diff;
            }
            LastBlockTime = BlockReading.getHeader().getTimeSeconds();

            StoredBlock BlockReadingPrev = blockStore.get(BlockReading.getHeader().getPrevBlockHash());
            if (BlockReadingPrev == null)
            {
                //assert(BlockReading); break;
                continue;
            }
            BlockReading = BlockReadingPrev;
        }

        BigInteger bnNew= PastDifficultyAverage;

        long nTargetTimespan = CountBlocks*targetSpacing;//nTargetSpacing;

        if (nActualTimespan < nTargetTimespan/3)
            nActualTimespan = nTargetTimespan/3;
        if (nActualTimespan > nTargetTimespan*3)
            nActualTimespan = nTargetTimespan*3;

        // Retarget
        bnNew = bnNew.multiply(BigInteger.valueOf(nActualTimespan));
        return bnNew.divide(BigInteger.valueOf(nTargetTimespan));
    }

    @Override
    public int getIntervalOffset(int height) {
        if(height <= interval + 1) return 0;
        return 1;
    }

    @Override
    protected boolean shouldRetarget(StoredBlock storedPrev) {
        int height = storedPrev.getHeight() + 1;
        if(height < 15200) return (height % getInterval(storedPrev.getHeight()) == 0);
        return true;
    }

    private static DarkcoinMainNetParams instance;
    public static synchronized DarkcoinMainNetParams get() {
        if (instance == null) {
            instance = new DarkcoinMainNetParams();
        }
        return instance;
    }

    public String getPaymentProtocolId() {
        return PAYMENT_PROTOCOL_ID_MAINNET;
    }
}
