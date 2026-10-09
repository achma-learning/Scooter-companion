// Knowledge views: the Marrakech wiki, offline and interactive.
import * as S from './store.js';
import { todayIso } from './planner.js';
import { esc, icon, fmtDate, pill, bar, topbar, section, navRow, callout, toast } from './ui.js';

export function guideHub({ content: c }) {
  const html = `
    ${topbar('Owner\'s guide', false)}
    <p class="muted">${esc(c.bike.tagline)}</p>
    ${section('Ride')}
    <div class="list">
      ${navRow('#/checklist', 'list-checks', 'Pre-ride checklist', `${c.checklist.length} checks · every ride & weekly`)}
      ${navRow('#/trouble', 'stethoscope', 'Troubleshooting', "Won't start, power loss, noises, overheating")}
      ${navRow('#/seasons', 'sun', 'Seasons & storage', 'Summer heat, winter starts, Ramadan storage')}
    </div>
    ${section('Maintain')}
    <div class="list">
      ${navRow('#/howto', 'wrench', 'How-to guides', `${c.guides.length} step-by-step procedures`)}
      ${navRow('#/issues', 'triangle-alert', 'Known failure modes', 'What breaks first in Marrakech, ranked', 'var(--due)')}
      ${navRow('#/oils', 'droplet', 'Oils', 'Two oil systems, grades by season')}
    </div>
    ${section('Reference')}
    <div class="list">
      ${navRow('#/specs', 'gauge', 'Specifications & torque', 'Factory figures from the manual p.40')}
      ${navRow('#/parts', 'shopping-cart', 'Parts, prices & resale', 'MAD prices, where to buy, when to sell')}
      ${navRow('#/gear', 'hard-hat', 'Gear & accessories', 'Helmet, visibility, lock, top case')}
    </div>`;
  return { html };
}

export function checklist({ content: c, state }, params) {
  const weekly = params.get('tab') === 'weekly';
  const items = c.checklist.filter((i) => i.group === (weekly ? 'weekly' : 'ride'));
  const html = `
    ${topbar('Pre-ride checklist')}
    <div class="tabs" role="tablist">
      <button role="tab" aria-selected="${!weekly}" data-tab="ride">Every ride</button>
      <button role="tab" aria-selected="${weekly}" data-tab="weekly">Weekly</button>
    </div>
    <div id="prog"></div>
    <div class="list" style="margin-top:10px">${items.map((i) => `<label class="card check"><input type="checkbox" data-id="${i.id}">
      <div><h3>${esc(i.title)}</h3><p class="small muted">${esc(i.detail)}</p></div></label>`).join('')}</div>
    <button class="btn block" id="done" style="margin-top:14px" disabled>Tick every item</button>
    ${callout("If anything feels wrong, don't ride", 'Spongy brakes, unusual engine noise, steering wobble or fluid leaks all warrant investigation first.', 'var(--due)')}
    ${section('Marrakech specifics')}
    ${c.localTips.map((t) => callout(t.title, t.text, 'var(--soon)')).join('')}`;
  const mount = (root) => {
    root.querySelectorAll('[data-tab]').forEach((b) => (b.onclick = () => (location.hash = `#/checklist?tab=${b.dataset.tab}`)));
    const boxes = [...root.querySelectorAll('[data-id]')];
    const btn = root.querySelector('#done');
    const upd = () => {
      const n = boxes.filter((b) => b.checked).length;
      root.querySelector('#prog').innerHTML = `${bar(n / boxes.length, 'var(--ok)')}<p class="small" style="margin-top:4px">${n} / ${boxes.length}${state.preRide?.last ? ` · last full check ${fmtDate(state.preRide.last)}` : ''}</p>`;
      btn.disabled = n !== boxes.length;
      btn.textContent = n === boxes.length ? 'All good — ride safe' : 'Tick every item';
    };
    boxes.forEach((b) => (b.onchange = upd));
    upd();
    btn.onclick = () => {
      S.markPreRide(todayIso());
      if (weekly) {
        S.saveService({ date: todayIso(), km: Math.max(0, ...state.services.map((s) => s.km), ...state.odometer.map((o) => o.km), state.bike.purchaseKm || 0),
          tasks: ['tyre-pressure', 'tyre-condition'], milestone: null, shop: '', cost: null, stamped: false, notes: '' });
      }
      toast('Checked — ride safe');
      history.back();
    };
  };
  return { html, mount };
}

