/*
 * This class was created by <Katrix>. It's distributed as
 * part of the Journey To Gensokyo Mod. Get the Source Code in github:
 * https://github.com/Katrix-/JTG
 *
 * Journey To Gensokyo is Open Source and distributed under the
 * a modifed Botania license: https://github.com/Katrix-/JTG/blob/devDanmakuCore/LICENSE.md
 */
package net.katsstuff.journeytogensokyo.entity.living

import java.lang.{Boolean => JBoolean}
import java.util.Random

import javax.annotation.Nullable

import scala.collection.JavaConverters._

import com.google.common.base.Optional

import net.katsstuff.danmakucore.entity.living.ai.EntityAIMoveRanged
import net.katsstuff.danmakucore.entity.living.{EnumSpecies, IAllyDanmaku}
import net.katsstuff.journeytogensokyo.entity.living.ai.{EntityAIFollowFriend, EntityAITemptStack}
import net.katsstuff.journeytogensokyo.handler.ConfigHandler
import net.katsstuff.journeytogensokyo.handler.ConfigHandler.Spawns.SpawnEntry
import net.katsstuff.journeytogensokyo.helper.LogHelper
import net.katsstuff.journeytogensokyo.lib.LibEntityName
import net.katsstuff.journeytogensokyo.phase.JTGPhases
import net.minecraft.block.BlockFlower
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.ai.{EntityAIHurtByTarget, EntityAILookIdle, EntityAINearestAttackableTarget, EntityAISwimming, EntityAIWander, EntityAIWatchClosest}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.{EntityLivingBase, IEntityLivingData}
import net.minecraft.item.{Item, ItemStack}
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.datasync.{DataParameter, DataSerializers, EntityDataManager}
import net.minecraft.util.math.BlockPos
import net.minecraft.util.{EnumHand, EnumParticleTypes}
import net.minecraft.world.{DifficultyInstance, EnumSkyBlock, World, WorldServer}
import net.minecraftforge.fml.common.registry.GameRegistry
import net.minecraftforge.fml.relauncher.ReflectionHelper

object EntityFairy {
  var counter = 0
  def nextCounter(): Byte = {
    if (counter == 3) counter = 0
    else counter += 1

    counter.toByte
  }

  private final val LikedFlower:   DataParameter[Optional[ItemStack]] = EntityDataManager.createKey(classOf[EntityFairy], DataSerializers.OPTIONAL_ITEM_STACK)
  private final val HoldingFlower: DataParameter[JBoolean]            = EntityDataManager.createKey(classOf[EntityFairy], DataSerializers.BOOLEAN)

  lazy val flowers: Seq[ItemStack] = {
    val typesForColor = ReflectionHelper.findField(classOf[BlockFlower.EnumFlowerType], "TYPES_FOR_BLOCK", "field_176981_k").get(null).asInstanceOf[Array[Array[BlockFlower.EnumFlowerType]]]

    BlockFlower.EnumFlowerColor.values().flatMap { flowerColor =>
      val blockTypes = typesForColor(flowerColor.ordinal())
      val block = flowerColor.getBlock
      blockTypes.map(tpe => new ItemStack(block, 1, tpe.getMeta))
    }.toSeq
  }

  def randomFlower(rand: Random): ItemStack = flowers(rand.nextInt(flowers.length))
}
class EntityFairy(_world: World) extends EntityForm(_world) with Callable with IAllyDanmaku {

  private var aiTempt: EntityAITemptStack = _
  private var attackPlayer: EntityAINearestAttackableTarget[EntityPlayer] = _
  private var followFriend: EntityAIFollowFriend = _

  private var throwAwayTime = 0
  private var _friend: Option[EntityPlayer] = None

  setSize(0.5F, 1F)
  experienceValue = 5

  form = {
    if (world.isRemote) 0
    else EntityFairy.nextCounter()
  }

  phaseManager.addPhase(JTGPhases.StageEnemy.instantiate(phaseManager))
  phaseManager.getCurrentPhase.init()

  setSpeed(0.4D)
  setSpecies(EnumSpecies.FAIRY)

  setFlyingHeight(2)
  setEntityCallDistance(30)
  setMaxHP(2F)

  override def initEntityAI(): Unit = {
    val liked = likedFlower.fold({
      val newLiked = EntityFairy.randomFlower(rand)
      likedFlower = newLiked
      newLiked
    })(identity)

    aiTempt = new EntityAITemptStack(this, getSpeed / 2, true, Set(liked))
    attackPlayer = new EntityAINearestAttackableTarget(this, classOf[EntityPlayer], true)
    followFriend = new EntityAIFollowFriend(this, getSpeed * 8, 2F, 16F)

    tasks.addTask(0, new EntityAISwimming(this))
    tasks.addTask(1, aiTempt)
    tasks.addTask(2, new EntityAIMoveRanged(this, getSpeed, 16F))
    tasks.addTask(6, new EntityAIWander(this, getSpeed))
    tasks.addTask(6, new EntityAIWatchClosest(this, classOf[EntityPlayer], 16F))
    tasks.addTask(7, new EntityAILookIdle(this))
    targetTasks.addTask(1, new EntityAIHurtByTarget(this, false))
    targetTasks.addTask(2, attackPlayer)
  }

