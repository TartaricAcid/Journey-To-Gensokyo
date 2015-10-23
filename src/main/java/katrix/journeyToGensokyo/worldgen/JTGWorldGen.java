/**
 * This class was created by <Katrix>. It's distributed as
 * part of the Journey To Gensokyo Mod. Get the Source Code in github:
 * https://github.com/Katrix-/JTG
 * 
 * Journey To Gensokyo is Open Source and distributed under the
 * a modifed Botania license: https://github.com/Katrix-/JTG/blob/master/LICENSE.md
 */

package katrix.journeyToGensokyo.worldgen;

import katrix.journeyToGensokyo.handler.ConfigHandler;
import katrix.journeyToGensokyo.util.LogHelper;
import katrix.journeyToGensokyo.worldgen.OreGen;
import cpw.mods.fml.common.registry.GameRegistry;

public class JTGWorldGen {
	
	public static void init() {
		
		if(ConfigHandler.OresEnabled) {
			LogHelper.warn("JTG Ores enabled, this is not a officialy suported feature");
			GameRegistry.registerWorldGenerator(new OreGen(), 1);
		}
	}
}