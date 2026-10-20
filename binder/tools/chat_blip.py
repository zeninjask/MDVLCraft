"""Writes the chat text blip (client/TypedChat) as a WAV; convert it with
    ffmpeg -i blip.wav -ac 1 -c:a libvorbis -q:a 6 src/main/resources/assets/mdvlcraft/sounds/chat/blip.ogg

DQ9-style text blip: a 663 Hz near-sine, flat for 36 ms then fading out by 50 ms. Needs numpy and scipy."""
import numpy as np, scipy.io.wavfile as w, sys
SR = 44100; F0 = 663.0

def blip():
    t = np.arange(int(0.050 * SR)) / SR
    tone = np.sin(2 * np.pi * F0 * t) + 0.014 * np.sin(2 * np.pi * 3 * F0 * t)
    env = np.ones_like(t)
    env[:int(0.0008 * SR)] = np.linspace(0, 1, int(0.0008 * SR))
    fade = t > 0.036
    env[fade] = np.cos((t[fade] - 0.036) / 0.014 * np.pi / 2) ** 2
    return 0.5 * tone * env

if __name__ == '__main__':
    w.write(sys.argv[1], SR, (blip() * 32767).astype(np.int16))
