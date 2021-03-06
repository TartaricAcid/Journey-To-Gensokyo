/*
 * This class was created by <Katrix>. It's distributed as
 * part of the Journey To Gensokyo Mod. Get the Source Code in github:
 * https://github.com/Katrix-/JTG
 *
 * Journey To Gensokyo is Open Source and distributed under the
 * a modifed Botania license: https://github.com/Katrix-/JTG/blob/devDanmakuCore/LICENSE.md
 */
package net.katsstuff.journeytogensokyo.client.render

import net.katsstuff.journeytogensokyo.client.model.ModelTenguCrow
import net.katsstuff.journeytogensokyo.entity.living.EntityTenguCrow
import net.katsstuff.journeytogensokyo.lib.LibMod
import net.minecraft.client.renderer.entity.{RenderLiving, RenderManager}
import net.minecraft.util.ResourceLocation

class RenderTenguCrow(renderManager: RenderManager) extends RenderLiving[EntityTenguCrow](renderManager, ModelTenguCrow, 0.5F) {
  protected def getEntityTexture(entity: EntityTenguCrow) = new ResourceLocation(LibMod.Id, "textures/entity/mob/tengu_crow.png")
}
