package me.xginko.snowballfight.protection;

import java.util.Collection;
import java.util.Set;

/** Immutable snapshot; world names are exact and may refer to unloaded worlds. */
public final class WorldPolicy {
    private final boolean blacklist;
    private final Set<String> worlds;

    public WorldPolicy(boolean blacklist, Collection<String> worlds) {
        this.blacklist = blacklist;
        this.worlds = Set.copyOf(worlds);
    }

    public boolean allows(String worldName) {
        return blacklist != worlds.contains(worldName);
    }
}
