package com.mdvlcraft.binder.gate;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/**
 * The boss chest's contents: one 27-slot container per player, rolled from the gate's boss loot table when the gate is
 * cleared. A player opening the chest sees only their own (a plain vanilla chest screen, so no client code). Leftovers
 * aren't lost: once their owner is in another world, or the chest is removed, they go into the owner's inventory (or drop
 * at their feet). An owner who is offline then loses them.
 */
public class GateRewardChestBlockEntity extends BlockEntity {
    private static final int SIZE = 27;
    private final Map<UUID, SimpleContainer> loot = new HashMap<>();

    public GateRewardChestBlockEntity(BlockPos pos, BlockState state) {
        super(GateRewards.REWARD_CHEST_ENTITY.get(), pos, state);
    }

    /** Rolls `table` into a fresh container for this player. */
    void roll(ServerPlayer player, ResourceLocation table) {
        if (!(this.level instanceof ServerLevel level)) {
            return;
        }
        LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(this.worldPosition))
            .withParameter(LootContextParams.THIS_ENTITY, player).withLuck(player.getLuck()).create(LootContextParamSets.CHEST);
        SimpleContainer container = container(player.getUUID());
        level.getServer().getLootData().getLootTable(table).fill(container, params, level.random.nextLong());
        this.setChanged();
    }

    private SimpleContainer container(UUID owner) {
        return this.loot.computeIfAbsent(owner, uuid -> {
            SimpleContainer container = new SimpleContainer(SIZE) {
                @Override
                public boolean stillValid(Player player) {
                    return !GateRewardChestBlockEntity.this.isRemoved() && player.distanceToSqr(Vec3.atCenterOf(GateRewardChestBlockEntity.this.worldPosition)) < 64.0;
                }
            };
            container.addListener(c -> this.setChanged());
            return container;
        });
    }

    void open(ServerPlayer player) {
        SimpleContainer container = this.loot.get(player.getUUID());
        if (container == null) {
            player.displayClientMessage(Component.translatable("gate.mdvlcraft.chest.not_yours"), true);
            return;
        }
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> ChestMenu.threeRows(id, inventory, container),
            Component.translatable("block.mdvlcraft.gate_reward_chest")));
        player.level().playSound(null, this.worldPosition, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.5F, 1.0F);
    }

    /** Once a second: owners who have left this world get their leftovers. */
    void serverTick() {
        if (this.level == null || this.level.getGameTime() % 20 != 0 || this.loot.isEmpty()) {
            return;
        }
        for (Iterator<Map.Entry<UUID, SimpleContainer>> it = this.loot.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, SimpleContainer> entry = it.next();
            ServerPlayer owner = this.level.getServer().getPlayerList().getPlayer(entry.getKey());
            if (owner != null && owner.level() != this.level) {
                give(owner, entry.getValue());
                it.remove();
                this.setChanged();
            }
        }
    }

    /** The chest is going away: every online owner gets their leftovers. */
    void deliverAll() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        this.loot.forEach((uuid, container) -> {
            ServerPlayer owner = this.level.getServer().getPlayerList().getPlayer(uuid);
            if (owner != null) {
                give(owner, container);
            }
        });
        this.loot.clear();
    }

    private static void give(ServerPlayer owner, SimpleContainer container) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.removeItemNoUpdate(i);
            if (!stack.isEmpty()) {
                owner.getInventory().placeItemBackInInventory(stack);
            }
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.loot.clear();
        CompoundTag players = tag.getCompound("players");
        for (String key : players.getAllKeys()) {
            SimpleContainer container = this.container(UUID.fromString(key));
            NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(players.getCompound(key), items);
            for (int i = 0; i < SIZE; i++) {
                container.setItem(i, items.get(i));
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        CompoundTag players = new CompoundTag();
        this.loot.forEach((uuid, container) -> {
            NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
            for (int i = 0; i < SIZE; i++) {
                items.set(i, container.getItem(i));
            }
            players.put(uuid.toString(), ContainerHelper.saveAllItems(new CompoundTag(), items));
        });
        tag.put("players", players);
    }
}
