// node animprev.js <model.bbmodel> <file.animation.json> <outdir> <fps> [names,comma] : Blockbench web plays each animation,
// frames rendered from three cameras side by side -> <outdir>/<anim>/f_0000.png
const { chromium } = require('/opt/node-tools/node_modules/playwright');
const fs = require('fs');
(async () => {
  const [model, animFile, out, fpsS, only] = process.argv.slice(2);
  const fps = +fpsS;
  const CAMS = process.env.CAMS ? JSON.parse(process.env.CAMS) : [[[45, 30, -75], [0, 22, -4]], [[90, 22, 0], [0, 20, 0]], [[0, 22, -95], [0, 22, 0]]];
  const browser = await chromium.launch({ executablePath: '/opt/pw-browsers/chromium-1194/chrome-linux/chrome', args: ['--use-gl=swiftshader', '--enable-unsafe-swiftshader'] });
  const page = await browser.newPage({ viewport: { width: 1400, height: 1000 } });
  page.on('pageerror', e => console.log('pageerror', e.message));
  page.on('console', m => { if (m.type() === 'error') console.log('console', m.text().slice(0, 200)); });
  await page.goto('https://web.blockbench.net/', { waitUntil: 'load', timeout: 120000 });
  await page.waitForFunction(() => window.Blockbench && window.Codecs && Codecs.project, null, { timeout: 120000 });
  await page.waitForTimeout(3000);
  const names = await page.evaluate(async ([text, anim]) => {
    Codecs.project.load(JSON.parse(text), { path: 'haunted_lord.bbmodel' });
    await new Promise(r => setTimeout(r, 1500));
    Modes.options.animate.select();
    Animator.loadFile({ path: 'haunted_lord.animation.json', content: anim });
    try { Canvas.scene.background = new THREE.Color(0xdddddd); } catch (e) {}
    return Animation.all.map(a => [a.name, a.length]);
  }, [fs.readFileSync(model, 'utf8'), fs.readFileSync(animFile, 'utf8')]);
  console.log(JSON.stringify(names));
  for (const [name, length] of names) {
    if (only && !only.split(',').some(s => name.endsWith('.' + s))) continue;
    const short = name.split('.').pop(); fs.mkdirSync(`${out}/${short}`, { recursive: true });
    const n = Math.max(1, Math.round(length * fps));
    for (let i = 0; i <= n; i++) {
      const urls = await page.evaluate(([name, t, CAMS]) => {
        const a = Animation.all.find(x => x.name === name); if (Animation.selected !== a) a.select();
        Timeline.setTime(t); Animator.preview();
        const pv = Preview.selected, shots = [];
        for (const [cam, tgt] of CAMS) {
          pv.camera.position.set(...cam); pv.controls.target.set(...tgt); pv.camera.lookAt(...tgt); pv.controls.update();
          pv.render(); shots.push(pv.canvas.toDataURL('image/png'));
        }
        return shots;
      }, [name, Math.min(i / fps, length), CAMS]);
      for (let k = 0; k < urls.length; k++) fs.writeFileSync(`${out}/${short}/c${k}_${String(i).padStart(4, '0')}.png`, Buffer.from(urls[k].split(',')[1], 'base64'));
    }
    console.log('rendered', short, n + 1);
  }
  await browser.close();
})();
