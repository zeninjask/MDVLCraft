"""Haran Geppaku Futsu (art/raiju/haran_geppaku_futsu.gltf, a Blockbench glTF export) as the item model Yakusanoikazuchi's
orbiting swords are drawn with (client/RaijuClient): each cube becomes an element with its faces' UVs, the whole sword
centred on the model's middle (8, 8, 8) so it turns about its own centre, the blade pointing +y. The texture is written
next to it (textures/item/raiju_sword.png).

    python3 tools/raiju/convert_sword.py
"""
import base64
import json
import struct
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "art/raiju/haran_geppaku_futsu.gltf"
ASSETS = ROOT / "src/main/resources/assets/mdvlcraft"

# For each face (by its normal): which axis runs left -> right and which top -> bottom, as Minecraft maps a face's uv
# (axis index, sign: +1 = increasing coordinate).
FACES = {
    (0, 0, -1): ("north", (0, -1), (1, -1)),
    (0, 0, 1): ("south", (0, 1), (1, -1)),
    (1, 0, 0): ("east", (2, -1), (1, -1)),
    (-1, 0, 0): ("west", (2, 1), (1, -1)),
    (0, 1, 0): ("up", (0, 1), (2, 1)),
    (0, -1, 0): ("down", (0, 1), (2, -1)),
}


def accessor(gltf, data, index):
    a = gltf["accessors"][index]
    view = gltf["bufferViews"][a["bufferView"]]
    n = {"VEC3": 3, "VEC2": 2, "SCALAR": 1}[a["type"]]
    fmt = {5126: "f", 5123: "H", 5125: "I"}[a["componentType"]]
    size = struct.calcsize(fmt)
    stride = view.get("byteStride", size * n)
    offset = view.get("byteOffset", 0) + a.get("byteOffset", 0)
    return [struct.unpack_from("<" + fmt * n, data, offset + k * stride) for k in range(a["count"])]


def main():
    gltf = json.loads(SOURCE.read_text())
    data = base64.b64decode(gltf["buffers"][0]["uri"].split(",", 1)[1])
    cubes = []
    for node in gltf["nodes"]:
        if "mesh" not in node:
            continue
        t = node.get("translation", [0, 0, 0])
        prim = gltf["meshes"][node["mesh"]]["primitives"][0]
        pos = [[(v[k] + t[k]) * 16 for k in range(3)] for v in accessor(gltf, data, prim["attributes"]["POSITION"])]
        nor = accessor(gltf, data, prim["attributes"]["NORMAL"])
        uv = [(u * 16, v * 16) for u, v in accessor(gltf, data, prim["attributes"]["TEXCOORD_0"])]
        cubes.append((pos, nor, uv))
    lo = [min(p[k] for c in cubes for p in c[0]) for k in range(3)]
    hi = [max(p[k] for c in cubes for p in c[0]) for k in range(3)]
    shift = [8 - (lo[k] + hi[k]) / 2 for k in range(3)]
    elements = []
    for pos, nor, uv in cubes:
        pos = [[p[k] + shift[k] for k in range(3)] for p in pos]
        faces = {}
        for f in range(0, len(pos), 4):
            normal = tuple(round(c) for c in nor[f])
            name, (ua, us), (va, vs) = FACES[normal]
            quad = list(range(f, f + 4))
            left = min(pos[i][ua] * us for i in quad)
            top = min(pos[i][va] * vs for i in quad)
            first = next(i for i in quad if pos[i][ua] * us == left and pos[i][va] * vs == top)
            last = next(i for i in quad if pos[i][ua] * us != left and pos[i][va] * vs != top)
            for i in quad:   # an unrotated mapping: u follows the left -> right axis only, v the top -> bottom one
                assert uv[i][0] == (uv[first][0] if pos[i][ua] * us == left else uv[last][0]), (name, "rotated uv")
                assert uv[i][1] == (uv[first][1] if pos[i][va] * vs == top else uv[last][1]), (name, "rotated uv")
            faces[name] = {"uv": [round(x, 4) for x in (*uv[first], *uv[last])], "texture": "#blade"}
        elements.append({
            "from": [round(min(p[k] for p in pos), 4) for k in range(3)],
            "to": [round(max(p[k] for p in pos), 4) for k in range(3)],
            "faces": faces,
        })
    model = {"textures": {"blade": "mdvlcraft:item/raiju_sword", "particle": "mdvlcraft:item/raiju_sword"}, "elements": elements}
    (ASSETS / "models/item/raiju_sword.json").write_text(json.dumps(model, indent=2) + "\n")
    image = gltf["images"][0]["uri"]
    (ASSETS / "textures/item/raiju_sword.png").write_bytes(base64.b64decode(image.split(",", 1)[1]))
    print(f"{len(elements)} cubes, {hi[1] - lo[1]:.2f} px long")


if __name__ == "__main__":
    main()
