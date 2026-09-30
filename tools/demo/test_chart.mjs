/**
 * SYNTHETIC standalone helper regression; no browser or live-system coverage.
 * Run from the repository root:
 *   node --test tools/demo/test_chart.mjs
 */
import assert from 'node:assert/strict';
import test from 'node:test';
import { buildOs265NumericSeries } from '../../shm-frontend-fable5-copy/src/utils/os265Value.js';
import { buildChartPoints, buildPolyline } from '../../shm-frontend-fable5-copy/src/utils/monitor/chartGeometry.js';

const SYNTHETIC = Array.from({ length: 6 }, (_, index) => ({
  sensorId: 'SYNTHETIC-OS265-001',
  collectTime: `2025-01-01 00:00:0${index}`,
  measuredValue: (10 + 0.25 * index).toFixed(2),
  wavelength: (1550 + 0.001 * index).toFixed(3),
}));
const EXPECTED_VALUES = [10, 10.25, 10.5, 10.75, 11, 11.25];
const EXPECTED_TIMES = [
  '2025-01-01 00:00:00',
  '2025-01-01 00:00:01',
  '2025-01-01 00:00:02',
  '2025-01-01 00:00:03',
  '2025-01-01 00:00:04',
  '2025-01-01 00:00:05',
];
const BOX = { startX: 40, endX: 540, startY: 20, endY: 220 };

function assertFinitePointsInBox(points) {
  assert.equal(points.length, 6);
  for (const point of points) {
    assert.ok(Number.isFinite(point.x), 'SVG X must be finite');
    assert.ok(Number.isFinite(point.y), 'SVG Y must be finite');
    assert.ok(point.x >= BOX.startX && point.x <= BOX.endX);
    assert.ok(point.y >= BOX.startY && point.y <= BOX.endY);
  }
  for (let index = 1; index < points.length; index += 1) {
    assert.ok(points[index].x > points[index - 1].x, 'SVG X must strictly increase');
  }
}

test('SYNTHETIC readings convert measured values and keep wavelength auxiliary', () => {
  const series = buildOs265NumericSeries(SYNTHETIC);
  assert.deepEqual(series.map(({ value }) => value), EXPECTED_VALUES);
  assert.deepEqual(
    series.map(({ wavelength }) => wavelength),
    [1550, 1550.001, 1550.002, 1550.003, 1550.004, 1550.005],
  );

  // A legacy rawValue containing wavelength must not replace measuredValue.
  const legacy = SYNTHETIC.map((record) => ({ ...record, rawValue: record.wavelength }));
  assert.deepEqual(buildOs265NumericSeries(legacy).map(({ value }) => value), EXPECTED_VALUES);
  const wavelengthOnly = SYNTHETIC.map(({ measuredValue, ...record }) => record);
  assert.deepEqual(buildOs265NumericSeries(wavelengthOnly), []);
});

test('chronological input stays chronological; conversion does not promise sorting', () => {
  const series = buildOs265NumericSeries(SYNTHETIC);
  assert.deepEqual(series.map(({ collectTime }) => collectTime), EXPECTED_TIMES);
  for (let index = 1; index < series.length; index += 1) {
    assert.ok(series[index].collectTime > series[index - 1].collectTime);
  }

  // The existing helper preserves caller order, including descending input.
  assert.deepEqual(
    buildOs265NumericSeries([...SYNTHETIC].reverse()).map(({ collectTime }) => collectTime),
    [...EXPECTED_TIMES].reverse(),
  );
});

test('actual geometry produces finite SVG coordinates with increasing X', () => {
  const points = buildChartPoints(buildOs265NumericSeries(SYNTHETIC), BOX);
  assertFinitePointsInBox(points);
  assert.deepEqual(points.map(({ collectTime }) => collectTime), EXPECTED_TIMES);
  assert.deepEqual(points.map(({ value }) => value), EXPECTED_VALUES);
  assert.equal(points[0].x, BOX.startX);
  assert.equal(points.at(-1).x, BOX.endX);
  for (let index = 1; index < points.length; index += 1) {
    assert.ok(points[index].y < points[index - 1].y, 'rising values move upward in SVG');
  }

  const coordinates = buildPolyline(points).split(' ').map((pair) => pair.split(',').map(Number));
  assert.deepEqual(coordinates, points.map(({ x, y }) => [x, y]));
  assert.ok(coordinates.flat().every(Number.isFinite));
});

test('constant measured values draw a truly flat line despite changing wavelength', () => {
  const records = SYNTHETIC.map((record) => ({ ...record, measuredValue: '10.00' }));
  const series = buildOs265NumericSeries(records);
  assert.equal(new Set(series.map(({ wavelength }) => wavelength)).size, 6);
  assert.deepEqual(series.map(({ value }) => value), [10, 10, 10, 10, 10, 10]);

  const points = buildChartPoints(series, BOX);
  assertFinitePointsInBox(points);
  assert.equal(new Set(points.map(({ y }) => y)).size, 1, 'flat values must have identical Y');
  assert.ok(Math.abs(points[0].y - (BOX.startY + BOX.endY) / 2) < 1e-9);
});
