package com.mdvlcraft.binder.modified;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import com.mdvlcraft.binder.ability.StanceElement;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;

/** Registers every built Modified ability as an Iron's spell; each must have an entry in modified_abilities.json and vice versa. */
public final class ModifiedSpells {
    private static final DeferredRegister<AbstractSpell> SPELLS = DeferredRegister.create(SpellRegistry.SPELL_REGISTRY_KEY, "mdvlcraft");
    private static final List<Supplier<ModifiedSpell>> ALL = all();

    private static List<Supplier<ModifiedSpell>> all() {
        List<Supplier<ModifiedSpell>> all = new ArrayList<>(List.of(
        MartialSpells.ShieldBash::new, MartialSpells.Provoke::new, MartialSpells.Javelin::new, MartialSpells.Iaido::new,
        MartialSpells.TwinFang::new, MartialSpells.QuakeSlam::new,
        BuffSpells.WarFrenzy::new, BuffSpells.Envenom::new, BuffSpells.DeathMark::new, BuffSpells.SmokeBomb::new, BuffSpells.StillMind::new,
        SummonSpells.BlazeSpirits::new, SummonSpells.SnowSentinels::new, SummonSpells.TideGuardians::new, SummonSpells.StormHounds::new,
        SummonSpells.WolfPack::new, SummonSpells.GuardianAngel::new, SummonSpells.SculkWatcher::new, SummonSpells.Treant::new,
        SummonSpells.Terracotta::new, SummonSpells.HuntingHawk::new, SummonSpells.PackMule::new,
        UtilitySpells.Rally::new, UtilitySpells.Campfire::new, UtilitySpells.Pickpocket::new,
        // batch 2: the Samurai and Monk vocations
        BodySpells.AdrenalSurge::new, BodySpells.EmberFists::new, BodySpells.IronBody::new, BodySpells.JiangshiFists::new,
        BodySpells.Mushin::new, BodySpells.ColdMeditation::new,
        StrikeSpells.CraneLeap::new, StrikeSpells.FlameRush::new, StrikeSpells.WhirlpoolKick::new, StrikeSpells.KunaiRain::new,
        StrikeSpells.JadeArrow::new, StrikeSpells.KiWave::new,
        CloneSpells.MirrorImages::new, CloneSpells.ShadowClone::new, CloneSpells.SpiritStep::new,
        SummonSpells.Oni::new, SummonSpells.SteppeHorse::new, UtilitySpells.Lotus::new, UtilitySpells.TrailRations::new,
        // the Lightning base, and Wukong's Shadow Clone
        StrikeSpells.ThunderStep::new, CloneSpells.BodyOutsideOfBody::new,
        // batch 3: skins, shields, charges, steps, daggers and the blood buffs
        SkinSpells::emberSkin, SkinSpells::frostArmor, SkinSpells::jadeSkin, SkinSpells::entStride,
        SkinSpells.ShadowGuard::new, SkinSpells.HolyShield::new,
        ChargeSpells.FrostCharge::new, ChargeSpells.ThunderCharge::new, ChargeSpells.TideCharge::new,
        DaggerSpells.FlashStep::new, DaggerSpells.MistStep::new, DaggerSpells.FlameStep::new,
        DaggerSpells::iceDagger, DaggerSpells::radiantDagger, DaggerSpells::voidShiv,
        DaggerSpells.Immolate::new, DaggerSpells.VampireBite::new,
        BuffSpells.Bloodlust::new, BuffSpells.BloodFrenzy::new,
        // not in any tree yet
        SeraphEmbodiment::new,
        // batch 4: summons, mounts and field abilities
        UndeadSummons.SkeletonArchers::new, UndeadSummons.BoneGuard::new, UndeadSummons.BurningDead::new, UndeadSummons.Reanimate::new,
        UndeadSummons.Strays::new, UndeadSummons.DrownedLegion::new, UndeadSummons.PirateCrew::new, UndeadSummons.DrownedHorde::new,
        UndeadSummons.SoulReaper::new,
        BeastSummons.FrostWolves::new, BeastSummons.SpiderBrood::new, BeastSummons.ShadowPack::new, BeastSummons.VoidHound::new,
        BeastSummons.SpiritWolf::new, BeastSummons.SpiritTiger::new, BeastSummons.Bear::new, BeastSummons.FallenAngel::new,
        BeastSummons.FrostAngel::new, BeastSummons.SnowSpirits::new, BeastSummons.Thunderbird::new, BeastSummons.Magpies::new,
        BeastSummons.StormCrows::new,
        MountSpells.BoneSteed::new, MountSpells.Camel::new, MountSpells.StormSteed::new, MountSpells.VoidStrider::new,
        MountSpells.SpectralBoat::new,
        FieldSpells.CorpseFlower::new, FieldSpells.Ballista::new, FieldSpells.Fence::new, FieldSpells.JadeSerpent::new,
        FieldSpells.RiverDragon::new,
        // batch 5: bolts, strikes and blades
        SmiteSpells.ChaosBolt::new, SmiteSpells.SmiteBolt::new, SmiteSpells.Superconductor::new, SmiteSpells.StaticSnare::new,
        SmiteSpells.Drown::new, SmiteSpells.Geyser::new, SmiteSpells.SoulHarvest::new,
        BladeSpells.SoulRend::new, BladeSpells.PhantomDraw::new, BladeSpells.LeapOfFaith::new,
        // batch 6
        SmiteSpells.BurningRoots::new,
        // wave 2 (two abilities per vocation): Underworld Gate here, the rest in Wave2<Group>.all() below
        UnderworldGate::new
        ));
        BoltSpells.all().forEach(bolt -> all.add(() -> bolt));
        ConeSpells.all().forEach(cone -> all.add(() -> cone));
        VolleySpells.all().forEach(volley -> all.add(() -> volley));
        GroundSpells.all().forEach(ground -> all.add(() -> ground));
        WallSpells.all().forEach(wall -> all.add(() -> wall));
        Wave2Necromancer.all().forEach(spell -> all.add(() -> spell));
        Wave2Berserker.all().forEach(spell -> all.add(() -> spell));
        Wave2Guardian.all().forEach(spell -> all.add(() -> spell));
        Wave2Brute.all().forEach(spell -> all.add(() -> spell));
        Wave2Assassin.all().forEach(spell -> all.add(() -> spell));
        Wave2Samurai.all().forEach(spell -> all.add(() -> spell));
        FormSpells.all().forEach(form -> all.add(() -> form));
        BoonSpells.all().forEach(boon -> all.add(() -> boon));
        SpiritSummons.all().forEach(summon -> all.add(() -> summon));
        BrandSpells.all().forEach(brand -> all.add(() -> brand));
        ArrowSpells.all().forEach(arrow -> all.add(() -> arrow));
        PalmSpells.all().forEach(palm -> all.add(() -> palm));
        Arrays.stream(StanceElement.values()).filter(e -> e.form != null).forEach(e -> all.add(() -> new StanceForms.Form(e)));
        return all;
    }

    private ModifiedSpells() {
    }

    public static void register(IEventBus modBus) {
        Set<String> built = new TreeSet<>();
        for (Supplier<ModifiedSpell> factory : ALL) {
            ModifiedSpell spell = factory.get();
            String key = spell.getSpellResource().getPath();
            if (!built.add(key)) {
                throw new IllegalStateException("Modified ability " + key + " has two spell classes");
            }
            SPELLS.register(key, () -> spell);
        }
        Set<String> configured = new TreeSet<>(ModifiedAbilities.all().keySet());
        if (!built.equals(configured)) {
            throw new IllegalStateException("modified_abilities.json and the spell classes disagree: "
                + configured.stream().filter(k -> !built.contains(k)).collect(Collectors.joining(", ", "no class for [", "]"))
                + built.stream().filter(k -> !configured.contains(k)).collect(Collectors.joining(", ", ", not built.py: [", "]")));
        }
        SPELLS.register(modBus);
        ModifiedEntities.register(modBus);
    }
}
