package com.natamus.configurableextramobdrops;

import com.natamus.collective.check.RegisterMod;
import com.natamus.collective.fabric.callbacks.CollectiveEntityEvents;
import com.natamus.configurableextramobdrops.cmd.CommandCemd;
import com.natamus.configurableextramobdrops.events.MobDropEvent;
import com.natamus.configurableextramobdrops.util.Reference;
import com.natamus.configurableextramobdrops.util.Util;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public class ModFabric implements ModInitializer {
	
	@Override
	public void onInitialize() {
		setGlobalConstants();
		ModCommon.init();

		loadEvents();

		RegisterMod.register(Reference.NAME, Reference.MOD_ID, Reference.VERSION, Reference.ACCEPTED_VERSIONS);
	}

	private void loadEvents() {
		try {
			Util.loadMobConfigFile();
		} catch (Exception ex) {
			System.out.println("[" + Reference.NAME + "] Error on loading the entity config file. The mod has been disabled.");
			return;
		}

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			CommandCemd.register(dispatcher);
		});

		CollectiveEntityEvents.ON_ENTITY_IS_DROPPING_LOOT.register((Level world, Entity entity, DamageSource damageSource) -> {
			MobDropEvent.mobItemDrop(world, entity, damageSource);
		});
	}

	private static void setGlobalConstants() {

	}
}
