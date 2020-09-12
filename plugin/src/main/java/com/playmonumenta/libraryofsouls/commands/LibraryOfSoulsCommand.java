package com.playmonumenta.libraryofsouls.commands;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.spigotmc.event.entity.EntityDismountEvent;

import com.goncalomb.bukkit.nbteditor.bos.BookOfSouls;
import com.goncalomb.bukkit.nbteditor.nbt.EntityNBT;
import com.playmonumenta.libraryofsouls.LibraryOfSouls;
import com.playmonumenta.libraryofsouls.Soul;
import com.playmonumenta.libraryofsouls.SoulEntry;
import com.playmonumenta.libraryofsouls.SoulsDatabase;
import com.playmonumenta.libraryofsouls.SoulsInventory;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.DynamicSuggestedStringArgument;
import io.github.jorelali.commandapi.api.arguments.DynamicSuggestedStringArgument.DynamicSuggestions;
import io.github.jorelali.commandapi.api.arguments.IntegerArgument;
import io.github.jorelali.commandapi.api.arguments.LiteralArgument;
import io.github.jorelali.commandapi.api.arguments.LocationArgument;
import io.github.jorelali.commandapi.api.exceptions.WrapperCommandSyntaxException;

public class LibraryOfSoulsCommand implements Listener {
	/* Several sub commands have this same tab completion */
	private static final DynamicSuggestions listMobs = () -> SoulsDatabase.getInstance().listMobNames().toArray(new String[0]);

	public static void register() {
		LinkedHashMap<String, Argument> arguments;
		final CommandAPI api = CommandAPI.getInstance();

		/* los open */
		arguments = new LinkedHashMap<>();
		arguments.put("open", new LiteralArgument("open"));
		api.register("los", CommandPermission.fromString("los.open"), arguments, (sender, args) -> {
			Player player = getPlayer(sender);
			(new SoulsInventory(player, SoulsDatabase.getInstance().getSouls(), ""))
				.openInventory(player, LibraryOfSouls.getInstance());
		});

		/* los get <name> */
		arguments = new LinkedHashMap<>();
		arguments.put("get", new LiteralArgument("get"));
		arguments.put("name", new DynamicSuggestedStringArgument(listMobs));
		api.register("los", CommandPermission.fromString("los.get"), arguments, (sender, args) -> {
			PlayerInventory inv = getPlayer(sender).getInventory();
			if (inv.firstEmpty() == -1) {
				CommandAPI.fail("Your inventory is full!");
			}
			inv.addItem(getSoul((String)args[0]).getBoS());
		});

		/* los history <name> */
		arguments = new LinkedHashMap<>();
		arguments.put("history", new LiteralArgument("history"));
		arguments.put("name", new DynamicSuggestedStringArgument(listMobs));
		api.register("los", CommandPermission.fromString("los.history"), arguments, (sender, args) -> {
			Player player = getPlayer(sender);
			(new SoulsInventory(player, getSoul((String)args[0]).getHistory(), "History"))
				.openInventory(player, LibraryOfSouls.getInstance());
		});

		/* los summon <location> <name> */
		arguments = new LinkedHashMap<>();
		arguments.put("summon", new LiteralArgument("summon"));
		arguments.put("location", new LocationArgument());
		arguments.put("name", new DynamicSuggestedStringArgument(listMobs));
		api.register("los", CommandPermission.fromString("los.summon"), arguments, (sender, args) -> {
			getSoul((String)args[1]).summon((Location)args[0]);
		});

		/* los search <area> */
		arguments = new LinkedHashMap<>();
		arguments.put("search", new LiteralArgument("search"));
		arguments.put("area", new DynamicSuggestedStringArgument(() -> SoulsDatabase.getInstance().listMobLocations().toArray(new String[0])));
		api.register("los", CommandPermission.fromString("los.search"), arguments, (sender, args) -> {
			Player player = getPlayer(sender);
			String area = (String)args[0];
			List<SoulEntry> souls = SoulsDatabase.getInstance().getSoulsByLocation((String)args[0]);
			if (souls == null) {
				CommandAPI.fail("Area '" + area + "' not found");
			}
			(new SoulsInventory(player, souls, area))
				.openInventory(player, LibraryOfSouls.getInstance());
		});

		arguments = new LinkedHashMap<>();
		arguments.put("mount", new LiteralArgument("mount"));
		arguments.put("name", new DynamicSuggestedStringArgument(listMobs));
		api.register("los", CommandPermission.fromString("los.mount"), arguments, (sender, args) -> {
			if (!(sender instanceof Player)) {
				return;
			}
			Player player = (Player)sender;
			Entity entity = EntityNBT.fromEntityData(getSoul((String)args[0]).getNBT()).spawn(player.getLocation());
			entity.addScoreboardTag("ismount");
			Bukkit.broadcastMessage(entity.getScoreboardTags().toString());
			entity.addPassenger(player);
		});

		arguments = new LinkedHashMap<>();
		arguments.put("eject", new LiteralArgument("eject"));
		arguments.put("location", new LocationArgument());
		arguments.put("radius", new IntegerArgument());
		api.register("los", CommandPermission.fromString("los.mount"), arguments, (sender, args) -> {
			Collection<LivingEntity> entities = ((Location)args[0]).getNearbyLivingEntities((Integer)args[1]);
			for (LivingEntity e : entities) {
				e.eject();
			}
		});

		arguments = new LinkedHashMap<>();
		arguments.put("eject", new LiteralArgument("eject"));
		arguments.put("location", new LocationArgument());
		arguments.put("radius", new IntegerArgument());
		arguments.put("name", new DynamicSuggestedStringArgument(listMobs));
		api.register("los", CommandPermission.fromString("los.mount"), arguments, (sender, args) -> {
			Soul soul = SoulsDatabase.getInstance().getSoul((String)args[2]);
			Collection<LivingEntity> entities = ((Location)args[0]).getNearbyLivingEntities((Integer)args[1]);
			for (LivingEntity e : entities) {
				if (soul.getName() == e.getName()) {
					e.eject();
				}
			}
		});
	}

