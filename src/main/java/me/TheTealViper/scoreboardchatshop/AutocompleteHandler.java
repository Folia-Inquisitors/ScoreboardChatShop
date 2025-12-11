package me.TheTealViper.scoreboardchatshop;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public class AutocompleteHandler implements TabCompleter{
	ScoreboardChatShop SCS;
	List<String> setpriceOptions = new ArrayList<String>();
	List<String> buysellOptions = new ArrayList<String>();

	public AutocompleteHandler (ScoreboardChatShop SCS) {
		this.SCS = SCS;
		setpriceOptions.add("buy");
		setpriceOptions.add("sell");
	}
	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
		List<String> completions = new ArrayList<String>();
		
		if (args.length == 0) {
			//Do nothing
		} else if (args.length == 1) {
			if (label.equalsIgnoreCase("price")) {
				StringUtil.copyPartialMatches(args[0], getInventoryPresentPriceMaterials(sender), completions);
				return completions;
			}else if (label.equalsIgnoreCase("setprice")) {
				StringUtil.copyPartialMatches(args[0], setpriceOptions, completions);
				return completions;
			} else if (label.equalsIgnoreCase("buy")) {
				StringUtil.copyPartialMatches(args[0], getInventoryPresentBuyMaterials(sender), completions);
				return completions;
			} else if (label.equalsIgnoreCase("sell")) {
				StringUtil.copyPartialMatches(args[0], getInventoryPresentSellMaterials(sender), completions);
				return completions;
			}
		} else if (args.length == 2) {
			if (label.equalsIgnoreCase("setprice")) {
				StringUtil.copyPartialMatches(args[1], SCS.materialExpansiveList, completions);
				return completions;
			} else if (label.equalsIgnoreCase("buy")) {
				Player p = (Player) sender;
				List<String> options = new ArrayList<String>();
				if (args[0].equalsIgnoreCase("hand") && p.getInventory().getItemInMainHand() != null && !p.getInventory().getItemInMainHand().getType().equals(Material.AIR))
					args[0] = p.getInventory().getItemInMainHand().getType().name();
				String parentName = SCS.aliasDatabase.get(args[0].toLowerCase());
				if (parentName == null) return options;
				if (!SCS.buyAmountDatabase.containsKey(parentName)) return options;
				
				options.add((SCS.buyAmountDatabase.get(parentName) * 1) + "");
				options.add((SCS.buyAmountDatabase.get(parentName) * 2) + "");
				options.add((SCS.buyAmountDatabase.get(parentName) * 3) + "");
				options.add("max");
				StringUtil.copyPartialMatches(args[1], options, completions);
				return completions;
			} else if (label.equalsIgnoreCase("sell")) {
				Player p = (Player) sender;
				List<String> options = new ArrayList<String>();
				if (args[0].equalsIgnoreCase("hand") && p.getInventory().getItemInMainHand() != null && !p.getInventory().getItemInMainHand().getType().equals(Material.AIR))
					args[0] = p.getInventory().getItemInMainHand().getType().name();
				String parentName = SCS.aliasDatabase.get(args[0].toLowerCase());
				if (parentName == null) return options;
				if (!SCS.sellAmountDatabase.containsKey(parentName)) return options;
				
				options.add((SCS.sellAmountDatabase.get(parentName) * 1) + "");
				options.add((SCS.sellAmountDatabase.get(parentName) * 2) + "");
				options.add((SCS.sellAmountDatabase.get(parentName) * 3) + "");
				options.add("max");
				StringUtil.copyPartialMatches(args[1], options, completions);
				return completions;
			}
		} else if (args.length == 3) {
			//Do nothing
		} else if (args.length == 4) {
			//Do nothing
		}
		
		return completions;
	}
	
	public List<String> getInventoryPresentPriceMaterials (CommandSender sender) {
		if (!(sender instanceof Player))
			return SCS.materialPriceList;
		else {
			Player p = (Player) sender;
			List<String> foundMaterials = new ArrayList<String>(SCS.materialPriceList);
			if (p.getInventory().getItemInMainHand() != null
					&& !p.getInventory().getItemInMainHand().getType().equals(Material.AIR)
					&& (SCS.buyPriceDatabase.containsKey(p.getInventory().getItemInMainHand().getType().name().toLowerCase()) || SCS.sellPriceDatabase.containsKey(p.getInventory().getItemInMainHand().getType().name().toLowerCase()))) {
				//Do nothing this means hand stays
			} else {
				foundMaterials.remove("hand");
			}
			return foundMaterials;
		}
	}
	
	public List<String> getInventoryPresentBuyMaterials (CommandSender sender) {
		if (!(sender instanceof Player))
			return SCS.materialBuyList;
		else {
			Player p = (Player) sender;
			List<String> foundMaterials = new ArrayList<String>(SCS.materialBuyList);
			if (p.getInventory().getItemInMainHand() != null
					&& !p.getInventory().getItemInMainHand().getType().equals(Material.AIR)
					&& SCS.buyPriceDatabase.containsKey(p.getInventory().getItemInMainHand().getType().name().toLowerCase())) {
				//Do nothing this means hand stays
			} else {
				foundMaterials.remove("hand");
			}
			return foundMaterials;
		}
	}
	
	public List<String> getInventoryPresentSellMaterials (CommandSender sender) {
		if (!(sender instanceof Player))
			return SCS.materialSellList;
		else {
			Player p = (Player) sender;
			List<String> foundMaterials = new ArrayList<String>();
			boolean bypassInvCheck = !SCS.getConfig().getBoolean("Minimal_Sell_Autocomplete");
			for (String matName : SCS.materialSellList) {
				String parentName = SCS.aliasDatabase.get(matName);
				if (parentName == null) {
					continue;
				}
				Material mat = Material.getMaterial(parentName.toUpperCase());
				if (mat != null && (p.getInventory().first(Material.valueOf(parentName.toUpperCase())) != -1 || bypassInvCheck))
					foundMaterials.add(matName);
				else if (parentName.equalsIgnoreCase("hand")
						&& p.getInventory().getItemInMainHand() != null
						&& !p.getInventory().getItemInMainHand().getType().equals(Material.AIR)
						&& SCS.sellPriceDatabase.containsKey(p.getInventory().getItemInMainHand().getType().name().toLowerCase())) {
					foundMaterials.add("hand");
				}
			}
			return foundMaterials;
		}
	}
	
	void debugList (List<String> list) {
		Bukkit.broadcastMessage("- - - - - - -");
		for (String s : list) {
			Bukkit.broadcastMessage(" - " + s);
		}
	}

}
