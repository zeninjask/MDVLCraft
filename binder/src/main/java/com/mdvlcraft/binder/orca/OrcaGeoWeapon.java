package com.mdvlcraft.binder.orca;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A JujutsuCraft weapon drawn by GeckoLib (Black Rope, Playful Cloud): it idles, sways while its holder sprints and
 * plays a swing animation on each swing, as in JujutsuCraft. The idle/sprint state is only sent when it changes.
 */
public class OrcaGeoWeapon extends SwordItem implements GeoItem {
    private static final String CONTROLLER = "popup_controller";
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final String id;
    private final String texture;
    /** The idle/sprint animation last sent for each holder. */
    private final Map<UUID, String> shown = new HashMap<>();

    public OrcaGeoWeapon(String id, String texture, int damage, float speed) {
        super(OrcaWeapons.CURSED_TOOL, damage, speed, OrcaWeapons.properties());
        this.id = id;
        this.texture = texture;
        software.bernie.geckolib.network.GeckoLibNetwork.registerSyncedAnimatable(this);
    }

    public ResourceLocation model() {
        return ResourceLocation.fromNamespaceAndPath("mdvlcraft", "geo/jjc/" + this.id + ".geo.json");
    }

    public ResourceLocation animation() {
        return ResourceLocation.fromNamespaceAndPath("mdvlcraft", "animations/jjc/" + this.id + ".animation.json");
    }

    public ResourceLocation texture() {
        return ResourceLocation.fromNamespaceAndPath("mdvlcraft", "textures/item/jjc/" + this.texture.replace("item/", "") + ".png");
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new com.mdvlcraft.binder.orca.client.OrcaGeoRenderer(OrcaGeoWeapon.this);
                }
                return this.renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<OrcaGeoWeapon> controller = new AnimationController<>(this, CONTROLLER, 1, state -> PlayState.STOP);
        controller.triggerableAnim("idle", RawAnimation.begin().then("idle", Animation.LoopType.LOOP));
        controller.triggerableAnim("sprint", RawAnimation.begin().then("sprint", Animation.LoopType.LOOP));
        controller.triggerableAnim("swing", RawAnimation.begin().thenPlay("swing"));
        if (this.id.equals("playful_cloud")) {
            controller.triggerableAnim("swing2", RawAnimation.begin().thenPlay("swing2"));
        }
        controllers.add(controller);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        if (entity.getMainHandItem() == stack && entity.level() instanceof ServerLevel level) {
            String swing = this.id.equals("playful_cloud") && entity.getRandom().nextBoolean() ? "swing2" : "swing";
            this.triggerAnim(entity, GeoItem.getOrAssignId(stack, level), CONTROLLER, swing);
            this.shown.remove(entity.getUUID());
        }
        return super.onEntitySwing(stack, entity);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (selected && entity instanceof LivingEntity living && level instanceof ServerLevel serverLevel && entity.tickCount % 10 == 0) {
            String anim = living.isSprinting() ? "sprint" : "idle";
            if (!anim.equals(this.shown.get(entity.getUUID()))) {
                this.shown.put(entity.getUUID(), anim);
                this.triggerAnim(entity, GeoItem.getOrAssignId(stack, serverLevel), CONTROLLER, anim);
            }
        }
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        OrcaWeapons.tooltip(this.id, tooltip);
    }
}
