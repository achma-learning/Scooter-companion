// Garage state, persisted in this browser's localStorage only. The shape is the Android
// app's backup format (data/backup/Backup.kt) plus local ids, so export/import moves a
// garage between the site and the app unchanged.

const KEY = 'agility-companion:garage:v1';
export const FORMAT = 'agility-companion';

const empty = () => ({
  bike: { nickname: '', plate: '', vin: '', engineNo: '', color: '', purchaseDate: null, purchaseKm: 0, dealer: '', warrantyMonths: 24 },
  odometer: [],
  services: [],
  fuel: [],
  documents: [],
  preRide: { last: null },
  hasProfile: false,
});

const uid = () => Math.random().toString(36).slice(2, 10) + Date.now().toString(36);

let state = load();
const listeners = new Set();

function load() {
  try {
    const raw = localStorage.getItem(KEY);
    if (raw) return { ...empty(), ...JSON.parse(raw) };
  } catch (e) {
    /* private mode / blocked storage: run in memory */
  }
  return empty();
}

function save() {
  try {
    localStorage.setItem(KEY, JSON.stringify(state));
  } catch (e) {
    /* storage unavailable: changes live until the tab closes */
  }
  listeners.forEach((fn) => fn(state));
}

export const getState = () => state;
export const subscribe = (fn) => (listeners.add(fn), () => listeners.delete(fn));

export function saveBike(bike) {
  state.bike = { ...state.bike, ...bike };
  state.hasProfile = true;
  save();
}

export function addOdometer(km, date) {
  state.odometer.push({ id: uid(), date, km });
  save();
}

function upsert(list, item) {
  if (item.id) {
    const i = list.findIndex((x) => x.id === item.id);
    if (i >= 0) list[i] = item;
    else list.push(item);
  } else list.push({ ...item, id: uid() });
}

export function saveService(entry) {
  upsert(state.services, entry);
  state.services.sort((a, b) => b.km - a.km || (a.date < b.date ? 1 : -1));
  save();
}
export function deleteService(id) {
  state.services = state.services.filter((s) => s.id !== id);
  save();
}

export function saveFuel(entry) {
  upsert(state.fuel, entry);
  state.fuel.sort((a, b) => b.km - a.km);
  save();
}
export function deleteFuel(id) {
  state.fuel = state.fuel.filter((s) => s.id !== id);
  save();
}

export function saveDocument(doc) {
  upsert(state.documents, doc);
  state.documents.sort((a, b) => (a.expiry || '9999') < (b.expiry || '9999') ? -1 : 1);
  save();
}
export function deleteDocument(id) {
  state.documents = state.documents.filter((s) => s.id !== id);
  save();
}

export function markPreRide(date) {
  state.preRide = { last: date };
  save();
}

export function exportBackup() {
  const strip = (list) => list.map(({ id, ...rest }) => rest);
  return {
    format: FORMAT,
    version: 1,
    exportedAt: new Date().toISOString().replace(/\.\d+Z$/, 'Z'),
    bike: state.bike,
    odometer: strip(state.odometer),
    services: strip(state.services).map((s) => ({ ...s, notes: s.notes || '' })),
    fuel: strip(state.fuel),
    documents: strip(state.documents),
    preRide: state.preRide,
  };
}

/** Replaces the whole garage with a backup (from the app or this site). */
export function importBackup(data) {
  if (!data || data.format !== FORMAT) throw new Error('Not an Agility Companion backup');
  const withIds = (list = []) => list.map((x) => ({ ...x, id: uid() }));
  const iso = (d) => (d ? String(d).slice(0, 10) : null);
  state = {
    ...empty(),
    bike: { ...empty().bike, ...data.bike, purchaseDate: iso(data.bike?.purchaseDate) },
    hasProfile: true,
    odometer: withIds(data.odometer).map((o) => ({ ...o, date: iso(o.date) })),
    services: withIds(data.services).map((s) => ({
      tasks: [], milestone: null, shop: '', cost: null, stamped: false, notes: '', ...s, date: iso(s.date),
    })),
    fuel: withIds(data.fuel).map((f) => ({ cost: null, full: true, ...f, date: iso(f.date) })),
    documents: withIds(data.documents).map((d) => ({ number: '', notes: '', ...d, expiry: iso(d.expiry) })),
    preRide: { last: iso(data.preRide?.last) },
  };
  save();
  return `${state.services.length} services, ${state.fuel.length} fill-ups, ${state.documents.length} documents imported`;
}

export function wipe() {
  state = empty();
  save();
}

// Theme preference (per browser).
const THEME_KEY = 'agility-companion:theme';
export function getTheme() {
  try {
    return localStorage.getItem(THEME_KEY) || 'system';
  } catch (e) {
    return 'system';
  }
}
export function setTheme(t) {
  try {
    localStorage.setItem(THEME_KEY, t);
  } catch (e) {
    /* ignore */
  }
  applyTheme();
}
export function applyTheme() {
  const t = getTheme();
  if (t === 'system') document.documentElement.removeAttribute('data-theme');
  else document.documentElement.setAttribute('data-theme', t);
}
