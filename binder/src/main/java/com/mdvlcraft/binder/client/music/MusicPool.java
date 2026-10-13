package com.mdvlcraft.binder.client.music;

import java.util.List;

/** What plays where; tracks are sound events mdvlcraft:music.<track> from the client pack. */
enum MusicPool {
    MENU("overture_ix"),
    LOADING("intermezzo_iv"),
    ALCHEMY("pub_polka"),
    VILLAGE_DAY("heavens_prayer", "come_to_our_town", "oboe_palace", "our_village_in_the_sun"),
    VILLAGE_NIGHT("our_dreamy_town", "evening_in_the_village", "painful_feelings"),
    CAVE("the_cave_waltz", "an_omen_of_looming_death", "background_cave"),
    OCEAN("sea_breeze"),
    WILDS("over_the_fields_and_mountains", "gather_my_people", "guided_by_fate", "with_my_friends"),
    OTHER_DIMENSION("with_my_friends"),
    STRUCTURE;

    final List<String> tracks;

    MusicPool(String... tracks) {
        this.tracks = List.of(tracks);
    }
}
