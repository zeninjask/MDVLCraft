package com.mdvlcraft.binder.orca;

import com.hm.efn.gameasset.EFNSkills;
import com.mdvlcraft.binder.MDVLBinder;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.collider.MultiOBBCollider;
import yesman.epicfight.api.forgeevent.WeaponCapabilityPresetRegistryEvent;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.gameasset.EpicFightSkills;
import yesman.epicfight.gameasset.EpicFightSounds;
import yesman.epicfight.particle.EpicFightParticles;
import yesman.epicfight.skill.guard.GuardSkill;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.CapabilityItem.Styles;
import yesman.epicfight.world.capabilities.item.WeaponCapability;
import yesman.epicfight.world.capabilities.item.WeaponCategory;

/**
 * Playful Cloud's Epic Fight moveset (weapon type mdvlcraft:jjc_playful_cloud): other addons' animations, picked in the
 * Playful Cloud Moveset picker (tools/playful_cloud), with the staff drawn across both hands by client/PlayfulCloudRenderer.
 * Its own category lets the guard skills and the Soulslike dodge (EFNDodgeSkillMixin) give it its own guard, parries and
 * roll. The weapon art is JjkSkill's Cloud Crusher.
 */
public final class PlayfulCloudSetup {
    /** Playful Cloud's Epic Fight weapon category. */
    public enum Category implements WeaponCategory {
        PLAYFUL_CLOUD;

        private final int id;

        Category() {
            this.id = WeaponCategory.ENUM_MANAGER.assign(this);
        }

        @Override
        public int universalOrdinal() {
            return this.id;
        }
    }

    private PlayfulCloudSetup() {
    }

    public static void register(IEventBus modBus) {
        WeaponCategory.ENUM_MANAGER.registerEnumCls("mdvlcraft", Category.class);
        modBus.addListener(PlayfulCloudSetup::onWeaponPresets);
        modBus.addListener(PlayfulCloudSetup::onLoadComplete);
    }

    /** An animation another mod registers, by id. */
    @SuppressWarnings("unchecked")
    public static <T extends StaticAnimation> AnimationAccessor<T> anim(String id) {
        return (AnimationAccessor<T>)AnimationManager.byKey(id);
    }

    /** The Soulslike dodge's roll for a direction (EFNDodgeSkill's forward, back, left, right). */
    public static AnimationAccessor<? extends StaticAnimation> roll(String direction) {
        return anim("cdmoveset:biped/new/dodge/psstep/roll_" + direction);
    }

    private static void onWeaponPresets(WeaponCapabilityPresetRegistryEvent event) {
        event.getTypeEntry().put(ResourceLocation.fromNamespaceAndPath("mdvlcraft", "jjc_playful_cloud"), item -> {
            String scythe = "efn:biped/scythe/combat/scythe_";
            AnimationAccessor<? extends AttackAnimation>[] combo = new AnimationAccessor[] {
                anim(scythe + "auto1"), anim(scythe + "auto2"), anim(scythe + "auto3"), anim(scythe + "auto4"), anim(scythe + "auto5"),
                anim(scythe + "dash"), anim(scythe + "airslash")};
            return WeaponCapability.builder()
                .category(Category.PLAYFUL_CLOUD)
                .styleProvider(patch -> Styles.TWO_HAND)
                .collider(new MultiOBBCollider(3, 0.4, 0.4, 0.8, 0.0, 0.0, -0.75))
                .swingSound(EpicFightSounds.WHOOSH_ROD.get())
                .hitSound(EpicFightSounds.BLUNT_HIT.get())
                .hitParticle(EpicFightParticles.HIT_BLUNT.get())
                .canBePlacedOffhand(false)
                .newStyleCombo(Styles.TWO_HAND, combo)
                .newStyleCombo(Styles.MOUNT, Animations.SWORD_MOUNT_ATTACK)
                .livingMotionModifier(Styles.TWO_HAND, LivingMotions.IDLE, anim("wom:biped/living/staff_idle"))
                .livingMotionModifier(Styles.TWO_HAND, LivingMotions.WALK, anim("cdmoveset:biped/new/dual_greatsword/living/greatsword_dual_walk"))
                .livingMotionModifier(Styles.TWO_HAND, LivingMotions.RUN, anim("cdmoveset:biped/new/dual_greatsword/living/greatsword_dual_run"))
                .livingMotionModifier(Styles.TWO_HAND, LivingMotions.JUMP, anim("epicfightx:biped/living/jump_move"))
                .livingMotionModifier(Styles.TWO_HAND, LivingMotions.BLOCK, anim("wom:biped/skill/solar_guard"));
        });
    }

    private static void onLoadComplete(FMLLoadCompleteEvent event) {
        event.enqueueWork(PlayfulCloudSetup::addGuards);
    }

    /** Guard hit, guard break and the two parries for every guard skill a player may have. */
    private static void addGuards() {
        List<AnimationAccessor<? extends StaticAnimation>> parries = List.of(
            anim("wom:biped/skill/herrscher_guard_parry"), anim("wom:biped/skill/solar_guard_parry"));
        for (Object skill : new Object[] {EFNSkills.EFN_PARRY, EpicFightSkills.PARRYING, EpicFightSkills.GUARD}) {
            if (!(skill instanceof GuardSkill guard)) {
                continue;
            }
            try {
                motions(guard, "guardMotions").put(Category.PLAYFUL_CLOUD, (item, player) -> anim("wom:biped/skill/solar_guard_hit"));
                motions(guard, "guardBreakMotions").put(Category.PLAYFUL_CLOUD, (item, player) -> Animations.BIPED_COMMON_NEUTRALIZED);
                if (skill != EpicFightSkills.GUARD) {
                    motions(guard, "advancedGuardMotions").put(Category.PLAYFUL_CLOUD, (item, player) -> parries);
                }
            } catch (ReflectiveOperationException | RuntimeException e) {
                MDVLBinder.LOGGER.warn("Could not give {} Playful Cloud's guard", skill, e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<WeaponCategory, BiFunction<CapabilityItem, PlayerPatch<?>, ?>> motions(GuardSkill skill, String field)
            throws ReflectiveOperationException {
        Field f = GuardSkill.class.getDeclaredField(field);
        f.setAccessible(true);
        return (Map<WeaponCategory, BiFunction<CapabilityItem, PlayerPatch<?>, ?>>)f.get(skill);
    }
}
