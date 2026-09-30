/**
 * Shared OS265 / vendor source value rules.
 *
 * Every monitoring module must read its primary value through this
 * helper. The OS265 intensity/energy measured value (around -10.x) is
 * the only primary business value; the wavelength (around 1563.x) is
 * an auxiliary reference and must never drive the main chart.
 */

const WAVELENGTH_MIN = 1500;
const WAVELENGTH_MAX = 1650;

export function toFiniteNumber(value) {
  if (value === null || value === undefined || value === "") {
    return null;
  }
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
}

export function looksLikeLegacyWavelength(value) {
  const parsed = toFiniteNumber(value);
  return parsed !== null && parsed >= WAVELENGTH_MIN && parsed <= WAVELENGTH_MAX;
}

// OS265-specific heuristic: intensity/measured values are typically in [-50, 0].
// This rule is not a general sensor validation rule.
export function isLikelyMeasuredValue(value) {
  const parsed = toFiniteNumber(value);
  return parsed !== null && Math.abs(parsed) < 100;
}

/**
 * Pick the primary business value of one unified vendor reading.
 *
 * Order: rawValue, then measuredValue, then intensity — each only when
 * it passes the measured-value heuristic. Wavelength-looking values are
 * never promoted; when nothing safe exists the result is null and the
 * caller shows an empty state. No guessing.
 */
export function pickOs265PrimaryValue(record) {
  if (!record || typeof record !== "object") {
    return null;
  }

  const rawValue = toFiniteNumber(record.rawValue ?? record.raw_value);
  if (rawValue !== null && Math.abs(rawValue) < 100) {
    return rawValue;
  }

  const measuredValue = toFiniteNumber(record.measuredValue ?? record.measured_value);
  if (measuredValue !== null && Math.abs(measuredValue) < 100) {
    return measuredValue;
  }

  const intensity = toFiniteNumber(record.intensity);
  if (intensity !== null && Math.abs(intensity) < 100) {
    return intensity;
  }

  // A rawValue in the 1500-1650 band is a legacy wavelength leak,
  // never a primary value.
  return null;
}

/**
 * Auxiliary wavelength for details/tooltips/tables only.
 */
export function pickOs265AuxiliaryWavelength(record) {
  if (!record || typeof record !== "object") {
    return null;
  }

  const wavelength = toFiniteNumber(record.wavelength);
  if (wavelength !== null) {
    return wavelength;
  }

  const rawValue = toFiniteNumber(record.rawValue ?? record.raw_value);
  if (looksLikeLegacyWavelength(rawValue)) {
    return rawValue;
  }

  return null;
}

/**
 * Shape raw unified readings into a numeric chart series. Records
 * without a safe primary value are dropped, never guessed.
 */
export function buildOs265NumericSeries(records) {
  if (!Array.isArray(records)) {
    return [];
  }

  return records
    .map((record, index) => {
      const value = pickOs265PrimaryValue(record);
      if (value === null) {
        return null;
      }
      const collectTime = record.collectTime || record.collect_time || "";
      return {
        key: `${record.sensorId || "sensor"}-${collectTime || index}-${index}`,
        sensorId: record.sensorId || "",
        value,
        wavelength: pickOs265AuxiliaryWavelength(record),
        collectTime,
      };
    })
    .filter(Boolean);
}

/**
 * Count the decimal places actually carried by the source value.
 * Used to preserve real source precision without inventing digits.
 */
export function countSourceDecimals(value) {
  const parsed = toFiniteNumber(value);
  if (parsed === null) {
    return 0;
  }
  const text = String(parsed);
  const dotIndex = text.indexOf(".");
  return dotIndex >= 0 ? text.length - dotIndex - 1 : 0;
}

/**
 * Display with at least 2 decimals, preserving up to 4 source decimals
 * when the source provides them (e.g. wavelength 1563.1412 stays
 * 1563.1412 while a one-decimal intensity -10.4 shows as -10.40).
 */
export function formatOs265Value(value) {
  const parsed = toFiniteNumber(value);
  if (parsed === null) {
    return "-";
  }
  const decimals = Math.min(Math.max(countSourceDecimals(parsed), 2), 4);
  return parsed.toFixed(decimals);
}
