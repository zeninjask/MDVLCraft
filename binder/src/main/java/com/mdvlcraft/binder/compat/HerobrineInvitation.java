package com.mdvlcraft.binder.compat;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

/** When Herobrine vanishes, the player he was watching has a 1% chance to find the Mimic's Invitation on them. */
public final class HerobrineInvitation {
    private static final ResourceLocation HEROBRINE = new ResourceLocation("true_herobrine", "herobrine");
    private static final ResourceLocation INVITATION = new ResourceLocation("mimic", "mimic_invitation");
    private static final float CHANCE = 0.01F;
    private static final double RANGE = 256.0;

    private HerobrineInvitation() {
    }

    public static void register() {
        if (ModList.get().isLoaded(HEROBRINE.getNamespace()) && ModList.get().isLoaded(INVITATION.getNamespace())) {
            MinecraftForge.EVENT_BUS.addListener(HerobrineInvitation::onLeaveLevel);
        }
    }

    /** Herobrine vanishes by discarding himself (when a player comes close); chunk unloads are not vanishing. */
    private static void onLeaveLevel(EntityLeaveLevelEvent event) {
        Entity entity = event.getEntity();
        if (event.getLevel().isClientSide() || entity.getRemovalReason() != Entity.RemovalReason.DISCARDED
            || !HEROBRINE.equals(ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()))) {
            return;
        }
        Player nearest = event.getLevel().getNearestPlayer(entity, RANGE);
        if (nearest instanceof ServerPlayer player && !player.isSpectator() && player.getRandom().nextFloat() < CHANCE) {
            Item invitation = ForgeRegistries.ITEMS.getValue(INVITATION);
            if (invitation == null || !INVITATION.equals(ForgeRegistries.ITEMS.getKey(invitation))) {
                throw new IllegalStateException("Item " + INVITATION + " is missing from the pack");
            }
            ItemStack stack = new ItemStack(invitation);
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
            player.connection.send(new ClientboundSetTitlesAnimationPacket(20, 60, 30));
            player.connection.send(new ClientboundSetTitleTextPacket(
                Component.translatable("message.mdvlcraft.mimic_invitation").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC)));
            player.level().playSound(null, player.blockPosition(), SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 1.0F, 0.8F);
        }
    }
}
