package me.xginko.snowballfight.protection;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class WorldPolicyTest {
    @Test void emptyBlacklistPreservesExistingGlobalBehaviour() {
        assertTrue(new WorldPolicy(true, List.of()).allows("survival"));
    }

    @Test void emptyWhitelistDisablesAllPluginWorlds() {
        assertFalse(new WorldPolicy(false, List.of()).allows("survival"));
    }

    @Test void blacklistExcludesOnlyListedWorlds() {
        WorldPolicy policy = new WorldPolicy(true, List.of("arena"));
        assertFalse(policy.allows("arena"));
        assertTrue(policy.allows("survival"));
    }

    @Test void whitelistIncludesOnlyListedWorldsAndMatchesExactNames() {
        WorldPolicy policy = new WorldPolicy(false, List.of("arena"));
        assertTrue(policy.allows("arena"));
        assertFalse(policy.allows("survival"));
        assertFalse(policy.allows("Arena"));
    }

    @Test void configurationSnapshotIsNotChangedByExternalListMutation() {
        List<String> names = new ArrayList<>(List.of("arena"));
        WorldPolicy policy = new WorldPolicy(false, names);
        names.clear();
        assertTrue(policy.allows("arena"));
    }
}
