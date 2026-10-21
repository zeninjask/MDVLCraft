package com.mdvlcraft.binder.gaunt;

import com.mdvlcraft.binder.ability.AbilityActions;
import com.mdvlcraft.binder.ability.AbilityGrants;
import com.mdvlcraft.binder.ability.SpellAbility;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.event.CurioChangeEvent;

/**
 * The Haunted Locket: worn in the Curios necklace slot (data/curios/tags/items/necklace.json), it grants Gaunt Vessel
 * (ability/AbilityGrants), so the transformation shows in the ability menu; taking it off ends the form and drops Gaunt
 * Vessel from the wheel. No recipe (it shows in JEI and the creative menu).
 */
public final class HauntedLocket {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "mdvlcraft");
    public static final RegistryObject<Item> LOCKET = ITEMS.register("haunted_locket", LocketItem::new);
    @Nullable
    private static SpellAbility gauntVessel;

    private HauntedLocket() {
    }

    public static void register(IEventBus modBus, IEventBus forgeBus) {
        ITEMS.register(modBus);
        modBus.addListener(HauntedLocket::onCreativeTab);
        forgeBus.addListener(HauntedLocket::onCurioChange);
    }

    public static boolean worn(ServerPlayer player) {
        return CuriosApi.getCuriosInventory(player).resolve().map(inventory -> inventory.isEquipped(LOCKET.get())).orElse(false);
    }

    /** The ability the locket grants. */
    public static SpellAbility ability() {
        if (gauntVessel == null) {
            AbstractSpell spell = SpellRegistry.getSpell(new ResourceLocation(GauntForm.SPELL));
            if (spell == SpellRegistry.none()) {
                throw new IllegalStateException("Spell " + GauntForm.SPELL + " is not registered");
            }
            gauntVessel = new SpellAbility(spell);
        }
        return gauntVessel;
    }

    private static void onCurioChange(CurioChangeEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && (event.getFrom().is(LOCKET.get()) || event.getTo().is(LOCKET.get()))) {
            AbilityGrants.refresh(player);
            if (!worn(player)) {
                if (GauntForm.in(player)) {
                    GauntForm.leave(player);
                }
                AbilityActions.dropUngranted(player);
            }
        }
    }

    private static void onCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(LOCKET);
        }
    }

    private static final class LocketItem extends Item {
        LocketItem() {
            super(new Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable("item.mdvlcraft.haunted_locket.desc").withStyle(ChatFormatting.DARK_AQUA));
        }
    }
}
