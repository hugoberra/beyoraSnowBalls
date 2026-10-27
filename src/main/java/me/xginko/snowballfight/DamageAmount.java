package me.xginko.snowballfight;

/** Migration and validation independent of Bukkit, so old amounts can be preserved exactly. */
public final class DamageAmount {
    private DamageAmount() { }

    public static double resolve(Object configured, Object legacy) {
        Object raw = configured != null ? configured : legacy != null ? legacy : 30.0;
        if (!(raw instanceof Number value) || !Double.isFinite(value.doubleValue()) || value.doubleValue() < 0) {
            throw new IllegalArgumentException("settings.damage.amount-snow-ball-damage must be a finite number >= 0");
        }
        return value.doubleValue();
    }
}