const TROUBLE_ICON = { key: 'key-round', speed: 'gauge', noise: 'volume-2', heat: 'thermometer' };

export function troubleList({ content: c }) {
  const html = `
    ${topbar('Troubleshooting')}
    <p class="muted" style="margin-bottom:10px">Answer a few questions to find the likely cause.</p>
    <div class="list">${c.troubleshooting.map((t) => navRow(`#/trouble/${t.id}`, TROUBLE_ICON[t.icon] || 'circle-help', t.title, t.nodes[t.start].q)).join('')}</div>`;
  return { html };
}

export function troubleTree({ content: c }, params, id) {
  const tree = c.troubleshooting.find((t) => t.id === id);
  if (!tree) return { html: topbar('Not found') };
  // Path lives in the URL (?p=a.b.c) so the browser back button steps back one answer.
  const path = (params.get('p') || '').split('.').filter(Boolean).map(Number);
  const nodes = [tree.start];
  const answers = [];
  for (const i of path) {
    const opt = tree.nodes[nodes[nodes.length - 1]].options?.[i];
    if (!opt) break;
    answers.push(opt[0]);
    nodes.push(opt[1]);
  }
  const node = tree.nodes[nodes[nodes.length - 1]];
  const level = { danger: 'var(--due)', warn: 'var(--soon)' }[node.level] || 'var(--ok)';
  const guide = node.guide && c.guides.find((g) => g.id === node.guide);
  const href = (i) => `#/trouble/${id}?p=${[...path, i].join('.')}`;
  const html = `
    ${topbar(tree.title)}
    ${answers.map((a, i) => `<div class="trail">${icon('check')}<div><p class="small muted">${esc(tree.nodes[nodes[i]].q)}</p><b>${esc(a)}</b></div></div>`).join('')}
    <div style="margin-top:10px">
    ${node.q ? `<div class="card"><p class="small bold" style="color:var(--primary)">Step ${nodes.length}</p><h2 style="font-size:21px;margin:4px 0 8px">${esc(node.q)}</h2>
        ${node.options.map((o, i) => `<a class="btn ghost answer" href="${href(i)}">${esc(o[0])}</a>`).join('')}</div>`
      : `<div class="result" style="--c:${level}"><div class="eyebrow">Likely cause</div><h2 style="font-size:21px;margin:4px 0 8px">${esc(node.result)}</h2><p>${esc(node.text)}</p></div>
        ${guide ? `<div style="margin-top:10px">${navRow(`#/howto/${guide.id}`, 'book-open', `Guide: ${guide.title}`, guide.interval)}</div>` : ''}`}
    </div>
    ${path.length ? `<a class="btn tonal block" style="margin-top:14px" href="#/trouble/${id}">${icon('rotate-ccw')} Start over</a>` : ''}`;
  return { html };
}

export function issues({ content: c }) {
  const html = `
    ${topbar('Known failure modes')}
    <p class="muted" style="margin-bottom:10px">Ranked by risk under Marrakech heat, dust and stop-and-go traffic.</p>
    <div class="list">${c.failureModes.map((f) => {
      const col = f.risk >= 60 ? 'var(--due)' : f.risk >= 40 ? 'var(--soon)' : 'var(--ok)';
      return `<div class="card"><div class="row"><h3 class="grow">${f.rank}. ${esc(f.name)}</h3>${pill(`${f.risk}%`, col)}</div>
        <div style="margin:8px 0">${bar(f.risk / 100, col)}</div>
        ${[['How it fails', f.how], ['Detect', f.detect], ['Prevent', f.prevent]].map(([l, t]) => `<p class="small bold" style="color:var(--primary);margin-top:6px">${l}</p><p class="small">${esc(t)}</p>`).join('')}</div>`;
    }).join('')}</div>`;
  return { html };
}

export function howtoList({ content: c }) {
  const html = `
    ${topbar('How-to guides')}
    <p class="muted" style="margin-bottom:10px">Always work on a cold engine unless a guide says otherwise.</p>
    <div class="list">${c.guides.map((g) => navRow(`#/howto/${g.id}`, 'wrench', g.title, `${g.interval} · ${g.time} · ${g.difficulty}`)).join('')}</div>`;
  return { html };
}

export function howto({ content: c }, params, id) {
  const g = c.guides.find((x) => x.id === id);
  if (!g) return { html: topbar('Not found') };
  const html = `
    ${topbar(g.title)}
    <div class="chips">${pill(g.interval)} ${pill(g.time, 'var(--muted)')} ${pill(g.difficulty, 'var(--ok)')}</div>
    ${g.need.length ? section('You need') + `<ul style="margin:0;padding-left:20px">${g.need.map((n) => `<li>${esc(n)}</li>`).join('')}</ul>` : ''}
    ${section('Steps')}
    <p class="small muted">Tap a step to tick it off.</p>
    <div class="steps">${g.steps.map(([t, d], i) => `<div class="step" data-step><span class="n">${i + 1}</span><div class="t"><h3>${esc(t)}</h3><p class="small muted">${esc(d)}</p></div></div>`).join('')}</div>
    ${g.tips.map(([kind, t, d]) => callout(t, d, kind === 'warn' ? 'var(--due)' : 'var(--soon)')).join('')}`;
  const mount = (root) => root.querySelectorAll('[data-step]').forEach((s, i) => (s.onclick = () => {
    s.classList.toggle('done');
    s.querySelector('.n').textContent = s.classList.contains('done') ? '✓' : i + 1;
  }));
  return { html, mount };
}

export function specs({ content: c }, params) {
  const torque = params.get('tab') === 'torque';
  const html = `
    ${topbar('Specifications')}
    <div class="tabs" role="tablist">
      <button role="tab" aria-selected="${!torque}" data-tab="specs">Specs</button>
      <button role="tab" aria-selected="${torque}" data-tab="torque">Torque</button>
    </div>
    ${torque
      ? `<p class="small muted">N·m. Replace crush washers and U-nuts every time.</p><div class="card" style="margin-top:10px"><table class="data">${c.torque.map(([n, v, note]) =>
        `<tr><td>${esc(n)}${note ? `<br><span class="small muted">${esc(note)}</span>` : ''}</td><td style="color:var(--primary)">${esc(v)} N·m</td></tr>`).join('')}</table></div>`
      : c.specs.map((g) => section(g.group) + `<div class="card"><dl class="kv">${g.rows.map(([k, v]) => `<dt>${esc(k)}</dt><dd>${esc(v)}</dd>`).join('')}</dl></div>`).join('')}`;
  const mount = (root) => root.querySelectorAll('[data-tab]').forEach((b) => (b.onclick = () => (location.hash = `#/specs?tab=${b.dataset.tab}`)));
  return { html, mount };
}

export function oils({ content: c }) {
  const card = (t, grade, body) => `<div class="card"><p class="small muted bold">${t}</p><h2>${grade}</h2><p class="small" style="margin-top:4px">${body}</p></div>`;
  const html = `
    ${topbar('Oils')}
    <div class="grid2">${card('Engine oil', '10W-40 → 10W-60', '700 ml · JASO MB<br>Drain: right, 17 mm<br>Every 2,000 km')}${card('Gear oil', '80W-90 GL-4', '120-150 ml<br>Drain: left rear, 12 mm<br>Every 4,000 km')}</div>
    ${callout('Never mix them', 'Gear oil in the engine starves the cam and valves; engine oil in the gearbox lacks EP additives and pits the gears. Label your bottles ENGINE and GEARS.', 'var(--due)')}
    ${section('What to pour, when')}
    <div class="list">${c.oilStrategy.map((o) => `<div class="card"><p class="small bold" style="color:var(--primary)">${esc(o.period)}</p><h3>${esc(o.oil)}</h3><p class="small muted">${esc(o.price)} · change every ${esc(o.interval)}</p></div>`).join('')}</div>
    ${callout('JASO MB, not MA/MA2', 'The Agility 50 4T has a dry centrifugal clutch — use scooter-rated JASO MB oil.', 'var(--soon)')}`;
  return { html };
}

export function seasons({ content: c }, params) {
  const month = +todayIso().slice(5, 7);
  const current = c.seasons.find((s) => s.months.includes(month)) || c.seasons[0];
  const season = c.seasons.find((s) => s.id === params.get('s')) || current;
  const html = `
    ${topbar('Seasons & storage')}
    <div class="chips">${c.seasons.map((s) => `<a class="chip" href="#/seasons?s=${s.id}" aria-pressed="${s.id === season.id}">${icon(s.id === 'winter' ? 'snowflake' : 'sun')}${esc(s.name)}${s.months.includes(month) ? ' · now' : ''}</a>`).join('')}</div>
    <p class="small bold" style="color:var(--primary);margin-top:14px">${esc(season.range)}</p>
    <h2>${esc(season.headline)}</h2>
    <div class="list" style="margin-top:10px">${season.rows.map(([a, t]) => `<div class="card"><p class="small muted bold">${esc(a)}</p><p>${esc(t)}</p></div>`).join('')}</div>
    ${section('Parked > 2 weeks (Ramadan, holidays)')}
    <ol style="padding-left:20px;margin:0">${c.storage.map((s) => `<li style="margin:6px 0">${esc(s)}</li>`).join('')}</ol>`;
  return { html };
}

export function parts({ content: c }) {
  const html = `
    ${topbar('Parts, prices & resale')}
    <p class="small muted">Indicative Marrakech prices (MAD), early 2026. Negotiate; bring the old part.</p>
    <div class="card" style="margin-top:10px"><table class="data">${c.parts.map(([n, spec, price, where]) =>
      `<tr><td><b>${esc(n)}</b><br><span class="small muted">${esc(spec)} · ${esc(where)}</span></td><td>${esc(price)}</td></tr>`).join('')}</table></div>
    ${section('Fixed yearly costs')}
    ${c.fixedCosts.map(([n, p, note]) => callout(`${n} — ${p}`, note, 'var(--muted)')).join('')}
    ${section('Workshops')}
    <div class="list">${c.shops.map((s) => navRow(s.maps, 'map-pin', s.name, `${s.role}. ${s.note}`)).join('')}</div>
    ${section('Resale')}
    ${callout(c.resale.headline, c.resale.text)}
    ${section('Links')}
    <div class="list">${c.links.map((l) => navRow(l.url, 'external-link', l.title, l.url.replace('https://', '').slice(0, 48))).join('')}</div>`;
  return { html };
}

export function gear({ content: c }) {
  const html = `
    ${topbar('Gear & accessories')}
    ${callout('Priority #1: be seen', "On a 50 cc in faster traffic, most crashes are a driver who didn't see you. Visibility spending pays back more than any comfort accessory.")}
    ${c.gear.map((t) => section(t.tier) + `<div class="card stack">${t.items.map(([n, w]) => `<div><h3>${esc(n)}</h3><p class="small muted">${esc(w)}</p></div>`).join('')}</div>`).join('')}`;
  return { html };
}
