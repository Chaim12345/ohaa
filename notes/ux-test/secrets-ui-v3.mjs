// secrets-ui-v3.mjs — live secrets CRUD through the SEMANTICS DOM (no OCR,
// no pixel guessing). Every control is found by role+name, clicked at its real
// rect center, and verified by re-reading the tree + the network log.
//
// Flow: Settings → (cleanup leftover OHAAGUITEST if present)
//       → [Add secret] → fill Name/Value → [Add] (expect POST 2xx)
//       → row 'OHAAGUITEST' appears → [Delete secret] bound to that row
//       → confirm dialog must name the secret (else abort, never click Delete)
//       → [Delete] (expect DELETE 2xx) → row gone.
//
// Exit: PASS/FAIL per step + network table. Screenshots in shots-v3/.
import { boot, collect, byName, clickName, typeInto, waitFor } from './semantics.mjs';

const NAME = 'OHAAGUITEST';
const VALUE = 'ohaa-gui-test-value-42';
const results = [];
let shotN = 0;

function rec(step, ok, detail = '') {
  results.push({ step, ok, detail });
  console.log(`${ok ? 'PASS' : 'FAIL'}  ${step}${detail ? '  — ' + detail : ''}`);
}

const { browser, page, state } = await boot({ url: 'http://localhost:12001' });
const shot = async (tag) => {
  shotN += 1;
  const p = `/tmp/opencode/ux-test/shots-v3/${String(shotN).padStart(2, '0')}-${tag}.png`;
  await page.screenshot({ path: p }).catch(() => {});
  return p;
};

const parseResp = (r) => {
  const i = r.indexOf(' '); const j = r.indexOf(' ', i + 1);
  return { st: Number(r.slice(0, i)), m: r.slice(i + 1, j), path: r.slice(j + 1) };
};
const find2xx = (from, method, pathRe) =>
  state.resp.slice(from).map(parseResp).find((x) => x.m === method && x.st >= 200 && x.st < 300 && pathRe.test(x.path));

const rowText = (els) => els.find((x) => x.kind === 'text' && x.name === NAME);

// Delete the row currently named NAME. Safe: the confirm dialog MUST name the
// secret, otherwise we abort before pressing Delete. Scrolls if the row's
// trash button is clipped by the bottom nav.
async function deleteRow(tag) {
  let els = await collect(page);
  let row = rowText(els);
  if (!row) return { ok: false, why: 'row not found' };
  let del = byName(els, 'Delete secret', { role: 'button' })
    .sort((a, b) => Math.abs(a.y - row.y) - Math.abs(b.y - row.y))[0];
  if (!del) return { ok: false, why: 'no Delete secret button' };
  if (del.h < 25) {
    await page.mouse.move(640, 450);
    await page.mouse.wheel(0, 300);
    await page.waitForTimeout(700);
    els = await collect(page);
    row = rowText(els);
    del = byName(els, 'Delete secret', { role: 'button' })
      .sort((a, b) => Math.abs(a.y - row.y) - Math.abs(b.y - row.y))[0];
  }
  await page.mouse.click(del.x, del.y);
  const dlg = await waitFor(page, (e) => {
    const cancel = byName(e, 'Cancel', { role: 'button', exact: true })[0];
    const confirm = byName(e, 'Delete', { role: 'button', exact: true })[0];
    const body = e.filter((x) => x.y0 > 250 && x.y1 < 700).map((x) => x.name).join(' ');
    return cancel && confirm ? { cancel, confirm, body } : null;
  }, 8000);
  if (!dlg.ok) return { ok: false, why: 'delete dialog never appeared' };
  const named = dlg.hit.body.includes(`'${NAME}'`) || dlg.hit.body.includes(NAME);
  const phrased = dlg.hit.body.includes('will be removed from your account');
  if (!(named && phrased)) {
    rec(`delete (${tag}): dialog names secret`, false, `body="${dlg.hit.body.slice(0, 90)}"`);
    return { ok: false, why: 'unsafe dialog — aborted without clicking Delete' };
  }
  rec(`delete (${tag}): confirm dialog names the secret`, true);
  if (tag === 'main') await shot('delete-dialog');
  const before = state.resp.length;
  await page.mouse.click(dlg.hit.confirm.x, dlg.hit.confirm.y);
  const gone = await waitFor(page, (e) => {
    const del2 = find2xx(before, 'DELETE', /secrets/);
    const dlgClosed = !byName(e, 'Delete', { role: 'button', exact: true })[0] && !byName(e, 'Cancel', { role: 'button', exact: true })[0];
    return del2 && dlgClosed && !rowText(e) ? { del2 } : null;
  }, 10000);
  rec(`delete (${tag}): DELETE 2xx + row gone`, gone.ok, gone.ok
    ? `${gone.hit.del2.st} ${gone.hit.del2.m} ${gone.hit.del2.path}`
    : `resp: ${state.resp.slice(before).join(' ; ') || 'none'}`);
  return { ok: gone.ok };
}

