package me.xginko.snowballfight;

import com.google.common.collect.ImmutableList;
import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import me.xginko.snowballfight.utils.Util;
import me.xginko.snowballfight.protection.WorldPolicy;
import org.bukkit.Color;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class SnowballConfig {

    private final @NotNull ConfigFile configFile;

    public final @NotNull List<Color> colors;
    public final @NotNull Duration snowballCacheDuration;
    public final @NotNull WorldPolicy worldPolicy;
    public final double previousDamageAmount;

    SnowballConfig() throws Exception {
        File pluginFolder = SnowballFight.getInstance().getDataFolder();
        Files.createDirectories(pluginFolder.toPath());
        File file = new File(pluginFolder, "config.yml");
        YamlConfiguration previous = new YamlConfiguration();
        if (file.exists()) previous.load(file);
        if (previous.contains("settings.worlds.use-list-as-a-blacklist")
                && !previous.isBoolean("settings.worlds.use-list-as-a-blacklist")) {
            throw new IllegalArgumentException("settings.worlds.use-list-as-a-blacklist must be true or false");
        }
        if (previous.contains("settings.worlds.list")) {
            Object rawWorlds = previous.get("settings.worlds.list");
            if (!(rawWorlds instanceof List<?> names)
                    || names.stream().anyMatch(name -> !(name instanceof String text) || text.isBlank())) {
                throw new IllegalArgumentException("settings.worlds.list must be a list of non-empty world names");
            }
        }
        this.previousDamageAmount = DamageAmount.resolve(previous.get("settings.damage.amount-snow-ball-damage"),
                previous.get("settings.damage.damage"));
        this.configFile = ConfigFile.loadConfig(file);
        if (previous.contains("settings.damage.damage")) {
            configFile.addDefault("settings.damage.damage", previous.get("settings.damage.damage"),
                    "Clave antigua conservada. En 2.0 tiene prioridad amount-snow-ball-damage.");
        }

        boolean worldBlacklist = getBoolean("settings.worlds.use-list-as-a-blacklist", true,
                "true: desactiva los efectos en los mundos de la lista. false: solo los activa en esos mundos.\n" +
                "Blacklist vacia: todos los mundos. Whitelist vacia: ninguno. Nombres exactos de los mundos.");
        this.worldPolicy = new WorldPolicy(worldBlacklist, getList("settings.worlds.list", Collections.emptyList()));

        this.snowballCacheDuration = Duration.ofSeconds(getInt("settings.snowball-cache-seconds", 20,
                "Don't touch unless you know what you're doing."));

        final List<String> defaults = Arrays.asList(
                "#B3E3F4",   // Snowy Dark Sky
                "#B5E5E7",   // Early Winter Snow White slightly Blue
                "#71C3DB",   // Wet Snow Blue White
                "#9BDBFF",   // Midwinter White slightly more Blue
                "#E8EBF0",   // Frost on thin twigs White
                "#59B1BD",   // Midday snow shadow blue
                "#407794"    // Evening slightly red sun snow shadow but more blue
        );
        this.colors = getList("settings.colors", defaults,
                "You need to configure at least 1 color. Format: 'B3E3F4' or '#B3E3F4'")
                .stream()
                .map(hexString -> {
                    try {
                        return Util.colorFromHexString(hexString);
                    } catch (NumberFormatException e) {
                        SnowballFight.logger().warn("Could not parse color '{}'. Is it formatted correctly?", hexString);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.collectingAndThen(Collectors.toList(), collected -> {
                    if (collected.isEmpty())
                        collected.addAll(defaults.stream().map(Util::colorFromHexString).collect(Collectors.toList()));
                    return ImmutableList.copyOf(collected);
                }));

        setStructure();
    }

    public void setStructure() {
        configFile.addDefault("settings.cooldown", null);
        configFile.addDefault("settings.infinite-snowballs", null);
        configFile.addDefault("settings.damage", null);
        configFile.addDefault("settings.snow", null);
        configFile.addDefault("settings.explosions", null);
        configFile.addDefault("settings.trails", null);
        configFile.addDefault("settings.fireworks", null);
        configFile.addDefault("settings.lightning", null);
        configFile.addDefault("settings.drop-armor", null);
        configFile.addDefault("settings.slowness", null);
        configFile.addDefault("settings.levitation", null);
    }

    public void saveConfig() {
        try {
            configFile.save();
        } catch (Exception e) {
            SnowballFight.logger().error("Failed to save config file!", e);
        }
    }

    public @NotNull ConfigFile master() {
        return configFile;
    }

    public boolean getBoolean(@NotNull String path, boolean def) {
        configFile.addDefault(path, def);
        return configFile.getBoolean(path, def);
    }

    public boolean getBoolean(@NotNull String path, boolean def, @NotNull String comment) {
        configFile.addDefault(path, def, comment);
        return configFile.getBoolean(path, def);
    }

    public int getInt(@NotNull String path, int def) {
        configFile.addDefault(path, def);
        return configFile.getInteger(path, def);
    }

    public int getInt(@NotNull String path, int def, @NotNull String comment) {
        configFile.addDefault(path, def, comment);
        return configFile.getInteger(path, def);
    }

    public float getFloat(@NotNull String path, float def) {
        configFile.addDefault(path, def);
        return configFile.getFloat(path, def);
    }

    public float getFloat(@NotNull String path, float def, @NotNull String comment) {
        configFile.addDefault(path, def, comment);
        return configFile.getFloat(path, def);
    }

    public double getDouble(@NotNull String path, double def) {
        configFile.addDefault(path, def);
        return configFile.getDouble(path, def);
    }

    public double getDouble(@NotNull String path, double def, @NotNull String comment) {
        configFile.addDefault(path, def, comment);
        return configFile.getDouble(path, def);
    }

    public @NotNull String getString(@NotNull String path, @NotNull String def) {
        configFile.addDefault(path, def);
        return configFile.getString(path, def);
    }

    public @NotNull String getString(@NotNull String path, @NotNull String def, @NotNull String comment) {
        configFile.addDefault(path, def, comment);
        return configFile.getString(path, def);
    }

    public @NotNull List<String> getList(@NotNull String path, @NotNull List<String> def) {
        configFile.addDefault(path, def);
        return configFile.getStringList(path);
    }

    public @NotNull List<String> getList(@NotNull String path, @NotNull List<String> def, @NotNull String comment) {
        configFile.addDefault(path, def, comment);
        return configFile.getStringList(path);
    }
}
