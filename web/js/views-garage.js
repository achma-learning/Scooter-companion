// Garage-side views: dashboard, service, passport, fuel, wallet, profile, SOS, more, settings.
import * as S from './store.js';
import { todayIso, daysBetween, toIso } from './planner.js';
import {
  esc, icon, fmtKm, fmtMad, fmtDate, fmtShort, STATUS, pill, bar, badge, dueText, topbar, section, navRow,
  callout, stat, empty, ring, toast, dialog, confirmDialog, SCOOTER_SVG,
} from './ui.js';
import { APK_URL } from './config.js';

const docType = (c, id) => c.documentTypes.find((t) => t.id === id) || c.documentTypes[c.documentTypes.length - 1];

export function taskCard(st) {
  const s = STATUS[st.status];
  return `<a class="card task" href="#/task/${st.task.id}">
    <div class="top"><h3 class="grow">${esc(st.task.name)}</h3>${pill(s.label, s.color)}</div>
    ${bar(st.progress, s.color)}
    <div class="bottom"><span class="grow">${esc(dueText(st.kmLeft, st.daysLeft))}</span>${st.dueKm != null ? `<span class="muted">at ${fmtKm(st.dueKm)}</span>` : ''}</div>
  </a>`;
}

// ------------------------------------------------------------------ Garage (home)

export function garage({ content: c, state, snap }) {
  const bike = state.bike;
  const upcoming = snap.statuses.filter((s) => s.status !== 'ok').slice(0, 5);
  const next = snap.milestones.find((m) => m.state === 'next');
  const alerts = state.documents
    .filter((d) => d.expiry && daysBetween(snap.today, d.expiry) <= docType(c, d.type).remindDays)
    .map((d) => ({ d, days: daysBetween(snap.today, d.expiry) }));
  const month = +snap.today.slice(5, 7);
  const season = c.seasons.find((s) => s.months.includes(month));
  const tip = c.warnings[Math.floor(Date.parse(snap.today) / 86400000) % c.warnings.length];
  const preRideToday = state.preRide?.last === snap.today;
  const qa = (href, ic, label) => `<a class="card qa" href="${href}">${badge(ic)}<span>${label}</span></a>`;

  const html = `
    <section class="hero">
      <div class="row" style="align-items:flex-start">
        <div class="grow">
          <h1>${esc(bike.nickname || 'My Agility')}</h1>
          <p class="muted">${esc(c.bike.model)}${bike.plate ? ` · ${esc(bike.plate)}` : ''}</p>
        </div>
        ${ring(snap.health)}
      </div>
      ${SCOOTER_SVG}
      <div class="row">
        <div class="grow"><div class="small muted bold">Odometer</div><div class="km">${fmtKm(snap.km)}</div></div>
        <button class="btn tonal" id="updKm">${icon('gauge')} Update km</button>
      </div>
    </section>
    ${!state.hasProfile ? `<div style="margin-top:12px">${navRow('#/profile', 'pencil', 'Set up your scooter', 'Purchase date & km make every reminder accurate.')}</div>` : ''}
    ${section('Quick actions')}
    <div class="grid3 wide">
      ${qa('#/log', 'wrench', 'Log service')}${qa('#/fuel', 'fuel', 'Fuel')}${qa('#/checklist', 'list-checks', preRideToday ? 'Checked ✓' : 'Pre-ride')}
      ${qa('#/trouble', 'stethoscope', 'Diagnose')}${qa('#/passport', 'badge-check', 'Passport')}${qa('#/sos', 'siren', 'SOS')}
    </div>
    ${section('Next up', '<a href="#/service" class="bold">All services</a>')}
    <div class="list">${(upcoming.length ? upcoming : snap.statuses.slice(0, 3)).map(taskCard).join('')}</div>
    ${next ? `${section('Passport')}${navRow('#/passport', 'badge-check', `Next stamp: ${fmtKm(next.milestone.km)}`, next.milestone.actions)}` : ''}
    ${alerts.length ? section('Documents') + `<div class="list">${alerts.map(({ d, days }) => navRow('#/wallet', 'contact', d.title,
      days < 0 ? `Expired ${-days} days ago` : `Expires in ${days} days · ${fmtDate(d.expiry)}`, days < 0 ? 'var(--due)' : 'var(--soon)')).join('')}</div>` : ''}
    ${season ? section('This season') + navRow('#/seasons', 'sun', `${season.name} · ${season.range}`, `${season.headline} ${season.rows[0][0]}: ${season.rows[0][1]}`, 'var(--soon)') : ''}
    <div style="margin-top:14px">${callout(`Tip · ${tip.title}`, tip.text)}</div>
    ${APK_URL ? `<div style="margin-top:14px"><a class="card banner" href="${APK_URL}">${badge('smartphone')}<div class="grow"><h3>Get the Android app</h3><p class="small muted">Same garage offline, with reminders. Move data via Settings → Export.</p></div>${icon('download')}</a></div>` : ''}`;

  const mount = (root) => {
    root.querySelector('#updKm').onclick = async () => {
      const { value, form } = await dialog(`<h2>Update odometer</h2><p class="small muted">Reading on the dashboard today.</p>
        <label class="field"><span>Kilometres</span><input name="km" type="number" inputmode="numeric" min="${snap.km}" value="${snap.km}" required></label>
        <div class="btnrow"><button class="btn tonal" value="cancel" formnovalidate>Cancel</button><button class="btn" value="ok">Save</button></div>`);
      const km = parseInt(form.km.value, 10);
      if (value === 'ok' && km >= snap.km) S.addOdometer(km, todayIso());
      else if (value === 'ok') toast(`Must be at least ${fmtKm(snap.km)}`);
    };
  };
  return { html, mount };
}

