package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.ability.StanceElement;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Client copy of players' Stances (by entity ID): the local player's for the wheel icon, everyone's for trail colours. */
public final class ClientStances {
    private record State(StanceElement element, boolean active) {
    }

    private static final Map<Integer, State> STATES = new HashMap<>();

    private ClientStances() {
    }

    public static void update(int entityId, StanceElement element, boolean active) {
        STATES.put(entityId, new State(element, active));
    }

    public static void reset() {
        STATES.clear();
    }

    /** The element of an active stance, if the entity has one. */
    public static Optional<StanceElement> active(int entityId) {
        State state = STATES.get(entityId);
        return state != null && state.active() ? Optional.of(state.element()) : Optional.empty();
    }

    /** The entity's chosen element, active or not (Fire until the server says otherwise). */
    public static StanceElement element(int entityId) {
        State state = STATES.get(entityId);
        return state == null ? StanceElement.FIRE : state.element();
    }
}
