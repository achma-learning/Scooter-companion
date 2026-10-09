// Service planner — JavaScript twin of app/.../domain/ServicePlanner.kt and FuelStats.kt.
// Keep the two in sync: web/tests/planner.test.mjs and ServicePlannerTest.kt cover the
// same scenarios. Dates are ISO "YYYY-MM-DD" calendar days, computed in UTC.

const DAY = 86400000;
export const MISSED_TOLERANCE_KM = 1000;

export const toDay = (iso) => (iso ? Date.UTC(+iso.slice(0, 4), +iso.slice(5, 7) - 1, +iso.slice(8, 10)) : null);
export const toIso = (ms) => new Date(ms).toISOString().slice(0, 10);
export const todayIso = () => {
  const d = new Date();
  return toIso(Date.UTC(d.getFullYear(), d.getMonth(), d.getDate()));
};
export const daysBetween = (fromIso, toIsoStr) => Math.round((toDay(toIsoStr) - toDay(fromIso)) / DAY);
export const addDays = (iso, n) => toIso(toDay(iso) + n * DAY);
export function addMonths(iso, n) {
  const d = new Date(toDay(iso));
  const day = d.getUTCDate();
  d.setUTCDate(1);
  d.setUTCMonth(d.getUTCMonth() + n);
  const last = new Date(Date.UTC(d.getUTCFullYear(), d.getUTCMonth() + 1, 0)).getUTCDate();
  d.setUTCDate(Math.min(day, last)); // same clamping as java.util.Calendar
  return toIso(d.getTime());
}

/** Highest km we know of: logbook, odometer updates, fuel fill-ups, purchase km. */
export function currentKm(state) {
  const all = [
    state.bike?.purchaseKm || 0,
    ...state.services.map((s) => s.km),
    ...state.odometer.map((o) => o.km),
    ...state.fuel.map((f) => f.km),
  ];
  return Math.max(0, ...all);
}

const STATUS_ORDER = { overdue: 0, soon: 1, ok: 2 };

export function taskStatus(task, services, km, today, baselineKm, baselineDate) {
  let last = null;
  for (const s of services) {
    if (!s.tasks.includes(task.id)) continue;
    if (!last || s.km > last.km || (s.km === last.km && s.date > last.date)) last = s;
  }
  const lastKm = last ? last.km : baselineKm;
  const lastDate = last ? last.date : baselineDate;

  let dueKm = null;
  let spanKm = 0;
  if (task.intervalKm != null) {
    const breakIns = (task.breakInKm || []).filter((b) => b > lastKm);
    const nextBreakIn = breakIns.length ? Math.min(...breakIns) : null;
    dueKm = nextBreakIn != null ? Math.min(nextBreakIn, lastKm + task.intervalKm) : lastKm + task.intervalKm;
    spanKm = Math.max(1, dueKm - lastKm);
  }

  let dueDate = null;
  let spanDays = 0;
  if (lastDate) {
    if (task.intervalMonths != null) dueDate = addMonths(lastDate, task.intervalMonths);
    if (task.intervalDays != null) {
      const byDays = addDays(lastDate, task.intervalDays);
      dueDate = dueDate && dueDate < byDays ? dueDate : byDays;
    }
    if (dueDate) spanDays = Math.max(1, daysBetween(lastDate, dueDate));
  }

  // A purely time-based check that was never logged is simply "due once".
  const neverTimed = task.intervalKm == null && !last;
  const kmLeft = dueKm != null ? dueKm - km : null;
  const daysLeft = neverTimed || !dueDate ? null : daysBetween(today, dueDate);

  const kmProgress = dueKm != null ? (km - lastKm) / spanKm : 0;
  const dayProgress = dueDate && lastDate ? daysBetween(lastDate, today) / spanDays : 0;
  const progress = neverTimed ? 1 : Math.max(0, kmProgress, dayProgress);

  const overdue = (kmLeft != null && kmLeft <= 0) || (daysLeft != null && daysLeft < 0);
  const soonKm = kmLeft != null && kmLeft <= Math.max(100, Math.round(spanKm * 0.15));
  const soonDays = daysLeft != null && daysLeft <= Math.max(1, Math.floor(spanDays / 5));
  let status = 'ok';
  if (overdue && !neverTimed) status = 'overdue';
  else if (overdue || soonKm || soonDays || neverTimed) status = 'soon';

  return { task, lastKm: last ? last.km : null, lastDate, doneBefore: !!last, dueKm, dueDate, kmLeft, daysLeft, progress, status };
}