// ------------------------------------------------------------------ Service

export function service({ content: c, state, snap }, params) {
  const tab = params.get('tab') || 'due';
  const groups = [['overdue', 'Due now'], ['soon', 'Coming up'], ['ok', 'On schedule']];
  const due = groups.map(([k, title]) => {
    const g = snap.statuses.filter((s) => s.status === k);
    return g.length ? section(`${title} · ${g.length}`) + `<div class="list">${g.map(taskCard).join('')}</div>` : '';
  }).join('');
  const history = state.services.length
    ? `<div class="list">${state.services.map((e) => {
      const names = e.tasks.map((id) => c.tasks.find((t) => t.id === id)?.name).filter(Boolean);
      const meta = [e.milestone ? `Passport ${fmtKm(e.milestone)}` : '', e.shop, e.cost != null ? fmtMad(e.cost) : ''].filter(Boolean);
      return `<a class="card" href="#/log?entry=${e.id}">
        <div class="row"><b>${fmtKm(e.km)}</b><span class="small muted grow">· ${fmtDate(e.date)}</span>${e.stamped ? pill('Stamped') : ''}</div>
        ${names.length ? `<p style="margin-top:4px">${esc(names.join(' · '))}</p>` : ''}
        ${meta.length ? `<p class="small muted">${esc(meta.join(' · '))}</p>` : ''}
        ${e.notes ? `<p class="small" style="margin-top:4px">${esc(e.notes)}</p>` : ''}</a>`;
    }).join('')}</div>`
    : empty('wrench', 'No services logged yet', 'Log oil changes and dealer visits to keep reminders accurate and your passport complete.');
  const html = `
    ${topbar('Service', false)}
    <div class="tabs" role="tablist">
      <button role="tab" aria-selected="${tab === 'due'}" data-tab="due">Due</button>
      <button role="tab" aria-selected="${tab === 'history'}" data-tab="history">History (${state.services.length})</button>
    </div>
    ${tab === 'due' ? `<p class="small muted">Intervals tuned for Marrakech heat & dust. At ${fmtKm(snap.km)}.</p>${due}` : history}
    <a class="btn fab" href="#/log">${icon('plus')} Log service</a>`;
  const mount = (root) => root.querySelectorAll('[data-tab]').forEach((b) => (b.onclick = () => (location.hash = `#/service?tab=${b.dataset.tab}`)));
  return { html, mount };
}

