package net.mcreator.jujutsucraft.entity;

import net.mcreator.jujutsucraft.init.JujutsucraftModEntities;
import net.mcreator.jujutsucraft.procedures.AIGravestoneProcedure;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages.SpawnEntity;
import net.minecraftforge.registries.ForgeRegistries;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.core.animation.AnimationController.State;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class EntityDomainInumakiEntity extends PathfinderMob implements GeoEntity {
   public static final EntityDataAccessor<Boolean> SHOOT = SynchedEntityData.m_135353_(EntityDomainInumakiEntity.class, EntityDataSerializers.f_135035_);
   public static final EntityDataAccessor<String> ANIMATION = SynchedEntityData.m_135353_(EntityDomainInumakiEntity.class, EntityDataSerializers.f_135030_);
   public static final EntityDataAccessor<String> TEXTURE = SynchedEntityData.m_135353_(EntityDomainInumakiEntity.class, EntityDataSerializers.f_135030_);
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private boolean swinging;
   private boolean lastloop;
   private long lastSwing;
   public String animationprocedure = "empty";
   String prevAnim = "empty";

   public EntityDomainInumakiEntity(SpawnEntity packet, Level world) {
      this((EntityType<EntityDomainInumakiEntity>)JujutsucraftModEntities.ENTITY_DOMAIN_INUMAKI.get(), world);
   }

   public EntityDomainInumakiEntity(EntityType<EntityDomainInumakiEntity> type, Level world) {
      super(type, world);
      this.f_21364_ = 0;
      this.m_21557_(true);
      this.m_274367_(0.6F);
      this.m_21530_();
   }

   protected void m_8097_() {
      super.m_8097_();
      this.f_19804_.m_135372_(SHOOT, false);
      this.f_19804_.m_135372_(ANIMATION, "undefined");
      this.f_19804_.m_135372_(TEXTURE, "inumaki_domain");
   }

   public void setTexture(String texture) {
      this.f_19804_.m_135381_(TEXTURE, texture);
   }

   public String getTexture() {
      return (String)this.f_19804_.m_135370_(TEXTURE);
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

   public SoundEvent m_7975_(DamageSource ds) {
      return (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.generic.hurt"));
   }

   public SoundEvent m_5592_() {
      return (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.generic.death"));
   }

   public boolean m_6469_(DamageSource source, float amount) {
      if (source.m_276093_(DamageTypes.f_268631_)) {
         return false;
      } else if (source.m_7640_() instanceof AbstractArrow) {
         return false;
      } else if (source.m_7640_() instanceof Player) {
         return false;
      } else if (source.m_7640_() instanceof ThrownPotion || source.m_7640_() instanceof AreaEffectCloud) {
         return false;
      } else if (source.m_276093_(DamageTypes.f_268671_)) {
         return false;
      } else if (source.m_276093_(DamageTypes.f_268585_)) {
         return false;
      } else if (source.m_276093_(DamageTypes.f_268722_)) {
         return false;
      } else if (source.m_276093_(DamageTypes.f_268450_)) {
         return false;
      } else if (source.m_276093_(DamageTypes.f_268565_)) {
         return false;
      } else if (source.m_276093_(DamageTypes.f_268714_)) {
         return false;
      } else if (source.m_276093_(DamageTypes.f_268526_)) {
         return false;
      } else if (source.m_276093_(DamageTypes.f_268482_)) {
         return false;
      } else if (source.m_276093_(DamageTypes.f_268493_)) {
         return false;
      } else {
         return source.m_276093_(DamageTypes.f_268641_) ? false : super.m_6469_(source, amount);
      }
   }

   public void m_7380_(CompoundTag compound) {
      super.m_7380_(compound);
      compound.m_128359_("Texture", this.getTexture());
   }

   public void m_7378_(CompoundTag compound) {
      super.m_7378_(compound);
      if (compound.m_128441_("Texture")) {
         this.setTexture(compound.m_128461_("Texture"));
      }
   }

   public void m_6075_() {
      super.m_6075_();
      AIGravestoneProcedure.execute(this.m_9236_(), this.m_20185_(), this.m_20186_(), this.m_20189_(), this);
      this.m_6210_();
   }

   public EntityDimensions m_6972_(Pose p_33597_) {
      return super.m_6972_(p_33597_).m_20388_(0.8F);
   }

   public boolean m_6094_() {
      return false;
   }

   protected void m_7324_(Entity entityIn) {
   }

   protected void m_6138_() {
   }

   public void m_8107_() {
      super.m_8107_();
      this.m_21203_();
   }

   public static void init() {
   }

   public static Builder createAttributes() {
      Builder builder = Mob.m_21552_();
      builder = builder.m_22268_(Attributes.f_22279_, 0.3);
      builder = builder.m_22268_(Attributes.f_22276_, 10.0);
      builder = builder.m_22268_(Attributes.f_22284_, 0.0);
      builder = builder.m_22268_(Attributes.f_22281_, 0.0);
      return builder.m_22268_(Attributes.f_22277_, 16.0);
   }

   private PlayState movementPredicate(AnimationState event) {
      return this.animationprocedure.equals("empty") ? event.setAndContinue(RawAnimation.begin().thenLoop("idle")) : PlayState.STOP;
   }

   private PlayState procedurePredicate(AnimationState event) {
      String syncedAnim = (String)this.f_19804_.m_135370_(ANIMATION);
      if (!syncedAnim.equals("undefined")) {
         this.animationprocedure = syncedAnim;
         this.f_19804_.m_135381_(ANIMATION, "undefined");
      }

      if (this.animationprocedure.equals("empty") || this.animationprocedure.equals("undefined")) {
         this.prevAnim = "empty";
         return PlayState.STOP;
      } else if (!this.animationprocedure.equals(this.prevAnim)) {
         event.getController().forceAnimationReset();
         event.getController().setAnimation(RawAnimation.begin().thenPlay(this.animationprocedure));
         this.prevAnim = this.animationprocedure;
         return PlayState.CONTINUE;
      } else if (event.getController().getAnimationState() == State.STOPPED) {
         this.animationprocedure = "empty";
         this.prevAnim = "empty";
         return PlayState.STOP;
      } else {
         return PlayState.CONTINUE;
      }
   }

   protected void m_6153_() {
      this.f_20919_++;
      if (this.f_20919_ == 20) {
         this.m_142687_(RemovalReason.KILLED);
         this.m_21226_();
      }
   }

   public String getSyncedAnimation() {
      return (String)this.f_19804_.m_135370_(ANIMATION);
   }

   public void setAnimation(String animation) {
      this.f_19804_.m_135381_(ANIMATION, animation);
      this.animationprocedure = animation;
   }

   public void registerControllers(ControllerRegistrar data) {
      data.add(new AnimationController[]{new AnimationController(this, "movement", 4, this::movementPredicate)});
      data.add(new AnimationController[]{new AnimationController(this, "procedure", 4, this::procedurePredicate)});
   }

   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }
}
