// nudge-test2.mjs — characterise the semantics tree across a dialog transition:
// dump node names at each stage, screenshot the visual truth, and pulse the CDP
// accessibility client unconditionally to see if it forces a rebuild.
import { boot, collect, byName, waitFor } from './semantics.mjs';

const { browser, page, context } = await boot({ url: 'http://localhost:12001' });

const rawNamed = async () => page.evaluate(() => {
  const out = [];
  const visit = (r) => {
    for (const el of r.querySelectorAll('*')) {
      const role = el.getAttribute('role');
      const aria = el.getAttribute('aria-label');
      if (role || aria) out.push(`${role || '-'}:${(aria || (el.childElementCount === 0 ? (el.textContent || '').trim() : '')).slice(0, 26)}`);
      if (el.shadowRoot) visit(el.shadowRoot);
    }
  };
  visit(document.body);
  return out;
});
const show = (label, names) => console.log(`  [${label}] ${names.length} nodes: ${names.join(' | ').slice(0, 300)}`);

try {
  show('boot', await rawNamed());

  const els = await collect(page);
  const nav = byName(els, 'Settings', { role: 'button' }).sort((a, b) => b.w - a.w)[0];
  await page.mouse.click(nav.x, nav.y);
  await waitFor(page, (e) => byName(e, 'Add secret', { role: 'button' })[0], 8000);
  show('settings', await rawNamed());

  const add = (await collect(page)).find((e) => e.role === 'button' && e.name === 'Add secret');
  await page.mouse.click(add.x, add.y);
  await page.waitForTimeout(1500);
  show('dialog-open', await rawNamed());

  const cancel = (await collect(page)).find((e) => e.role === 'button' && e.name === 'Cancel');
  await page.mouse.click(cancel.x, cancel.y);
  await page.waitForTimeout(1200);
  await page.screenshot({ path: '/tmp/opencode/ux-test/shots-v3/nudge-after-cancel.png' });
  show('after-cancel (visual checked in screenshot)', await rawNamed());

  const cdp = await context.newCDPSession(page);
  await cdp.send('Accessibility.enable').catch(() => {});
  await cdp.send('Accessibility.getFullAXTree').catch(() => {});
  await page.waitForTimeout(800);
  show('after-pulse', await rawNamed());

  // does a plain mouse move / click on empty canvas force a refresh?
  await page.mouse.move(640, 100);
  await page.waitForTimeout(600);
  show('after-mousemove', await rawNamed());

  await page.mouse.click(640, 740);   // click inside the Secrets card area
  await page.waitForTimeout(900);
  show('after-canvas-click', await rawNamed());
} catch (e) {
  console.log('EXCEPTION: ' + e.message);
} finally {
  await browser.close();
}