export function task({ content: c, snap }, params, id) {
  const st = snap.statuses.find((s) => s.task.id === id);
  if (!st) return { html: topbar('Not found') };
  const t = st.task;
  const s = STATUS[st.status];
  const guide = t.guide && c.guides.find((g) => g.id === t.guide);
  const html = `
    ${topbar(t.name)}
    <div class="card">
      <div class="row"><span class="small muted grow">${esc(t.fr)}</span>${pill(s.label, s.color)}</div>
      <div style="font-size:24px;font-weight:750;color:${s.color};margin:10px 0 8px">${esc(dueText(st.kmLeft, st.daysLeft))}</div>
      ${bar(st.progress, s.color)}
      <dl class="kv" style="margin-top:14px">
        ${st.dueKm != null ? `<dt>Due at</dt><dd>${fmtKm(st.dueKm)}</dd>` : ''}
        ${st.dueDate ? `<dt>Due by</dt><dd>${fmtDate(st.dueDate)}</dd>` : ''}
        <dt>Last done</dt><dd>${st.doneBefore ? [st.lastKm != null ? fmtKm(st.lastKm) : '', fmtDate(st.lastDate)].filter(Boolean).join(' · ') : 'Not logged yet'}</dd>
        <dt>Marrakech interval</dt><dd>${esc(t.label)}</dd>
        <dt>Factory interval</dt><dd>${esc(t.factory)}</dd>
        ${t.cost ? `<dt>Typical cost</dt><dd>${esc(t.cost)}</dd>` : ''}
      </dl>
    </div>
    <div class="btnrow" style="margin:12px 0">
      <button class="btn" id="doneToday">${icon('check')} Done today</button>
      <a class="btn ghost" href="#/log?task=${t.id}">Log with details</a>
    </div>
    ${callout('Why & how', t.notes)}
    ${guide ? navRow(`#/howto/${guide.id}`, 'book-open', `Step-by-step: ${guide.title}`, `${guide.time} · ${guide.difficulty}`) : ''}`;
  const mount = (root) => {
    root.querySelector('#doneToday').onclick = async () => {
      if (await confirmDialog('Mark as done?', `Logs "${t.name}" today at ${fmtKm(snap.km)}. Update the odometer first if it changed.`, 'Log it')) {
        S.saveService({ date: todayIso(), km: snap.km, tasks: [t.id], milestone: null, shop: '', cost: null, stamped: false, notes: '' });
        toast('Logged');
      }
    };
  };
  return { html, mount };
}