	public static void registerWriteAccessCommands() {
		LinkedHashMap<String, Argument> arguments;
		final CommandAPI api = CommandAPI.getInstance();

		/* los add */
		arguments = new LinkedHashMap<>();
		arguments.put("add", new LiteralArgument("add"));
		api.register("los", CommandPermission.fromString("los.add"), arguments, (sender, args) -> {
			Player player = getPlayer(sender);
			BookOfSouls bos = getBos(player);
			if (bos == null) {
				CommandAPI.fail("You must be holding a Book of Souls");
			}

			SoulsDatabase.getInstance().add(player, bos);
		});

		/* los update */
		arguments = new LinkedHashMap<>();
		arguments.put("update", new LiteralArgument("update"));
		api.register("los", CommandPermission.fromString("los.update"), arguments, (sender, args) -> {
			Player player = getPlayer(sender);
			BookOfSouls bos = getBos(player);
			if (bos == null) {
				CommandAPI.fail("You must be holding a Book of Souls");
			}

			SoulsDatabase.getInstance().update(player, bos);
		});

		/* los del <name> */
		arguments = new LinkedHashMap<>();
		arguments.put("del", new LiteralArgument("del"));
		arguments.put("name", new DynamicSuggestedStringArgument(listMobs));
		api.register("los", CommandPermission.fromString("los.del"), arguments, (sender, args) -> {
			SoulsDatabase.getInstance().del(sender, (String)args[0]);
		});
	}

	private static SoulEntry getSoul(String name) throws WrapperCommandSyntaxException {
		SoulEntry soul = SoulsDatabase.getInstance().getSoul(name);
		if (soul != null) {
			return soul;
		}

		CommandAPI.fail("Soul '" + name + "' not found");
		return null;
	}

	private static Player getPlayer(CommandSender sender) throws WrapperCommandSyntaxException {
		if (sender instanceof Player) {
			return (Player) sender;
		} else if ((sender instanceof ProxiedCommandSender) && (((ProxiedCommandSender)sender).getCallee() instanceof Player)) {
			return (Player) ((ProxiedCommandSender)sender).getCallee();
		}

		CommandAPI.fail("This command must be run by / as a player");
		return null;
	}

	private static BookOfSouls getBos(Player player) throws WrapperCommandSyntaxException {
		ItemStack item = player.getInventory().getItemInMainHand();
		if (BookOfSouls.isValidBook(item)) {
			BookOfSouls bos = BookOfSouls.getFromBook(item);
			if (bos != null) {
				return bos;
			}
			CommandAPI.fail("That Book of Souls is corrupted!");
		}
		CommandAPI.fail("You must be holding a Book of Souls!");
		return null;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void entityDismountEvent(EntityDismountEvent event) {
		Entity entity = event.getDismounted();
		Bukkit.broadcastMessage(entity.getScoreboardTags().toString());
		if (entity.getScoreboardTags().contains("ismount")) {
			if (entity instanceof LivingEntity) {
				LivingEntity lEntity = (LivingEntity) entity;
				lEntity.teleport(entity.getLocation().subtract(0, 1000, 0));
				lEntity.setHealth(0);
			} else {
				entity.teleport(entity.getLocation().subtract(0, 1000, 0));
			}
		}
	}
}
