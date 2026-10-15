// semantics.mjs — shadow-piercing access to the Compose-on-wasm semantics DOM.
//
// Why this exists: the app renders to a canvas, BUT Compose Multiplatform
// publishes a real semantics tree as DOM elements inside a SHADOW ROOT
// (role/aria-label + exact bounding boxes). document.querySelectorAll() does
// not pierce shadow roots, which is why plain DOM queries see nothing.
//
// Model:
//   boot()            → launch a browser, open the app, wait for wasm boot
//   collect(page)     → [{kind:'element'|'text', role, name, x, y, w, h}] with rects
//   byName(page, ...) → find candidates by name (exact/contains, optional role filter)
//   clickName(page,..)→ real mouse click at the element's rect center
//   typeInto(page, el, text) → click the field, type, verify (fallback insertText)
//   waitFor(page, pred, ms) → poll collect() until pred matches (returns the hit)
//
// Refs: role elements carry aria-label OR their (possibly concatenated) text;
// plain text lives in leaf elements (kind:'text'). Buttons/fields are reliable
// by name; rows bind by y-overlap.
import { chromium } from 'playwright';

export const CHROME_ARGS = ['--no-sandbox', '--use-gl=angle', '--use-angle=swiftshader',
  '--enable-unsafe-swiftshader', '--disable-dev-shm-usage'];

export async function boot({ url = 'http://localhost:12001', width = 1280, height = 900, bootWaitMs = 25000 } = {}) {
  const browser = await chromium.launch({ executablePath: '/usr/local/bin/chromium', headless: true, args: CHROME_ARGS });
  const context = await browser.newContext({ viewport: { width, height } });
  const page = await context.newPage();
  const origin = new URL(url).origin;
  const state = { errors: [], net: [], resp: [], screenshots: 0 };
  page.on('console', (m) => { if (m.type() === 'error' && !m.text().includes('404')) state.errors.push(m.text().slice(0, 200)); });
  page.on('pageerror', (e) => state.errors.push('PAGEERROR: ' + e.message.slice(0, 200)));
  page.on('request', (r) => { if (r.url().includes('/cloud/')) state.net.push(`${r.method()} ${r.url().replace(origin + '/', '')}`); });
  page.on('response', (r) => { const u = r.url(); if (u.includes('/cloud/')) state.resp.push(`${r.status()} ${r.request().method()} ${u.replace(origin + '/', '')}`); });
  await page.goto(url, { waitUntil: 'load', timeout: 60000 });
  await page.waitForTimeout(bootWaitMs);
  // Insurance: if the semantics DOM is not present, nudge via CDP a11y once.
  if ((await collect(page)).filter((e) => e.role).length === 0) {
    try {
      const cdp = await context.newCDPSession(page);
      await cdp.send('Accessibility.enable');
      await cdp.send('Accessibility.getFullAXTree');
      await page.waitForTimeout(1200);
    } catch { /* best effort */ }
  }
  return { browser, context, page, state, origin };
}

const COLLECT_FN = () => {
  const out = [];
  const visit = (root) => {
    for (const el of root.querySelectorAll('*')) {
      const r = el.getBoundingClientRect();
      if (r.width > 0 && r.height > 0) {
        const role = el.getAttribute('role');
        const aria = el.getAttribute('aria-label');
        const text = (el.textContent || '').trim();
        if (role || aria) {
          const name = (aria || (text.length <= 160 ? text : '')).replace(/\s+/g, ' ');
          out.push({ kind: 'element', role: role || '', name: name.slice(0, 140), x: r.x + r.width / 2, y: r.y + r.height / 2, w: r.width, h: r.height, y0: r.y, y1: r.y + r.height });
        } else if (el.childElementCount === 0 && text) {
          out.push({ kind: 'text', role: '', name: text.replace(/\s+/g, ' ').slice(0, 140), x: r.x + r.width / 2, y: r.y + r.height / 2, w: r.width, h: r.height, y0: r.y, y1: r.y + r.height });
        }
      }
      if (el.shadowRoot) visit(el.shadowRoot);
    }
  };
  visit(document.body);
  return out;
};

export async function collect(page) { return page.evaluate(COLLECT_FN); }

export function byName(elems, name, { exact = true, role = null, minW = 0, maxW = 1e9, minH = 0, maxH = 1e9 } = {}) {
  const lc = name.toLowerCase();
  return elems.filter((e) => {
    const n = e.name.toLowerCase();
    const nameOk = exact ? n === lc : n.includes(lc);
    const roleOk = role ? e.role === role : true;
    return nameOk && roleOk && e.w >= minW && e.w <= maxW && e.h >= minH && e.h <= maxH;
  });
}

export async function clickName(page, name, opts = {}) {
  const els = await collect(page);
  const hits = byName(els, name, opts);
  if (!hits.length) return { ok: false, hits: [], all: els };
  const hit = hits.sort((a, b) => b.w * b.h - a.w * a.h)[0]; // prefer the largest target
  await page.mouse.move(hit.x, hit.y);
  await page.waitForTimeout(120);
  await page.mouse.click(hit.x, hit.y);
  return { ok: true, hit, hits };
}

export async function typeInto(page, field, text) {
  await page.mouse.click(field.x, field.y);
  await page.waitForTimeout(350);
  await page.keyboard.type(text, { delay: 25 });
  await page.waitForTimeout(350);
  let els = await collect(page);
  let filled = byName(els, text, { exact: false }).length > 0;
  if (!filled) {
    // fallback: single input event
    await page.keyboard.press('Control+A');
    await page.keyboard.press('Backspace');
    await page.keyboard.insertText(text);
    await page.waitForTimeout(400);
    els = await collect(page);
    filled = byName(els, text, { exact: false }).length > 0;
  }
  return { filled, els };
}

export async function waitFor(page, pred, timeoutMs = 8000, stepMs = 350) {
  const t0 = Date.now();
  let els = [];
  while (Date.now() - t0 < timeoutMs) {
    els = await collect(page);
    const hit = pred(els);
    if (hit) return { ok: true, hit, els };
    await page.waitForTimeout(stepMs);
  }
  return { ok: false, hit: null, els };
}

export function joinTexts(els) { return els.map((e) => e.name).join(' | '); }