export function logService({ content: c, state, snap }, params) {
  const existing = state.services.find((e) => e.id === params.get('entry'));
  const presetMs = c.milestones.find((m) => m.km === +params.get('milestone'));
  const e = existing || {
    date: todayIso(), km: snap.km, tasks: presetMs ? [...presetMs.tasks] : params.get('task') ? [params.get('task')] : [],
    milestone: presetMs?.km ?? null, shop: '', cost: null, stamped: !!presetMs, notes: '',
  };
  const candidates = snap.milestones.filter((m) => m.state !== 'upcoming' || m.milestone.km <= snap.km + 2500).slice(-8);
  const chip = (attr, val, label, on) => `<button type="button" class="chip" data-${attr}="${esc(val)}" aria-pressed="${on}">${esc(label)}</button>`;
  const html = `
    ${topbar(existing ? 'Edit service' : 'Log service', true, existing ? `<button class="iconbtn" id="del" aria-label="Delete">${icon('trash-2')}</button>` : '')}
    <form id="f" novalidate>
      <div class="grid2">
        <label class="field"><span>Date</span><input type="date" name="date" value="${e.date}" required></label>
        <label class="field"><span>Odometer (km)</span><input type="number" inputmode="numeric" name="km" value="${e.km}" min="0" required></label>
      </div>
      ${section('Passport milestone')}
      <div class="chips scroll">${chip('ms', '', 'None', e.milestone == null)}${candidates.map((m) =>
        chip('ms', m.milestone.km, (m.state === 'stamped' ? '★ ' : '') + fmtKm(m.milestone.km), e.milestone === m.milestone.km)).join('')}</div>
      <p class="small muted" id="msInfo" style="margin-top:6px"></p>
      ${section('Work done')}
      ${c.categories.map((cat) => `<p class="small muted bold" style="margin:10px 0 6px">${esc(cat.name)}</p>
        <div class="chips">${c.tasks.filter((t) => t.cat === cat.id).map((t) => chip('task', t.id, t.name, e.tasks.includes(t.id))).join('')}</div>`).join('')}
      ${section('Where & cost')}
      <label class="field"><span>Workshop / dealer</span><input name="shop" value="${esc(e.shop)}" list="shops"></label>
      <datalist id="shops">${[...c.shops.map((s) => s.name), 'DIY'].map((n) => `<option value="${esc(n)}">`).join('')}</datalist>
      <label class="field"><span>Cost (MAD)</span><input name="cost" type="number" inputmode="decimal" step="0.01" min="0" value="${e.cost ?? ''}"></label>
      <label class="switch"><input type="checkbox" name="stamped" ${e.stamped ? 'checked' : ''}><span class="grow">Stamped in the paper passport<br><span class="small muted">Approved-dealer stamps keep the warranty valid.</span></span></label>
      <label class="field"><span>Notes</span><textarea name="notes" rows="2">${esc(e.notes)}</textarea></label>
      <button class="btn block" style="margin-top:12px">${existing ? 'Save changes' : 'Save service'}</button>
    </form>`;
  const mount = (root) => {
    const f = root.querySelector('#f');
    let milestone = e.milestone;
    const tasks = new Set(e.tasks);
    const info = () => (root.querySelector('#msInfo').textContent = c.milestones.find((m) => m.km === milestone)?.actions || '');
    info();
    root.querySelectorAll('[data-ms]').forEach((b) => (b.onclick = () => {
      milestone = b.dataset.ms ? +b.dataset.ms : null;
      root.querySelectorAll('[data-ms]').forEach((x) => x.setAttribute('aria-pressed', x === b));
      const m = c.milestones.find((x) => x.km === milestone);
      m?.tasks.forEach((id) => {
        tasks.add(id);
        root.querySelector(`[data-task="${id}"]`)?.setAttribute('aria-pressed', 'true');
      });
      info();
    }));
    root.querySelectorAll('[data-task]').forEach((b) => (b.onclick = () => {
      const id = b.dataset.task;
      tasks.has(id) ? tasks.delete(id) : tasks.add(id);
      b.setAttribute('aria-pressed', tasks.has(id));
    }));
    f.onsubmit = (ev) => {
      ev.preventDefault();
      const km = parseInt(f.km.value, 10);
      if (!f.date.value || Number.isNaN(km)) return toast('Date and odometer are required');
      if (!tasks.size && !f.notes.value.trim()) return toast('Pick at least one task or add a note');
      S.saveService({
        ...(existing ? { id: existing.id } : {}), date: f.date.value, km, tasks: [...tasks], milestone,
        shop: f.shop.value.trim(), cost: f.cost.value === '' ? null : +f.cost.value, stamped: f.stamped.checked, notes: f.notes.value.trim(),
      });
      toast('Service saved');
      history.back();
    };
    root.querySelector('#del')?.addEventListener('click', async () => {
      if (await confirmDialog('Delete this service?', `${fmtKm(existing.km)} · ${fmtDate(existing.date)}`, 'Delete', true)) {
        S.deleteService(existing.id);
        history.back();
      }
    });
  };
  return { html, mount };
}

// ------------------------------------------------------------------ Passport

