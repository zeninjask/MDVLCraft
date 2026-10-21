"""The conjured-food abilities give their food to players only: cast each one as the online player (Tester) and count
what it received (`clear` reports the items removed). Run with the player online (any game mode)."""
import sys
import time

sys.argv = sys.argv[:1] + sys.argv[1:]
from harness import c  # noqa: E402

FOODS = ['desert_dates', 'offal_feast', 'grave_fruit', 'ghouls_jerky', 'acorn_of_plenty', 'chanko_nabe', 'blubber_ration',
         'golden_elixir', 'grog_ration', 'raiders_feast', 'offering_onigiri', 'mountain_tea', 'sunbaked_flatbread',
         'wildroot_stew', 'winterberry_bunch', 'nomad_stew', 'onigiri_offering', 'storm_jerky', 'bogberry_brew',
         'blessed_bread', 'road_onigiri', 'alms', 'forage', 'immortal_peach']

for spell in sys.argv[1:] or FOODS:
    c('clear Tester')
    out = c(f'cast Tester mdvlcraft:{spell}')
    time.sleep(2.5)   # long casts take a moment
    print(f'{spell:20} cast: {out.strip()[:60] or "ok"} | {c("clear Tester").strip()}')
