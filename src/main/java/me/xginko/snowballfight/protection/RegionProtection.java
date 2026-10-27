package me.xginko.snowballfight.protection;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.projectiles.ProjectileSource;

/** Does not reference optional WorldGuard classes. */
interface RegionProtection {
    boolean canPlace(Location location, ProjectileSource source, boolean ice);
    boolean canBreak(Location location, ProjectileSource source);
    boolean canHarm(Entity target, ProjectileSource source);
    boolean canExplode(Location location, ProjectileSource source);
    boolean canIgnite(Location location, ProjectileSource source);
    boolean canSpreadFire(Location location, ProjectileSource source);
    boolean canBurn(Location location, ProjectileSource source);
    boolean canStrike(Location location, ProjectileSource source);
    boolean canFireworkDamage(Entity target, ProjectileSource source);
}
