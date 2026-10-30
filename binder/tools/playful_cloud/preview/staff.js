// Playful Cloud staff: three sections A-B-C linked by short chains. Works in the armature's (Blender) space: z up.
// Each section has ends e0, e1 (A.e1-B.e0 and B.e1-C.e1 are chained). Held sections follow the Tool joints (their -Z
// axis), gripped at the pommel end; the middle one swings as a Verlet rod. The same rules go into the in-game renderer.
(function () {
  const SEC = 0.76, BACK = 0.035, CHAIN = 0.1, G = -22, SUB = 8, ITER = 6;
  const V = THREE.Vector3;
  function Staff() {
    this.s = [0, 1, 2].map(() => ({ e0: new V(), e1: new V(), p0: new V(), p1: new V(), held: false, blend: 0 }));
    this.mode = 'ends'; this.init = false;
  }
  function toolOf(w, name) {
    const m = w[name], e = m.elements;
    return { pos: new V(e[12], e[13], e[14]), aim: new V(-e[8], -e[9], -e[10]).normalize() };
  }
  function grip(t) { return [t.pos.clone().addScaledVector(t.aim, -BACK), t.pos.clone().addScaledVector(t.aim, SEC - BACK)]; }
  // held targets for this frame: {index: [e0, e1]}. Each hand holds an end section like a sword hilt; the middle one
  // always dangles between them on its chains.
  Staff.prototype.targets = function (R, L) {
    this.mode = 'ends';
    return { 0: grip(R), 2: grip(L) };
  };
  Staff.prototype.step = function (world, dt) {
    const R = toolOf(world, 'Tool_R'), L = toolOf(world, 'Tool_L'), tg = this.targets(R, L);
    if (!this.init) {
      // start hanging straight down from wherever it's held
      for (let i = 0; i < 3; i++) {
        const s = this.s[i]; const g = tg[i] || grip(R);
        s.e0.copy(g[0]); s.e1.copy(g[1]); s.p0.copy(g[0]); s.p1.copy(g[1]);
      }
      this.init = true;
    }
    dt = Math.min(dt, 1 / 20);
    const h = dt / SUB;
    for (let k = 0; k < SUB; k++) {
      const f = (k + 1) / SUB;
      for (let i = 0; i < 3; i++) {
        const s = this.s[i];
        if (tg[i]) {
          // held: move towards the hand (blend in over ~0.12 s after a re-grab)
          s.blend = s.held ? 1 : Math.min(1, s.blend + h / 0.12);
          const a = tg[i][0], b = tg[i][1];
          const n0 = s.e0.clone().lerp(a, s.held ? f : s.blend), n1 = s.e1.clone().lerp(b, s.held ? f : s.blend);
          if (s.blend >= 1) s.held = true;
          s.p0.copy(s.e0); s.p1.copy(s.e1); s.e0.copy(n0); s.e1.copy(n1);
        } else {
          if (s.held) { s.held = false; s.blend = 0; }
          for (const [e, p] of [[s.e0, s.p0], [s.e1, s.p1]]) {
            const v = e.clone().sub(p).multiplyScalar(0.995); p.copy(e); e.add(v); e.z += G * h * h;
          }
        }
      }
      for (let it = 0; it < ITER; it++) {
        for (let i = 0; i < 3; i++) if (!tg[i]) rod(this.s[i].e0, this.s[i].e1, SEC, 0.5, 0.5);
        link(this.s[0].e1, this.s[1].e0, tg[0], tg[1]);
        link(this.s[1].e1, this.s[2].e1, tg[1], tg[2]);
        for (let i = 0; i < 3; i++) if (!tg[i]) for (const e of [this.s[i].e0, this.s[i].e1]) if (e.z < 0.03) e.z = 0.03;
      }
    }
  };
  function rod(a, b, len, wa, wb) {
    const d = b.clone().sub(a), l = d.length() || 1e-6, c = (l - len) / l;
    a.addScaledVector(d, c * wa); b.addScaledVector(d, -c * wb);
  }
  function link(a, b, ha, hb) {
    const d = b.clone().sub(a), l = d.length();
    if (l <= CHAIN || (ha && hb)) return;
    const c = (l - CHAIN) / l, wa = ha ? 0 : hb ? 1 : 0.5, wb = hb ? 0 : ha ? 1 : 0.5;
    a.addScaledVector(d, c * wa); b.addScaledVector(d, -c * wb);
  }
  // meshes
  Staff.prototype.build = function (group) {
    const wood = new THREE.MeshLambertMaterial({ color: 0xb8322a }), gold = new THREE.MeshLambertMaterial({ color: 0x1b1b1f }),
      steel = new THREE.MeshLambertMaterial({ color: 0x8a8f99 });
    this.rods = this.s.map(() => {
      const g = new THREE.Group();
      const body = new THREE.Mesh(new THREE.BoxGeometry(0.085, 0.085, 1), wood); g.add(body);
      for (const z of [-0.47, 0.47]) { const cap = new THREE.Mesh(new THREE.BoxGeometry(0.105, 0.105, 0.06), gold); cap.position.z = z; g.add(cap); }
      group.add(g); return g;
    });
    this.chains = [0, 1].map(() => { const m = new THREE.Mesh(new THREE.BoxGeometry(0.035, 0.035, 1), steel); group.add(m); return m; });
  };
  function place(obj, a, b, len) {
    obj.position.copy(a).add(b).multiplyScalar(0.5);
    const d = b.clone().sub(a), l = Math.max(d.length(), 1e-4);
    obj.quaternion.setFromUnitVectors(new V(0, 0, 1), d.divideScalar(l));
    obj.scale.set(1, 1, len ? len / 1 : l);
  }
  Staff.prototype.sync = function () {
    this.s.forEach((s, i) => place(this.rods[i], s.e0, s.e1, SEC));
    place(this.chains[0], this.s[0].e1, this.s[1].e0);
    place(this.chains[1], this.s[1].e1, this.s[2].e1);
  };
  window.CloudStaff = Staff;
})();
