package net.mcreator.jujutsucraft.entity;

import javax.annotation.Nullable;
import net.mcreator.jujutsucraft.init.JujutsucraftModEntities;
import net.mcreator.jujutsucraft.procedures.AIDomainExpansionEntityProcedure;
import net.mcreator.jujutsucraft.procedures.DomainExpansionEntityOnInitialEntitySpawnProcedure;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages.SpawnEntity;

public class DomainExpansionEntityEntity extends PathfinderMob {
   public DomainExpansionEntityEntity(SpawnEntity packet, Level world) {
      this((EntityType<DomainExpansionEntityEntity>)JujutsucraftModEntities.DOMAIN_EXPANSION_ENTITY.get(), world);
   }

   public DomainExpansionEntityEntity(EntityType<DomainExpansionEntityEntity> type, Level world) {
      super(type, world);
      this.m_274367_(0.6F);
      this.f_21364_ = 0;
      this.m_21557_(true);
      this.m_21530_();
   }

   public Packet<ClientGamePacketListener> m_5654_() {
      return NetworkHooks.getEntitySpawningPacket(this);
   }

   public MobType m_6336_() {
      return MobType.f_21640_;
   }

   public boolean m_6785_(double distanceToClosestPlayer) {
      return false;
   }

   public boolean m_6469_(DamageSource damagesource, float amount) {
      if (damagesource.m_276093_(DamageTypes.f_268631_)) {
         return false;
      } else if (damagesource.m_7640_() instanceof AbstractArrow) {
         return false;
      } else if (damagesource.m_7640_() instanceof Player) {
         return false;
      } else if (damagesource.m_7640_() instanceof ThrownPotion || damagesource.m_7640_() instanceof AreaEffectCloud) {
         return false;
      } else if (damagesource.m_276093_(DamageTypes.f_268671_)) {
         return false;
      } else if (damagesource.m_276093_(DamageTypes.f_268585_)) {
         return false;
      } else if (damagesource.m_276093_(DamageTypes.f_268722_)) {
         return false;
      } else if (damagesource.m_276093_(DamageTypes.f_268450_)) {
         return false;
      } else if (damagesource.m_276093_(DamageTypes.f_268565_) || damagesource.m_276093_(DamageTypes.f_268448_)) {
         return false;
      } else if (damagesource.m_276093_(DamageTypes.f_268714_)) {
         return false;
      } else if (damagesource.m_276093_(DamageTypes.f_268526_)) {
         return false;
      } else if (damagesource.m_276093_(DamageTypes.f_268482_)) {
         return false;
      } else {
         return !damagesource.m_276093_(DamageTypes.f_268493_) && !damagesource.m_276093_(DamageTypes.f_268641_) ? super.m_6469_(damagesource, amount) : false;
      }
   }

   public boolean m_6128_() {
      return true;
   }

   public boolean m_5825_() {
      return true;
   }

   public SpawnGroupData m_6518_(
      ServerLevelAccessor world, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData livingdata, @Nullable CompoundTag tag
   ) {
      SpawnGroupData retval = super.m_6518_(world, difficulty, reason, livingdata, tag);
      DomainExpansionEntityOnInitialEntitySpawnProcedure.execute(this);
      return retval;
   }

   public void m_6075_() {
      super.m_6075_();
      AIDomainExpansionEntityProcedure.execute(this.m_9236_(), this.m_20185_(), this.m_20186_(), this.m_20189_(), this);
   }

   public boolean m_6094_() {
      return false;
   }

   protected void m_7324_(Entity entityIn) {
   }

   protected void m_6138_() {
   }

   public static void init() {
   }

   public static Builder createAttributes() {
      Builder builder = Mob.m_21552_();
      builder = builder.m_22268_(Attributes.f_22279_, 0.0);
      builder = builder.m_22268_(Attributes.f_22276_, 100.0);
      builder = builder.m_22268_(Attributes.f_22284_, 0.0);
      builder = builder.m_22268_(Attributes.f_22281_, 0.0);
      return builder.m_22268_(Attributes.f_22277_, 16.0);
   }

   public boolean m_6087_() {
      return false;
   }

   public boolean m_6097_() {
      return false;
   }

   public boolean m_5829_() {
      return false;
   }
}
