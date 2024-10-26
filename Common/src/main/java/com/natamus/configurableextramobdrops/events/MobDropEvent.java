package com.natamus.configurableextramobdrops.events;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.natamus.collective.data.GlobalVariables;
import com.natamus.collective.functions.ItemFunctions;
import com.natamus.configurableextramobdrops.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class MobDropEvent {
	public static void onWorldLoad(Level level) {
		Util.attemptLoadMobConfigFile(level);
	}

	public static void mobItemDrop(Level level, Entity entity, DamageSource damageSource) {
		if (level.isClientSide) {
			return;
		}
		
		EntityType<?> entitytype = entity.getType();
		if (!Util.mobdrops.containsKey(entitytype)) {
			return;
		}
		
		CopyOnWriteArrayList<ItemStack> extradrops = Util.mobdrops.get(entitytype);
		if (extradrops.size() > 0) {
			BlockPos epos = entity.blockPosition();
			
			for (ItemStack itemstack : extradrops) {
				ItemStack newstack = itemstack.copy();

				CompoundTag tag;
				try {
					tag = TagParser.parseTag(ItemFunctions.getNBTStringFromItemStack(level, newstack));
				}
				catch (CommandSyntaxException ex) {
					continue;
				}

				if (tag.contains("dropchance")) {
					double dropchance = tag.getDouble("dropchance");
					if (dropchance != 1.0) {
						double chanceroll = GlobalVariables.random.nextDouble();
						if (chanceroll > dropchance) {
							continue;
						}
					}
					tag.remove("dropchance");
					if (tag.size() == 0) {
						tag.remove("tag");
					}

					Optional<ItemStack> optionalNewStack = ItemStack.parse(level.registryAccess(), tag);
					if (optionalNewStack.isPresent()) {
						newstack = optionalNewStack.get();
					}
				}
				
				level.addFreshEntity(new ItemEntity(level, epos.getX(), epos.getY()+1, epos.getZ(), newstack.copy()));
			}
		}
	}
}
