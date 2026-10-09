// The two official documents: hub, full-text search over the OCR index, contents,
// bookmarks and a pdf.js reader (loaded from cdnjs only when a document is opened).
import { esc, icon, topbar, section, navRow, empty, toast } from './ui.js';

const PDFJS = 'https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/';
const LIB_KEY = 'agility-companion:library';
const docUrl = (file) => `docs/${file}`;

function lib() {
  try {
    return { bookmarks: [], recent: [], ...JSON.parse(localStorage.getItem(LIB_KEY) || '{}') };
  } catch (e) {
    return { bookmarks: [], recent: [] };
  }
}
function saveLib(l) {
  try {
    localStorage.setItem(LIB_KEY, JSON.stringify(l));
  } catch (e) {
    /* ignore */
  }
}

const cache = {};
const fetchJson = (path) => (cache[path] ||= fetch(path).then((r) => r.json()));

export function manual({ content: c }) {
  const l = lib();
  const last = (id) => l.recent.find((r) => r.doc === id)?.page;
  const html = `
    ${topbar('Documents', false)}
    <p class="muted">The paperwork that came with the scooter — cached for offline reading once opened.</p>
    <div class="list" style="margin-top:12px">${c.docs.map((d) => `
      <div class="card doc">
        <img src="docs/${d.cover}" alt="" loading="lazy">
        <div class="grow">
          <a href="#/reader/${d.id}/${last(d.id) || 1}"><h2 style="color:var(--text)">${esc(d.title)}</h2></a>
          <p class="small muted">${esc(d.subtitle)}</p>
          <a class="small bold" href="#/reader/${d.id}/${last(d.id) || 1}" style="display:block;margin:6px 0">${last(d.id) ? `Continue at page ${last(d.id)}` : 'Open from page 1'}</a>
          <div class="chips">${d.highlights.map((h) => `<a class="chip" href="#/reader/${d.id}/${h.page}">${esc(h.label)} · p.${h.page}</a>`).join('')}</div>
        </div>
      </div>`).join('')}</div>
    ${section('Find')}
    <div class="list">
      ${navRow('#/search', 'search', 'Search both documents', 'Full text — French / Arabic / English OCR')}
      ${navRow('#/contents', 'list', 'Table of contents', 'Jump to any chapter')}
      ${navRow('#/bookmarks', 'bookmark', 'Bookmarks', `${l.bookmarks.length} saved page${l.bookmarks.length === 1 ? '' : 's'}`)}
    </div>`;
  return { html };
}

export function search({ content: c }, params) {
  const q = params.get('q') || '';
  const html = `
    ${topbar('Search')}
    <form id="f"><input type="search" name="q" value="${esc(q)}" placeholder="e.g. huile, pression, fuse, garantie" autofocus></form>
    <div id="res" class="list" style="margin-top:12px"></div>`;
  const mount = async (root) => {
    const f = root.querySelector('#f');
    f.onsubmit = (e) => {
      e.preventDefault();
      history.replaceState(null, '', `#/search?q=${encodeURIComponent(f.q.value.trim())}`);
      run(f.q.value.trim());
    };
    let timer;
    f.q.oninput = () => {
      clearTimeout(timer);
      timer = setTimeout(() => run(f.q.value.trim()), 250);
    };
    const res = root.querySelector('#res');
    async function run(term) {
      if (term.length < 2) return (res.innerHTML = '');
      const lower = term.toLowerCase();
      const hits = [];
      for (const d of c.docs) {
        const idx = await fetchJson(docUrl(d.index));
        for (const p of idx.pages) {
          const i = p.text.toLowerCase().indexOf(lower);
          if (i < 0) continue;
          const start = Math.max(0, i - 50);
          const snip = p.text.slice(start, i + term.length + 70).replace(/\s+/g, ' ');
          const at = snip.toLowerCase().indexOf(lower);
          hits.push(`<a class="card hit" href="#/reader/${d.id}/${p.page}"><p class="small bold" style="color:var(--primary)">${esc(d.title)} · page ${p.page}</p>
            <p class="small">${start ? '…' : ''}${esc(snip.slice(0, at))}<mark>${esc(snip.slice(at, at + term.length))}</mark>${esc(snip.slice(at + term.length))}…</p></a>`);
        }
      }
      res.innerHTML = hits.length ? hits.join('') : empty('search', 'No match', 'The scans are OCR text — try a shorter or French word.');
    }
    if (q) run(q);
  };
  return { html, mount };
}

export function contents({ content: c }) {
  const html = `${topbar('Table of contents')}<div id="toc"></div>`;
  const mount = async (root) => {
    const toc = await fetchJson('docs/toc.json');
    const item = (doc, it, depth) => `<a class="card navrow" href="#/reader/${doc}/${it.page}" style="margin-left:${depth * 16}px;padding:10px 14px">
      <div class="grow">${depth ? esc(it.title) : `<b>${esc(it.title)}</b>`}</div><span class="small muted">p.${it.page}</span></a>
      ${(it.children || []).map((ch) => item(doc, ch, depth + 1)).join('')}`;
    root.querySelector('#toc').innerHTML = c.docs.map((d) => section(d.title) + `<div class="list">${(toc[d.id === 'MANUAL' ? 'manual' : 'passport'] || []).map((it) => item(d.id, it, 0)).join('')}</div>`).join('');
  };
  return { html, mount };
}

