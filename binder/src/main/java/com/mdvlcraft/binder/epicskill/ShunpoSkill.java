package com.mdvlcraft.binder.epicskill;

import com.mdvlcraft.binder.attribute.BinderAttributes;
import com.mdvlcraft.binder.mixin.epicfight.ParryingSkillAccessor;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import reascer.wom.gameasset.WOMAnimations;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.gameasset.EpicFightSkills;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.guard.GuardSkill;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener.EventType;
import yesman.epicfight.world.entity.eventlistener.TakeDamageEvent;

/**
 * Shunpo (the Shinigami's guard skill): its owner cannot guard or block (no guard stance, no shield), with any weapon or
 * none. Pressing the guard key with Parrying's timing (an attack landing within Parrying's window after the press, the
 * same window Parrying uses, parry-window bonuses included) instead dodges it with Dodge Master's sidestep, afterimage and
 * all, for no stamina. One dodge per press; a press that dodges nothing in its window costs 10 stamina. Registered and kept off shields by ShunpoSetup (not here: Forge's event scan
 * of a GuardSkill subclass touches its client-only methods on a dedicated server).
 */
public class ShunpoSkill extends GuardSkill {
    private static final UUID LISTENER = UUID.fromString("0f3c6a52-2d87-4a5e-9a3b-6b1c2e5d8f41");
    @SuppressWarnings("unchecked")
    private static final AnimationAccessor<? extends StaticAnimation>[] DODGES = new AnimationAccessor[]{
        WOMAnimations.DODGEMASTER_BACKWARD, WOMAnimations.DODGEMASTER_LEFT, WOMAnimations.DODGEMASTER_RIGHT};
    /** Server: the tick (player.tickCount) of each owner's last guard press, until it is spent on a dodge. */
    private static final Map<UUID, Integer> PRESSED = new HashMap<>();
    /** Stamina a failed Shunpo costs: a press whose window ends without dodging anything. */
    private static final float FAIL_STAMINA = 10.0F;
    public ShunpoSkill(GuardSkill.Builder builder) {
        super(builder);
    }

    /** Any weapon, or none: Epic Fight's own checks without the guard's weapon check. */
    @Override
    public boolean canExecute(SkillContainer container) {
        return this.checkExecuteCondition(container);
    }

    @Override
    public void onInitiate(SkillContainer container) {
        super.onInitiate(container);
        var listener = container.getExecutor().getEventListener();
        // no guard: drop the guard's block, slowdown and hit listeners
        listener.removeListener(EventType.TAKE_DAMAGE_EVENT_ATTACK, EVENT_UUID);
        listener.removeListener(EventType.MOVEMENT_INPUT_EVENT, EVENT_UUID);
        listener.removeListener(EventType.DEAL_DAMAGE_EVENT_DAMAGE, EVENT_UUID);
        listener.addEventListener(EventType.TAKE_DAMAGE_EVENT_ATTACK, LISTENER, this::onAttacked);
    }

    @Override
    public void onRemoved(SkillContainer container) {
        container.getExecutor().getEventListener().removeListener(EventType.TAKE_DAMAGE_EVENT_ATTACK, LISTENER);
        PRESSED.remove(container.getExecutor().getOriginal().getUUID());
        super.onRemoved(container);
    }

    /** No guard stance on the client either. */
    @Override
    public void onInitiateClient(SkillContainer container) {
    }

    @Override
    public void onRemoveClient(SkillContainer container) {
    }

    @Override
    public void startHolding(SkillContainer container) {
        PlayerPatch<?> executor = container.getExecutor();
        if (!executor.isLogicalClient()) {
            PRESSED.put(executor.getOriginal().getUUID(), executor.getOriginal().tickCount);
        }
    }

    @Override
    public void holdTick(SkillContainer container) {
    }

    /** A press still unspent when its window ends failed: it costs FAIL_STAMINA. */
    @Override
    public void updateContainer(SkillContainer container) {
        super.updateContainer(container);
        PlayerPatch<?> executor = container.getExecutor();
        if (!executor.isLogicalClient() && executor.getOriginal() instanceof ServerPlayer player) {
            Integer pressed = PRESSED.get(player.getUUID());
            if (pressed != null && player.tickCount - pressed >= window(player)) {
                PRESSED.remove(player.getUUID());
                executor.setStamina(Math.max(0.0F, executor.getStamina() - FAIL_STAMINA));
            }
        }
    }

    private void onAttacked(TakeDamageEvent.Attack event) {
        ServerPlayerPatch patch = event.getPlayerPatch();
        ServerPlayer player = patch.getOriginal();
        Integer pressed = PRESSED.get(player.getUUID());
        if (pressed == null || player.tickCount - pressed >= window(player) || !dodgeable(event.getDamageSource())) {
            return;
        }

        PRESSED.remove(player.getUUID());
        event.setCanceled(true);
        event.setResult(AttackResult.ResultType.MISSED);
        patch.playAnimationSynchronized(DODGES[player.getRandom().nextInt(DODGES.length)], 0.0F);
    }

    /** Parrying's window, with the same parry-window bonus (mixin/epicfight/ParryingSkillMixin). */
    private static int window(ServerPlayer player) {
        int base = ((ParryingSkillAccessor)EpicFightSkills.PARRYING).mdvlcraft$parryWindow();
        return (int)Math.round(base * (1.0 + player.getAttributeValue(BinderAttributes.PARRY_WINDOW.get())));
    }

    private static boolean dodgeable(DamageSource source) {
        return source.getEntity() != null && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }
}
