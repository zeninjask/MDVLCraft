"""Enchant-Icons for modded enchantments.

The Enchant-Icons resource pack puts a coloured icon of what an enchantment goes on (helmet, chestplate, leggings, boots,
sword, pickaxe, trident, bow, crossbow, fishing rod, a star for anything, a skull for curses) before each vanilla
enchantment's name, through font glyphs in its lang files. This writes the same for every modded enchantment into the
pack's own MDVLCraft-Astrologer-UI.zip (loaded above Enchant-Icons, so the glyphs are there), merged with the lang files
already in it.

usage: enchant_icons.py <mods dir>
tools/enchantments.tsv comes from /mdvl dump (id, EnchantmentCategory name, curse)."""
import csv, io, json, os, sys, zipfile

TOOLS = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(TOOLS, '..', '..')
PACKS = [os.path.join(ROOT, 'base', b, 'overrides', 'resourcepacks', 'MDVLCraft-Astrologer-UI.zip') for b in ('client', 'client-performance')]

HEAD, CHEST, LEGS, FEET = '', '', '', ''
SWORD, PICK, TRIDENT, BOW, CROSSBOW = '', '', '', '', ''
FISHING, STAR, SKULL = '', '', ''
# category -> (glyph, colour code)
CATEGORY = {
    'ARMOR': (CHEST, '9'), 'WEARABLE': (CHEST, '9'), 'ARMOR_HEAD': (HEAD, '9'), 'ARMOR_CHEST': (CHEST, '9'),
    'ARMOR_LEGS': (LEGS, '9'), 'ARMOR_FEET': (FEET, '9'),
    'WEAPON': (SWORD, 'c'), 'YAMATO': (SWORD, 'c'), 'SCYTHE': (SWORD, 'c'), 'BROADBLADE': (SWORD, 'c'),
    'MFTE_SOUL_MELEE_WEAPON': (SWORD, 'c'), 'knife': (SWORD, 'c'), 'desolate_dagger': (SWORD, 'c'),
    'primitive_club': (SWORD, 'c'), 'galena_gauntlet': (SWORD, 'c'),
    'DIGGER': (PICK, '2'),
    'TRIDENT': (TRIDENT, 'b'), 'extinction_spear': (TRIDENT, 'b'), 'ortholance': (TRIDENT, 'b'),
    'BOW': (BOW, '6'), 'dreadbow': (BOW, '6'), 'MFTE_SOULPIERCER': (BOW, '6'),
    'CROSSBOW': (CROSSBOW, '6'), 'MFTE_AMMO_LOAD_WEAPON': (CROSSBOW, '6'),
    'FISHING_ROD': (FISHING, '3'), 'candy_cane_hook': (FISHING, '3'),
}
OTHER = (STAR, 'd')   # breakable, vanishable, staffs, pets and anything else
CURSE = (SKULL, '4')


def mod_lang(mods):
    lang = {}
    for name in sorted(os.listdir(mods)):
        if not name.endswith('.jar'):
            continue
        try:
            with zipfile.ZipFile(os.path.join(mods, name)) as jar:
                for entry in jar.namelist():
                    if entry.endswith('/lang/en_us.json') and entry.startswith('assets/'):
                        try:
                            lang.update(json.loads(jar.read(entry).decode('utf-8', 'ignore')))
                        except ValueError:
                            pass
        except zipfile.BadZipFile:
            pass
    return lang


def main(mods):
    lang = mod_lang(mods)
    by_ns = {}
    with open(os.path.join(TOOLS, 'enchantments.tsv'), encoding='utf-8') as f:
        for row in csv.DictReader(f, delimiter='\t'):
            ns, path = row['id'].split(':', 1)
            if ns == 'minecraft':
                continue
            key = f'enchantment.{ns}.{path}'
            name = lang.get(key) or path.replace('_', ' ').title()
            glyph, colour = CURSE if row['curse'] == 'true' else CATEGORY.get(row['category'], OTHER)
            by_ns.setdefault(ns, {})[key] = f'§{colour}{glyph} {name}'
    for pack in PACKS:
        with zipfile.ZipFile(pack) as old:
            files = {n: old.read(n) for n in old.namelist()}
        for ns, entries in by_ns.items():
            entry = f'assets/{ns}/lang/en_us.json'
            merged = json.loads(files[entry].decode('utf-8')) if entry in files else {}
            merged.update(entries)
            files[entry] = json.dumps(merged, indent=2, ensure_ascii=False).encode('utf-8')
        buf = io.BytesIO()
        with zipfile.ZipFile(buf, 'w', zipfile.ZIP_DEFLATED) as new:
            for n in sorted(files):
                info = zipfile.ZipInfo(n, date_time=(2026, 1, 1, 0, 0, 0))
                info.compress_type = zipfile.ZIP_DEFLATED
                new.writestr(info, files[n])
        with open(pack, 'wb') as f:
            f.write(buf.getvalue())
        print(pack, sum(len(v) for v in by_ns.values()), 'enchantments in', len(by_ns), 'namespaces')


if __name__ == '__main__':
    main(sys.argv[1])
