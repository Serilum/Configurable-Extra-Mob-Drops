package com.natamus.configurableextramobdrops.cmd;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.natamus.collective.functions.MessageFunctions;
import com.natamus.configurableextramobdrops.util.Util;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;

public class CommandCemd {
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("cemd").requires((iCommandSender) -> iCommandSender.hasPermission(2))
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
				
				ArrayList<String> mobnames = new ArrayList<String>();
				for (EntityType<?> et : Util.mobdrops.keySet()) {
					String lowerregister = Registry.ENTITY_TYPE.getKey(et).toString().toLowerCase();
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
					if (!output.toString().equals("")) {
						output.append(", ");
					}
					
					output.append(mobname);
				}
				
				output.append(".");
				
				MessageFunctions.sendMessage(source, "Available entity names:", ChatFormatting.DARK_GREEN, true);
				MessageFunctions.sendMessage(source, output.toString(), ChatFormatting.YELLOW);
				MessageFunctions.sendMessage(source, "To add a drop: /cemd addhand <entity-name>", ChatFormatting.DARK_GRAY);
				MessageFunctions.sendMessage(source, "Note: for modded entities use - not :", ChatFormatting.RED);
				MessageFunctions.sendMessage(source, "", ChatFormatting.RED);
				return 1;
			}))
			.then(Commands.literal("reload")
			.executes((command) -> {
				CommandSourceStack source = command.getSource();
				
				try {
					Util.loadMobConfigFile();
				} catch (Exception ex) {
					MessageFunctions.sendMessage(source, "Something went wrong while reloading the mob drop config file.", ChatFormatting.RED);
					ex.printStackTrace();
					return 0;
				}
				
				MessageFunctions.sendMessage(source, "Successfully loaded the mob drop config file.", ChatFormatting.DARK_GREEN);
				return 1;
			}))
			.then(Commands.literal("addhand")
			.then(Commands.argument("entity-name", StringArgumentType.word())
			.executes((command) -> {
				return processAddhand(command, 1.0);
			})))
			.then(Commands.literal("addhand")
			.then(Commands.argument("entity-name", StringArgumentType.word())
			.then(Commands.argument("drop-chance", DoubleArgumentType.doubleArg())
			.executes((command) -> {
				CommandSourceStack source = command.getSource();
				
				double chance = DoubleArgumentType.getDouble(command, "drop-chance");
				if (chance < 0 || chance > 1.0) {
					MessageFunctions.sendMessage(source, "The chance has to be in between 0 and 1.0.", ChatFormatting.RED);
					return 0;
				}
				
				return processAddhand(command, chance);
			}))))
			.then(Commands.literal("cleardrops")
			.then(Commands.argument("entity-name", StringArgumentType.word())
			.executes((command) -> {
				CommandSourceStack source = command.getSource();

				String entityname = StringArgumentType.getString(command, "entity-name").toLowerCase().trim();
				EntityType<?> entitytype = null;
				
				for (EntityType<?> et : Util.mobdrops.keySet()) {
					String registrystring = Registry.ENTITY_TYPE.getKey(et).toString();
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
					MessageFunctions.sendMessage(source, "Unable to find an entity with the name '" + entityname + "'.", ChatFormatting.RED);
					showList(source);
					return 0;
				}
				
				if (!Util.mobdrops.containsKey(entitytype)) {
					MessageFunctions.sendMessage(source, "Unable to find an entity with the name '" + entityname + "' in the drop hashmap.", ChatFormatting.RED);
					showList(source);
					return 0;					
				}
				
				Util.mobdrops.put(entitytype, new CopyOnWriteArrayList<ItemStack>());
				
				try {
					if (!Util.writeDropsMapToFile()) {
						MessageFunctions.sendMessage(source, "!Something went wrong while writing the new config.", ChatFormatting.RED);
					}
				} catch (Exception ex) {
					MessageFunctions.sendMessage(source, "Something went wrong while writing the new config.", ChatFormatting.RED);
					ex.printStackTrace();
				}
				
				MessageFunctions.sendMessage(source, "Successfully cleared all drops for the entity '" + entitytype.getDescription().getString() + "'.", ChatFormatting.DARK_GREEN);
				return 1;
			})))
		);
	}
	
	private static int processAddhand(CommandContext<CommandSourceStack> command, double dropchance) {
		CommandSourceStack source = command.getSource();
		
		Player player;
		try {
			player = source.getPlayerOrException();
		}
		catch (CommandSyntaxException ex) {
			MessageFunctions.sendMessage(source, "This command can only be executed as a player in-game.", ChatFormatting.RED);
			return 1;
		}
		
		String entityname = StringArgumentType.getString(command, "entity-name").toLowerCase().trim();
		EntityType<?> entitytype = null;
		
		for (EntityType<?> et : Util.mobdrops.keySet()) {
			String registrystring = Registry.ENTITY_TYPE.getKey(et).toString();
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
			MessageFunctions.sendMessage(source, "Unable to find an entity with the name '" + entityname + "'.", ChatFormatting.RED);
			showList(source);
			return 0;
		}
		
		if (!Util.mobdrops.containsKey(entitytype)) {
			MessageFunctions.sendMessage(source, "Unable to find an entity with the name '" + entityname + "' in the drop hashmap.", ChatFormatting.RED);
			showList(source);
			return 0;					
		}
		
		ItemStack hand = player.getMainHandItem();
		if (hand.isEmpty()) {
			MessageFunctions.sendMessage(source, "Your hand is empty! Unable to add drop.", ChatFormatting.RED);
			return 0;
		}
		
		ItemStack toadd = hand.copy();
		CompoundTag nbt = toadd.getOrCreateTag();
		nbt.putDouble("dropchance", dropchance);
		toadd.setTag(nbt);
		
		Util.mobdrops.get(entitytype).add(toadd.copy());
		
		try {
			if (!Util.writeDropsMapToFile()) {
				MessageFunctions.sendMessage(source, "!Something went wrong while writing the new config.", ChatFormatting.RED);
			}
		} catch (Exception ex) {
			MessageFunctions.sendMessage(source, "Something went wrong while writing the new config.", ChatFormatting.RED);
			ex.printStackTrace();
		}
		
		MessageFunctions.sendMessage(source, "Successfully added '" + toadd.getCount() + " " + toadd.getHoverName().getString().toLowerCase() + "' as a drop for the entity '" + entitytype.getDescription().getString() + "' with a drop chance of '" + dropchance + "'.", ChatFormatting.DARK_GREEN);
		return 1;
	}
	
	private static void showUsage(CommandSourceStack source) {
		MessageFunctions.sendMessage(source, "Configurable Extra Mob Drops Usage:", ChatFormatting.DARK_GREEN, true);
		MessageFunctions.sendMessage(source, " /cemd usage", ChatFormatting.DARK_GREEN);
		MessageFunctions.sendMessage(source, "  Show this message.", ChatFormatting.DARK_GRAY);
		MessageFunctions.sendMessage(source, " /cemd list", ChatFormatting.DARK_GREEN);
		MessageFunctions.sendMessage(source, "  Lists available entities to add drops to.", ChatFormatting.DARK_GRAY);
		MessageFunctions.sendMessage(source, " /cemd reload", ChatFormatting.DARK_GREEN);
		MessageFunctions.sendMessage(source, "  Reloads the config file.", ChatFormatting.DARK_GRAY);
		MessageFunctions.sendMessage(source, " /cemd addhand <entity-name>", ChatFormatting.DARK_GREEN);
		MessageFunctions.sendMessage(source, "  Add your hand to the entity's drops with a 100% chance.", ChatFormatting.DARK_GRAY);
		MessageFunctions.sendMessage(source, " /cemd addhand <entity-name> <drop-chance>", ChatFormatting.DARK_GREEN);
		MessageFunctions.sendMessage(source, "  Add your hand to the entity's drops with drop-chance in between 0 and 1.0.", ChatFormatting.DARK_GRAY);
		MessageFunctions.sendMessage(source, " /cemd cleardrops <entity-name>", ChatFormatting.DARK_GREEN);
		MessageFunctions.sendMessage(source, "  Clears all drops of the specified entity.", ChatFormatting.DARK_GRAY);
	}
	
	private static void showList(CommandSourceStack source) {
		MessageFunctions.sendMessage(source, " /cemd list", ChatFormatting.DARK_GREEN);
		MessageFunctions.sendMessage(source, "  Lists available entities to add drops to.", ChatFormatting.DARK_GRAY);
	}
}
