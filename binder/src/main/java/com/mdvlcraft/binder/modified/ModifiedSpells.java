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
        SeraphEmbodiment::new
        ));
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
