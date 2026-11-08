package com.serilum.configurableextramobdrops.util;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.natamus.collective.data.GlobalVariables;
import com.natamus.collective.functions.DataFunctions;
import com.natamus.collective.functions.ItemFunctions;
import com.natamus.collective.functions.StringFunctions;
import com.serilum.configurableextramobdrops.data.Constants;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Util {
	private static final String dirpath = DataFunctions.getConfigDirectory() + File.separator + "configurableextramobdrops";
	private static final File dir = new File(dirpath);
	private static final File file = new File(dirpath + File.separator + "mobdropconfig.txt");

	public static HashMap<EntityType<?>, CopyOnWriteArrayList<ItemStack>> mobdrops = new HashMap<>();
	private static final List<EntityType<?>> specialmiscmobs = new ArrayList<>(Arrays.asList(EntityType.IRON_GOLEM, EntityType.SNOW_GOLEM, EntityType.VILLAGER));
	private static final List<String> dropSettingKeys = Arrays.asList("dropChance", "minAmount", "maxAmount", "enchantLevel");

	private static boolean loadedMobConfigFile = false;

	public static String readMobDropConfig() {
		try {
			return Files.readString(file.toPath());
		}
		catch (IOException ignored) {
			return "";
		}
	}

	public static void writeMobDropConfig(MinecraftServer server, String text) {
		try {
			PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8);
			writer.print(text);
			writer.close();
		}
		catch (IOException ignored) {
			Constants.logger.warn("[" + Reference.NAME + "] Unable to write the entity config file.");
			return;
		}

		loadedMobConfigFile = false;
		if (server != null) {
			attemptLoadMobConfigFile(server.overworld());
		}
	}

	public static void attemptLoadMobConfigFile(Level level) {
		if (loadedMobConfigFile) {
			return;
		}

		try {
			loadMobConfigFile(level);
		} catch (Exception ex) {
			Constants.logger.warn("[" + Reference.NAME + "] Error on loading the entity config file. The mod has been disabled.");
		}

		loadedMobConfigFile = true;
	}

	public static void loadMobConfigFile(Level level) throws IOException {
		mobdrops = new HashMap<>();

		String configContent = "";
		if (file.isFile()) {
			configContent = Files.readString(file.toPath());
		}

		for (String line : configContent.split("\n")) {
			if (line.trim().endsWith(",")) {
				line = line.trim();
				line = line.substring(0, line.length() - 1).trim();
			}

			if (line.length() < 5) {
				continue;
			}

			if (!line.contains("' : '")) {
				continue;
			}

			String[] lineSplit = line.split("' : '");
			if (lineSplit.length < 2) {
				continue;
			}

			String entityId = lineSplit[0].substring(1).trim();
			String itemString = lineSplit[1].trim();
			itemString = itemString.substring(0, itemString.length() - 1).trim();

			Optional<EntityType<?>> optionalEntityType = BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.parse(entityId));
			if (optionalEntityType.isEmpty()) {
				continue;
			}

			CopyOnWriteArrayList<ItemStack> drops = new CopyOnWriteArrayList<>();
			if (itemString.length() > 3) {
				for (String itemData : itemString.split(StringFunctions.escapeSpecialRegexChars("|||"))) {
					ItemStack itemStack = null;
					try {
						CompoundTag itemTag = TagParser.parseTag(itemData);
						Optional<ItemStack> optionalItemStack = ItemStack.parse(level.registryAccess(), itemTag);
						if (optionalItemStack.isPresent()) {
							itemStack = optionalItemStack.get();
						}
					} catch (CommandSyntaxException ignored) { }

					if (itemStack != null) {
						drops.add(itemStack.copy());
					}
				}
			}

			mobdrops.put(optionalEntityType.get(), drops);
		}

		addMissingEntityTypesToFile(configContent);
	}

	private static void addMissingEntityTypesToFile(String configContent) throws IOException {
		List<String> missingEntityLines = new ArrayList<>();

		for (ResourceLocation rl : BuiltInRegistries.ENTITY_TYPE.keySet()) {
			EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(rl);
			if (isConfigurableEntityType(entityType) && !mobdrops.containsKey(entityType)) {
				missingEntityLines.add("'" + rl + "'" + " : '',");

				mobdrops.put(entityType, new CopyOnWriteArrayList<>());
			}
		}

		if (missingEntityLines.isEmpty()) {
			return;
		}

		boolean ignored = dir.mkdirs();
		PrintWriter writer = new PrintWriter(new FileWriter(file, StandardCharsets.UTF_8, true));
		if (!configContent.isEmpty() && !configContent.endsWith("\n")) {
			writer.println();
		}

		for (String missingEntityLine : missingEntityLines) {
			writer.println(missingEntityLine);
		}

		writer.close();
	}

	public static boolean isConfigurableEntityType(EntityType<?> entityType) {
		return !entityType.getCategory().equals(MobCategory.MISC) || specialmiscmobs.contains(entityType);
	}

	public static CompoundTag getDropSettings(ItemStack extraDrop) {
		CompoundTag dropSettings = new CompoundTag();
		CompoundTag customTag = extraDrop.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		for (String dropSettingKey : dropSettingKeys) {
			Tag dropSetting = customTag.get(dropSettingKey);
			if (dropSetting != null) {
				dropSettings.put(dropSettingKey, dropSetting);
			}
		}

		return dropSettings;
	}

	public static ItemStack withoutDropSettings(ItemStack extraDrop) {
		ItemStack itemStack = extraDrop.copy();
		CompoundTag customTag = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		for (String dropSettingKey : dropSettingKeys) {
			customTag.remove(dropSettingKey);
		}

		if (customTag.isEmpty()) {
			itemStack.remove(DataComponents.CUSTOM_DATA);
		}
		else {
			itemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(customTag));
		}

		return itemStack;
	}

	public static List<ItemStack> getDropsThatPassTheirChance(List<ItemStack> extraDrops) {
		List<ItemStack> passedDrops = new ArrayList<>();
		for (ItemStack extraDrop : extraDrops) {
			if (passesDropChance(getDropSettings(extraDrop))) {
				passedDrops.add(extraDrop);
			}
		}

		return passedDrops;
	}

	public static List<ItemStack> pickOneAtRandom(List<ItemStack> drops) {
		if (drops.size() <= 1) {
			return drops;
		}

		return List.of(drops.get(GlobalVariables.random.nextInt(drops.size())));
	}

	public static boolean passesDropChance(CompoundTag dropSettings) {
		if (!dropSettings.contains("dropChance")) {
			return true;
		}

		double dropChance = dropSettings.getDouble("dropChance");
		if (dropChance >= 1.0) {
			return true;
		}

		return GlobalVariables.random.nextDouble() <= dropChance;
	}

	public static List<ItemStack> getDropStacks(Level level, ItemStack extraDrop) {
		CompoundTag dropSettings = getDropSettings(extraDrop);
		ItemStack itemStack = withoutDropSettings(extraDrop);
		int dropAmount = getDropAmount(itemStack, dropSettings);
		List<ItemStack> dropStacks = new ArrayList<>();

		if (dropSettings.contains("enchantLevel")) {
			int enchantLevel = dropSettings.getInt("enchantLevel");
			for (int i = 0; i < dropAmount; i++) {
				dropStacks.add(getEnchantedItem(level, itemStack.copyWithCount(1), enchantLevel));
			}

			return dropStacks;
		}

		int amountLeft = dropAmount;
		while (amountLeft > 0) {
			int stackCount = Math.min(amountLeft, itemStack.getMaxStackSize());
			dropStacks.add(itemStack.copyWithCount(stackCount));
			amountLeft -= stackCount;
		}

		return dropStacks;
	}

	public static int getDropAmount(ItemStack itemStack, CompoundTag dropSettings) {
		if (!dropSettings.contains("minAmount") || !dropSettings.contains("maxAmount")) {
			return itemStack.getCount();
		}

		int minAmount = dropSettings.getInt("minAmount");
		int maxAmount = dropSettings.getInt("maxAmount");
		if (maxAmount <= minAmount) {
			return minAmount;
		}

		return minAmount + GlobalVariables.random.nextInt(maxAmount - minAmount + 1);
	}

	public static ItemStack getEnchantedItem(Level level, ItemStack itemStack, int enchantLevel) {
		Optional<HolderSet.Named<Enchantment>> enchantingTableEnchantments = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getTag(EnchantmentTags.IN_ENCHANTING_TABLE);
		return EnchantmentHelper.enchantItem(level.getRandom(), itemStack, enchantLevel, level.registryAccess(), enchantingTableEnchantments);
	}

	public static String getAmountText(ItemStack itemStack, CompoundTag dropSettings) {
		if (!dropSettings.contains("minAmount") || !dropSettings.contains("maxAmount")) {
			return String.valueOf(itemStack.getCount());
		}

		return dropSettings.getInt("minAmount") + "-" + dropSettings.getInt("maxAmount");
	}

	public static boolean writeDropsMapToFile(Level level) throws IOException {
		if (!dir.isDirectory() || !file.isFile()) {
			boolean ignored = dir.mkdirs();
		}

		PrintWriter writer = new PrintWriter(dirpath + File.separator + "mobdropconfig.txt", StandardCharsets.UTF_8);

		for (ResourceLocation rl : BuiltInRegistries.ENTITY_TYPE.keySet()) {
			EntityType<?> entitytype = BuiltInRegistries.ENTITY_TYPE.get(rl);
			if (isConfigurableEntityType(entitytype)) {
				StringBuilder itemdata = new StringBuilder();
				if (mobdrops.containsKey(entitytype)) {
					CopyOnWriteArrayList<ItemStack> drops = mobdrops.get(entitytype);
					if (!drops.isEmpty()) {
						for (ItemStack drop : drops) {
							if (!itemdata.toString().isEmpty()) {
								itemdata.append("|||");
							}

							String nbtstring = ItemFunctions.getNBTStringFromItemStack(level, drop);

							itemdata.append(nbtstring);
						}
					}
				}

				writer.println("'" + rl + "'" + " : '" + itemdata + "',");
			}
		}

		writer.close();
		return true;
	}
}