export function statuses(tasks, services, km, today, baselineKm, baselineDate) {
  return tasks
    .map((t) => taskStatus(t, services, km, today, baselineKm, baselineDate))
    .sort((a, b) => STATUS_ORDER[a.status] - STATUS_ORDER[b.status] || b.progress - a.progress);
}

export function healthScore(list) {
  if (!list.length) return 100;
  let total = 0;
  let earned = 0;
  for (const s of list) {
    const w = s.task.severity === 'critical' ? 3 : s.task.severity === 'high' ? 2 : 1;
    total += w;
    earned += w * (s.status === 'ok' ? 1 : s.status === 'soon' ? 0.6 : 0);
  }
  return Math.round((100 * earned) / total);
}

/** Passport grid: stamped / done / next / missed / upcoming per milestone. */
export function milestoneStates(milestones, services, km) {
  let nextAssigned = false;
  return milestones.map((m) => {
    const recs = services.filter((s) => s.milestone === m.km).sort((a, b) => (a.date < b.date ? 1 : -1));
    const record = recs[0] || null;
    let state;
    if (record && record.stamped) state = 'stamped';
    else if (record) state = 'done';
    else if (!nextAssigned && m.km >= km - MISSED_TOLERANCE_KM) {
      nextAssigned = true;
      state = 'next';
    } else if (m.km < km - MISSED_TOLERANCE_KM) state = 'missed';
    else state = 'upcoming';
    return { milestone: m, record, state };
  });
}

/** Full-tank method; see FuelStats.kt. */
export function fuelSummary(fills) {
  const sorted = [...fills].sort((a, b) => a.km - b.km || (a.date < b.date ? -1 : 1));
  let segLiters = 0;
  let segDistance = 0;
  let lastSegment = null;
  let anchor = null;
  let pending = 0;
  for (const f of sorted) {
    if (!anchor) {
      if (f.full) anchor = f;
      continue;
    }
    pending += f.liters;
    if (f.full) {
      const d = f.km - anchor.km;
      if (d > 0) {
        segLiters += pending;
        segDistance += d;
        lastSegment = (pending * 100) / d;
      }
      anchor = f;
      pending = 0;
    }
  }
  const totalCost = sorted.reduce((s, f) => s + (f.cost || 0), 0);
  const distance = sorted.length >= 2 ? sorted[sorted.length - 1].km - sorted[0].km : 0;
  const costAfterFirst = sorted.slice(1).reduce((s, f) => s + (f.cost || 0), 0);
  return {
    litersPer100: segDistance > 0 ? (segLiters * 100) / segDistance : null,
    lastLitersPer100: lastSegment,
    costPerKm: distance > 0 && costAfterFirst > 0 ? costAfterFirst / distance : null,
    totalCost,
    totalLiters: sorted.reduce((s, f) => s + f.liters, 0),
    distance,
  };
}

/** Everything the dashboard needs in one pass. */
export function snapshot(content, state, today = todayIso()) {
  const km = currentKm(state);
  const bike = state.bike || {};
  const list = statuses(content.tasks, state.services, km, today, bike.purchaseKm || 0, bike.purchaseDate || null);
  return {
    km,
    today,
    statuses: list,
    health: healthScore(list),
    milestones: milestoneStates(content.milestones, state.services, km),
    fuel: fuelSummary(state.fuel),
    warrantyEnd: bike.purchaseDate ? addMonths(bike.purchaseDate, bike.warrantyMonths || 24) : null,
  };
}
