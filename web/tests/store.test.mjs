// Backup interop: the website must import what the Android app exports (same fixture as
// app/src/test/.../BackupInteropTest.kt) and export the same shape back.
import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

const mem = new Map();
globalThis.localStorage = { getItem: (k) => mem.get(k) ?? null, setItem: (k, v) => mem.set(k, String(v)), removeItem: (k) => mem.delete(k) };
const S = await import('../js/store.js');
const fixture = JSON.parse(readFileSync(fileURLToPath(new URL('../../shared/fixtures/backup-sample.json', import.meta.url)), 'utf8'));

test('imports an app backup', () => {
  S.importBackup(fixture);
  const st = S.getState();
  assert.equal(st.bike.nickname, 'Zitoun');
  assert.equal(st.services.length, 2);
  assert.ok(st.services.every((s) => s.id));
  assert.equal(st.fuel[1].cost, null);
  assert.equal(st.documents[0].expiry, '2026-10-18');
  assert.equal(st.preRide.last, '2026-10-08');
});

test('exports the same shape back (no local ids)', () => {
  const out = S.exportBackup();
  assert.equal(out.format, 'agility-companion');
  assert.deepEqual(out.services, fixture.services);
  assert.deepEqual(out.fuel, fixture.fuel);
  assert.deepEqual(out.documents, fixture.documents);
  assert.deepEqual(out.bike, fixture.bike);
});

test('rejects foreign JSON', () => {
  assert.throws(() => S.importBackup({ hello: 1 }), /Not an Agility Companion backup/);
});
