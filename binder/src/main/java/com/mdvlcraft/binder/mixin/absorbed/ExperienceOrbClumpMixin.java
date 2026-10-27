package com.mdvlcraft.binder.mixin.absorbed;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * From Clumps (Jared, MIT): experience orbs merge into one orb that remembers every orb's value (value -> how many), so
 * a pile of orbs is one entity. Touching it gives (and mends with) all of them at once, with no pickup delay.
 */
@Mixin(value = ExperienceOrb.class, priority = 1001)
public abstract class ExperienceOrbClumpMixin extends Entity {
    @Shadow
    private int count;
    @Shadow
    private int age;
    @Shadow
    private int value;
    /** value -> how many orbs of it this orb holds. */
    @Unique
    private Map<Integer, Integer> mdvlcraft$clumped;
    @Unique
    private Entry<EquipmentSlot, ItemStack> mdvlcraft$mending;

    @Shadow
    protected abstract int repairPlayerItems(Player player, int value);

    @Shadow
    protected abstract int durabilityToXp(int durability);

    protected ExperienceOrbClumpMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Inject(method = "canMerge(Lnet/minecraft/world/entity/ExperienceOrb;)Z", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$anyOrbMerges(ExperienceOrb other, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(other.isAlive() && !this.is(other));
    }

    @Inject(method = "canMerge(Lnet/minecraft/world/entity/ExperienceOrb;II)Z", at = @At("HEAD"), cancellable = true)
    private static void mdvlcraft$anyOrbMergesWith(ExperienceOrb orb, int id, int value, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(orb.isAlive());
    }

    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$takeAll(Player player, CallbackInfo ci) {
        if (this.level().isClientSide) {
            return;
        }
        ci.cancel();
        if (MinecraftForge.EVENT_BUS.post(new PlayerXpEvent.PickupXp(player, (ExperienceOrb)(Object)this))) {
            return;
        }
        player.takeXpDelay = 0;
        player.take(this, 1);
        if (this.value != 0 || this.mdvlcraft$resolve()) {
            int give = 0;
            for (Entry<Integer, Integer> entry : this.mdvlcraft$clumped().entrySet()) {
                for (int i = 0; i < entry.getValue(); i++) {
                    give += Math.max(0, this.repairPlayerItems(player, entry.getKey()));
                }
            }
            if (give > 0) {
                player.giveExperiencePoints(give);
            }
        }
        this.discard();
    }

    @ModifyVariable(method = "repairPlayerItems", index = 3, at = @At(value = "INVOKE_ASSIGN",
        target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getRandomItemWith(Lnet/minecraft/world/item/enchantment/Enchantment;Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Predicate;)Ljava/util/Map$Entry;"))
    private Entry<EquipmentSlot, ItemStack> mdvlcraft$captureMending(Entry<EquipmentSlot, ItemStack> entry) {
        this.mdvlcraft$mending = entry;
        return entry;
    }

    /** Mending repairs with the whole value, then spends what is left on the next damaged item. */
    @Inject(method = "repairPlayerItems", cancellable = true, at = @At(value = "INVOKE_ASSIGN",
        target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getRandomItemWith(Lnet/minecraft/world/item/enchantment/Enchantment;Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Predicate;)Ljava/util/Map$Entry;"))
    private void mdvlcraft$repairAll(Player player, int value, CallbackInfoReturnable<Integer> cir) {
        if (this.mdvlcraft$mending == null) {
            cir.setReturnValue(value);
            return;
        }
        ItemStack item = this.mdvlcraft$mending.getValue();
        int repaired = Math.min((int)(value * item.getXpRepairRatio()), item.getDamageValue());
        item.setDamageValue(item.getDamageValue() - repaired);
        int left = value - this.durabilityToXp(repaired);
        cir.setReturnValue(left > 0 ? this.repairPlayerItems(player, left) : 0);
    }

    @Inject(method = "merge", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ExperienceOrb;discard()V"), cancellable = true)
    private void mdvlcraft$mergeClumps(ExperienceOrb other, CallbackInfo ci) {
        ExperienceOrbClumpMixin that = (ExperienceOrbClumpMixin)(Object)other;
        Map<Integer, Integer> merged = new HashMap<>(this.mdvlcraft$clumped());
        that.mdvlcraft$clumped().forEach((value, amount) -> merged.merge(value, amount, Integer::sum));
        this.age = Math.min(this.age, that.age);
        this.mdvlcraft$set(merged);
        other.discard();
        ci.cancel();
    }

    @Inject(method = "tryMergeToExisting", at = @At("HEAD"), cancellable = true)
    private static void mdvlcraft$mergeIntoNearby(ServerLevel level, Vec3 pos, int value, CallbackInfoReturnable<Boolean> cir) {
        List<ExperienceOrb> orbs = level.getEntities(EntityTypeTest.forClass(ExperienceOrb.class), AABB.ofSize(pos, 1.0, 1.0, 1.0), Entity::isAlive);
        if (orbs.isEmpty()) {
            cir.setReturnValue(false);
            return;
        }
        ExperienceOrbClumpMixin orb = (ExperienceOrbClumpMixin)(Object)orbs.get(0);
        Map<Integer, Integer> merged = new HashMap<>(orb.mdvlcraft$clumped());
        merged.merge(value, 1, Integer::sum);
        orb.mdvlcraft$set(merged);
        orb.age = 0;
        cir.setReturnValue(true);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void mdvlcraft$save(CompoundTag tag, CallbackInfo ci) {
        if (this.mdvlcraft$clumped != null) {
            CompoundTag map = new CompoundTag();
            this.mdvlcraft$clumped.forEach((value, amount) -> map.putInt(String.valueOf(value), amount));
            tag.put("clumpedMap", map);
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void mdvlcraft$load(CompoundTag tag, CallbackInfo ci) {
        Map<Integer, Integer> map = new HashMap<>();
        if (tag.contains("clumpedMap")) {
            CompoundTag saved = tag.getCompound("clumpedMap");
            saved.getAllKeys().forEach(key -> map.put(Integer.parseInt(key), saved.getInt(key)));
        } else {
            map.put(this.value, this.count);
        }
        this.mdvlcraft$set(map);
    }

    @Unique
    private Map<Integer, Integer> mdvlcraft$clumped() {
        if (this.mdvlcraft$clumped == null) {
            this.mdvlcraft$clumped = new HashMap<>();
            this.mdvlcraft$clumped.put(this.value, 1);
        }
        return this.mdvlcraft$clumped;
    }

    /** The clumped orbs, their total value and their count. */
    @Unique
    private void mdvlcraft$set(Map<Integer, Integer> clumped) {
        this.mdvlcraft$clumped = clumped;
        this.count = clumped.values().stream().mapToInt(Integer::intValue).sum();
        this.mdvlcraft$resolve();
    }

    @Unique
    private boolean mdvlcraft$resolve() {
        this.value = this.mdvlcraft$clumped().entrySet().stream().mapToInt(entry -> entry.getKey() * entry.getValue()).sum();
        return this.value > 0;
    }
}
