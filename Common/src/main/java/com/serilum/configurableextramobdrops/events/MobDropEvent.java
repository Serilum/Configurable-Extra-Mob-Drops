package com.serilum.configurableextramobdrops.events;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.serilum.configurableextramobdrops.config.ConfigHandler;
import com.serilum.configurableextramobdrops.util.Util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class MobDropEvent {
	public static void onWorldLoad(Level level) {
		Util.attemptLoadMobConfigFile();
	}

	public static void mobItemDrop(Level level, Entity entity, DamageSource damageSource) {
		if (level.isClientSide) {
			return;
		}

		EntityType<?> entityType = entity.getType();
		if (!Util.mobdrops.containsKey(entityType)) {
			return;
		}

		CopyOnWriteArrayList<ItemStack> extraDrops = Util.mobdrops.get(entityType);
		if (!extraDrops.isEmpty()) {
			BlockPos ePos = entity.blockPosition();

			List<ItemStack> passedDrops = Util.getDropsThatPassTheirChance(extraDrops);
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
