package com.serilum.configurableextramobdrops.forge.events;

import com.serilum.configurableextramobdrops.cmd.CommandCemd;
import com.serilum.configurableextramobdrops.events.MobDropEvent;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ForgeMobDropEvent {
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
