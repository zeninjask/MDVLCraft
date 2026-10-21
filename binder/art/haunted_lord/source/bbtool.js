// node bbtool.js <model.bbmodel> <outdir>: load in Blockbench web; write roundtrip.gltf, haunted_lord.geo.json, view PNGs.
const { chromium } = require('/opt/node-tools/node_modules/playwright');
const fs = require('fs');
(async () => {
  const [model, out] = process.argv.slice(2);
  const browser = await chromium.launch({ executablePath: '/opt/pw-browsers/chromium-1194/chrome-linux/chrome', args: ['--use-gl=swiftshader', '--enable-unsafe-swiftshader'] });
  const page = await browser.newPage({ viewport: { width: 900, height: 1000 } });
  page.on('pageerror', e => console.log('pageerror', e.message));
  await page.goto('https://web.blockbench.net/', { waitUntil: 'load', timeout: 120000 });
  await page.waitForFunction(() => window.Blockbench && window.Codecs && Codecs.project, null, { timeout: 120000 });
  await page.waitForTimeout(3000);
  const text = fs.readFileSync(model, 'utf8');
  const info = await page.evaluate(async (text) => {
    Codecs.project.load(JSON.parse(text), { path: 'haunted_lord.bbmodel' });
    await new Promise(r => setTimeout(r, 1500));
    const gltf = await Codecs.gltf.compile({ embed_textures: false });
    return { elements: Outliner.elements.length, groups: Group.all.length, format: Format.id, version: Blockbench.version,
             gltf: typeof gltf === 'string' ? gltf : JSON.stringify(gltf), geo: Codecs.bedrock.compile() };
  }, text);
  fs.writeFileSync(`${out}/roundtrip.gltf`, info.gltf);
  fs.writeFileSync(`${out}/haunted_lord.geo.json`, typeof info.geo === 'string' ? info.geo : JSON.stringify(info.geo));
  console.log(JSON.stringify({ elements: info.elements, groups: info.groups, format: info.format, version: info.version }));
  const views = { front: [0, 30, -90], frontright: [65, 40, -65], right: [90, 30, 0], back: [0, 30, 90], face: [0, 30, -45] };
  for (const v of Object.keys(views)) {
    await page.evaluate((p) => {
      const pv = Preview.selected, tgt = p[2] === -45 ? [0, 28, 0] : [0, 22, 0];
      pv.camera.position.set(p[0], p[1], p[2]); pv.controls.target.set(...tgt); pv.camera.lookAt(...tgt); pv.controls.update();
      try { Canvas.scene.background = new THREE.Color(0xdddddd); } catch (e) {}
      Canvas.updateAll();
    }, views[v]);
    await page.waitForTimeout(800);
    await ((await page.$('#preview')) || page).screenshot({ path: `${out}/view_${v}.png` });
  }
  await browser.close();
})();
