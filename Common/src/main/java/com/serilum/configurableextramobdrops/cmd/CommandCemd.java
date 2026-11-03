package com.serilum.configurableextramobdrops.cmd;
import com.serilum.configurableextramobdrops.util.Reference;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.natamus.collective.functions.MessageFunctions;
import com.serilum.configurableextramobdrops.util.Util;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;

public class CommandCemd {
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("cemd").requires((iCommandSender) -> iCommandSender.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
			.executes((command) -> {
				CommandSourceStack source = command.getSource();
				
				showUsage(source);
				return 1;
			})
			.then(Commands.literal("usage")
			.executes((command) -> {
				CommandSourceStack source = command.getSource();
				
				showUsage(source);
				return 1;
			}))
			.then(Commands.literal("list")
			.executes((command) -> {
				CommandSourceStack source = command.getSource();
				
				ArrayList<String> mobnames = new ArrayList<>();
				for (EntityType<?> et : Util.mobdrops.keySet()) {
					String lowerregister = BuiltInRegistries.ENTITY_TYPE.getKey(et).toString().toLowerCase();
					String[] nspl = lowerregister.split(":");
					if (nspl.length < 2) {
						continue;
					}
					
					String after = nspl[1];
					if (!nspl[0].equalsIgnoreCase("minecraft")) {
						after = lowerregister.replace(":", "-");
					}
					
					mobnames.add(after);
				}
				
				Collections.sort(mobnames);
				
				StringBuilder output = new StringBuilder();
				for (String mobname : mobnames) {
					if (!output.toString().isEmpty()) {
						output.append(", ");
					}
					
					output.append(mobname);
				}
				
				output.append(".");
				
				MessageFunctions.sendTranslatableMessage(source, "collective.configurableextramobdrops.message.availableentitynames", true, ChatFormatting.DARK_GREEN);
				MessageFunctions.sendMessage(source, output.toString(), ChatFormatting.YELLOW);
				MessageFunctions.sendTranslatableMessage(source, "collective.configurableextramobdrops.message.adddropcemd", ChatFormatting.DARK_GRAY);
				MessageFunctions.sendTranslatableMessage(source, "collective.configurableextramobdrops.message.notemoddedentities", ChatFormatting.RED);
				MessageFunctions.sendMessage(source, "", ChatFormatting.RED);
				return 1;
			}))
			.then(Commands.literal("reload")
			.executes((command) -> {
				CommandSourceStack source = command.getSource();
				
				try {
					Util.loadMobConfigFile(source.getLevel());
				} catch (Exception ex) {
					MessageFunctions.sendTranslatableMessage(source, "collective.configurableextramobdrops.message.somethingwentwrongwhilereloading", ChatFormatting.RED);
					return 0;
				}
				
				MessageFunctions.sendTranslatableMessage(source, "collective.configurableextramobdrops.message.successfullyloadedmob", ChatFormatting.DARK_GREEN);
				return 1;
			}))
			.then(Commands.literal("addhand")
			.then(Commands.argument("entity-name", StringArgumentType.word())
			.executes((command) -> processAddhand(command, 1.0, new CompoundTag()))
			.then(Commands.argument("drop-chance", DoubleArgumentType.doubleArg())
			.executes((command) -> processAddhand(command, DoubleArgumentType.getDouble(command, "drop-chance"), new CompoundTag()))
			.then(Commands.argument("min-amount", IntegerArgumentType.integer(0, 64))
			.then(Commands.argument("max-amount", IntegerArgumentType.integer(0, 64))
			.executes((command) -> processAddhand(command, DoubleArgumentType.getDouble(command, "drop-chance"), getAmountSettings(command)))
			.then(Commands.argument("enchant-level", IntegerArgumentType.integer(1, 50))
			.executes((command) -> {
				CompoundTag dropSettings = getAmountSettings(command);
				dropSettings.putInt("enchantLevel", IntegerArgumentType.getInteger(command, "enchant-level"));
				return processAddhand(command, DoubleArgumentType.getDouble(command, "drop-chance"), dropSettings);
			})))))))
			.then(Commands.literal("cleardrops")
			.then(Commands.argument("entity-name", StringArgumentType.word())
			.executes((command) -> {
				CommandSourceStack source = command.getSource();

				String entityname = StringArgumentType.getString(command, "entity-name").toLowerCase().trim();
				EntityType<?> entitytype = null;
				
				for (EntityType<?> et : Util.mobdrops.keySet()) {
					String registrystring = BuiltInRegistries.ENTITY_TYPE.getKey(et).toString();
					if (!registrystring.contains(":")) {
						continue;
					}
					
					if (entityname.contains("-")) {
						if (registrystring.equalsIgnoreCase(entityname.replace("-", ":"))) {
							entitytype = et;
							break;
						}
					}
					else if (registrystring.split(":")[1].equalsIgnoreCase(entityname)) {
						entitytype = et;
						break;
					}
				}
				
				if (entitytype == null) {
					MessageFunctions.sendTranslatableMessage(source, "collective.configurableextramobdrops.message.unablefindentityname", ChatFormatting.RED, entityname);
					showList(source);
					return 0;
				}
				
				if (!Util.mobdrops.containsKey(entitytype)) {
					MessageFunctions.sendTranslatableMessage(source, "collective.configurableextramobdrops.message.unablefindentitynamedrop", ChatFormatting.RED, entityname);
					showList(source);
					return 0;					
				}
				
				Util.mobdrops.put(entitytype, new CopyOnWriteArrayList<>());
				
				try {
					if (!Util.writeDropsMapToFile(source.getLevel())) {
						MessageFunctions.sendTranslatableMessage(source, "collective.configurableextramobdrops.message.somethingwentwrongwhile", ChatFormatting.RED);
					}
				} catch (Exception ex) {
					MessageFunctions.sendTranslatableMessage(source, "collective.configurableextramobdrops.message.somethingwentwrongwhilewriting", ChatFormatting.RED);
				}
				
				MessageFunctions.sendTranslatableMessage(source, "collective.configurableextramobdrops.message.successfullycleareddrops", ChatFormatting.DARK_GREEN, entitytype.getDescription().getString());
				return 1;
			})))
		);
	}
	
	private static CompoundTag getAmountSettings(CommandContext<CommandSourceStack> command) {
		CompoundTag dropSettings = new CompoundTag();
		dropSettings.putInt("minAmount", IntegerArgumentType.getInteger(command, "min-amount"));
		dropSettings.putInt("maxAmount", IntegerArgumentType.getInteger(command, "max-amount"));
		return dropSettings;
	}

	private static int processAddhand(CommandContext<CommandSourceStack> command, double dropChance, CompoundTag dropSettings) {
		CommandSourceStack source = command.getSource();

		if (dropChance < 0 || dropChance > 1.0) {
			MessageFunctions.sendTranslatableMessage(source, "collective.configurableextramobdrops.message.chancebetween0", ChatFormatting.RED);
			return 0;
		}

		if (dropSettings.getIntOr("minAmount", 0) > dropSettings.getIntOr("maxAmount", 64)) {
			MessageFunctions.sendTranslatableMessage(source, "collective.configurableextramobdrops.message.minamounthigher", ChatFormatting.RED);
			return 0;
		}

		Player player;
		try {
			player = source.getPlayerOrException();
		}
		catch (CommandSyntaxException ex) {
			MessageFunctions.sendTranslatableMessage(source, "collective.shared.message.playeronly", ChatFormatting.RED);
			return 1;
		}
		
		String entityname = StringArgumentType.getString(command, "entity-name").toLowerCase().trim();
		EntityType<?> entitytype = null;
		
		for (EntityType<?> et : Util.mobdrops.keySet()) {
			String registrystring = BuiltInRegistries.ENTITY_TYPE.getKey(et).toString();
			if (!registrystring.contains(":")) {
				continue;
			}
			
			if (entityname.contains("-")) {
				if (registrystring.equalsIgnoreCase(entityname.replace("-", ":"))) {
					entitytype = et;
					break;
				}
			}
			else if (registrystring.split(":")[1].equalsIgnoreCase(entityname)) {
				entitytype = et;
				break;
			}
		}
		
		if (entitytype == null) {
			MessageFunctions.sendTranslatableMessage(source, "collective.configurableextramobdrops.message.unablefindentityname", ChatFormatting.RED, entityname);
			showList(source);
			return 0;
		}
		
		if (!Util.mobdrops.containsKey(entitytype)) {
			MessageFunctions.sendTranslatableMessage(source, "collective.configurableextramobdrops.message.unablefindentitynamedrop", ChatFormatting.RED, entityname);
			showList(source);
			return 0;					
		}
		
		ItemStack hand = player.getMainHandItem();
		if (hand.isEmpty()) {
			MessageFunctions.sendTranslatableMessage(source, "collective.configurableextramobdrops.message.handemptyunable", ChatFormatting.RED);
			return 0;
		}

		Level level = player.level();

		dropSettings.putDouble("dropChance", dropChance);

		ItemStack toAddStack = hand.copy();
		CompoundTag customTag = toAddStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		customTag.merge(dropSettings);
		toAddStack.set(DataComponents.CUSTOM_DATA, CustomData.of(customTag));

		Util.mobdrops.get(entitytype).add(toAddStack);
		
		try {
			if (!Util.writeDropsMapToFile(level)) {
				MessageFunctions.sendTranslatableMessage(source, "collective.configurableextramobdrops.message.somethingwentwrongwhile", ChatFormatting.RED);
			}
		} catch (Exception ex) {
			MessageFunctions.sendTranslatableMessage(source, "collective.configurableextramobdrops.message.somethingwentwrongwhilewriting", ChatFormatting.RED);
		}
		
		MessageFunctions.sendTranslatableMessage(source, "collective.configurableextramobdrops.message.successfullyaddeddrop", ChatFormatting.DARK_GREEN, Util.getAmountText(toAddStack, dropSettings), toAddStack.getHoverName().getString().toLowerCase(), entitytype.getDescription().getString(), dropChance);
		if (dropSettings.contains("enchantLevel")) {
			MessageFunctions.sendTranslatableMessage(source, "collective.configurableextramobdrops.message.enchantedeachdrop", ChatFormatting.DARK_GREEN, dropSettings.getIntOr("enchantLevel", 1));
		}
		return 1;
	}

	private static void showUsage(CommandSourceStack source) {
		MessageFunctions.sendCommandUsageHeader(source, Reference.NAME);
		MessageFunctions.sendCommandUsageLine(source, "cemd usage", "collective.shared.message.showmessage");
		MessageFunctions.sendCommandUsageLine(source, "cemd list", "collective.configurableextramobdrops.message.listsavailableentities");
		MessageFunctions.sendCommandUsageLine(source, "cemd reload", "collective.shared.message.reloadsconfigfile");
		MessageFunctions.sendCommandUsageLine(source, "cemd addhand <entity-name>", "collective.configurableextramobdrops.message.addhandentitydrops");
		MessageFunctions.sendCommandUsageLine(source, "cemd addhand <entity-name> <drop-chance>", "collective.configurableextramobdrops.message.addhandentitydropsdrop");
		MessageFunctions.sendCommandUsageLine(source, "cemd addhand <entity-name> <drop-chance> <min-amount> <max-amount>", "collective.configurableextramobdrops.message.addhandentitydropsamount");
		MessageFunctions.sendCommandUsageLine(source, "cemd addhand <entity-name> <drop-chance> <min-amount> <max-amount> <enchant-level>", "collective.configurableextramobdrops.message.addhandentitydropsenchant");
		MessageFunctions.sendCommandUsageLine(source, "cemd cleardrops <entity-name>", "collective.configurableextramobdrops.message.clearsdropsspecified");
	}
	
	private static void showList(CommandSourceStack source) {
		MessageFunctions.sendMessage(source, " /cemd list", ChatFormatting.DARK_GREEN);
		MessageFunctions.sendTranslatableMessage(source, "  ", "collective.configurableextramobdrops.message.listsavailableentities", ChatFormatting.DARK_GRAY);
	}
}
