package com.swingcraft4j.animatedchart;

import javax.swing.*;
import java.util.function.DoubleConsumer;

/**
 * Timed animation on the Swing thread that passes each frame's eased progress to a callback.
 */
public class ChartAnimator {

    private final Timer timer = new Timer(16, e -> tick());
    private int duration = 1000;
    private Easing easing = Easing.EASE_OUT;
    private long startTime;
    private DoubleConsumer frame;
    private Runnable finished;
    private boolean eased;
    private boolean enabled = true;
    private int nextDelay;

    /**
     * Whether animations run; when they don't, each jumps straight to its end.
     */
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Holds the next animation at its first frame for a time in milliseconds before it runs.
     */
    public void delayNext(int delay) {
        nextDelay = delay;
    }

    /**
     * Length of one animation in milliseconds; 0 or less jumps to the end.
     */
    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public Easing getEasing() {
        return easing;
    }

    public void setEasing(Easing easing) {
        this.easing = easing;
    }

    /**
     * Starts a new animation, replacing the running one; {@code frame} gets the progress, ending at exactly 1.
     */
    public void start(DoubleConsumer frame) {
        start(frame, true, null);
    }

    /**
     * Like {@link #start}, then runs {@code finished} after the last frame, unless another animation replaced it.
     */
    public void start(DoubleConsumer frame, Runnable finished) {
        start(frame, true, finished);
    }

    /**
     * Like {@link #start}, but passes linear time, for callers that ease each item on their own, e.g. staggered.
     */
    public void startLinear(DoubleConsumer frame, Runnable finished) {
        start(frame, false, finished);
    }

    private void start(DoubleConsumer frame, boolean eased, Runnable finished) {
        this.frame = frame;
        this.eased = eased;
        this.finished = finished;
        startTime = System.nanoTime() + nextDelay * 1_000_000L;
        nextDelay = 0;
        frame.accept(0);
        if (duration <= 0 || !enabled) {
            timer.stop();
            finish();
        } else {
            timer.restart();
        }
    }

    private void finish() {
        frame.accept(1);
        if (finished != null) {
            finished.run();
        }
    }

    public void stop() {
        timer.stop();
    }

    public boolean isRunning() {
        return timer.isRunning();
    }

    private void tick() {
        double t = Math.min(1, (System.nanoTime() - startTime) / 1e6 / duration);
        if (t < 0) {
            // still waiting for a delayed animation to start
            return;
        }
        if (t >= 1) {
            timer.stop();
            finish();
        } else {
            frame.accept(eased ? easing.apply(t) : t);
        }
    }
}
