package com.swingcraft4j.switchbutton;

/**
 * Switch look: track and thumb proportions (relative to the switch height) and which color each part takes.
 */
public enum SwitchType {

    /** Filled track with a white thumb inset. */
    CLASSIC(1f, Tone.ACCENT, 0.8f, Tone.THUMB, false),
    /** Filled track with a smaller white thumb. */
    INSET(1f, Tone.ACCENT, 0.6f, Tone.THUMB, false),
    /** Outlined thumb overhanging a slimmer filled track. */
    OVERHANG(0.55f, Tone.ACCENT, 1f, Tone.THUMB, true),
    /** Outlined thumb on a line. */
    LINE(0.1f, Tone.ACCENT, 1f, Tone.THUMB, true),
    /** Outlined thumb on a soft full-height track. */
    SOFT(1f, Tone.SOFT, 1f, Tone.THUMB, true),
    /** Filled track with a soft thumb inset. */
    TONAL(1f, Tone.ACCENT, 0.6f, Tone.SOFT, false),
    /** Filled thumb on a soft, slimmer track. */
    MATERIAL(0.55f, Tone.SOFT, 1f, Tone.ACCENT, false),
    /** Outlined soft thumb on a line. */
    LINE_TINTED(0.1f, Tone.ACCENT, 1f, Tone.SOFT, true);

    /** Which switch color a part is painted with. */
    enum Tone {
        ACCENT, SOFT, THUMB
    }

    final float trackHeight;
    final Tone track;
    final float thumbSize;
    final Tone thumb;
    final boolean thumbRing;

    SwitchType(float trackHeight, Tone track, float thumbSize, Tone thumb, boolean thumbRing) {
        this.trackHeight = trackHeight;
        this.track = track;
        this.thumbSize = thumbSize;
        this.thumb = thumb;
        this.thumbRing = thumbRing;
    }
}
