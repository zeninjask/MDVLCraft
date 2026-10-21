#!/usr/bin/env python3
"""Cuts the Haunted Lord's sounds (client/GauntClient) from CC0 Freesound recordings into mono OGGs and writes
assets/mdvlcraft/sounds.json entries for them. Each sound event gets several variants (Minecraft picks one at random).
Needs curl and ffmpeg. Usage (from the binder folder): python3 tools/haunted_lord/sounds.py <download dir> [event,event]
"""
import json
import os
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(HERE, '..', '..', 'src', 'main', 'resources', 'assets', 'mdvlcraft')
# event: (Freesound id, preview, [(start, end) seconds], loudness target or None to peak-normalise short hits)
SOUNDS = {
    # Mediasaur, "Masked_Breathing_Raspy_Loop": one breath every ~4.1 s, cut to the idle's 4 s cycle
    'idle': (788055, 'https://cdn.freesound.org/previews/788/788055_10164671-hq.mp3',
             [(0.8, 4.8), (4.95, 8.95), (9.05, 13.05), (13.7, 17.7)], -18),
    # worthahep88, "Foot Drags Dirt 2": single foot drags
    'walk': (319210, 'https://cdn.freesound.org/previews/319/319210_3443504-hq.mp3',
             [(0.45, 0.85), (2.9, 3.45), (4.7, 5.15), (6.4, 6.8), (8.7, 9.1)], None),
    # Dmgrinder, "Creature Screeching": short screeches
    'sprint': (436199, 'https://cdn.freesound.org/previews/436/436199_6329318-hq.mp3',
               [(6.35, 7.15), (8.4, 8.95), (14.0, 15.4), (20.4, 21.7), (22.1, 23.45)], -16),
    # SMuntsinger, "Knuckles Cracking.wav": single cracks
    'crawl': (449608, 'https://cdn.freesound.org/previews/449/449608_9235153-hq.mp3',
              [(0.33, 0.62), (0.98, 1.3), (1.55, 1.85), (3.78, 4.05), (5.0, 5.3)], None),
    # unfa, "Deafening Whispers": 4 s of whispering, the cast's cycle
    'cast': (185059, 'https://cdn.freesound.org/previews/185/185059_1038806-hq.mp3',
             [(1.0, 5.0), (9.0, 13.0), (17.0, 21.0), (27.0, 31.0)], -18),
    # onderwish, "Ghost Scream": the Horrify screech (gaunt/GauntLeap.horrify)
    'horrify': (469141, 'https://cdn.freesound.org/previews/469/469141_2694940-hq.mp3', [(1.0, 5.0)], -14),
    # morgothFLOW, "bonebreaker.mp3": bone snaps (the transformation)
    'snap': (124971, 'https://cdn.freesound.org/previews/124/124971_1247793-hq.mp3',
             [(1.92, 2.25), (2.36, 2.78), (2.8, 3.2), (3.68, 4.0)], None),
    # danielrutterfilm, "Necksnap_build.wav" (T1): the execution's neck snap; the crack peaks 0.3 s into the cut
    # (gaunt/GauntExecute.SNAP_SOUND_LEAD)
    'neck_snap': (134025, 'https://cdn.freesound.org/previews/134/134025_1911785-hq.mp3', [(0.42, 1.56)], None),
}

download = sys.argv[1]
os.makedirs(download, exist_ok=True)
out_dir = os.path.join(ASSETS, 'sounds', 'haunted_lord')
os.makedirs(out_dir, exist_ok=True)
events = {}
only = sys.argv[2].split(',') if len(sys.argv) > 2 else list(SOUNDS)
for event, (sid, url, cuts, loudness) in SOUNDS.items():
    if event not in only:
        continue
    src = os.path.join(download, f'{sid}.mp3')
    if not os.path.exists(src):
        subprocess.run(['curl', '-sSfL', '-A', 'Mozilla/5.0', '-o', src, url], check=True)
    names = []
    for k, (a, b) in enumerate(cuts, 1):
        length = b - a
        fade = min(0.3, length / 4) if loudness is not None else 0.04
        piece = os.path.join(download, f'{event}_{k}.wav')
        subprocess.run(['ffmpeg', '-loglevel', 'error', '-y', '-i', src, '-ac', '1', '-ar', '44100', '-af',
                        f'atrim=start={a}:duration={length:.3f},asetpts=PTS-STARTPTS,afade=t=in:d=0.01,afade=t=out:st={length - fade:.3f}:d={fade}',
                        piece], check=True)
        if loudness is not None:
            level = f'loudnorm=I={loudness}:TP=-1.5'
        else:   # short hits: peak at -3 dB
            probe = subprocess.run(['ffmpeg', '-hide_banner', '-i', piece, '-af', 'volumedetect', '-f', 'null', '-'], capture_output=True, text=True).stderr
            level = f"volume={-3.0 - float(probe.split('max_volume:')[1].split('dB')[0]):.2f}dB"
        name = f'{event}_{k}'
        subprocess.run(['ffmpeg', '-loglevel', 'error', '-y', '-i', piece, '-af', level, '-ar', '44100', '-c:a', 'libvorbis', '-q:a', '5',
                        os.path.join(out_dir, name + '.ogg')], check=True)
        names.append(f'mdvlcraft:haunted_lord/{name}')
    events[f'haunted_lord.{event}'] = {'subtitle': f'subtitles.mdvlcraft.haunted_lord.{event}', 'sounds': names}

path = os.path.join(ASSETS, 'sounds.json')
sounds = json.load(open(path)) if os.path.exists(path) else {}
sounds.update(events)
with open(path, 'w') as f:
    json.dump(sounds, f, indent=2)
    f.write('\n')
print({k: len(v['sounds']) for k, v in events.items()})
