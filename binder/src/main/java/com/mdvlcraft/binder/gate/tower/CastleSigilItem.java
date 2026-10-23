package com.mdvlcraft.binder.gate.tower;

import com.mdvlcraft.binder.gate.GateDimensions;
import com.mdvlcraft.binder.gate.GateEntity;
import com.mdvlcraft.binder.gate.GateInstances;
import com.mdvlcraft.binder.gate.GateRank;
import com.mdvlcraft.binder.network.BinderNetwork;
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
import net.minecraft.world.phys.Vec3;

/**
 * The Castle Sigil (plan 10.1): used on the top of a block it opens a dark-red castle gate there, facing its user, into a
 * new Demon Castle run, the way a key opens a gate; it is not used up. Using it opens the start menu (plan 10.3,
 * CastleMenuPacket -> client/CastleStartScreen -> CastleStartPacket): continue from the reached floor, or start at any
 * block start up to the checkpoint (1, 6, 11 ... to farm lower floors). Sneak-use is a shortcut for the checkpoint.
 * Allies standing near count too, so the run starts at the lowest reached floor among them. Tradeable like keys. One
 * open castle gate per player.
 */
public class CastleSigilItem extends Item {
    /** How far the player may be from the gate spot when the menu's choice arrives. */
    private static final double MENU_RANGE = 8.0;

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
        Component refusal = refusal(player, server, base);
        if (refusal != null) {
            player.displayClientMessage(refusal, true);
            return InteractionResult.FAIL;
        }
        TowerProgress.Entry entry = TowerProgress.get(player.server).of(player.getUUID());
        if (player.isShiftKeyDown()) {
            return start(player, base, entry.checkpoint) != null ? InteractionResult.CONSUME : InteractionResult.FAIL;
        }
        BinderNetwork.sendTo(player, new CastleMenuPacket(base, entry.reached, entry.checkpoint));
        return InteractionResult.CONSUME;
    }

    /** Why a castle gate can't open at `base` now, or null if it can. */
    @Nullable
    private static Component refusal(ServerPlayer player, ServerLevel level, BlockPos base) {
        if (GateDimensions.isGate(level.dimension())) {
            return Component.translatable("gate.mdvlcraft.key.inside");
        }
        if (GateInstances.get(player.server).ofPlayer(player.getUUID()) != null) {
            return Component.translatable("gate.mdvlcraft.other_gate");
        }
        if (TowerRun.ownedBy(player.server, player.getUUID()) != null) {
            return Component.translatableWithFallback("tower.mdvlcraft.already_open", "Your castle gate is already open.");
        }
        if (!level.noCollision(GateEntity.spaceAt(GateRank.S, base))) {
            return Component.translatable("gate.mdvlcraft.key.no_space");
        }
        return null;
    }

    /** The floors the menu offers: the reached floor, and every block start (1, 6, 11 ...) up to the checkpoint. */
    public static boolean allowed(TowerProgress.Entry entry, int floor) {
        return floor == entry.reached || floor >= 1 && floor <= entry.checkpoint && TowerFloor.blockStart(floor) == floor;
    }

    /** The menu's choice (CastleStartPacket): checked again on the server, since the client could send anything. */
    static void menuChoice(ServerPlayer player, BlockPos base, int floor) {
        boolean holding = player.getMainHandItem().getItem() instanceof CastleSigilItem || player.getOffhandItem().getItem() instanceof CastleSigilItem;
        if (holding && player.position().distanceTo(Vec3.atBottomCenterOf(base)) <= MENU_RANGE) {
            start(player, base, floor);
        }
    }

    /**
     * Opens a run at `floor` (lowered for allies who have reached less) and its castle gate at `base`, facing the player.
     * Null, with a message, if the floor isn't one the player may pick or the gate can't open there.
     */
    @Nullable
    public static TowerRun start(ServerPlayer player, BlockPos base, int floor) {
        ServerLevel server = player.serverLevel();
        Component refusal = allowed(TowerProgress.get(player.server).of(player.getUUID()), floor) ? refusal(player, server, base)
            : Component.translatableWithFallback("tower.mdvlcraft.floor_locked", "You can't start the castle at floor %s.", floor);
        if (refusal != null) {
            player.displayClientMessage(refusal, true);
            return null;
        }
        TowerRun run = TowerRun.open(player, floor);
        if (run == null) {
            return null;
        }
        float yaw = Mth.wrapDegrees(Math.round((player.getYRot() + 180.0F) / 90.0F) * 90.0F);
        CastleGateEntity.place(server, base, yaw, run.id);
        player.displayClientMessage(Component.translatableWithFallback("tower.mdvlcraft.opened", "The Demon Castle opens: floor %s.", run.floor()), true);
        return run;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatableWithFallback("tower.mdvlcraft.sigil.use", "Use on the ground: open the Demon Castle (pick your start floor)")
            .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatableWithFallback("tower.mdvlcraft.sigil.sneak", "Sneak-use: start from your checkpoint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