export function passport({ content: c, state, snap }) {
  const b = state.bike;
  const stamped = snap.milestones.filter((m) => m.state === 'stamped').length;
  const w = snap.warrantyEnd;
  const wDays = w ? daysBetween(snap.today, w) : null;
  const f = (label, value) => `<div class="f grow"><span>${label}</span><b>${esc(value || '—')}</b></div>`;
  const stampHtml = (m) => {
    const sub = { stamped: fmtShort(m.record?.date), done: fmtShort(m.record?.date), next: 'next', missed: 'missed', upcoming: m.milestone.source === 'wiki' ? 'wiki' : '' }[m.state];
    const href = m.record ? `#/log?entry=${m.record.id}` : `#/log?milestone=${m.milestone.km}`;
    return `<a class="stamp ${m.state}" href="${href}" title="${esc(m.milestone.actions)}"><b>${m.milestone.km.toLocaleString('fr-FR').replace(/ | /g, ' ')}</b><small>km</small><small>${sub}</small></a>`;
  };
  const html = `
    ${topbar('Digital passport', true, `<a class="iconbtn" href="#/profile" aria-label="Edit bike">${icon('pencil')}</a>`)}
    <div class="passport">
      <div class="eyebrow">PASSEPORT D'ENTRETIEN</div>
      <h1 style="color:#fff;margin-top:2px">${esc(b.nickname || c.bike.model)}</h1>
      <p class="small" style="opacity:.85">${esc(c.bike.model)} · ${esc(c.bike.code)}</p>
      <div class="row" style="margin-top:12px">${f('Plate', b.plate)}${f('Chassis (VIN)', b.vin)}</div>
      <div class="row" style="margin-top:8px">${f('Purchased', b.purchaseDate ? fmtDate(b.purchaseDate) : '')}${f('Dealer', b.dealer)}</div>
    </div>
    <div class="grid2" style="margin-top:12px">${stat(stamped, 'dealer stamps')}${stat(fmtKm(snap.km), 'odometer')}</div>
    ${w ? callout(wDays >= 0 ? `Warranty valid until ${fmtDate(w)}` : `Warranty ended ${fmtDate(w)}`,
      'Have every service stamped by an approved dealer (YOUKA Moto, Mob Shop…) — an unstamped service can void a claim. The stamped booklet is also your #1 resale argument.',
      wDays >= 0 ? 'var(--ok)' : 'var(--muted)') : callout('Add your purchase date', 'It sets the warranty end date and the first reminders.')}
    ${section('Service stamps')}
    <div class="stamps">${snap.milestones.map(stampHtml).join('')}</div>
    <p class="small muted" style="margin-top:14px">Red = dealer-stamped · green outline = done without stamp · dashed = next. Kymco's booklet stamps 300, 1,000, 3,000 km then every 2,000 km; the wiki adds 2,000 km plus 8,000/10,000 km wear checks.</p>`;
  return { html };
}

// ------------------------------------------------------------------ Fuel

async function fuelDialog(entry) {
  const { value, form } = await dialog(`<h2>${entry.id ? 'Edit fill-up' : 'New fill-up'}</h2>
    <div class="grid2"><label class="field"><span>Date</span><input type="date" name="date" value="${entry.date}" required></label>
    <label class="field"><span>Odometer (km)</span><input type="number" name="km" inputmode="numeric" value="${entry.km}" required></label></div>
    <div class="grid2"><label class="field"><span>Litres</span><input type="number" name="liters" step="0.01" min="0.01" inputmode="decimal" value="${entry.liters || ''}" required></label>
    <label class="field"><span>Paid (MAD)</span><input type="number" name="cost" step="0.01" min="0" inputmode="decimal" value="${entry.cost ?? ''}"></label></div>
    <label class="switch"><input type="checkbox" name="full" ${entry.full ? 'checked' : ''}><span>Filled the tank</span></label>
    <div class="btnrow" style="margin-top:12px">${entry.id ? '<button class="btn danger" value="delete" formnovalidate>Delete</button>' : ''}
    <button class="btn tonal" value="cancel" formnovalidate>Cancel</button><button class="btn" value="ok">Save</button></div>`);
  if (value === 'delete') S.deleteFuel(entry.id);
  if (value === 'ok') {
    S.saveFuel({ ...entry, date: form.date.value, km: +form.km.value, liters: +form.liters.value, cost: form.cost.value === '' ? null : +form.cost.value, full: form.full.checked });
  }
}

