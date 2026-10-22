package com.mdvlcraft.binder.gate;

import com.mdvlcraft.binder.gate.GateInstances.Instance;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * The Return Stone (plan 6.1): used inside a normal gate it sends its user out at once, keeping their roster place, and is
 * used up. It does nothing in red gates or the Demon Castle. Bought at the Gate Forge, sometimes found in boss chests.
 */
public class ReturnStoneItem extends Item {
    public ReturnStoneItem() {
        super(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.success(stack);
        }
        Instance instance = GateInstances.get(serverPlayer.server).ofPlayer(player.getUUID());
        if (!GateDimensions.isGate(level.dimension()) || level.dimension() == GateDimensions.DEMON_CASTLE || instance == null
            || instance.dimension != level.dimension()) {
            player.displayClientMessage(Component.translatable("gate.mdvlcraft.return_stone.outside"), true);
            return InteractionResultHolder.fail(stack);
        }
        if (instance.red) {
            player.displayClientMessage(Component.translatable("gate.mdvlcraft.return_stone.red"), true);
            return InteractionResultHolder.fail(stack);
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        GateTravel.leave(serverPlayer, instance, false);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.mdvlcraft.return_stone.desc").withStyle(ChatFormatting.GRAY));
    }
}
