package com.mdvlcraft.binder.music;

/** Short one-shot cues that duck the background music while they play (sound events mdvlcraft:music.<track>). */
public enum Jingle {
    SKILL_POINT_GAIN("skill_point_gain"),
    JOB_CHANGE("job_change"),
    SLEEP("sleep"),
    QUEST_ACCEPT("quest_accept"),
    QUEST_CLEAR("quest_clear"),
    CURSED("cursed"),
    DEFEATED("defeated");

    public final String track;

    Jingle(String track) {
        this.track = track;
    }
}
