/**
 * Shared SVG chart shaping for monitoring series.
 *
 * Takes a numeric series (see buildOs265NumericSeries) and produces
 * point coordinates, a polyline string, and padded axis bounds for a
 * given drawing box.
 */

// Minimum visible y-span so small real variations stay readable and a
// near-constant series does not visually collapse. This never alters
// the data: a flat source still draws a flat line, only the axis keeps
// a nominal +/-0.05 window around it.
export const MIN_VISIBLE_Y_SPAN = 0.05;

export function getChartBounds(values) {
  if (!values.length) {
    return { min: 0, max: 1 };
  }

  const minValue = Math.min(...values);
  const maxValue = Math.max(...values);
  const range = maxValue - minValue;

  if (range < MIN_VISIBLE_Y_SPAN) {
    const center = (minValue + maxValue) / 2;
    return { min: center - MIN_VISIBLE_Y_SPAN, max: center + MIN_VISIBLE_Y_SPAN };
  }

  const padding = Math.max(range * 0.1, 0.01);
  return { min: minValue - padding, max: maxValue + padding };
}

export function buildChartPoints(series, box) {
  if (!Array.isArray(series) || series.length === 0) {
    return [];
  }

  const { startX, endX, startY, endY } = box;
  const values = series.map((item) => item.value);
  const bounds = getChartBounds(values);
  const range = bounds.max - bounds.min || 1;
  const stepX = series.length > 1 ? (endX - startX) / (series.length - 1) : 0;

  return series.map((item, index) => {
    const x = series.length === 1 ? (startX + endX) / 2 : startX + index * stepX;
    const y = endY - ((item.value - bounds.min) / range) * (endY - startY);
    return { ...item, x, y };
  });
}

export function buildPolyline(points) {
  return points.map((point) => `${point.x},${point.y}`).join(" ");
}

export function formatShortTime(value) {
  if (!value || typeof value !== "string") {
    return "--";
  }
  const parts = value.split(" ");
  return parts.length === 2 ? parts[1] : value;
}
