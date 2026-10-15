// reload-test.mjs — after a dialog closes, CMP's semantics DOM is dead
// (2 nodes, never rebuilds: confirmed no pulse/mousemove/click revives it).
// Does a page reload (wasm is browser-cached) restore a fresh tree, and how
// long does it take? That decides whether browser E2E can do
// "one dialog interaction per page load".
import { chromium } from 'playwright';
import { CHROME_ARGS } from './semantics.mjs';

const browser = await chromium.launch({ executablePath: '/usr/local/bin/chromium', headless: true, args: CHROME_ARGS });
const page = await (await browser.newContext({ viewport: { width: 1280, height: 900 } })).newPage();

const roles = () => page.evaluate(() => {
  let n = 0;
  const visit = (r) => { for (const el of r.querySelectorAll('*')) { if (el.getAttribute('role') || el.getAttribute('aria-label')) n++; if (el.shadowRoot) visit(el.shadowRoot); } };
  visit(document.body);
  return n;
});
const waitRoles = async (min, capMs) => {
  const t0 = Date.now();
  while (Date.now() - t0 < capMs) {
    if ((await roles()) >= min) return Date.now() - t0;
    await page.waitForTimeout(400);
  }
  return -1;
};
// The nav renders slightly after the first semantics nodes; 'Settings' is the
// readiness anchor used by the harness.
const waitSettings = async (capMs) => {
  const t0 = Date.now();
  while (Date.now() - t0 < capMs) {
    const found = await page.evaluate(() => {
      let hit = false;
      const visit = (r) => { for (const el of r.querySelectorAll('*')) { if (el.getAttribute('aria-label') === 'Settings') hit = true; if (el.shadowRoot) visit(el.shadowRoot); } };
      visit(document.body); return hit;
    });
    if (found) return Date.now() - t0;
    await page.waitForTimeout(300);
  }
  return -1;
};

try {
  console.log('── cold load ──');
  const t0 = Date.now();
  await page.goto('http://localhost:12001', { waitUntil: 'load', timeout: 60000 });
  const cold = await waitSettings(60000);
  console.log(`cold: Settings nav present after ${cold} ms (goto+wait ${Date.now() - t0} ms); roles=${await roles()}`);

  // open + cancel a dialog to kill the tree
  const nav = await page.evaluate(() => {
    const out = [];
    const visit = (r) => { for (const el of r.querySelectorAll('*')) { if (el.getAttribute('aria-label') === 'Settings') { const b = el.getBoundingClientRect(); out.push({ x: b.x + b.width / 2, y: b.y + b.height / 2 }); } if (el.shadowRoot) visit(el.shadowRoot); } };
    visit(document.body); return out.sort((a, b) => b.y - a.y)[0];
  });
  await page.mouse.click(nav.x, nav.y);
  await page.waitForTimeout(2500);
  const add = await page.evaluate(() => {
    const out = [];
    const visit = (r) => { for (const el of r.querySelectorAll('*')) { if (el.getAttribute('aria-label') === 'Add secret') { const b = el.getBoundingClientRect(); out.push({ x: b.x + b.width / 2, y: b.y + b.height / 2 }); } if (el.shadowRoot) visit(el.shadowRoot); } };
    visit(document.body); return out[0];
  });
  await page.mouse.click(add.x, add.y);
  await page.waitForTimeout(1500);
  const cancel = await page.evaluate(() => {
    const out = [];
    const visit = (r) => { for (const el of r.querySelectorAll('*')) { const t = (el.textContent || '').trim(); if (t === 'Cancel' && el.getAttribute('role') === 'button') { const b = el.getBoundingClientRect(); out.push({ x: b.x + b.width / 2, y: b.y + b.height / 2 }); } if (el.shadowRoot) visit(el.shadowRoot); } };
    visit(document.body); return out[0];
  });
  await page.mouse.click(cancel.x, cancel.y);
  await page.waitForTimeout(1500);
  console.log(`after dialog close: roles=${await roles()}  (dead tree if ~2)`);

  console.log('── warm reload ──');
  const t1 = Date.now();
  await page.reload({ waitUntil: 'load', timeout: 60000 });
  const warm = await waitSettings(60000);
  console.log(`reload: Settings nav present after ${warm} ms (total ${Date.now() - t1} ms); roles=${await roles()}`);
  await page.screenshot({ path: '/tmp/opencode/ux-test/shots-v3/reload-after.png' });
} catch (e) {
  console.log('EXCEPTION: ' + e.message);
} finally {
  await browser.close();
}
