package it.unipi.sample;

import android.hardware.SensorEvent;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class FilteredData {

    // Contains the values in current window as <timestamp (long), magnitude (float)>
    private SortedMap<Long, Float> map =  new TreeMap<>();
    // TAG for logging
    private static String TAG = "FilteredData";
    // Exponential moving average
    private float value;
    // Smoothing factor
    private float alpha = 0.9f;
    // Activity threshold
    private final float ACT_TH = 1.5f;
    // Minimum distance between crossings (in ns)
    private final long MIN_DIST = 250000000L;
    // Total number of steps since app started
    private int stepCount;
    // Duration of window
    private final long FIVE_SECONDS_IN_NANOS = 5000000000L;
    // Main activity, used for updating the UI
    private MainActivity ma;

    public FilteredData(MainActivity ma) {
        this.ma = ma;
    }

    /**
     * Smoothing implemented as exponential moving average
     *
     * @param v the most-recent value provided by the accelerometer
     * @return
     */
    private float smoothValue(float v) {
        value = alpha*value + (1-alpha)*v;
        return value;
    }

    /**
     * A new acceleration value is received.
     * Magnitude and timestamp are stored in map.
     * If window is complete process values.
     * @param m the new acceleration value
     */
    public void addToQueue(SensorEvent m) {
        // timestamp
        long time = m.timestamp;
        // compute magnitude
        float mag = getMagnitude(m.values);
        // apply exp. averaging
        mag = smoothValue(mag);
        // insert in map
        map.put(time, mag);
        // check if window complete
        if(map.lastKey() - map.firstKey() > FIVE_SECONDS_IN_NANOS) {
            // find steps
            processQueue();
            // discard values
            map.clear();
        }
    }

    /**
     * Process the values in the just-finished window
     */
    public void processQueue() {
        // Find average value in window
        float avg = average(map.values());

        if(avg < ACT_TH) {
            Log.i(TAG, "No activity, avg= " + avg + ", samples=" + map.size());
            return;
        }
        // center around average
        subtract(map, avg);
        // find 0-crossing, only from positive to negative
        List<Long> indexes = findCrosses(map);
        Log.i(TAG, indexes.size() + " steps: " + indexes);
        // update UI
        stepCount += indexes.size();
    }

    private List<Long> findCrosses(SortedMap<Long, Float> m) {
        List<Long> r = new ArrayList<Long>();
        Long[] q = m.keySet().toArray(new Long[m.size()]);
        for(int i=0; i<q.length-1; i++){
            if(m.get(q[i]) >= 0 && m.get(q[i+1])<0) {
                // ci salviamo un nuovo passo solo se non troppo vicino al precedente perchè non è possibile
                // che una persona faccia passi troppo velocemente
                if (r.isEmpty() || (q[i] - r.get(r.size() - 1)) > MIN_DIST)
                    r.add(q[i]);
            }
        }
        return r;
    }

    /**
     * Subtracts v from the values stored in m
     * @param m
     * @param v
     */
    private void subtract(SortedMap<Long, Float> m, float v) {
        for (Long l: m.keySet()) {
            Float o = m.get(l);
            Float n = o - v;
            m.put(l, n);
        }
    }

    /**
     * Computes average value
     * @param c
     * @return average value
     */
    private float average(Collection<Float> c) {
        float r = 0f;
        for(Float f: c) {
            r += f;
        }
        return r/c.size();
    }

    /**
     * Compute the magnitude of the acceleration vector (calcolando la distanza euclidea)
     * @param v
     * @return
     */
    public static float getMagnitude(float[] v) {
        return (float)Math.sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2]);
    }
}
