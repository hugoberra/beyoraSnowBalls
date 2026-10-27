package me.xginko.snowballfight.protection;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.projectiles.ProjectileSource;

/** Loaded only after WorldGuard is detected. Never grants an OP/bypass exception. */
final class WorldGuardProtection implements RegionProtection {
    private final RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();

    private LocalPlayer actor(ProjectileSource source) {
        if (!WorldGuardPlugin.inst().isEnabled()) throw new IllegalStateException("WorldGuard is disabled");
        return source instanceof Player player ? WorldGuardPlugin.inst().wrapPlayer(player) : null;
    }

    private boolean flag(Location location, ProjectileSource source, StateFlag... flags) {
        return query.testState(BukkitAdapter.adapt(location), actor(source), flags);
    }

    @Override
    public boolean canPlace(Location location, ProjectileSource source, boolean ice) {
        return query.testBuild(BukkitAdapter.adapt(location), actor(source), Flags.BLOCK_PLACE)
                && flag(location, source, ice ? Flags.ICE_FORM : Flags.SNOW_FALL);
    }

    @Override
    public boolean canBreak(Location location, ProjectileSource source) {
        return query.testBuild(BukkitAdapter.adapt(location), actor(source), Flags.BLOCK_BREAK)
                && flag(location, source, Flags.OTHER_EXPLOSION);
    }

    @Override
    public boolean canHarm(Entity target, ProjectileSource source) {
        actor(source); // Verify that the optional dependency is still enabled for every target type.
        if (target instanceof Player player) {
            LocalPlayer defender = WorldGuardPlugin.inst().wrapPlayer(player);
            if (!WorldGuardPlugin.inst().isEnabled()) return false;
            if (query.testState(BukkitAdapter.adapt(player.getLocation()), defender, Flags.INVINCIBILITY)) return false;
            if (source instanceof Player attacker) {
                return query.testState(BukkitAdapter.adapt(player.getLocation()), defender, Flags.PVP)
                        && flag(player.getLocation(), source, Flags.PVP)
                        && flag(attacker.getLocation(), source, Flags.PVP);
            }
            if (!(source instanceof LivingEntity)
                    && !query.testState(BukkitAdapter.adapt(player.getLocation()), defender, Flags.PVP)) return false;
            return query.testState(BukkitAdapter.adapt(player.getLocation()), defender, Flags.MOB_DAMAGE);
        }
        if (target instanceof Animals) {
            return query.testBuild(BukkitAdapter.adapt(target.getLocation()), actor(source), Flags.DAMAGE_ANIMALS);
        }
        if (target instanceof ArmorStand || !(target instanceof LivingEntity)) {
            return query.testBuild(BukkitAdapter.adapt(target.getLocation()), actor(source), Flags.INTERACT);
        }
        return true;
    }

    @Override
    public boolean canExplode(Location location, ProjectileSource source) {
        return flag(location, source, Flags.OTHER_EXPLOSION);
    }

    @Override
    public boolean canIgnite(Location location, ProjectileSource source) {
        return query.testBuild(BukkitAdapter.adapt(location), actor(source), Flags.LIGHTER, Flags.BLOCK_PLACE);
    }

    @Override
    public boolean canSpreadFire(Location location, ProjectileSource source) {
        return canIgnite(location, source) && flag(location, source, Flags.FIRE_SPREAD);
    }

    @Override
    public boolean canBurn(Location location, ProjectileSource source) {
        return query.testBuild(BukkitAdapter.adapt(location), actor(source), Flags.BLOCK_BREAK)
                && flag(location, source, Flags.FIRE_SPREAD);
    }

    @Override
    public boolean canStrike(Location location, ProjectileSource source) {
        return flag(location, source, Flags.LIGHTNING);
    }

    @Override
    public boolean canFireworkDamage(Entity target, ProjectileSource source) {
        return flag(target.getLocation(), source, Flags.FIREWORK_DAMAGE);
    }
}
