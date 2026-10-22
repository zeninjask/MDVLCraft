package com.mdvlcraft.binder.gate.tower;

import com.mdvlcraft.binder.gate.GateDimensions;
import com.mdvlcraft.binder.gate.GateEntity;
import com.mdvlcraft.binder.gate.GateInstances;
import com.mdvlcraft.binder.gate.GateRank;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * The Castle Sigil (plan 10.1): used on the top of a block it opens a dark-red castle gate there, facing its user, into a
 * new Demon Castle run, the way a key opens a gate; it is not used up. The run starts at the user's saved floor, or
 * sneaking, at their checkpoint (to farm lower floors); allies standing near count too, so the run starts at the lowest
 * floor among them (plan 10.3). Tradeable like keys. One open castle gate per player.
 */
public class CastleSigilItem extends Item {
    public CastleSigilItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (context.getClickedFace() != Direction.UP) {
            return InteractionResult.PASS;
        }
        BlockPos base = context.getClickedPos().above();
        if (!(context.getPlayer() instanceof ServerPlayer player) || !(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        if (GateDimensions.isGate(level.dimension())) {
            player.displayClientMessage(Component.translatable("gate.mdvlcraft.key.inside"), true);
            return InteractionResult.FAIL;
        }
        if (GateInstances.get(player.server).ofPlayer(player.getUUID()) != null) {
            player.displayClientMessage(Component.translatable("gate.mdvlcraft.other_gate"), true);
            return InteractionResult.FAIL;
        }
        if (TowerRun.ownedBy(player.server, player.getUUID()) != null) {
            player.displayClientMessage(Component.translatableWithFallback("tower.mdvlcraft.already_open", "Your castle gate is already open."), true);
            return InteractionResult.FAIL;
        }
        if (!level.noCollision(GateEntity.spaceAt(GateRank.S, base))) {
            player.displayClientMessage(Component.translatable("gate.mdvlcraft.key.no_space"), true);
            return InteractionResult.FAIL;
        }
        TowerRun run = TowerRun.open(player, player.isShiftKeyDown());
        if (run == null) {
            return InteractionResult.FAIL;
        }
        float yaw = Mth.wrapDegrees(Math.round((player.getYRot() + 180.0F) / 90.0F) * 90.0F);
        CastleGateEntity.place(server, base, yaw, run.id);
        player.displayClientMessage(Component.translatableWithFallback("tower.mdvlcraft.opened", "The Demon Castle opens: floor %s.", run.floor()), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatableWithFallback("tower.mdvlcraft.sigil.use", "Use on the ground: open the Demon Castle at your floor")
            .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatableWithFallback("tower.mdvlcraft.sigil.sneak", "Sneak-use: start from your checkpoint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