  override def entityInit(): Unit = {
    super.entityInit()

    dataManager.register(EntityFairy.LikedFlower, Optional.of(EntityFairy.randomFlower(rand)))
    dataManager.register(EntityFairy.HoldingFlower, Boolean.box(false))
  }

  override def onUpdate(): Unit = {
    super.onUpdate()
    if(aiTempt != null) {
      if(aiTempt.isTempted) {
        targetTasks.removeTask(attackPlayer)

        if(ticksExisted % 10 == 0 && world.isInstanceOf[WorldServer]) {
          world.asInstanceOf[WorldServer].spawnParticle(EnumParticleTypes.HEART, false, posX, posY, posZ, 1 + rand.nextInt(2), 0D, 0D, 0D, 0.1D)
        }
      }
      else if(!holdingFlower) {
        targetTasks.addTask(2, attackPlayer)
      }

    }

    if(throwAwayTime > 0) {
      throwAwayTime -= 1

      if(throwAwayTime == 0) {
        holdingFlower = false
        likedFlower.foreach(entityDropItem(_, 0F))
      }
    }
  }

  override def onEntityCall(caller: EntityLivingBase, target: EntityLivingBase): Unit = {
    def distanceTo(entity: EntityLivingBase): Double = entity.getPositionVector.distanceTo(getPositionVector)
    if (getAttackTarget == null || distanceTo(getAttackTarget) > distanceTo(target)) {
      setAttackTarget(target)
    }
  }

  override def onInitialSpawn(difficulty: DifficultyInstance, livingData: IEntityLivingData): IEntityLivingData = {
    val superData = super.onInitialSpawn(difficulty, livingData)

    val groupData = superData match {
      case fairy: FairyGroupData => fairy
      case _ => FairyGroupData(form)
    }

    form = groupData.form

    groupData
  }

  override def processInteract(player: EntityPlayer, hand: EnumHand, @Nullable stack: ItemStack): Boolean = {
    if ((aiTempt == null || aiTempt.isTempted) && stack != null && likedFlower.exists(_.isItemEqual(stack)) && player.getDistanceSqToEntity(this) < 9.0D) {
      if (!player.capabilities.isCreativeMode) stack.stackSize -= 1

      if (!world.isRemote) {
        holdingFlower = true
        _friend = Some(player)
      }

      true
    }
    else super.processInteract(player, hand, stack)
  }

  override def isValidLightLevel: Boolean = {
    val blockpos = new BlockPos(this.posX, this.getEntityBoundingBox.minY, this.posZ)
    world.getLightFor(EnumSkyBlock.SKY, blockpos) > 8
  }

  override def getBlockPathWeight(pos: BlockPos): Float = world.getLightBrightness(pos) - 0.5F

  override def lootTableName: String = LibEntityName.Fairy

  override def spawnEntry: SpawnEntry = ConfigHandler.spawns.fairy
  override def spawnBlockCheck(state: IBlockState): Boolean = {
    val spawnMaterial = Seq(Material.GRASS, Material.GROUND, Material.SAND)
    spawnMaterial.contains(state.getMaterial)
  }

  def holdingFlower: Boolean = dataManager.get(EntityFairy.HoldingFlower)
  def holdingFlower_=(holding: Boolean): Unit = {
    if(holding) {
      throwAwayTime = 400 + rand.nextInt(1201)
      targetTasks.removeTask(attackPlayer)
      tasks.addTask(3, followFriend)
    }
    else {
      _friend = None
      targetTasks.addTask(2, attackPlayer)
      tasks.removeTask(followFriend)
    }

    dataManager.set(EntityFairy.HoldingFlower, Boolean.box(holding))
  }

  def likedFlower: Option[ItemStack] = Option(dataManager.get(EntityFairy.LikedFlower).orNull())
  def likedFlower_=(flower: ItemStack): Unit = {
    dataManager.set(EntityFairy.LikedFlower, Optional.of(flower))
    if(aiTempt != null) {
      aiTempt.temptStacks = Set(flower)
    }
  }

  def friend: Option[EntityPlayer] = _friend

  override def readEntityFromNBT(tag: NBTTagCompound): Unit = {
    super.readEntityFromNBT(tag)
    val flowerName = tag.getString("likedFlowerName")
    val flowerMeta = tag.getByte("likedFlowerMeta")
    likedFlower = GameRegistry.makeItemStack(flowerName, flowerMeta, 1, "")
  }

  override def writeEntityToNBT(tag: NBTTagCompound): Unit = {
    super.writeEntityToNBT(tag)
    likedFlower.foreach { liked =>
      tag.setString("likedFlowerName", Item.REGISTRY.getNameForObject(liked.getItem).toString)
      tag.setByte("likedFlowerMeta", liked.getItemDamage.toByte)
    }
  }
}
case class FairyGroupData(form: Byte) extends IEntityLivingData