export function fuel({ state, snap }) {
  const f = snap.fuel;
  const html = `
    ${topbar('Fuel log')}
    <div class="grid2">${stat(f.litersPer100 != null ? f.litersPer100.toFixed(2) : '—', 'L/100 km avg')}${stat(f.costPerKm != null ? f.costPerKm.toFixed(2) : '—', 'MAD / km')}
    ${stat(fmtMad(f.totalCost), 'total spent')}${stat(fmtKm(f.distance), 'logged')}</div>
    <p class="small muted" style="margin:10px 0">Factory figure: &lt; 2 L/100 km. Log full tanks for accurate averages — a sudden rise often means a dirty air filter or a dragging brake.</p>
    ${state.fuel.length ? `<div class="list">${state.fuel.map((e) => `<button class="card navrow" data-id="${e.id}">${badge('fuel')}
      <div class="grow"><h3>${e.liters.toFixed(2)} L${e.cost != null ? ` · ${fmtMad(e.cost)}` : ''}</h3><p class="small muted">${fmtKm(e.km)} · ${fmtDate(e.date)}${e.full ? '' : ' · partial'}</p></div></button>`).join('')}</div>`
      : empty('fuel', 'No fill-ups yet', 'Add each fill-up with the odometer reading.')}
    <button class="btn fab" id="add">${icon('plus')} Fill-up</button>`;
  const mount = (root) => {
    root.querySelector('#add').onclick = () => fuelDialog({ date: todayIso(), km: snap.km, liters: 0, cost: null, full: true });
    root.querySelectorAll('[data-id]').forEach((b) => (b.onclick = () => fuelDialog(state.fuel.find((x) => x.id === b.dataset.id))));
  };
  return { html, mount };
}

// ------------------------------------------------------------------ Wallet

async function docDialog(c, doc) {
  const { value, form } = await dialog(`<h2>${doc.id ? 'Edit document' : 'Add document'}</h2>
    <label class="field"><span>Type</span><select name="type">${c.documentTypes.map((t) => `<option value="${t.id}" ${t.id === doc.type ? 'selected' : ''}>${esc(t.label)} — ${esc(t.fr)}</option>`).join('')}</select></label>
    <p class="small muted" id="hint"></p>
    <label class="field"><span>Name</span><input name="title" value="${esc(doc.title)}" required></label>
    <label class="field"><span>Number / policy</span><input name="number" value="${esc(doc.number)}"></label>
    <label class="field"><span>Expiry date</span><input type="date" name="expiry" value="${doc.expiry || ''}"></label>
    <label class="field"><span>Notes</span><input name="notes" value="${esc(doc.notes)}"></label>
    <div class="btnrow" style="margin-top:12px">${doc.id ? '<button class="btn danger" value="delete" formnovalidate>Delete</button>' : ''}
    <button class="btn tonal" value="cancel" formnovalidate>Cancel</button><button class="btn" value="ok">Save</button></div>`, (d) => {
    const sel = d.querySelector('[name=type]');
    const title = d.querySelector('[name=title]');
    const upd = () => {
      const t = docType(c, sel.value);
      d.querySelector('#hint').textContent = t.hint;
      if (!title.value || c.documentTypes.some((x) => x.label === title.value)) title.value = t.label;
    };
    sel.onchange = upd;
    upd();
  });
  if (value === 'delete') S.deleteDocument(doc.id);
  if (value === 'ok') S.saveDocument({ ...doc, type: form.type.value, title: form.title.value.trim(), number: form.number.value.trim(), expiry: form.expiry.value || null, notes: form.notes.value.trim() });
}

export function wallet({ content: c, state, snap }) {
  const color = (d) => {
    if (!d.expiry) return 'var(--muted)';
    const days = daysBetween(snap.today, d.expiry);
    return days < 0 ? 'var(--due)' : days <= docType(c, d.type).remindDays ? 'var(--soon)' : 'var(--ok)';
  };
  const html = `
    ${topbar('Documents wallet')}
    ${callout('Keep these under the seat', 'Insurance certificate + carte grise must be on the bike. Riding with an expired licence or no certificate voids your insurance cover.')}
    ${state.documents.length ? `<div class="list">${state.documents.map((d) => {
      const days = d.expiry ? daysBetween(snap.today, d.expiry) : null;
      return `<button class="card navrow" data-id="${d.id}">${badge('contact', color(d))}
        <div class="grow"><h3>${esc(d.title)}</h3><p class="small muted">${esc([docType(c, d.type).fr, d.number].filter(Boolean).join(' · '))}</p>
        ${d.expiry ? `<p class="small">Expires ${fmtDate(d.expiry)}</p>` : ''}</div>
        ${days != null ? pill(days < 0 ? 'Expired' : `${days} d`, color(d)) : ''}</button>`;
    }).join('')}</div>` : empty('contact', 'No documents yet', 'Add your insurance, carte grise and licence to get expiry reminders.')}
    <button class="btn fab" id="add">${icon('plus')} Add document</button>`;
  const mount = (root) => {
    root.querySelector('#add').onclick = () => docDialog(c, { type: 'insurance', title: '', number: '', expiry: null, notes: '' });
    root.querySelectorAll('[data-id]').forEach((b) => (b.onclick = () => docDialog(c, state.documents.find((x) => x.id === b.dataset.id))));
  };
  return { html, mount };
}

