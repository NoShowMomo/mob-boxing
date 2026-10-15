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
    public float punchDamage = 2.0f;
    /** Ticks between punches (20 ticks = 1 second). */
    public int punchCooldownTicks = 14;
    /** Walking speed multiplier while fighting. */
    public double fightMoveSpeed = 1.2;
    /** A bout ends after this many ticks even if nobody is down. */
    public int maxFightTicks = 400;
    /** Minimum rest between bouts for one animal, in ticks. */
    public int restTicks = 300;

    /** Mob-vs-mob punches never kill: at or below this health the victim is knocked out instead. */
    public float knockoutHealth = 2.0f;
    /** How long a knocked-out animal stays down (it also regenerates meanwhile). */
    public int knockoutTicks = 240;

    /** How far (degrees) a boxer rears up. 90 = fully upright. */
    public float standAngleDegrees = 65.0f;

    /** Entity IDs that never box (flying, swimming, mounts, and animals with their own attack AI). */
    public List<String> excludedEntities = new ArrayList<>(List.of(
            "minecraft:horse", "minecraft:donkey", "minecraft:mule", "minecraft:camel",
            "minecraft:llama", "minecraft:trader_llama", "minecraft:bee", "minecraft:parrot",
            "minecraft:axolotl", "minecraft:turtle", "minecraft:frog", "minecraft:sniffer",
            "minecraft:strider", "minecraft:wolf", "minecraft:fox", "minecraft:cat",
            "minecraft:ocelot", "minecraft:polar_bear", "minecraft:panda", "minecraft:armadillo"
    ));

    public static BoxingConfig get() {
        return instance;
    }

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("mobboxing.json");
        BoxingConfig loaded = null;
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                loaded = GSON.fromJson(reader, BoxingConfig.class);
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
