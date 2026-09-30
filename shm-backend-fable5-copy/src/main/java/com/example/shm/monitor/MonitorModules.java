package com.example.shm.monitor;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The exact set of business monitoring modules exposed by this system.
 *
 * Device-specific concepts (fiber, vibratingWire, vibrationDat) are not
 * business modules; they only survive as compatibility adapters around
 * the unified vendor source layer.
 */
public final class MonitorModules {

    public static final String DISPLACEMENT = "displacement";
    public static final String ACCELERATION = "acceleration";
    public static final String STRAIN = "strain";
    public static final String VIBRATION = "vibration";
    public static final String STRESS = "stress";
    public static final String DEFLECTION = "deflection";

    public static final Set<String> ALL = new LinkedHashSet<>(List.of(
            DISPLACEMENT, ACCELERATION, STRAIN, VIBRATION, STRESS, DEFLECTION));

    private MonitorModules() {
    }

    public static boolean isBusinessModule(String moduleKey) {
        return moduleKey != null && ALL.contains(moduleKey);
    }
}