// ------------------------------------------------------------------ Profile

export function profile({ content: c, state }) {
  const b = state.bike;
  const field = (name, label, value, type = 'text', extra = '') =>
    `<label class="field"><span>${label}</span><input name="${name}" type="${type}" value="${esc(value ?? '')}" ${extra}></label>`;
  const html = `
    ${topbar('My scooter')}
    <form id="f">
      ${field('nickname', 'Nickname', b.nickname, 'text', 'placeholder="My Agility"')}
      ${field('plate', 'Plate number', b.plate)}
      ${field('vin', 'Chassis number (VIN)', b.vin)}
      ${field('engineNo', 'Engine number', b.engineNo)}
      ${field('color', 'Colour', b.color)}
      ${field('purchaseDate', 'Purchase date', b.purchaseDate, 'date')}
      ${field('purchaseKm', 'Km at purchase', b.purchaseKm, 'number', 'min="0" inputmode="numeric"')}
      <p class="small muted">0 for a new scooter. Reminders count from here until you log services.</p>
      ${field('dealer', 'Selling dealer', b.dealer, 'text', 'list="dealers"')}
      <datalist id="dealers">${c.shops.map((s) => `<option value="${esc(s.name)}">`).join('')}</datalist>
      ${field('warrantyMonths', 'Warranty length (months)', b.warrantyMonths, 'number', 'min="0"')}
      <button class="btn block" style="margin-top:12px">Save</button>
      <p class="small muted" style="margin-top:12px">Model: ${esc(c.bike.model)} (${esc(c.bike.code)})</p>
    </form>`;
  const mount = (root) => {
    const f = root.querySelector('#f');
    f.onsubmit = (ev) => {
      ev.preventDefault();
      S.saveBike({
        nickname: f.nickname.value.trim(), plate: f.plate.value.trim(), vin: f.vin.value.trim(), engineNo: f.engineNo.value.trim(),
        color: f.color.value.trim(), purchaseDate: f.purchaseDate.value || null, purchaseKm: parseInt(f.purchaseKm.value, 10) || 0,
        dealer: f.dealer.value.trim(), warrantyMonths: parseInt(f.warrantyMonths.value, 10) || 24,
      });
      toast('Saved');
      history.back();
    };
  };
  return { html, mount };
}

// ------------------------------------------------------------------ More / SOS / Settings

export function more({ content: c }) {
  const html = `
    ${topbar('More', false)}
    ${section('Ownership')}
    <div class="list">
      ${navRow('#/passport', 'badge-check', 'Digital passport', 'Service stamps & warranty')}
      ${navRow('#/fuel', 'fuel', 'Fuel log', 'Consumption & running costs')}
      ${navRow('#/wallet', 'contact', 'Documents wallet', 'Insurance, carte grise, licence — expiry dates')}
      ${navRow('#/profile', 'pencil', 'My scooter', 'Plate, VIN, purchase date, dealer')}
    </div>
    ${section('Help')}
    <div class="list">
      ${navRow('#/sos', 'siren', 'SOS & accident', 'Emergency numbers, dealers, what to do', 'var(--due)')}
      ${navRow(c.bike.wiki, 'external-link', 'Marrakech maintenance wiki', c.bike.wiki)}
    </div>
    ${section('App')}
    <div class="list">
      ${navRow('#/settings', 'settings', 'Settings & backup', 'Theme, export / import to the Android app')}
      ${APK_URL ? navRow(APK_URL, 'smartphone', 'Download the Android app', 'APK from the latest GitHub release') : ''}
    </div>
    <p class="small muted" style="margin-top:16px">Knowledge base updated ${esc(c.updated)}. Your garage is stored only in this browser. Not affiliated with KYMCO.</p>`;
  return { html };
}

