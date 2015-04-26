package com.daranguiz.recolift.utils;

import com.daranguiz.recolift.datatype.SensorData;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import Jama.Matrix;

public class CountingPhase {
    public CountingPhase(SensorData sensorDataRef) {
        mSensorData = sensorDataRef;
        mRecoMath = new RecoMath();

        /* Get file pointer, don't open yet */
        String timestamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
        String filename = timestamp + "_counting_results.csv";
        csvFile = new RecoFileUtils().getFileInDcimStorage(filename);
    }

    /* Constants */
    private static final String TAG = "CountingPhase";
    private static final double minPeriod = 0.75; // TODO: LUT for constants per lift
    private static final int NUM_DOFS = 3;
    private static final int NUM_PEAK_THRESHOLD = 2;
    private static final int F_S = 25;
    private static final double MIN_PERIOD = 0.6 * F_S;

    /* Sensor */
    private SensorData mSensorData;
    private RecoMath mRecoMath;

    /* Logging */
    private static RecoFileUtils mRecoFileUtils;
    private static File csvFile;

    /* Thoughts!
     * So with my approach, I don't think I really need to worry about doing it 100%
     * online. Like, I could wait until a full exercise has completed then do counting
     * across the whole buffer. I think that'll be easier.
     *
     * From python simulation, algorithm:
     * 1:
     *  - Find the local maxima
     *  - Sort by amplitude
     *  - Add to candidate peak set if distance to any peak already in the set is at
     *      least minPeriod away
     *
     * 2:
     * For each candidate peak:
     *  - Perform autoc around peak, find maximum value. This gives local period P
     *  - Loop through candidate peaks inside window
     *      > Remove from set if <0.75P away from any other peak
     *
     * 3:
     *  - Find peak at 40th percentile
     *  - Reject any peaks smaller than half the amplitude of that peak
     *  - Return number of remaining peaks as final value
     */
    public void performBatchCounting(int startIdx, int endIdx) {
        /* Get the slice of sensor data */
        SensorData countingBuffer = getCountingBuffer(startIdx, endIdx);
        int bufferLen = countingBuffer.accel.size();
        double[][] accelBuffer = bufferToDoubleArray(countingBuffer, bufferLen);

        /* Condense axes to single principal component */
        Matrix firstPrincipalComponent = mRecoMath.computePCA(accelBuffer, NUM_DOFS, bufferLen);
        double[] primaryProjection = mRecoMath.projectPCA(accelBuffer, firstPrincipalComponent);

        /* #1, find local maxima in signal and do initial culling */
        Set<Integer> initialCandidatePeakIndices = findInitialCandidatePeaks(primaryProjection);

        // TODO: Cull peaks by period

        // TODO: Remove small peaks
    }

    // TODO: Incorporate all sensor sources
    public SensorData getCountingBuffer(int startIdx, int endIdx) {
        SensorData buffer = new SensorData();

        /* List is implemented as a vector */
        buffer.accel = mSensorData.accel.subList(startIdx, endIdx);

        return buffer;
    }

    // TODO: SensorValue is poorly optimized for quick array copies. Check if timing met.
    /* Convert our SensorValue buffer to a float array for easy computation */
    private double[][] bufferToDoubleArray(SensorData buffer, int bufferLen) {
        double outputArr[][] = new double[NUM_DOFS][bufferLen];

        /* No easy way to do quick array copies in current form */
        for (int i = 0; i < NUM_DOFS; i++) {
            for (int j = 0; j < bufferLen; j++) {
                outputArr[i][j] = (double) buffer.accel.get(j).values[i];
            }
        }

        return outputArr;
    }

    /* #1 in the algorithm */
    private Set<Integer> findInitialCandidatePeaks(double[] primaryProjection) {
        Set<Integer> candidatePeakIndices = new TreeSet<Integer>();

        /* Find local maxima in smoothed signal */
        List<Integer> sortedPeakIndices = new Vector<Integer>();
        findPeaks(sortedPeakIndices, primaryProjection);

        /* Sort indices by peak value */
        Collections.sort(sortedPeakIndices, new PeakComparator(primaryProjection));
        Collections.reverse(sortedPeakIndices);

        /* Add to set if the distance to any peak already in the set is less than minPeriod away */
        for (int idx : sortedPeakIndices) {
            boolean addToSet = true;
            for (int alreadyAcceptedPeakIdx : candidatePeakIndices) {
                if (Math.abs(alreadyAcceptedPeakIdx - idx) < MIN_PERIOD)
                    addToSet = false;
            }
            if (addToSet) {
                candidatePeakIndices.add(idx);
            }
        }

        return candidatePeakIndices;
    }

    private void findPeaks(List<Integer> candidatePeakIndices, double[] signal) {
        for (int i = NUM_PEAK_THRESHOLD; i < (signal.length - NUM_PEAK_THRESHOLD); i++) {
            double[] windowedSignal = Arrays.copyOfRange(signal, i - NUM_PEAK_THRESHOLD, i + NUM_PEAK_THRESHOLD + 1);
            int maxIdx = mRecoMath.getArrayMaxIdx(windowedSignal);
            if (maxIdx == NUM_PEAK_THRESHOLD + 1) {
                candidatePeakIndices.add(i);
            }
        }
    }

    private class PeakComparator implements Comparator<Integer> {
        public PeakComparator(double signal[]) {
            curSignal = signal;
        }
        private double[] curSignal;

        @Override
        public int compare(Integer peakIdx1, Integer peakIdx2) {
            return Double.compare(curSignal[peakIdx1], curSignal[peakIdx2]);
        }
    }

    private void cullPeaksByLocalPeriodicity(Set<Integer> candidatePeakIndices, double[] signal) {

    }
}
