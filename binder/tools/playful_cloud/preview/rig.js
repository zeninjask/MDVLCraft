// EF biped rig: CPU linear-blend skinning of Epic Fight's biped.json, posed from EF animation JSON (matrix keyframes).
(function () {
  function mat(a) { const m = new THREE.Matrix4(); m.set(...a); return m; }
  function Rig(biped, texture) {
    const arm = biped.armature, v = biped.vertices;
    this.names = arm.joints;
    this.parent = {}; this.rest = {};
    const walk = (j, p) => { this.parent[j.name] = p; this.rest[j.name] = mat(j.transform); (j.children || []).forEach(c => walk(c, j.name)); };
    walk(arm.hierarchy[0], null);
    this.order = [];
    const ord = (j) => { this.order.push(j.name); (j.children || []).forEach(ord); };
    ord(arm.hierarchy[0]);
    this.restWorld = this.world(this.rest);
    this.invBind = {};
    for (const n of this.order) this.invBind[n] = this.restWorld[n].clone().invert();
    // per-vertex influences
    const P = v.positions.array, U = v.uvs.array, N = v.normals.array, vc = v.vcounts.array, W = v.weights.array, vi = v.vindices.array;
    this.inf = []; let k = 0;
    for (let i = 0; i < vc.length; i++) { const l = []; for (let c = 0; c < vc[i]; c++) { l.push([this.names[vi[k]], W[vi[k + 1]]]); k += 2; } this.inf.push(l); }
    this.P = P;
    const tris = []; const outer = ['hat', 'jacket', 'leftSleeve', 'rightSleeve', 'leftPants', 'rightPants'];
    const makeGeo = (parts) => {
      const idx = [], uv = [];
      for (const name of parts) { const a = v.parts[name].array; for (let i = 0; i < a.length; i += 3) { idx.push(a[i]); uv.push(U[a[i + 1] * 2], U[a[i + 1] * 2 + 1]); } }
      const g = new THREE.BufferGeometry();
      g.setAttribute('position', new THREE.Float32BufferAttribute(new Float32Array(idx.length * 3), 3));
      g.setAttribute('uv', new THREE.Float32BufferAttribute(uv, 2));
      return { g, idx };
    };
    const inner = Object.keys(v.parts).filter(p => !outer.includes(p));
    this.geos = [makeGeo(inner), makeGeo(outer.filter(p => v.parts[p]))];
    this.group = new THREE.Group();
    const m1 = new THREE.MeshLambertMaterial({ map: texture, alphaTest: 0.5, side: THREE.DoubleSide });
    const m2 = new THREE.MeshLambertMaterial({ map: texture, alphaTest: 0.5, side: THREE.DoubleSide, polygonOffset: true, polygonOffsetFactor: -1 });
    this.meshes = [new THREE.Mesh(this.geos[0].g, m1), new THREE.Mesh(this.geos[1].g, m2)];
    this.meshes.forEach(m => this.group.add(m));
    // Blender (z up) -> three (y up)
    this.group.rotation.x = -Math.PI / 2;
    this.pose(null, 0);
  }
  Rig.prototype.world = function (local) {
    const w = {};
    for (const n of this.order) { const p = this.parent[n]; w[n] = p ? w[p].clone().multiply(local[n]) : local[n].clone(); }
    return w;
  };
  // sample one joint's matrix at t (linear translation, slerp rotation), like Epic Fight
  function sample(track, t) {
    const T = track.time, X = track.transform;
    if (t <= T[0] || T.length === 1) return mat(X[0]);
    if (t >= T[T.length - 1]) return mat(X[X.length - 1]);
    let i = 0; while (t > T[i + 1]) i++;
    const f = (t - T[i]) / (T[i + 1] - T[i]);
    const a = mat(X[i]), b = mat(X[i + 1]);
    const pa = new THREE.Vector3(), qa = new THREE.Quaternion(), sa = new THREE.Vector3();
    const pb = new THREE.Vector3(), qb = new THREE.Quaternion(), sb = new THREE.Vector3();
    a.decompose(pa, qa, sa); b.decompose(pb, qb, sb);
    return new THREE.Matrix4().compose(pa.lerp(pb, f), qa.slerp(qb, f), sa.lerp(sb, f));
  }
  Rig.prototype.length = function (anim) { let l = 0; for (const tr of anim.animation) l = Math.max(l, tr.time[tr.time.length - 1]); return l; };
  Rig.prototype.pose = function (anim, t) {
    const local = Object.assign({}, this.rest);
    if (anim) for (const tr of anim.animation) if (this.rest[tr.name]) local[tr.name] = sample(tr, t);
    this.poseLocal(local);
  };
  // pose from local joint matrices ({joint: Matrix4}, missing joints stay at rest)
  Rig.prototype.poseLocal = function (over) {
    const local = Object.assign({}, this.rest, over);
    const w = this.world(local);
    const skin = {}; for (const n of this.order) skin[n] = w[n].clone().multiply(this.invBind[n]);
    this.lastWorld = w;
    const tmp = new THREE.Vector3(), acc = new THREE.Vector3();
    const posed = [];
    for (let i = 0; i < this.inf.length; i++) {
      acc.set(0, 0, 0);
      for (const [n, wt] of this.inf[i]) { tmp.set(this.P[i * 3], this.P[i * 3 + 1], this.P[i * 3 + 2]).applyMatrix4(skin[n]).multiplyScalar(wt); acc.add(tmp); }
      posed.push(acc.x, acc.y, acc.z);
    }
    for (const { g, idx } of this.geos) {
      const a = g.attributes.position.array;
      for (let i = 0; i < idx.length; i++) { a[i * 3] = posed[idx[i] * 3]; a[i * 3 + 1] = posed[idx[i] * 3 + 1]; a[i * 3 + 2] = posed[idx[i] * 3 + 2]; }
      g.attributes.position.needsUpdate = true; g.computeVertexNormals(); g.computeBoundingSphere();
    }
  };
  window.EFRig = Rig;
})();

// compact clip: {len, loop, n, j: {joint: [qx,qy,qz,qw,px,py,pz, ...per frame]}} sampled at 30 fps
window.CloudClip = function (d) { this.d = d; };
CloudClip.prototype.sample = function (t) {
  const d = this.d, out = {}, f = Math.max(0, Math.min(d.n - 1, t * 30)), i = Math.min(d.n - 2, Math.floor(f)), a = f - i;
  const q0 = new THREE.Quaternion(), q1 = new THREE.Quaternion(), p0 = new THREE.Vector3(), p1 = new THREE.Vector3(), one = new THREE.Vector3(1, 1, 1);
  for (const n in d.j) {
    const v = d.j[n];
    if (v.length === 7) { out[n] = new THREE.Matrix4().compose(p0.set(v[4], v[5], v[6]), q0.set(v[0], v[1], v[2], v[3]), one); continue; }
    const o = i * 7, k = (i + 1) * 7;
    q0.set(v[o], v[o + 1], v[o + 2], v[o + 3]); q1.set(v[k], v[k + 1], v[k + 2], v[k + 3]);
    p0.set(v[o + 4], v[o + 5], v[o + 6]); p1.set(v[k + 4], v[k + 5], v[k + 6]);
    out[n] = new THREE.Matrix4().compose(p0.clone().lerp(p1, a), q0.clone().slerp(q1, a), one);
  }
  return out;
};
