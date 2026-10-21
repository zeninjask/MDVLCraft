package com.mdvlcraft.binder.raiju;

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
 * Raijū's Fang: worn in the Curios necklace slot (data/curios/tags/items/necklace.json), it grants Yakusanoikazuchi
 * (ability/AbilityGrants), so the transformation shows in the ability menu; taking it off ends the form and drops it from
 * the wheel. No recipe (it shows in JEI and the creative menu).
 */
public final class RaijuFang {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "mdvlcraft");
    public static final RegistryObject<Item> FANG = ITEMS.register("raiju_fang", FangItem::new);
    @Nullable
    private static SpellAbility yakusanoikazuchi;

    private RaijuFang() {
    }

    public static void register(IEventBus modBus, IEventBus forgeBus) {
        ITEMS.register(modBus);
        modBus.addListener(RaijuFang::onCreativeTab);
        forgeBus.addListener(RaijuFang::onCurioChange);
    }

    public static boolean worn(ServerPlayer player) {
        return CuriosApi.getCuriosInventory(player).resolve().map(inventory -> inventory.isEquipped(FANG.get())).orElse(false);
    }

    /** The ability the fang grants. */
    public static SpellAbility ability() {
        if (yakusanoikazuchi == null) {
            AbstractSpell spell = SpellRegistry.getSpell(new ResourceLocation(RaijuForm.SPELL));
            if (spell == SpellRegistry.none()) {
                throw new IllegalStateException("Spell " + RaijuForm.SPELL + " is not registered");
            }
            yakusanoikazuchi = new SpellAbility(spell);
        }
        return yakusanoikazuchi;
    }

    private static void onCurioChange(CurioChangeEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && (event.getFrom().is(FANG.get()) || event.getTo().is(FANG.get()))) {
            AbilityGrants.refresh(player);
            if (!worn(player)) {
                RaijuForm.leave(player);
                AbilityActions.dropUngranted(player);
            }
        }
    }

    private static void onCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(FANG);
        }
    }

    private static final class FangItem extends Item {
        FangItem() {
            super(new Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable("item.mdvlcraft.raiju_fang.desc").withStyle(ChatFormatting.YELLOW));
        }
    }
}
