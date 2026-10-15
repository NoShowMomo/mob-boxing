# Mob Boxing (Fabric, Minecraft 1.21.1)

Peaceful animals square up and box. They rear onto their hind legs, jab with their front legs
(chickens use wings), lean into each punch, and ring a bell when one goes down. Mob-vs-mob punches
never kill: at <= 1 heart the loser is **knocked out** (lies on its side, regenerates, then gets up).

## Build
Needs JDK 21 and internet access for Gradle.

    gradle wrapper --gradle-version 8.10.2   # once (or just open the folder in IntelliJ IDEA)
    ./gradlew build        # jar ends up in build/libs/mob-boxing-1.0.0.jar
    ./gradlew runClient    # test in a dev client

Install the jar (not the -sources one) in `mods/` with Fabric Loader + Fabric API for 1.21.1.

## Try it
Spawn a few cows/pigs/sheep close together in a flat area and wait ~10 s, or punch one: it hits back.

## Tuning
`config/mobboxing.json` is created on first launch (punch damage, fight frequency, KO health,
rear-up angle, excluded mobs...). If the animals lean the wrong way or too far, change
`standAngleDegrees` (90 = fully upright).

## How it works
- `ai/BoxingGoal` - server AI: retaliate or start a fight, walk up, punch (damage + swing animation).
- `MobBoxing` - registers the Knocked Out effect, attaches the goal to every AnimalEntity,
  and turns lethal mob-vs-mob punches into knockouts.
- `mixin/client/*` + `client/BoxerPose` - rendering: whole-body rear-up/lean/fall-over for any model,
  plus arm animation for quadruped models (cow, mooshroom, pig, sheep) and chickens.
  To support another mob's arms, add a model mixin like `QuadrupedEntityModelMixin`.
