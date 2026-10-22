package com.mdvlcraft.binder.gate;

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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * A Gate Key of one rank (plan 4.1). Used on the ground it opens a gate of that rank there, facing its user, and is used
 * up. NBT "theme" (set by the attunement recipes, which add a theme's catalyst) picks the theme; without it the theme is
 * random. Keys can be traded. A and S keys carry gate modifiers (NBT "modifiers", GateModifiers).
 */
public class GateKeyItem extends Item {
    public static final String THEME = "theme";
    private final GateRank rank;

    public GateKeyItem(GateRank rank) {
        super(new Item.Properties().stacksTo(16).rarity(GateItems.rarity(rank)));
        this.rank = rank;
    }

    public GateRank rank() {
        return this.rank;
    }

    @Nullable
    public static GateTheme theme(ItemStack stack) {
        return stack.getTag() == null ? null : GateTheme.byKey(stack.getTag().getString(THEME));
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
        if (!level.noCollision(GateEntity.spaceAt(this.rank, base))) {
            player.displayClientMessage(Component.translatable("gate.mdvlcraft.key.no_space"), true);
            return InteractionResult.FAIL;
        }
        // face the player, square to the world so the gate's box matches its ring
        float yaw = Mth.wrapDegrees(Math.round((player.getYRot() + 180.0F) / 90.0F) * 90.0F);
        ItemStack stack = context.getItemInHand();
        GateEntity gate = GateEntity.open(player, server, base, yaw, this.rank, theme(stack));
        if (gate == null) {
            return InteractionResult.FAIL;
        }
        GateModifiers.attach(gate.instanceId(), stack);   // late game: the key's modifiers go to the gate
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        GateTheme theme = theme(stack);
        tooltip.add(theme == null
            ? Component.translatable("gate.mdvlcraft.key.random").withStyle(ChatFormatting.GRAY)
            : Component.translatable("gate.mdvlcraft.key.attuned", Component.translatable("gate.mdvlcraft.theme." + theme.key())).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("gate.mdvlcraft.key.party", this.rank.partyCap).withStyle(ChatFormatting.GRAY));
        GateModifiers.tooltip(stack, tooltip);
        tooltip.add(Component.translatable("gate.mdvlcraft.key.use").withStyle(ChatFormatting.DARK_GRAY));
    }

    /** A and S keys roll their modifiers when crafted (an attuned key keeps its own); see GateModifiers. */
    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        GateModifiers.onCrafted(stack, this.rank, level, player);
    }

    /** Keys that arrive any other way (loot, commands) roll on their first tick in an inventory. */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        GateModifiers.ensureRolled(stack, this.rank, level);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return theme(stack) != null;
    }
}
