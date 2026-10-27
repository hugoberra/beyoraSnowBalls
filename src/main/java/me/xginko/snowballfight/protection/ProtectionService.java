package me.xginko.snowballfight.protection;

import me.xginko.snowballfight.SnowballFight;
import org.bukkit.Location;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityCombustByBlockEvent;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.Metadatable;
import org.bukkit.plugin.Plugin;
import org.bukkit.projectiles.ProjectileSource;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** Shared gates for all modules and for secondary native explosion/firework/lightning events. */
public final class ProtectionService implements Listener {
    public enum Effect { EXPLOSION, FIREWORK, LIGHTNING }
    private record Context(Snowball snowball, ProjectileSource shooter, Effect effect, boolean damage, boolean knockback) { }
    private record PendingKnockback(Context context, int tick) { }

    private final NamespacedKey originAllowed;
    private final SnowballFight plugin;
    private static final String EFFECT_METADATA = "snowballfight.effect-source";
    private static final String BURN_METADATA = "snowballfight.burn-source";
    private static final String SHOOTER_METADATA = "snowballfight.launch-shooter";
    private static final String KNOCKBACK_METADATA = "snowballfight.knockback-source";
    private final RegionProtection regions;
    private final boolean worldGuardExpected;
    private final ThreadLocal<Context> activeEffect = new ThreadLocal<>();
    private boolean reportedFailure;

    public ProtectionService(SnowballFight plugin) {
        this.plugin = plugin;
        originAllowed = new NamespacedKey(plugin, "world_allowed_at_launch");
        Plugin worldGuard = plugin.getServer().getPluginManager().getPlugin("WorldGuard");
        worldGuardExpected = worldGuard != null;
        RegionProtection detected = null;
        if (worldGuardExpected) {
            try {
                if (!worldGuard.isEnabled()) throw new IllegalStateException("WorldGuard is not enabled");
                detected = new WorldGuardProtection();
                SnowballFight.logger().info("WorldGuard protection enabled.");
            } catch (RuntimeException | LinkageError error) {
                SnowballFight.logger().error("WorldGuard integration unavailable; harmful effects will be blocked.", error);
            }
        }
        regions = detected;
    }

    private boolean regionCheck(BooleanSupplier check) {
        if (!worldGuardExpected) return true;
        if (regions == null || reportedFailure) return false;
        try {
            return check.getAsBoolean();
        } catch (RuntimeException | LinkageError error) {
            if (!reportedFailure) {
                reportedFailure = true;
                SnowballFight.logger().error("WorldGuard check failed; affected actions are blocked.", error);
            }
            return false;
        }
    }

    public void resetFailure() {
        reportedFailure = false;
    }

    public boolean worldAllowed(Location location) {
        return location.getWorld() != null && SnowballFight.config() != null
                && SnowballFight.config().worldPolicy.allows(location.getWorld().getName());
    }

