package com.daranguiz.recolift;

public class SegmentationPhase {
    public SegmentationPhase(SensorData sensorDataRef) {
        bufferPointer = 0;
        mSensorData = sensorDataRef;
    }

    /* Constants */
    private static final int WINDOW_SIZE = 25 * 5;
    private static final int SLIDE_AMOUNT = 5;

    private int bufferPointer;
    private SensorData mSensorData;

    /* Okay, talking myself through on this one.
       I have a large vector of data that i'll have to grab a sliding window over.
       Say I have... 6 seconds of data. With sliding windows of 5-sec a piece and 0.2s slide,
       I'll have 5 total windows. Ideally, I'll have a function that takes all my unused samples and
       splits them into multiple buffers of length 5s, with the proper sliding window split.

       Buffer function will keep track of how far into the vector it made. There is going to have to
       be some sort of garbage collection, ie if no exercise is detected over a 10 second span
       (determined by the aggregator), the last 5 seconds (unused in any subsequent buffers)
       is deleted from the vector. NOTE: This must be a synchronous operation.

       Once I have my buffer to work with, it should be a bit easier. Compute features, run through
       classifier.

       NOTE: Synchronizing phone resampling timestamps with watch resampling timestamps
       may be non-trivial. Think on this.

       Aggregator thoughts: Paper says to increment on +exercise, decrement on -exercise. Flag as
       exercise when six seconds of +exercise (at 0.2sec per, == 30). If there is ANY exercise
       occurring in the window, it should be flagged. *matters on the ground truth input.
     */
    public void performBatchSegmentation() {
        while (isBufferAvailable()) {
            SensorData buffer = getNextBuffer();
        }
    }

    /* Check if there are enough samples for a new buffer */
    private boolean isBufferAvailable() {
        int nextBufferStart = bufferPointer + SLIDE_AMOUNT;
        int nextBufferEnd = nextBufferStart + WINDOW_SIZE;
        boolean retVal = true;

        /* Corner case, size() == idx? */
        if (nextBufferEnd >= mSensorData.accel.size()) {
            retVal = false;
        }

        return retVal;
    }

    /* Create a new SensorData instance with just the buffer of interest.
     * Right now, this only accounts for single-source accelerometer data, but it'll be
     * extensible shortly.
     */
    private SensorData getNextBuffer() {
        SensorData buffer = new SensorData();
        int nextBufferPointer = bufferPointer + SLIDE_AMOUNT;

        /* List is implemented as a vector */
        buffer.accel = mSensorData.accel.subList(bufferPointer, nextBufferPointer);
        bufferPointer = nextBufferPointer;

        return buffer;
    }
}
