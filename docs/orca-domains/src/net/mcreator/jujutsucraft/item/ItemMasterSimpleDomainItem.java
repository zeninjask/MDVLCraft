package net.mcreator.jujutsucraft.item;

import java.util.List;
import net.mcreator.jujutsucraft.procedures.MasterSkillsProcedure;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;

public class ItemMasterSimpleDomainItem extends Item {
   public ItemMasterSimpleDomainItem() {
      super(new Properties().m_41487_(1).m_41486_().m_41497_(Rarity.RARE));
   }

   public UseAnim m_6164_(ItemStack itemstack) {
      return UseAnim.EAT;
   }

   public void m_7373_(ItemStack itemstack, Level level, List<Component> list, TooltipFlag flag) {
      super.m_7373_(itemstack, level, list, flag);
      list.add(Component.m_237115_("item.jujutsucraft.item_master_simple_domain.description_0"));
   }

   public InteractionResultHolder<ItemStack> m_7203_(Level world, Player entity, InteractionHand hand) {
      InteractionResultHolder<ItemStack> ar = super.m_7203_(world, entity, hand);
      MasterSkillsProcedure.execute(world, entity.m_20185_(), entity.m_20186_(), entity.m_20189_(), entity, (ItemStack)ar.m_19095_());
      return ar;
   }
}
