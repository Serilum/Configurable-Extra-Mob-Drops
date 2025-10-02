package com.natamus.configurableextramobdrops.events;

import com.natamus.collective.data.GlobalVariables;
import com.natamus.configurableextramobdrops.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

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
			
			for (ItemStack extraDropStack : extradrops) {
				ItemStack newStack = extraDropStack.copy();

				if (newStack.has(DataComponents.CUSTOM_DATA)) {
					CustomData customData = newStack.get(DataComponents.CUSTOM_DATA);
					if (customData == null) {
						continue;
					}

					CompoundTag compoundTag = customData.copyTag();
					if (compoundTag.contains("dropChance")) {
						double dropChance = compoundTag.getDoubleOr("dropChance", 1.0);
						if (dropChance != 1.0) {
							double chanceroll = GlobalVariables.random.nextDouble();
							if (chanceroll > dropChance) {
								continue;
							}
						}

						if (compoundTag.size() == 1) {
							newStack.remove(DataComponents.CUSTOM_DATA);
						}
						else {
							compoundTag.remove("dropChance");
							newStack.set(DataComponents.CUSTOM_DATA, CustomData.of(compoundTag));
						}
					}
				}
				
				level.addFreshEntity(new ItemEntity(level, ePos.getX(), ePos.getY()+1, ePos.getZ(), newStack));
			}
		}
	}
}