export function sos({ content: c, state }) {
  const ins = state.documents.find((d) => d.type === 'insurance');
  const html = `
    ${topbar('SOS & accident')}
    ${section('Emergency numbers (Morocco)')}
    <div class="list">${c.emergency.map((e) => `<a class="card navrow" href="tel:${e.number}">${badge('phone', 'var(--due)')}
      <div class="grow"><h3>${esc(e.label)}</h3>${e.note ? `<p class="small muted">${esc(e.note)}</p>` : ''}</div>
      <b style="font-size:24px;color:var(--due)">${esc(e.number)}</b></a>`).join('')}</div>
    ${section('After an accident')}
    <ol style="padding-left:20px;margin:0">${c.accidentSteps.map((s) => `<li style="margin:6px 0">${esc(s)}</li>`).join('')}</ol>
    ${callout('Insurance: declare within 5 days', ins
      ? `${ins.title}${ins.number ? ` · n° ${ins.number}` : ''}${ins.expiry ? ` · valid until ${fmtDate(ins.expiry)}` : ''}`
      : 'Add your insurance certificate to the wallet so its number is here when you need it.', 'var(--due)')}
    <a class="btn tonal block" href="#/wallet">Open documents wallet</a>
    ${section('Approved dealers')}
    <div class="list">${c.shops.map((s) => navRow(s.maps, 'map-pin', s.name, `${s.role} — open in Maps`)).join('')}</div>`;
  return { html };
}

export function settings() {
  const theme = S.getTheme();
  const html = `
    ${topbar('Settings & backup')}
    ${section('Theme')}
    <div class="chips">${['light', 'dark', 'system'].map((t) => `<button class="chip" data-theme-pick="${t}" aria-pressed="${theme === t}">${t[0].toUpperCase() + t.slice(1)}</button>`).join('')}</div>
    ${section('Backup & Android app')}
    <p class="small muted">Export your garage (bike, services, passport stamps, fuel, documents) to a JSON file. The Android app imports the same file (More → Settings → Import), and vice-versa.</p>
    <div class="btnrow" style="margin:10px 0">
      <button class="btn" id="export">${icon('download')} Export</button>
      <label class="btn ghost" style="cursor:pointer">${icon('upload')} Import…<input type="file" id="import" accept="application/json,.json" hidden></label>
    </div>
    ${section('Privacy')}
    <p class="small">Your garage lives only in this browser's local storage — nothing is sent anywhere. Clearing site data erases it, so export a backup now and then.</p>
    <button class="btn danger" id="wipe" style="margin-top:12px">${icon('trash-2')} Erase garage data…</button>`;
  const mount = (root) => {
    root.querySelectorAll('[data-theme-pick]').forEach((b) => (b.onclick = () => {
      S.setTheme(b.dataset.themePick);
      root.querySelectorAll('[data-theme-pick]').forEach((x) => x.setAttribute('aria-pressed', x === b));
    }));
    root.querySelector('#export').onclick = () => {
      const blob = new Blob([JSON.stringify(S.exportBackup(), null, 2)], { type: 'application/json' });
      const a = document.createElement('a');
      a.href = URL.createObjectURL(blob);
      a.download = `agility-garage-${toIso(Date.now())}.json`;
      a.click();
      setTimeout(() => URL.revokeObjectURL(a.href), 1000);
    };
    root.querySelector('#import').onchange = async (ev) => {
      const file = ev.target.files[0];
      if (!file) return;
      if (!(await confirmDialog('Replace garage with backup?', 'Your current bike profile, services, fuel log and documents in this browser will be replaced.', 'Replace'))) return;
      try {
        toast(S.importBackup(JSON.parse(await file.text())));
      } catch (e) {
        toast(`Import failed: ${e.message}`);
      }
    };
    root.querySelector('#wipe').onclick = async () => {
      if (await confirmDialog('Erase all garage data?', 'Deletes the bike profile, every logged service, fuel fill-up and document from this browser.', 'Erase', true)) {
        S.wipe();
        toast('Garage erased');
      }
    };
  };
  return { html, mount };
}
