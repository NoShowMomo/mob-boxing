package com.mobboxing;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Settings, stored in {@code config/mobboxing.json}. Edit the file and restart the game.
 * The client reads the same file, so keep "excludedEntities" and "standAngleDegrees" the same
 * on both sides when playing multiplayer.
 */
public class BoxingConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static BoxingConfig instance = new BoxingConfig();

    private static final int CURRENT_VERSION = 2;
    /** Bumped when defaults change; an older file is replaced by the new defaults. */
    public int configVersion = CURRENT_VERSION;

    /** Master switch. */
    public boolean enabled = true;
    /** Idle animals occasionally pick a fight with a nearby animal. */
    public boolean randomFights = true;
    /** Cows may fight cows, etc. If false, only different species fight. */
    public boolean sameSpeciesFight = true;
    /** Animals that get punched by a player punch back. */
    public boolean retaliateAgainstPlayers = true;

    /** Each tick an idle animal has a 1-in-N chance to look for a fight (200 = roughly every 10 s). */
    public int fightStartChance = 200;
    /** How far (blocks) an animal looks for an opponent. */
    public double searchRadius = 12.0;
    /** Damage per punch (2.0 = one heart). */
    public float punchDamage = 1.0f;
    /** Ticks between punches (20 ticks = 1 second). */
    public int punchCooldownTicks = 14;
    /** Walking speed multiplier while fighting. */
    public double fightMoveSpeed = 1.2;
    /** A bout ends after this many ticks even if nobody is down. */
    public int maxFightTicks = 400;
    /** Minimum rest between bouts for one animal, in ticks. */
    public int restTicks = 300;

    /** Mob-vs-mob punches never kill: at or below this health the victim is knocked out instead. */
    public float knockoutHealth = 1.0f;
    /** How long a knocked-out animal stays down (it also regenerates meanwhile). */
    public int knockoutTicks = 240;

    /** How far (degrees) a boxer rears up. 90 = fully upright. */
    public float standAngleDegrees = 65.0f;

    /** Entity IDs that never box. Remove an ID to let that mob box. */
    public List<String> excludedEntities = new ArrayList<>(List.of(
            // fly or swim, so rearing up makes no sense
            "minecraft:bee", "minecraft:parrot", "minecraft:axolotl", "minecraft:turtle", "minecraft:allay",
            "minecraft:frog", "minecraft:tadpole", "minecraft:strider",
            // run on the Brain AI system, which ignores the goals this mod adds
            "minecraft:villager", "minecraft:wandering_trader", "minecraft:goat", "minecraft:camel",
            "minecraft:sniffer", "minecraft:armadillo"
    ));

    public static BoxingConfig get() {
        return instance;
    }

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("mobboxing.json");
        BoxingConfig loaded = null;
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                com.google.gson.JsonObject json = GSON.fromJson(reader, com.google.gson.JsonObject.class);
                // A missing "configVersion" means a file from an older release: use the new defaults.
                if (json != null && json.has("configVersion")
                        && json.get("configVersion").getAsInt() == CURRENT_VERSION) {
                    loaded = GSON.fromJson(json, BoxingConfig.class);
                } else {
                    MobBoxing.LOGGER.info("Old or empty mobboxing.json found - replacing it with the new defaults");
                }
            } catch (Exception e) {
                MobBoxing.LOGGER.warn("Could not read {} - using defaults", path, e);
            }
        }
        instance = loaded != null ? loaded : new BoxingConfig();
        // Rewrite so new options show up in older files.
        try (Writer writer = Files.newBufferedWriter(path)) {
            GSON.toJson(instance, writer);
        } catch (IOException e) {
            MobBoxing.LOGGER.warn("Could not write {}", path, e);
        }
        BoxingHelper.clearCache();
    }
}
