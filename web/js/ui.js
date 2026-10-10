// Small rendering helpers shared by every view.
import { ICONS } from './icons.js';
import { daysBetween } from './planner.js';

const ESC = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' };
export const esc = (v) => String(v ?? '').replace(/[&<>"']/g, (c) => ESC[c]);

export const icon = (name, cls = '') =>
  `<svg class="i ${cls}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${ICONS[name] || ''}</svg>`;

export const fmtKm = (km) => `${Math.round(km).toLocaleString('fr-FR').replace(/ | /g, ' ')} km`;
export const fmtMad = (v) => (v >= 100 ? `${Math.round(v).toLocaleString('fr-FR').replace(/ | /g, ' ')} MAD` : `${v.toFixed(2)} MAD`);
const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sept', 'Oct', 'Nov', 'Dec'];
export const fmtDate = (iso) => (iso ? `${+iso.slice(8, 10)} ${MONTHS[+iso.slice(5, 7) - 1]} ${iso.slice(0, 4)}` : '—');
export const fmtShort = (iso) => (iso ? `${iso.slice(8, 10)}/${iso.slice(5, 7)}/${iso.slice(2, 4)}` : '');

export const STATUS = {
  overdue: { label: 'Due', color: 'var(--due)' },
  soon: { label: 'Soon', color: 'var(--soon)' },
  ok: { label: 'OK', color: 'var(--ok)' },
};

export const pill = (text, color = 'var(--primary)') => `<span class="pill" style="--c:${color}">${esc(text)}</span>`;
export const bar = (progress, color) => `<div class="bar" style="--c:${color}"><i style="width:${Math.min(100, Math.max(0, progress * 100)).toFixed(1)}%"></i></div>`;
export const badge = (name, tint = 'var(--primary)', cls = '') => `<div class="badge ${cls}" style="--tint:${tint}">${icon(name)}</div>`;

export function dueText(kmLeft, daysLeft) {
  const parts = [];
  if (kmLeft != null) parts.push(kmLeft <= 0 ? `${fmtKm(-kmLeft)} overdue` : `in ${fmtKm(kmLeft)}`);
  if (daysLeft != null) {
    if (daysLeft < 0) parts.push(`${-daysLeft} d overdue`);
    else if (daysLeft === 0) parts.push('today');
    else if (daysLeft < 14) parts.push(`in ${daysLeft} d`);
    else if (daysLeft < 60) parts.push(`in ${Math.floor(daysLeft / 7)} wk`);
    else parts.push(`in ${Math.floor(daysLeft / 30)} mo`);
  }
  return parts.join(' · ') || 'Not logged yet — do it once to start the clock';
}

export const topbar = (title, back = true, actions = '') => `
  <div class="topbar">
    ${back ? `<button class="iconbtn" data-back aria-label="Back">${icon('arrow-left')}</button>` : ''}
    <h1 class="grow">${esc(title)}</h1>${actions}
  </div>`;

export const section = (title, action = '') => `<div class="section"><h2>${esc(title)}</h2>${action}</div>`;

export const navRow = (href, iconName, title, sub = '', tint) => `
  <a class="card navrow" href="${href}">
    ${badge(iconName, tint)}
    <div class="grow"><h3>${esc(title)}</h3>${sub ? `<p class="small muted">${esc(sub)}</p>` : ''}</div>
    <span class="chev">${icon('chevron-right')}</span>
  </a>`;

export const callout = (title, text, color = 'var(--primary)') =>
  `<div class="callout" style="--c:${color}"><b>${esc(title)}</b><p>${esc(text)}</p></div>`;

export const stat = (value, label) => `<div class="card stat"><div class="v">${esc(value)}</div><div class="l">${esc(label)}</div></div>`;

export const empty = (iconName, title, text) => `
  <div class="empty">${badge(iconName, 'var(--primary)', 'lg')}<h2>${esc(title)}</h2><p class="muted small" style="margin-top:4px">${esc(text)}</p></div>`;

export function ring(score) {
  const c = score >= 80 ? 'var(--ok)' : score >= 50 ? 'var(--soon)' : 'var(--due)';
  const r = 32;
  const len = 2 * Math.PI * r;
  return `<div class="ring" style="--c:${c}" role="img" aria-label="Health ${score} of 100">
    <svg viewBox="0 0 76 76"><circle cx="38" cy="38" r="${r}" fill="none" stroke="var(--surface-2)" stroke-width="8"/>
    <circle cx="38" cy="38" r="${r}" fill="none" stroke="${c}" stroke-width="8" stroke-linecap="round" stroke-dasharray="${((len * score) / 100).toFixed(1)} ${len.toFixed(1)}"/></svg>
    <div class="lbl"><b>${score}</b><span>health</span></div></div>`;
}

export const docDays = (today, expiry) => (expiry ? daysBetween(today, expiry) : null);

let toastTimer;
export function toast(msg) {
  document.querySelector('.toast')?.remove();
  const el = document.createElement('div');
  el.className = 'toast';
  el.setAttribute('role', 'status');
  el.textContent = msg;
  document.body.append(el);
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => el.remove(), 3500);
}

/** Opens a <dialog> with the given inner HTML; resolves with the clicked button's value. */
export function dialog(html, onMount) {
  return new Promise((resolve) => {
    const d = document.createElement('dialog');
    d.innerHTML = `<form method="dialog">${html}</form>`;
    document.body.append(d);
    onMount?.(d);
    d.addEventListener('close', () => {
      resolve({ value: d.returnValue, form: d.querySelector('form') });
      d.remove();
    });
    d.showModal();
  });
}

export const confirmDialog = async (title, text, ok = 'Confirm', danger = false) =>
  (await dialog(`<h2>${esc(title)}</h2><p class="muted">${esc(text)}</p>
    <div class="btnrow" style="margin-top:16px"><button class="btn tonal" value="cancel">Cancel</button>
    <button class="btn ${danger ? 'danger' : ''}" value="ok">${esc(ok)}</button></div>`)).value === 'ok';

export const SCOOTER_SVG = `<svg class="art" viewBox="6 0 214 140" role="img" aria-label="Kymco Agility scooter">
  <g fill="var(--primary)">
    <path d="M16 90 C16 72 32 62 54 60 L122 58 L128 72 L124 96 L90 96 C86 84 76 78 61 78 C46 78 36 86 33 96 L20 96 C17 95 16 93 16 90 Z"/>
    <path d="M120 96 L123 88 L146 88 C152 70 156 50 158 36 L161 24 L172 22 C175 36 179 52 187 67 C196 77 207 85 208 95 C208 98 206 100 204 98 C200 91 194 88 186 88 C176 87 167 93 164 101 L148 101 L144 96 Z"/>
  </g>
  <path fill="#2a2d35" d="M46 60 C48 48 62 43 84 43 L116 45 C125 46 127 55 120 58 L50 61 Z"/>
  <path fill="none" stroke="#c9ccd3" stroke-width="3" stroke-linecap="round" d="M148 24 L178 15 M163 19 L158 7"/>
  <ellipse cx="156" cy="5" rx="6" ry="3" fill="#c9ccd3"/>
  <path fill="#fff4c2" d="M171 22 C178 22 185 27 185 33 L180 37 C176 33 172 31 170 30 Z"/>
  <rect x="13" y="82" width="6" height="6" rx="2" fill="#ff8a80"/>
  <path fill="#3a3e48" d="M66 106 L112 102 C116 102 118 105 118 108 L117 111 L68 114 C64 114 62 108 66 106 Z"/>
  <g fill="none" stroke="#1d1f25" stroke-width="9"><circle cx="61" cy="116" r="20"/><circle cx="186" cy="116" r="20"/></g>
  <g fill="none" stroke="#9aa0ab" stroke-width="3"><circle cx="61" cy="116" r="11"/><circle cx="186" cy="116" r="11"/></g>
  <g fill="#9aa0ab"><circle cx="61" cy="116" r="3.5"/><circle cx="186" cy="116" r="3.5"/></g>
</svg>`;