export function bookmarks({ content: c }) {
  const l = lib();
  const title = (id) => c.docs.find((d) => d.id === id)?.title || id;
  const html = `
    ${topbar('Bookmarks')}
    ${l.bookmarks.length ? `<div class="list">${l.bookmarks.map((b) => navRow(`#/reader/${b.doc}/${b.page}`, 'bookmark', `${title(b.doc)} — page ${b.page}`)).join('')}</div>`
      : empty('bookmark', 'No bookmarks yet', 'Tap the bookmark icon while reading to save a page.')}`;
  return { html };
}

let pdfjsReady;
function loadPdfJs() {
  pdfjsReady ||= new Promise((resolve, reject) => {
    const s = document.createElement('script');
    s.src = `${PDFJS}pdf.min.js`;
    s.onload = () => {
      window.pdfjsLib.GlobalWorkerOptions.workerSrc = `${PDFJS}pdf.worker.min.js`;
      resolve(window.pdfjsLib);
    };
    s.onerror = () => {
      pdfjsReady = null;
      reject(new Error('PDF viewer could not load (offline?)'));
    };
    document.head.append(s);
  });
  return pdfjsReady;
}

export function reader({ content: c }, params, docId, pageStr) {
  const d = c.docs.find((x) => x.id === docId);
  if (!d) return { html: topbar('Not found') };
  const start = Math.max(1, Math.min(d.pages, parseInt(pageStr, 10) || 1));
  const marked = () => lib().bookmarks.some((b) => b.doc === docId && b.page === current);
  let current = start;
  const html = `
    <div class="reader">
      <header>
        <button class="iconbtn" data-back aria-label="Back">${icon('arrow-left')}</button>
        <div class="grow"><b>${esc(d.title)}</b> <span class="small muted" id="pg">${start} / ${d.pages}</span></div>
        <button class="iconbtn" id="bm" aria-label="Bookmark page">${icon('bookmark')}</button>
        <a class="iconbtn" href="${docUrl(d.file)}" download aria-label="Download PDF">${icon('download')}</a>
      </header>
      <div class="pages" id="pages">${Array.from({ length: d.pages }, (_, i) => `<div class="page" data-p="${i + 1}" style="aspect-ratio:${d.id === 'MANUAL' ? '0.7' : '0.71'}"><span class="no">${i + 1}</span></div>`).join('')}</div>
    </div>`;
  const mount = async (root) => {
    const pagesEl = root.querySelector('#pages');
    const bm = root.querySelector('#bm');
    const paintBm = () => (bm.style.color = marked() ? 'var(--primary)' : '');
    const setPage = (p) => {
      current = p;
      root.querySelector('#pg').textContent = `${p} / ${d.pages}`;
      paintBm();
      const lib2 = lib();
      lib2.recent = [{ doc: docId, page: p }, ...lib2.recent.filter((r) => r.doc !== docId)].slice(0, 6);
      saveLib(lib2);
      history.replaceState(null, '', `#/reader/${docId}/${p}`);
    };
    bm.onclick = () => {
      const lib2 = lib();
      if (marked()) lib2.bookmarks = lib2.bookmarks.filter((b) => !(b.doc === docId && b.page === current));
      else lib2.bookmarks.push({ doc: docId, page: current });
      saveLib(lib2);
      paintBm();
      toast(marked() ? `Page ${current} bookmarked` : 'Bookmark removed');
    };
    paintBm();
    const target = pagesEl.querySelector(`[data-p="${start}"]`);
    requestAnimationFrame(() => target.scrollIntoView());

    let pdf;
    try {
      const pdfjs = await loadPdfJs();
      pdf = await pdfjs.getDocument(docUrl(d.file)).promise;
    } catch (e) {
      pagesEl.innerHTML = `<div class="card" style="max-width:420px">${esc(e.message)}. <a href="${docUrl(d.file)}">Open the PDF directly</a>.</div>`;
      return;
    }
    const rendered = new Set();
    const render = async (el) => {
      const n = +el.dataset.p;
      if (rendered.has(n)) return;
      rendered.add(n);
      const page = await pdf.getPage(n);
      const vp1 = page.getViewport({ scale: 1 });
      const scale = (el.clientWidth * Math.min(2, window.devicePixelRatio || 1)) / vp1.width;
      const vp = page.getViewport({ scale });
      const canvas = document.createElement('canvas');
      canvas.width = vp.width;
      canvas.height = vp.height;
      el.style.aspectRatio = `${vp1.width} / ${vp1.height}`;
      el.prepend(canvas);
      await page.render({ canvasContext: canvas.getContext('2d'), viewport: vp }).promise;
    };
    const io = new IntersectionObserver((entries) => entries.forEach((e) => e.isIntersecting && render(e.target)), { root: pagesEl, rootMargin: '600px 0px' });
    const pager = new IntersectionObserver((entries) => {
      const vis = entries.filter((e) => e.isIntersecting).sort((a, b) => b.intersectionRatio - a.intersectionRatio)[0];
      if (vis && +vis.target.dataset.p !== current) setPage(+vis.target.dataset.p);
    }, { root: pagesEl, threshold: [0.5] });
    pagesEl.querySelectorAll('.page').forEach((el) => {
      io.observe(el);
      pager.observe(el);
    });
    setPage(start);
    target.scrollIntoView();
  };
  return { html, mount, fullscreen: true };
}
