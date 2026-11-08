package com.serilum.configurableextramobdrops;

import com.natamus.collective.config.DuskConfigCustomFile;
import com.serilum.configurableextramobdrops.config.ConfigHandler;
import com.serilum.configurableextramobdrops.util.Reference;
import com.serilum.configurableextramobdrops.util.Util;

public class ModCommon {

	public static void init() {
		ConfigHandler.initConfig();
		load();
	}

	private static void load() {
		DuskConfigCustomFile.addText(Reference.MOD_ID, "mobdropconfig.txt", "Mob Drops", "The extra items each mob drops, as item NBT with optional drop settings.", Util::readMobDropConfig, Util::writeMobDropConfig);
	}
}