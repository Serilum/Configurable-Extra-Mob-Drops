package com.natamus.configurableextramobdrops.forge.events;

import com.natamus.configurableextramobdrops.cmd.CommandCemd;
import com.natamus.configurableextramobdrops.events.MobDropEvent;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber
public class ForgeMobDropEvent {
    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent e) {
    	CommandCemd.register(e.getDispatcher());
    }

	@SubscribeEvent
	public void mobItemDrop(LivingDropsEvent e) {
		Entity entity = e.getEntity();
		MobDropEvent.mobItemDrop(entity.level(), entity, e.getSource());
	}
}
