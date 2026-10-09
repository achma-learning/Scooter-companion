// Same scenarios as app/src/test/.../ServicePlannerTest.kt and FuelStatsTest.kt.
// Run: node --test web/tests/*.test.mjs
import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import * as P from '../js/planner.js';

const content = JSON.parse(readFileSync(fileURLToPath(new URL('../../shared/content/agility50.json', import.meta.url)), 'utf8'));
const task = (id) => content.tasks.find((t) => t.id === id);
const day0 = '2026-01-01';
const rec = (km, tasks, extra = {}) => ({ date: day0, km, tasks, milestone: null, stamped: false, ...extra });

test('new bike: first oil change at 300 km', () => {
  const s = P.taskStatus(task('engine-oil'), [], 120, day0, 0, day0);
  assert.equal(s.dueKm, 300);
  assert.equal(s.kmLeft, 180);
  assert.equal(s.status, 'ok');
});

test('break-in sequence then regular interval', () => {
  const due = (km) => P.taskStatus(task('engine-oil'), [rec(km, ['engine-oil'])], km, day0, 0, day0).dueKm;
  assert.deepEqual([300, 1000, 2000, 3000, 5000].map(due), [1000, 2000, 3000, 5000, 7000]);
});

test('gear oil: break-in then every 4,000 km', () => {
  assert.equal(P.taskStatus(task('gear-oil'), [rec(1000, ['gear-oil'])], 1200, day0, 0, day0).dueKm, 5000);
});

test('overdue and soon thresholds', () => {
  const r = [rec(3000, ['engine-oil'])];
  assert.equal(P.taskStatus(task('engine-oil'), r, 5050, day0, 0, day0).status, 'overdue');
  assert.equal(P.taskStatus(task('engine-oil'), r, 4750, day0, 0, day0).status, 'soon');
  assert.equal(P.taskStatus(task('engine-oil'), r, 4000, day0, 0, day0).status, 'ok');
});

test('time limit triggers without km', () => {
  const s = P.taskStatus(task('brake-fluid'), [rec(1000, ['brake-fluid'])], 1500, P.addMonths(day0, 13), 0, day0);
  assert.equal(s.status, 'overdue');
  assert.ok(s.daysLeft < 0);
});

test('weekly check never done is soon, not overdue', () => {
  const s = P.taskStatus(task('tyre-pressure'), [], 500, day0, 0, null);
  assert.equal(s.dueKm, null);
  assert.equal(s.status, 'soon');
  const s2 = P.taskStatus(task('tyre-pressure'), [], 500, P.addMonths(day0, 6), 0, day0);
  assert.equal(s2.status, 'soon');
  assert.equal(s2.daysLeft, null);
});

test('weekly check done ten days ago is overdue', () => {
  const s = P.taskStatus(task('tyre-pressure'), [rec(100, ['tyre-pressure'])], 500, P.addDays(day0, 10), 0, day0);
  assert.equal(s.status, 'overdue');
  assert.equal(s.daysLeft, -3);
});

test('passport milestones', () => {
  const services = [rec(310, ['engine-oil'], { milestone: 300, stamped: true }), rec(1020, ['engine-oil'], { milestone: 1000 })];
  const m = Object.fromEntries(P.milestoneStates(content.milestones, services, 3600).map((x) => [x.milestone.km, x.state]));
  assert.equal(m[300], 'stamped');
  assert.equal(m[1000], 'done');
  assert.equal(m[2000], 'missed');
  assert.equal(m[3000], 'next');
  assert.equal(m[5000], 'upcoming');
});

test('health score bounds', () => {
  const fresh = P.healthScore(P.statuses(content.tasks, [], 0, day0, 0, day0));
  assert.ok(fresh >= 1 && fresh <= 100);
  const worn = P.healthScore(P.statuses(content.tasks, [], 50000, P.addMonths(day0, 24), 0, day0));
  assert.ok(worn < 10);
});

test('fuel: full-to-full consumption', () => {
  const s = P.fuelSummary([
    { date: day0, km: 1000, liters: 4, cost: 52, full: true },
    { date: day0, km: 1100, liters: 1, cost: 13, full: false },
    { date: day0, km: 1200, liters: 3, cost: 39, full: true },
  ]);
  assert.equal(s.litersPer100, 2);
  assert.equal(s.costPerKm, 52 / 200);
  assert.equal(s.distance, 200);
});

test('addMonths clamps like java.util.Calendar', () => {
  assert.equal(P.addMonths('2026-01-31', 1), '2026-02-28');
  assert.equal(P.addMonths('2025-11-09', 12), '2026-11-09');
});
