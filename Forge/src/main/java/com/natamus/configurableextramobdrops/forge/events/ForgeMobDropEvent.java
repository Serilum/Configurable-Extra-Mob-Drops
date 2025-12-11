package com.natamus.configurableextramobdrops.forge.events;

import com.natamus.collective.functions.WorldFunctions;
import com.natamus.configurableextramobdrops.cmd.CommandCemd;
import com.natamus.configurableextramobdrops.events.MobDropEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;

import java.lang.invoke.MethodHandles;

public class ForgeMobDropEvent {
	public static void registerEventsInBus() {
		BusGroup.DEFAULT.register(MethodHandles.lookup(), ForgeMobDropEvent.class);
	}

	@SubscribeEvent
	public static void onWorldLoad(LevelEvent.Load e) {
		Level level = WorldFunctions.getWorldIfInstanceOfAndNotRemote(e.getLevel());
		if (level == null) {
			return;
		}

		MobDropEvent.onWorldLoad((ServerLevel)level);
	}

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent e) {
    	CommandCemd.register(e.getDispatcher());
    }

	@SubscribeEvent
	public static void mobItemDrop(LivingDropsEvent e) {
		Entity entity = e.getEntity();
		MobDropEvent.mobItemDrop(entity.level(), entity, e.getSource());
	}
}
