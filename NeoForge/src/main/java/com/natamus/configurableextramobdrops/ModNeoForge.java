package com.natamus.configurableextramobdrops;

import com.natamus.collective.check.RegisterMod;
import com.natamus.configurableextramobdrops.neoforge.events.NeoForgeMobDropEvent;
import com.natamus.configurableextramobdrops.util.Reference;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(Reference.MOD_ID)
public class ModNeoForge {
	
	public ModNeoForge(IEventBus modEventBus) {
		modEventBus.addListener(this::loadComplete);

		setGlobalConstants();
		ModCommon.init();

		RegisterMod.register(Reference.NAME, Reference.MOD_ID, Reference.VERSION, Reference.ACCEPTED_VERSIONS);
	}

	private void loadComplete(final FMLLoadCompleteEvent event) {
		NeoForge.EVENT_BUS.register(NeoForgeMobDropEvent.class);
	}

	private static void setGlobalConstants() {

	}
}