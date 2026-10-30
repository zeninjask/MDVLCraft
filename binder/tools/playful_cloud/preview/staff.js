// Playful Cloud staff: two end sections held at their middle (one per fist) and a middle section on chains between
// them. Works in the armature's (Blender) space: z up. No free physics: the middle section takes a settled hanging
// shape from where the two chain ends are, eased so it doesn't snap, and is kept in front of the body.
// The same rules go into the in-game renderer.
(function () {
  const SEC = 0.76, HALF = SEC / 2, CHAIN = 0.18, MAX_CHAIN = 0.3, FRONT = 0.3, EASE = 0.06;
  const V = THREE.Vector3;
  function Staff() { this.init = false; this.ends = [[new V(), new V()], [new V(), new V()], [new V(), new V()]]; this.p = [new V(), new V()]; this.sign = { R: 1, L: 1 }; }
  function col(m, i) { const e = m.elements; return new V(e[i * 4], e[i * 4 + 1], e[i * 4 + 2]); }
  function pos(m) { const e = m.elements; return new V(e[12], e[13], e[14]); }
  // the body's frame: forward (horizontal), right, up
  function body(w) {
    const f = col(w.Root, 2).negate(); f.z = 0; if (f.lengthSq() < 1e-6) f.set(0, 1, 0); f.normalize();
    const up = new V(0, 0, 1), r = new V().crossVectors(f, up);
    return { f, r, up, o: pos(w.Root) };
  }
  function bvec(b, x, y, z) { return b.r.clone().multiplyScalar(x).addScaledVector(b.up, y).addScaledVector(b.f, z).normalize(); }
  // which end of each blade-following section is chained to the middle: the one more often in front of the body
  Staff.prototype.calibrate = function (rig, clips) {
    let sR = 0, sL = 0;
    for (const c of clips) for (let i = 0; i <= 12; i++) {
      rig.poseLocal(c.sample(c.d.len * i / 12)); const w = rig.lastWorld, b = body(w);
      sR += col(w.Tool_R, 2).negate().dot(b.f); sL += col(w.Tool_L, 2).negate().dot(b.f);
    }
    this.sign = { R: sR >= 0 ? 1 : -1, L: sL >= 0 ? 1 : -1 };
  };
  // held sections for this frame: [chainEnd, freeEnd] per hand
  Staff.prototype.held = function (w, cfg) {
    const b = body(w), out = { b };
    const zR = col(w.Tool_R, 2).negate(), zL = col(w.Tool_L, 2).negate();
    for (const s of ['R', 'L']) {
      const p = pos(w['Tool_' + s]);
      if (cfg.mode === 'toji') {
        // reference pose: right section up and across, left section down; a little of the hand's turn shows through
        const base = s === 'R' ? bvec(b, -0.35, 0.85, 0.3) : bvec(b, 0.1, -0.9, 0.35);
        const a = base.multiplyScalar(0.75).addScaledVector(s === 'R' ? zR : zL, 0.25).normalize();
        const up = a.z >= 0 ? a : a.clone().negate();
        // right: chained at its lower end; left: at its upper end
        out[s] = s === 'R' ? [p.clone().addScaledVector(up, -HALF), p.clone().addScaledVector(up, HALF)]
          : [p.clone().addScaledVector(up, HALF), p.clone().addScaledVector(up, -HALF)];
        continue;
      }
      const a = cfg.mode === 'paired' ? zR : (s === 'R' ? zR : zL);
      const sg = (cfg.flip && cfg.flip[s] ? -1 : 1) * (cfg.mode === 'paired' ? this.sign.R : this.sign[s]);
      out[s] = [p.clone().addScaledVector(a, sg * HALF), p.clone().addScaledVector(a, -sg * HALF)];
    }
    return out;
  };
  Staff.prototype.step = function (w, dt, cfg) {
    const h = this.held(w, cfg), b = h.b, P = h.R[0], Q = h.L[0];
    this.ends[0] = h.R; this.ends[2] = h.L;
    // settled hang of the middle section between the chain ends
    const D = P.distanceTo(Q), span = SEC + 2 * CHAIN;
    let d = Q.clone().sub(P); if (d.lengthSq() < 1e-6) d = b.r.clone(); d.normalize();
    const sag = Math.sqrt(Math.max(0, span * span - D * D)) * 0.42;
    const c = P.clone().add(Q).multiplyScalar(0.5); c.z -= sag;
    // tilted down a little rather than stiffly level
    d.z -= 0.35; d.normalize();
    const e0 = c.clone().addScaledVector(d, -HALF), e1 = c.clone().addScaledVector(d, HALF);
    // never behind the body: push the rod forward until both ends are in front of the chest
    const ahead = (x) => x.clone().sub(b.o).dot(b.f);
    const push = Math.max(0, FRONT - Math.min(ahead(e0), ahead(e1)));
    for (const x of [e0, e1]) { x.addScaledVector(b.f, push); if (x.z < 0.05) x.z = 0.05; }
    if (!this.init) { this.p = [e0.clone(), e1.clone()]; this.init = true; }
    const k = 1 - Math.exp(-dt / EASE);
    this.p[0].lerp(e0, k); this.p[1].lerp(e1, k);
    const mid = this.p[0].clone().add(this.p[1]).multiplyScalar(0.5), dir = this.p[1].clone().sub(this.p[0]).normalize();
    let m0 = mid.clone().addScaledVector(dir, -HALF), m1 = mid.clone().addScaledVector(dir, HALF);
    // the chains never stretch past MAX_CHAIN: pull the rod's ends in to them (the rod may give a little in length)
    if (m0.distanceTo(P) + m1.distanceTo(Q) > m0.distanceTo(Q) + m1.distanceTo(P)) [m0, m1] = [m1, m0];
    const tie = (m, a) => { const v = m.clone().sub(a), l = v.length(); return l > MAX_CHAIN ? a.clone().addScaledVector(v, MAX_CHAIN / l) : m; };
    m0 = tie(m0, P); m1 = tie(m1, Q);
    const len = m0.distanceTo(m1), want = Math.min(SEC * 1.15, Math.max(SEC * 0.8, len));
    if (Math.abs(want - len) > 1e-4) {
      const c2 = m0.clone().add(m1).multiplyScalar(0.5); let u = m1.clone().sub(m0); if (u.lengthSq() < 1e-8) u = b.r.clone(); u.normalize();
      m0 = c2.clone().addScaledVector(u, -want / 2); m1 = c2.clone().addScaledVector(u, want / 2);
    }
    this.ends[1] = [m0, m1];
  };
  Staff.prototype.build = function (group) {
    const wood = new THREE.MeshLambertMaterial({ color: 0xb8322a }), cap = new THREE.MeshLambertMaterial({ color: 0x1b1b1f }),
      steel = new THREE.MeshLambertMaterial({ color: 0x8a8f99 });
    this.rods = [0, 1, 2].map(() => {
      const g = new THREE.Group();
      g.add(new THREE.Mesh(new THREE.BoxGeometry(0.085, 0.085, 1), wood));
      for (const z of [-0.47, 0.47]) { const m = new THREE.Mesh(new THREE.BoxGeometry(0.105, 0.105, 0.06), cap); m.position.z = z; g.add(m); }
      group.add(g); return g;
    });
    this.chains = [0, 1].map(() => { const m = new THREE.Mesh(new THREE.BoxGeometry(0.035, 0.035, 1), steel); group.add(m); return m; });
  };
  function place(obj, a, b, len) {
    obj.position.copy(a).add(b).multiplyScalar(0.5);
    const d = b.clone().sub(a), l = Math.max(d.length(), 1e-4);
    obj.quaternion.setFromUnitVectors(new V(0, 0, 1), d.divideScalar(l));
    obj.scale.set(1, 1, len || l);
  }
  Staff.prototype.sync = function () {
    this.ends.forEach((e, i) => place(this.rods[i], e[0], e[1], i === 1 ? 0 : SEC));
    const [m0, m1] = this.ends[1], P = this.ends[0][0], Q = this.ends[2][0];
    const swap = m0.distanceTo(P) + m1.distanceTo(Q) > m0.distanceTo(Q) + m1.distanceTo(P);
    place(this.chains[0], P, swap ? m1 : m0); place(this.chains[1], Q, swap ? m0 : m1);
  };
  window.CloudStaff = Staff;
})();