try {
  // 0) boot sanity
  let els = await collect(page);
  const nRoles = els.filter((e) => e.role).length;
  rec('boot: semantics DOM present', nRoles > 5, `${nRoles} role nodes`);
  if (nRoles <= 5) throw new Error('semantics DOM empty — cannot continue');

  // 1) Settings via bottom nav
  const nav = byName(els, 'Settings', { role: 'button' }).sort((a, b) => b.w - a.w)[0];
  if (!nav) throw new Error('Settings nav button not found');
  await page.mouse.click(nav.x, nav.y);
  const settings = await waitFor(page, (e) => byName(e, 'Add secret', { role: 'button' })[0], 8000);
  rec('nav → Settings', settings.ok, settings.ok ? `Add secret @(${Math.round(settings.hit.x)},${Math.round(settings.hit.y)})` : 'Add secret never appeared');
  if (!settings.ok) throw new Error('not on settings');

  // 1b) cleanup leftover from a previous aborted run (idempotency)
  if (rowText(settings.els)) {
    rec('cleanup: leftover row found', true);
    const c = await deleteRow('cleanup');
    rec('cleanup: leftover row removed', c.ok, c.why || '');
  } else {
    rec('cleanup: no leftover row', true);
  }
  await shot('settings');

  // 2) open the Add-secret dialog (anchor-wait: the tree returns to the
  // screen layer only after the dialog layer fully closes)
  const anchor = await waitFor(page, (e) => byName(e, 'Add secret', { role: 'button' })[0], 8000);
  if (!anchor.ok) throw new Error('screen layer did not return (no Add secret button)');
  const addOpen = await clickName(page, 'Add secret', { role: 'button' });
  if (!addOpen.ok) throw new Error('Add secret button not found');
  const dlg = await waitFor(page, (e) => {
    const fields = e.filter((x) => x.role === 'textbox' && x.w < 700 && x.y0 > 300 && x.y1 < 700).sort((a, b) => a.y0 - b.y0);
    return fields.length >= 3 ? fields : null;
  }, 8000);
  rec('open Add secret dialog', dlg.ok, dlg.ok ? '3 dialog fields found' : 'dialog fields never appeared');
  if (!dlg.ok) throw new Error('add dialog missing');
  await shot('add-dialog');
  const [fName] = dlg.hit;

  // 3) fill Name + Value
  const t1 = await typeInto(page, fName, NAME);
  rec('type Name', t1.filled, t1.filled ? NAME : 'name field did not register text');
  if (!t1.filled) throw new Error('name typing failed');
  const fields = (await collect(page)).filter((x) => x.role === 'textbox' && x.w < 700 && x.y0 > 300 && x.y1 < 700).sort((a, b) => a.y0 - b.y0);
  const t2 = await typeInto(page, fields[1], VALUE);
  rec('type Value', true, t2.filled ? 'value visible in tree' : 'value masked (expected for password field)');
  await shot('add-filled');

  // 4) click Add → expect POST 2xx and the row to appear
  const before = state.resp.length;
  const addConfirm = await clickName(page, 'Add', { role: 'button', exact: true });
  if (!addConfirm.ok) throw new Error('Add button not found');
  const created = await waitFor(page, (e) => {
    const post = find2xx(before, 'POST', /secrets/);
    const row = rowText(e);
    return post && row ? { post, row } : null;
  }, 10000);
  rec('Add → POST 2xx + row visible', created.ok, created.ok
    ? `${created.hit.post.st} ${created.hit.post.m} ${created.hit.post.path} @y${Math.round(created.hit.row.y)}`
    : `resp: ${state.resp.slice(before).join(' ; ') || 'none'}`);
  if (!created.ok) throw new Error('creation did not confirm');
  await shot('row-added');

  // 5) delete the created row (fully verified)
  const d = await deleteRow('main');
  if (!d.ok) throw new Error('delete failed: ' + (d.why || ''));
  await shot('after-delete');

  // 6) summary
  const failed = results.filter((r) => !r.ok);
  console.log('\n══ network (cloud requests) ══');
  for (const r of state.resp) console.log('  ' + r);
  console.log('\n══ console errors ══');
  console.log(state.errors.length ? state.errors.map((e) => '  ' + e).join('\n') : '  (none)');
  console.log(`\nRESULT: ${failed.length ? `FAIL (${failed.length}/${results.length})` : `PASS (${results.length}/${results.length} steps)`}`);
} catch (e) {
  await shot('exception');
  console.log('\nEXCEPTION: ' + e.message);
  const failed = results.filter((r) => !r.ok);
  console.log(`\nRESULT: FAIL (${failed.length}/${results.length} steps)`);
} finally {
  await browser.close();
}
