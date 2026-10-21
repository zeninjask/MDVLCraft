// node bbsave.js <model.bbmodel> <file.animation.json> <out.bbmodel>: bake the animations into the Blockbench project.
const { chromium } = require('/opt/node-tools/node_modules/playwright');
const fs = require('fs');
(async () => {
  const [model, anim, out] = process.argv.slice(2);
  const browser = await chromium.launch({ executablePath: '/opt/pw-browsers/chromium-1194/chrome-linux/chrome', args: ['--use-gl=swiftshader', '--enable-unsafe-swiftshader'] });
  const page = await browser.newPage();
  await page.goto('https://web.blockbench.net/', { waitUntil: 'load', timeout: 120000 });
  await page.waitForFunction(() => window.Blockbench && window.Codecs && Codecs.project, null, { timeout: 120000 });
  await page.waitForTimeout(3000);
  const text = await page.evaluate(async ([m, a]) => {
    Codecs.project.load(JSON.parse(m), { path: 'haunted_lord.bbmodel' });
    await new Promise(r => setTimeout(r, 1500));
    Animator.loadFile({ path: 'haunted_lord.animation.json', content: a });
    return Codecs.project.compile({ raw: false });
  }, [fs.readFileSync(model, 'utf8'), fs.readFileSync(anim, 'utf8')]);
  fs.writeFileSync(out, typeof text === 'string' ? text : JSON.stringify(text));
  await browser.close();
})();
