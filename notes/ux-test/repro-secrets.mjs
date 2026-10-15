// repro-secrets.mjs — verbose reproduction of the "row not found after Add"
// step. Logs the tree state every 700ms after clicking Add, plus field
// details before the click. Uses a probe name so it never collides with the
// main test's OHAAGUITEST.
import { boot, collect, byName, clickName, typeInto, waitFor } from './semantics.mjs';

const PNAME = 'OHAAGUIPROBE';
const PVAL = 'probe-value-9';
const { browser, page, state } = await boot({ url: 'http://localhost:12001' });

const cut = (s, n = 60) => s.replace(/\s+/g, ' ').slice(0, n);
const dumpFields = (els, label) => {
  const f = els.filter((x) => x.role === 'textbox' && x.w < 700 && x.y0 > 300 && x.y1 < 700).sort((a, b) => a.y0 - b.y0);
  console.log(`  [${label}] fields:`, f.map((x) => `y${Math.round(x.y0)} "${cut(x.name, 40)}"`).join(' | ') || 'none');
};
const dumpProbe = (els, label) => {
  const hits = els.filter((x) => /PROBE/i.test(x.name));
  console.log(`  [${label}] PROBE nodes:`, hits.map((x) => `${x.kind}/${x.role || '-'}=${cut(x.name, 44)}@y${Math.round(x.y0)}`).join(' | ') || 'none');
};

try {
  // settings
  let els = await collect(page);
  const nav = byName(els, 'Settings', { role: 'button' }).sort((a, b) => b.w - a.w)[0];
  await page.mouse.click(nav.x, nav.y);
  const st = await waitFor(page, (e) => byName(e, 'Add secret', { role: 'button' })[0], 8000);
  if (!st.ok) throw new Error('not on settings');

  // open dialog
  await clickName(page, 'Add secret', { role: 'button' });
  const dlg = await waitFor(page, (e) => {
    const f = e.filter((x) => x.role === 'textbox' && x.w < 700 && x.y0 > 300 && x.y1 < 700).sort((a, b) => a.y0 - b.y0);
    return f.length >= 3 ? f : null;
  }, 8000);
  if (!dlg.ok) throw new Error('dialog missing');
  console.log('== dialog open ==');
  dumpFields(dlg.hit, 'initial');

  // type the probe name into field 0
  const r1 = await typeInto(page, dlg.hit[0], PNAME);
  console.log(`typeInto(name) filled=${r1.filled}`);
  dumpFields(await collect(page), 'after name');

  // type value into the field that is currently 2nd in the dialog band
  const f2 = (await collect(page)).filter((x) => x.role === 'textbox' && x.w < 700 && x.y0 > 300 && x.y1 < 700).sort((a, b) => a.y0 - b.y0)[1];
  const r2 = await typeInto(page, f2, PVAL);
  console.log(`typeInto(value) filled=${r2.filled}`);
  const afterVal = await collect(page);
  dumpFields(afterVal, 'after value');
  dumpProbe(afterVal, 'after value');
  // any node mentioning the validator text?
  console.log('  hint nodes:', afterVal.filter((x) => /Letters, digits/.test(x.name)).map((x) => `"${cut(x.name)}"@y${Math.round(x.y0)}`).join(' | '));
  await page.screenshot({ path: '/tmp/opencode/ux-test/shots-v3/repro-filled.png' });

  // click Add
  const before = state.resp.length;
  await clickName(page, 'Add', { role: 'button', exact: true });
  console.log('== Add clicked; polling ==');
  for (let i = 0; i < 16; i++) {
    await page.waitForTimeout(700);
    const e = await collect(page);
    const dialogAdd = byName(e, 'Add', { role: 'button', exact: true }).length;
    const dialogCancel = byName(e, 'Cancel', { role: 'button', exact: true }).length;
    const probeNodes = e.filter((x) => /PROBE/i.test(x.name));
    const tail = state.resp.slice(before).join(' ; ') || '-';
    console.log(`t=${(i + 1) * 700}: nodes=${e.length} dlgAdd=${dialogAdd} dlgCancel=${dialogCancel} probe=${probeNodes.map((x) => x.kind + '@y' + Math.round(x.y0)).join(',') || 'none'} resp=${cut(tail, 80)}`);
  }
  await page.screenshot({ path: '/tmp/opencode/ux-test/shots-v3/repro-after.png' });

  // cleanup: delete OHAAGUIPROBE if it exists
  els = await collect(page);
  const row = els.find((x) => x.kind === 'text' && x.name === PNAME);
  if (row) {
    const del = byName(els, 'Delete secret', { role: 'button' }).sort((a, b) => Math.abs(a.y - row.y) - Math.abs(b.y - row.y))[0];
    await page.mouse.click(del.x, del.y);
    const cdlg = await waitFor(page, (e) => byName(e, 'Delete', { role: 'button', exact: true })[0], 6000);
    if (cdlg.ok) {
      const body = (await collect(page)).filter((x) => x.y0 > 250 && x.y1 < 700).map((x) => x.name).join(' ');
      if (body.includes(PNAME)) {
        await page.mouse.click(cdlg.hit.x, cdlg.hit.y);
        const gone = await waitFor(page, (e) => !e.find((x) => x.kind === 'text' && x.name === PNAME), 8000);
        console.log('cleanup probe: ' + (gone.ok ? 'removed' : 'STILL PRESENT'));
      } else {
        console.log('cleanup probe: dialog did not name it — left in place. body="' + cut(body, 100) + '"');
      }
    } else console.log('cleanup probe: dialog missing');
  } else console.log('cleanup probe: no row (never created?)');
} catch (e) {
  console.log('EXCEPTION: ' + e.message);
  await page.screenshot({ path: '/tmp/opencode/ux-test/shots-v3/repro-exception.png' }).catch(() => {});
} finally {
  console.log('resp tail:', state.resp.slice(-6).join(' ; '));
  await browser.close();
}