    public boolean allows(Snowball snowball) {
        Byte origin = snowball.getPersistentDataContainer().get(originAllowed, PersistentDataType.BYTE);
        return (origin == null || origin != 0) && worldAllowed(snowball.getLocation());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity() instanceof Snowball snowball) {
            snowball.getPersistentDataContainer().set(originAllowed, PersistentDataType.BYTE,
                    (byte) (worldAllowed(snowball.getLocation()) ? 1 : 0));
            if (snowball.getShooter() != null) {
                snowball.setMetadata(SHOOTER_METADATA, new FixedMetadataValue(plugin, snowball.getShooter()));
            }
        }
    }

    private ProjectileSource shooter(Snowball snowball) {
        return snowball.getMetadata(SHOOTER_METADATA).stream()
                .filter(value -> value.getOwningPlugin() == plugin)
                .map(value -> value.value())
                .filter(ProjectileSource.class::isInstance)
                .map(ProjectileSource.class::cast)
                .findFirst().orElseGet(snowball::getShooter);
    }

    public boolean canAffect(Snowball snowball, Entity target) {
        return canAffect(snowball, target, shooter(snowball));
    }

    public void damageOnImpact(Snowball snowball, LivingEntity target, double amount) {
        if (amount <= 0 || !canAffect(snowball, target)) return;
        DamageSource.Builder damage = DamageSource.builder(DamageType.THROWN).withDirectEntity(snowball);
        if (shooter(snowball) instanceof Entity attacker) damage.withCausingEntity(attacker);
        target.damage(amount, damage.build());
    }

    private boolean canAffect(Snowball snowball, Entity target, ProjectileSource shooter) {
        if (!allows(snowball) || !snowball.getWorld().equals(target.getWorld()) || !worldAllowed(target.getLocation())) return false;
        boolean mobSource = shooter instanceof LivingEntity && !(shooter instanceof Player);
        if (target instanceof Player && !mobSource && !target.getWorld().getPVP()) return false;
        return regionCheck(() -> regions.canHarm(target, shooter));
    }

    public boolean canPlace(Snowball snowball, Block block, boolean ice) {
        return allows(snowball) && snowball.getWorld().equals(block.getWorld()) && worldAllowed(block.getLocation())
                && regionCheck(() -> regions.canPlace(block.getLocation(), shooter(snowball), ice));
    }

    public boolean canExplode(Snowball snowball, Location location) {
        return allows(snowball) && snowball.getWorld().equals(location.getWorld()) && worldAllowed(location)
                && regionCheck(() -> regions.canExplode(location, shooter(snowball)));
    }

    public boolean canStrike(Snowball snowball, Location location) {
        return allows(snowball) && snowball.getWorld().equals(location.getWorld()) && worldAllowed(location)
                && regionCheck(() -> regions.canStrike(location, shooter(snowball)));
    }

    public <T> T during(Snowball snowball, Effect effect, boolean damage, Supplier<T> action) {
        Context previous = activeEffect.get();
        activeEffect.set(new Context(snowball, shooter(snowball), effect, damage, true));
        try { return action.get(); }
        finally {
            if (previous == null) activeEffect.remove();
            else activeEffect.set(previous);
        }
    }

    public void track(Entity entity, Snowball snowball, Effect effect, boolean damage) {
        track(entity, snowball, effect, damage, true);
    }

    public void track(Entity entity, Snowball snowball, Effect effect, boolean damage, boolean knockback) {
        entity.setMetadata(EFFECT_METADATA, new FixedMetadataValue(plugin, new Context(snowball, shooter(snowball), effect, damage, knockback)));
    }

    private Context context(Entity entity) {
        Context current = activeEffect.get();
        if (current != null || entity == null) return current;
        return storedContext(entity, EFFECT_METADATA);
    }

    private Context storedContext(Metadatable object, String key) {
        if (object == null) return null;
        return object.getMetadata(key).stream()
                .filter(value -> value.getOwningPlugin() == plugin)
                .map(value -> value.value())
                .filter(Context.class::isInstance)
                .map(Context.class::cast)
                .findFirst().orElse(null);
    }

    // Some native knockback events omit the source. Observe damage even if another plugin cancels it.
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void observeEffectDamage(EntityDamageEvent event) {
        Entity source = event instanceof EntityDamageByEntityEvent byEntity ? byEntity.getDamager() : null;
        Context effect = context(source);
        if (effect == null) event.getEntity().removeMetadata(KNOCKBACK_METADATA, plugin);
        else event.getEntity().setMetadata(KNOCKBACK_METADATA,
                new FixedMetadataValue(plugin, new PendingKnockback(effect, Bukkit.getCurrentTick())));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        Entity damager = event instanceof EntityDamageByEntityEvent byEntity ? byEntity.getDamager() : null;
        Context context = context(damager);
        if (context == null && event instanceof EntityDamageByBlockEvent byBlock) {
            context = storedContext(byBlock.getDamager(), EFFECT_METADATA);
        }
        if (context == null && event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK) {
            context = storedContext(event.getEntity(), BURN_METADATA);
            if (event.getEntity().getFireTicks() <= 0) event.getEntity().removeMetadata(BURN_METADATA, plugin);
        }
        if (context == null) {
            if (damager instanceof Snowball snowball && allows(snowball) && !canAffect(snowball, event.getEntity())) {
                event.setCancelled(true);
            }
            return;
        }
        final Context effect = context;
        if (!effect.damage || !canAffect(effect.snowball, event.getEntity(), effect.shooter)
                || effect.effect == Effect.EXPLOSION && !regionCheck(() -> regions.canExplode(event.getEntity().getLocation(), effect.shooter))
                || effect.effect == Effect.FIREWORK && !regionCheck(() -> regions.canFireworkDamage(event.getEntity(), effect.shooter))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        Context context = context(event.getEntity());
        if (context == null) return;
        if (!canExplode(context.snowball, event.getLocation())) {
            event.setCancelled(true);
            return;
        }
        event.blockList().removeIf(block -> !worldAllowed(block.getLocation())
                || !regionCheck(() -> regions.canBreak(block.getLocation(), context.shooter)));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent event) {
        Context context = context(event.getIgnitingEntity());
        if (context == null) context = storedContext(event.getIgnitingBlock(), EFFECT_METADATA);
        if (context == null) {
            event.getBlock().removeMetadata(EFFECT_METADATA, plugin);
            return;
        }
        final Context effect = context;
        if (!allows(effect.snowball) || !worldAllowed(event.getBlock().getLocation())
                || !regionCheck(() -> regions.canIgnite(event.getBlock().getLocation(), effect.shooter))) {
            event.setCancelled(true);
        } else {
            event.getBlock().setMetadata(EFFECT_METADATA, new FixedMetadataValue(plugin, effect));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFireSpread(BlockSpreadEvent event) {
        Context effect = storedContext(event.getSource(), EFFECT_METADATA);
        if (effect == null) return;
        if (!allows(effect.snowball) || !worldAllowed(event.getBlock().getLocation())
                || !regionCheck(() -> regions.canSpreadFire(event.getBlock().getLocation(), effect.shooter))) {
            event.setCancelled(true);
        } else {
            event.getBlock().setMetadata(EFFECT_METADATA, new FixedMetadataValue(plugin, effect));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFireBurn(BlockBurnEvent event) {
        Context effect = storedContext(event.getIgnitingBlock(), EFFECT_METADATA);
        if (effect != null && (!allows(effect.snowball) || !worldAllowed(event.getBlock().getLocation())
                || !regionCheck(() -> regions.canBurn(event.getBlock().getLocation(), effect.shooter)))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFireFade(BlockFadeEvent event) {
        event.getBlock().removeMetadata(EFFECT_METADATA, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFireBreak(BlockBreakEvent event) {
        event.getBlock().removeMetadata(EFFECT_METADATA, plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCombust(EntityCombustEvent event) {
        Context effect = event instanceof EntityCombustByEntityEvent byEntity ? context(byEntity.getCombuster())
                : event instanceof EntityCombustByBlockEvent byBlock ? storedContext(byBlock.getCombuster(), EFFECT_METADATA)
                : activeEffect.get();
        if (effect == null) {
            event.getEntity().removeMetadata(BURN_METADATA, plugin);
        } else if (!effect.damage || !canAffect(effect.snowball, event.getEntity(), effect.shooter)) {
            event.setCancelled(true);
        } else {
            event.getEntity().setMetadata(BURN_METADATA, new FixedMetadataValue(plugin, effect));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onKnockback(io.papermc.paper.event.entity.EntityKnockbackEvent event) {
        Entity source = event instanceof io.papermc.paper.event.entity.EntityPushedByEntityAttackEvent attack
                ? attack.getPushedBy() : null;
        Context context = context(source);
        if (context == null) {
            context = event.getEntity().getMetadata(KNOCKBACK_METADATA).stream()
                    .filter(value -> value.getOwningPlugin() == plugin)
                    .map(value -> value.value())
                    .filter(PendingKnockback.class::isInstance)
                    .map(PendingKnockback.class::cast)
                    .filter(pending -> pending.tick == Bukkit.getCurrentTick())
                    .map(pending -> pending.context).findFirst().orElse(null);
        }
        event.getEntity().removeMetadata(KNOCKBACK_METADATA, plugin);
        if (context != null && (!context.knockback || !canAffect(context.snowball, event.getEntity(), context.shooter))
                || context == null && source instanceof Snowball snowball
                && allows(snowball) && !canAffect(snowball, event.getEntity())) {
            event.setCancelled(true);
        }
    }
}
