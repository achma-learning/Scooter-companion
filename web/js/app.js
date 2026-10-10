// Agility Companion — hash router + boot. Views are pure functions returning
// { html, mount? }; the store notifies us to re-render after every change.
import * as S from './store.js';
import { snapshot } from './planner.js';
import { icon, esc } from './ui.js';
import * as G from './views-garage.js';
import * as K from './views-guide.js';
import * as M from './views-manual.js';

const TABS = [
  ['#/', 'bike', 'Garage'],
  ['#/service', 'wrench', 'Service'],
  ['#/manual', 'book-open', 'Manual'],
  ['#/guide', 'compass', 'Guide'],
  ['#/more', 'ellipsis', 'More'],
];

// [pattern, view, tab it belongs to]
const ROUTES = [
  [/^$/, G.garage, 0], [/^service$/, G.service, 1], [/^task\/([\w-]+)$/, G.task, 1], [/^log$/, G.logService, 1],
  [/^passport$/, G.passport, 4], [/^fuel$/, G.fuel, 4], [/^wallet$/, G.wallet, 4], [/^profile$/, G.profile, 4],
  [/^more$/, G.more, 4], [/^sos$/, G.sos, 4], [/^settings$/, G.settings, 4],
  [/^manual$/, M.manual, 2], [/^search$/, M.search, 2], [/^contents$/, M.contents, 2], [/^bookmarks$/, M.bookmarks, 2],
  [/^reader\/(\w+)\/(\d+)$/, M.reader, 2],
  [/^guide$/, K.guideHub, 3], [/^checklist$/, K.checklist, 3], [/^trouble$/, K.troubleList, 3], [/^trouble\/([\w-]+)$/, K.troubleTree, 3],
  [/^issues$/, K.issues, 3], [/^howto$/, K.howtoList, 3], [/^howto\/([\w-]+)$/, K.howto, 3], [/^specs$/, K.specs, 3],
  [/^oils$/, K.oils, 3], [/^seasons$/, K.seasons, 3], [/^parts$/, K.parts, 3], [/^gear$/, K.gear, 3],
];

let content;
const view = document.getElementById('view');
const rail = document.getElementById('rail');

function parse() {
  const raw = location.hash.replace(/^#\/?/, '');
  const [path, query = ''] = raw.split('?');
  return { path: decodeURIComponent(path), params: new URLSearchParams(query) };
}

let lastPath = null;
function render() {
  const { path, params } = parse();
  const route = ROUTES.find(([re]) => re.test(path));
  if (!route) {
    location.replace('#/');
    return;
  }
  const [re, fn, tab] = route;
  const args = path.match(re).slice(1);
  const state = S.getState();
  const ctx = { content, state, snap: snapshot(content, state) };
  let out;
  try {
    out = fn(ctx, params, ...args);
  } catch (e) {
    console.error(e);
    out = { html: `<div class="card"><h2>Something went wrong</h2><p class="small muted">${esc(e.message)}</p></div>` };
  }
  rail.querySelectorAll('a').forEach((a, i) => a.classList.toggle('active', i === tab));
  rail.style.display = out.fullscreen ? 'none' : '';
  const scroll = path === lastPath ? window.scrollY : 0;
  view.innerHTML = out.html;
  view.querySelectorAll('[data-back]').forEach((b) => (b.onclick = () => (history.length > 1 ? history.back() : (location.hash = TABS[tab][0]))));
  out.mount?.(view);
  window.scrollTo(0, scroll);
  if (path !== lastPath) view.focus({ preventScroll: true });
  lastPath = path;
}

async function boot() {
  rail.insertAdjacentHTML('beforeend', TABS.map(([href, ic, label]) =>
    `<a href="${href}"><span class="pillicon">${icon(ic)}</span><span>${label}</span></a>`).join(''));
  try {
    content = await (await fetch('data/agility50.json')).json();
  } catch (e) {
    view.innerHTML = '<div class="card"><h2>Could not load data</h2><p class="small muted">Check your connection and reload.</p></div>';
    return;
  }
  window.addEventListener('hashchange', render);
  // Re-render in place after store changes. While a dialog is open, defer until it closes —
  // and only if something changed, so a cancelled confirm never wipes a half-filled form.
  let pending = false;
  S.subscribe(() => {
    if (document.querySelector('dialog[open]')) pending = true;
    else render();
  });
  document.addEventListener('close', () => {
    if (!pending) return;
    pending = false;
    setTimeout(render);
  }, true);
  render();

  if ('serviceWorker' in navigator && location.protocol === 'https:') {
    navigator.serviceWorker.register('sw.js').catch(() => {});
  }
}

boot();
