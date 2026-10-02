package com.swingcraft4j.animatedchart;

/**
 * Easing curves that map linear time (0 to 1) to animation progress.
 */
public enum Easing {

    LINEAR("Linear") {
        @Override
        public double apply(double t) {
            return t;
        }
    },
    EASE_OUT("Ease out") {
        @Override
        public double apply(double t) {
            return 1 - Math.pow(1 - t, 3);
        }
    },
    EASE_IN_OUT("Ease in-out") {
        @Override
        public double apply(double t) {
            return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
        }
    },
    BACK("Back") {
        @Override
        public double apply(double t) {
            double c1 = 1.70158;
            double c3 = c1 + 1;
            return 1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2);
        }
    },
    ELASTIC("Elastic") {
        @Override
        public double apply(double t) {
            if (t <= 0 || t >= 1) {
                return t <= 0 ? 0 : 1;
            }
            return Math.pow(2, -10 * t) * Math.sin((t * 10 - 0.75) * (2 * Math.PI / 3)) + 1;
        }
    },
    BOUNCE("Bounce") {
        @Override
        public double apply(double t) {
            double n1 = 7.5625;
            double d1 = 2.75;
            if (t < 1 / d1) {
                return n1 * t * t;
            } else if (t < 2 / d1) {
                t -= 1.5 / d1;
                return n1 * t * t + 0.75;
            } else if (t < 2.5 / d1) {
                t -= 2.25 / d1;
                return n1 * t * t + 0.9375;
            }
            t -= 2.625 / d1;
            return n1 * t * t + 0.984375;
        }
    };

    private final String name;

    Easing(String name) {
        this.name = name;
    }

    /**
     * Progress at time {@code t} (0 to 1); may overshoot 0 or 1 for Back and Elastic.
     */
    public abstract double apply(double t);

    @Override
    public String toString() {
        return name;
    }
}
