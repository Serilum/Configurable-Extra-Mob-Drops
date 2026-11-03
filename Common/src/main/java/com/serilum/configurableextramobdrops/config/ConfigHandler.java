package com.serilum.configurableextramobdrops.config;

import com.natamus.collective.config.DuskConfig;
import com.serilum.configurableextramobdrops.util.Reference;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ConfigHandler extends DuskConfig {
	public static HashMap<String, List<String>> configMetaData = new HashMap<>();

	@Entry public static boolean onlyOneRandomDrop = false;

	public static void initConfig() {
		configMetaData.put("onlyOneRandomDrop", Arrays.asList(
			"Whether a mob drops only one of its extra drops, picked at random from the ones that passed their drop chance."
		));

		DuskConfig.init(Reference.NAME, Reference.MOD_ID, ConfigHandler.class);
	}
}
