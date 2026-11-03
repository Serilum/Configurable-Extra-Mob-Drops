package com.serilum.configurableextramobdrops.events;

import com.serilum.configurableextramobdrops.config.ConfigHandler;
import com.serilum.configurableextramobdrops.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MobDropEvent {
	public static void onWorldLoad(Level level) {
		Util.attemptLoadMobConfigFile(level);
	}

	public static void mobItemDrop(Level level, Entity entity, DamageSource damageSource) {
		if (level.isClientSide()) {
			return;
		}
		
		EntityType<?> entityType = entity.getType();
		if (!Util.mobdrops.containsKey(entityType)) {
			return;
		}
		
		CopyOnWriteArrayList<ItemStack> extradrops = Util.mobdrops.get(entityType);
		if (!extradrops.isEmpty()) {
			BlockPos ePos = entity.blockPosition();
			
			List<ItemStack> passedDrops = Util.getDropsThatPassTheirChance(extradrops);
			if (ConfigHandler.onlyOneRandomDrop) {
				passedDrops = Util.pickOneAtRandom(passedDrops);
			}

			for (ItemStack passedDrop : passedDrops) {
				for (ItemStack dropStack : Util.getDropStacks(level, passedDrop)) {
					level.addFreshEntity(new ItemEntity(level, ePos.getX(), ePos.getY()+1, ePos.getZ(), dropStack));
				}
			}
		}
	}
}
