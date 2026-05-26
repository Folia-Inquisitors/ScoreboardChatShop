package me.TheTealViper.scoreboardchatshop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;

import me.TheTealViper.scoreboardchatshop.utils.PluginFile;
import me.TheTealViper.scoreboardchatshop.utils.StringUtils;
import me.TheTealViper.scoreboardchatshop.utils.UtilityEquippedJavaPlugin;
import me.clip.placeholderapi.PlaceholderAPI;
import net.milkbowl.vault.economy.Economy;

public class ScoreboardChatShop extends UtilityEquippedJavaPlugin implements Listener{
	AutocompleteHandler AH;
//	public List<String> materialList = new ArrayList<String>(); //TODO: Make a set instead of a list
	public List<String> materialBuyList = new ArrayList<String>(); //TODO: Make a set instead of a list
	public List<String> materialSellList = new ArrayList<String>(); //TODO: Make a set instead of a list
	public List<String> materialExpansiveList = new ArrayList<String>(); //TODO: Make a set instead of a list
	public List<String> materialPriceList = new ArrayList<String>(); //This only has materials that can be bought/sold
	public Map<String, String> aliasDatabase = new HashMap<String, String>();
	Map<String, Integer> buyPriceDatabase = new HashMap<String, Integer>();
	Map<String, Integer> buyAmountDatabase = new HashMap<String, Integer>();
	Map<String, Integer> sellPriceDatabase = new HashMap<String, Integer>();
	Map<String, Integer> sellAmountDatabase = new HashMap<String, Integer>();
	PluginFile priceFile;
	PluginFile statsFile;
	PluginFile messagesFile;
	ScoreboardChatShopPlaceholderExpansion placeholderExpansion;
	private final Object statsLock = new Object();
	private static Economy econ = null;
	private boolean foundPlaceholderAPI = false;
	
	public void onEnable(){
		StartupPlugin(this, "-1");
		
		//Handle setup
		if (!setupEconomy()) {
            getLogger().severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
		if (!setupPlaceholders()) {
			getLogger().severe(String.format("[%s] - Limited functionality enabled because PlaceholderAPI was not found!", getDescription().getName()));
		}
		
		//Init yml files
		priceFile = new PluginFile(this, "prices.db");
		statsFile = new PluginFile(this, "stats.db");
		messagesFile = new PluginFile(this, "messages.yml", "messages.yml");
		loadMessageDefaults();
		
		//Load stuff in
		loadPriceDatabase();
		loadAliasAndMaterialDatabases();

		if (foundPlaceholderAPI) {
			placeholderExpansion = new ScoreboardChatShopPlaceholderExpansion(this);
			placeholderExpansion.register();
		}
		
		AH = new AutocompleteHandler(this);
		getCommand("price").setTabCompleter(AH);
		getCommand("setprice").setTabCompleter(AH);
		getCommand("buy").setTabCompleter(AH);
		getCommand("sell").setTabCompleter(AH);
		getCommand("shop").setTabCompleter(AH);
	}
	
	private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
        	getLogger().severe("Vault is not found. Disabling plugin.");
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
        	getLogger().severe("Vault has no registered economy. Disabling plugin.");
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }
	private boolean setupPlaceholders() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
        	foundPlaceholderAPI = true;
        }
        return foundPlaceholderAPI;
    }
	
	private void loadPriceDatabase() {
		ConfigurationSection buySec = priceFile.getConfigurationSection("Buy");
		if (buySec != null) {
			for (String matName : buySec.getKeys(false)) {
				buyPriceDatabase.put(matName.toLowerCase(), buySec.getInt(matName + ".price"));
				buyAmountDatabase.put(matName.toLowerCase(), buySec.getInt(matName + ".amount"));
			}
		}
		ConfigurationSection sellSec = priceFile.getConfigurationSection("Sell");
		if (sellSec != null) {
			for (String matName : sellSec.getKeys(false)) {
				sellPriceDatabase.put(matName.toLowerCase(), sellSec.getInt(matName + ".price"));
				sellAmountDatabase.put(matName.toLowerCase(), sellSec.getInt(matName + ".amount"));
			}
		}
	}

	private void loadMessageDefaults() {
		setMessageDefault("Help_Shop", "/shop check (username) - View a player's top shop items");
		setMessageDefault("Shop_Check_Header", "&6Shop stats for &e%scoreboardchatshop_player%&6:");
		setMessageDefault("Shop_Check_BuyHeader", "&aTop bought:");
		setMessageDefault("Shop_Check_SellHeader", "&aTop sold:");
		setMessageDefault("Shop_Check_BuyLine", "&7#%scoreboardchatshop_rank% &f%scoreboardchatshop_material% &7x%scoreboardchatshop_amount% &8- &cspent $%scoreboardchatshop_money%");
		setMessageDefault("Shop_Check_SellLine", "&7#%scoreboardchatshop_rank% &f%scoreboardchatshop_material% &7x%scoreboardchatshop_amount% &8- &aearned $%scoreboardchatshop_money%");
		setMessageDefault("Shop_Check_NoBoughtStats", "&7No bought item stats yet.");
		setMessageDefault("Shop_Check_NoSoldStats", "&7No sold item stats yet.");
		setMessageDefault("Shop_Check_NoStats", "&7No shop stats found for %scoreboardchatshop_player%.");
	}

	private void setMessageDefault(String path, String value) {
		if (messagesFile.isSet(path))
			return;
		messagesFile.set(path, value);
	}
	
	private void loadAliasAndMaterialDatabases() {
//		materialList = new ArrayList<String>(); //TODO: Make a set instead of a list
		materialBuyList = new ArrayList<String>(); //TODO: Make a set instead of a list
		materialSellList = new ArrayList<String>(); //TODO: Make a set instead of a list
		materialExpansiveList = new ArrayList<String>(); //TODO: Make a set instead of a list
		materialPriceList = new ArrayList<String>();
		aliasDatabase = new HashMap<String, String>();
		
		//Load base materials into expansive mat list
		for (Material m : Material.values()) {
			aliasDatabase.put(m.name().toLowerCase(), m.name().toLowerCase());
			materialExpansiveList.add(m.name().toLowerCase());
		}
		
		//Load mats from prices into mat list
		for (String matName : buyPriceDatabase.keySet()) {
//			materialList.add(matName);
			materialBuyList.add(matName);
			materialPriceList.add(matName);
			
		}
		for (String matName : sellPriceDatabase.keySet()) {
//			if (!materialList.contains(matName))
//				materialList.add(matName);
			materialSellList.add(matName);
			if (!materialPriceList.contains(matName))
				materialPriceList.add(matName);
		}
		
		//Load aliases
		ConfigurationSection sec = getConfig().getConfigurationSection("Aliases");
		if (sec != null) {
			for (String parentMatName : sec.getKeys(false)) {
				String parentMatNameLower = parentMatName.toLowerCase();
				for (String aliasName : sec.getStringList(parentMatName)) {
					String aliasNameLower = aliasName.toLowerCase();
					aliasDatabase.put(aliasNameLower, parentMatNameLower);
//				materialList.add(aliasName.toLowerCase());
					if (materialBuyList.contains(parentMatNameLower))
						materialBuyList.add(aliasNameLower);
					if (materialSellList.contains(parentMatNameLower))
						materialSellList.add(aliasNameLower);
					materialExpansiveList.add(aliasNameLower);
				}
			}
		}
		
		//Sort material list
//		Collections.sort(materialList); //This is useless because the client alphabetizes automatically no matter what???
		
		//Load hand into top of mat list
		//This was meant to force it to be the first option in the list but the client fucking alphabetizes soooo yeah dumb people at microsoft >:(
		aliasDatabase.put("hand", "hand");
//		materialList.add(0, "hand");
		materialBuyList.add("hand");
		materialSellList.add("hand");
		materialExpansiveList.add("hand");
		materialPriceList.add("hand");
	}
	
	public void onDisable(){
		if (placeholderExpansion != null)
			placeholderExpansion.unregister();
		getServer().getConsoleSender().sendMessage("SimpleChatShop from TheTealViper shutting down. Bshzzzzzz");
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
		// price (item/hand) <amount>
		// setprice (sell/buy) (item/hand) ($) <amount>
		// buy (item/hand) <amount>
		// sell (item/hand) <amount>
		// If attempting to buy or sell that requires change, don't allow and tell stack size
		
		//Handle admin functionality
		Player p = (sender instanceof Player) ? (Player) sender : null;
		if (label.equalsIgnoreCase("shop"))
			return handleShopCommand(sender, args);
		if (args.length == 0) {
			if (label.equalsIgnoreCase("price")) {
				if (p != null) {
					p.sendMessage(formatString(p, messagesFile.getString("Help_Price"), null, null, null, null, null, null));
				} else {
					sender.sendMessage(formatString(p, messagesFile.getString("Price_ConsoleAttempt"), null, null, null, null,null, null));
				}
			} else if (label.equalsIgnoreCase("setprice")) {
				if (sender.hasPermission("ScoreboardChatShop"))
					explainCommands(sender, "setprice");
				else
					warnMissingPerms(sender);
				sender.sendMessage(formatString(p, messagesFile.getString("Help_SetPrice"), null, null, null, null,null, null));
			} else if (label.equalsIgnoreCase("buy")) {
				sender.sendMessage(formatString(p, messagesFile.getString("Help_Buy"), null, null, null, null,null, null));
			} else if (label.equalsIgnoreCase("sell")) {
				sender.sendMessage(formatString(p, messagesFile.getString("Help_Sell"), null, null, null, null,null, null));
			} else {
				explainCommands(sender, null);
			}
		} else if (args.length == 1) {
			if (label.equalsIgnoreCase("price")) {
				handlePrice(sender, args[0], 1);
			} else if (label.equalsIgnoreCase("setprice")) {
				if (sender.hasPermission("ScoreboardChatShop"))
					explainCommands(sender, "setprice");
				else
					warnMissingPerms(sender);
			} else if (label.equalsIgnoreCase("buy")) {
				if (p != null)
					handleBuy(p, args[0].toLowerCase(), "1");
				else
					warnConsoleSender(sender);
			} else if (label.equalsIgnoreCase("sell")) {
				if (p != null)
					handleSell(p, args[0].toLowerCase(), "1");
				else
					warnConsoleSender(sender);
			} else {
				explainCommands(sender, null);
			}
		} else if (args.length == 2) {
			if (label.equalsIgnoreCase("price")) {
				try {
					handlePrice(sender, args[0], Integer.parseInt(args[1]));
				} catch (NumberFormatException e) {
					warnBadNumberFormat(sender);
				}
			} else if (label.equalsIgnoreCase("setprice")) {
				if (sender.hasPermission("ScoreboardChatShop"))
					explainCommands(sender, "setprice");
				else
					warnMissingPerms(sender);
			} else if (label.equalsIgnoreCase("buy")) {
				if (p != null) {
					handleBuy(p, args[0].toLowerCase(), args[1]);
				} else {
					warnConsoleSender(sender);
				}
			} else if (label.equalsIgnoreCase("sell")) {
				if (p != null) {
					handleSell(p, args[0].toLowerCase(), args[1]);
				} else {
					warnConsoleSender(sender);
				}
			} else {
				explainCommands(sender, null);
			}
		} else if (args.length == 3) {
			if (label.equalsIgnoreCase("price")) {
				explainCommands(sender, "price");
			} else if (label.equalsIgnoreCase("setprice")) {
				try {
					if (sender.hasPermission("ScoreboardChatShop"))
						handleSetPrice(sender, args[0], args[1], Integer.parseInt(args[2]), 1);
					else
						warnMissingPerms(sender);
				} catch (NumberFormatException e) {
					warnBadNumberFormat(sender);
				}
			} else if (label.equalsIgnoreCase("buy")) {
				if (p != null) {
					explainCommands(sender, "buy");
				} else {
					warnConsoleSender(sender);
				}
			} else if (label.equalsIgnoreCase("sell")) {
				if (p != null) {
					explainCommands(sender, "sell");
				} else {
					warnConsoleSender(sender);
				}
			} else {
				explainCommands(sender, null);
			}
		} else if (args.length == 4) {
			if (label.equalsIgnoreCase("price")) {
				explainCommands(sender, "price");
			} else if (label.equalsIgnoreCase("setprice")) {
				try {
					if (sender.hasPermission("ScoreboardChatShop"))
						handleSetPrice(sender, args[0], args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]));
					else
						warnMissingPerms(sender);
				} catch (NumberFormatException e) {
					warnBadNumberFormat(sender);
				}
			} else if (label.equalsIgnoreCase("buy")) {
				if (p != null) {
					explainCommands(sender, "buy");
				} else {
					warnConsoleSender(sender);
				}
			} else if (label.equalsIgnoreCase("sell")) {
				if (p != null) {
					explainCommands(sender, null);
				} else {
					warnConsoleSender(sender);
				}
			} else {
				explainCommands(sender, null);
			}
		}
        
        return true;
	}

	@SuppressWarnings("deprecation")
	private boolean handleShopCommand(CommandSender sender, String[] args) {
		OfflinePlayer messageTarget = Bukkit.getOfflinePlayer(sender.getName());
		if (args.length == 2 && args[0].equalsIgnoreCase("check")) {
			OfflinePlayer target = getStatsOfflinePlayer(args[1]);
			sendShopCheck(sender, target, args[1]);
			return true;
		}
		sender.sendMessage(formatString(messageTarget, messagesFile.getString("Help_Shop"), null, null, null, null, null, null));
		return true;
	}

	@SuppressWarnings("deprecation")
	private OfflinePlayer getStatsOfflinePlayer(String requestedName) {
		String playerKey = findStatsPlayerKey(requestedName);
		if (playerKey != null) {
			try {
				return Bukkit.getOfflinePlayer(UUID.fromString(playerKey));
			} catch (IllegalArgumentException e) {
				// Fall back to name lookup below.
			}
		}
		return Bukkit.getOfflinePlayer(requestedName);
	}

	private void sendShopCheck(CommandSender sender, OfflinePlayer target, String requestedName) {
		String displayName = target.getName() == null ? requestedName : target.getName();
		String playerKey = getStatsPlayerKey(target);
		if (playerKey == null)
			playerKey = findStatsPlayerKey(requestedName);

		Map<String, String> replacements = new HashMap<String, String>();
		replacements.put("%scoreboardchatshop_player%", displayName);

		if (playerKey == null) {
			sender.sendMessage(formatString(target, messagesFile.getString("Shop_Check_NoStats"), replacements));
			return;
		}

		List<ShopStat> buyStats = getTopStatsFromKey(playerKey, "Buy", 5);
		List<ShopStat> sellStats = getTopStatsFromKey(playerKey, "Sell", 5);
		if (buyStats.isEmpty() && sellStats.isEmpty()) {
			sender.sendMessage(formatString(target, messagesFile.getString("Shop_Check_NoStats"), replacements));
			return;
		}

		sender.sendMessage(formatString(target, messagesFile.getString("Shop_Check_Header"), replacements));
		sender.sendMessage(formatString(target, messagesFile.getString("Shop_Check_BuyHeader"), replacements));
		if (buyStats.isEmpty())
			sender.sendMessage(formatString(target, messagesFile.getString("Shop_Check_NoBoughtStats"), replacements));
		else
			sendShopStatLines(sender, target, displayName, buyStats, "Shop_Check_BuyLine");

		sender.sendMessage(formatString(target, messagesFile.getString("Shop_Check_SellHeader"), replacements));
		if (sellStats.isEmpty())
			sender.sendMessage(formatString(target, messagesFile.getString("Shop_Check_NoSoldStats"), replacements));
		else
			sendShopStatLines(sender, target, displayName, sellStats, "Shop_Check_SellLine");
	}

	private void sendShopStatLines(CommandSender sender, OfflinePlayer target, String displayName, List<ShopStat> stats, String messageKey) {
		for (int i = 0; i < stats.size(); i++) {
			ShopStat stat = stats.get(i);
			Map<String, String> replacements = new HashMap<String, String>();
			replacements.put("%scoreboardchatshop_player%", displayName);
			replacements.put("%scoreboardchatshop_rank%", "" + (i + 1));
			replacements.put("%scoreboardchatshop_material%", stat.getMaterial().toUpperCase());
			replacements.put("%scoreboardchatshop_amount%", "" + stat.getAmount());
			replacements.put("%scoreboardchatshop_money%", "" + stat.getMoney());
			sender.sendMessage(formatString(target, messagesFile.getString(messageKey), replacements));
		}
	}
	
	@SuppressWarnings("deprecation")
	private void explainCommands (CommandSender sender, String base) {
		if (base == null || base.equalsIgnoreCase("price"))
			sender.sendMessage(formatString(Bukkit.getOfflinePlayer(sender.getName()), messagesFile.getString("Help_Price"), null, null, null, null, null, null));
		if ((base == null || base.equalsIgnoreCase("setprice"))
				&& sender.hasPermission("ScoreboardChatShop"))
			sender.sendMessage(formatString(Bukkit.getOfflinePlayer(sender.getName()), messagesFile.getString("Help_SetPrice"), null, null, null, null, null, null));
		if (base == null || base.equalsIgnoreCase("buy"))
			sender.sendMessage(formatString(Bukkit.getOfflinePlayer(sender.getName()), messagesFile.getString("Help_Buy"), null, null, null, null, null, null));
		if (base == null || base.equalsIgnoreCase("sell"))
			sender.sendMessage(formatString(Bukkit.getOfflinePlayer(sender.getName()), messagesFile.getString("Help_Sell"), null, null, null, null, null, null));
		if (base == null || base.equalsIgnoreCase("shop"))
			sender.sendMessage(formatString(Bukkit.getOfflinePlayer(sender.getName()), messagesFile.getString("Help_Shop"), null, null, null, null, null, null));
	}
	
	@SuppressWarnings({ "unused", "deprecation" })
	private void warnMissingPerms (CommandSender sender) {
		sender.sendMessage(formatString(Bukkit.getOfflinePlayer(sender.getName()), messagesFile.getString("Error_MissingPerms"), null, null, null, null, null, null));
	}
	
	@SuppressWarnings("deprecation")
	private void warnConsoleSender (CommandSender sender) {
		sender.sendMessage(formatString(Bukkit.getOfflinePlayer(sender.getName()), messagesFile.getString("Error_ConsoleAttempt"), null, null, null, null, null, null));
	}
	
	@SuppressWarnings("deprecation")
	private void warnBadNumberFormat (CommandSender sender) {
		sender.sendMessage(formatString(Bukkit.getOfflinePlayer(sender.getName()), messagesFile.getString("Error_NumberFormat"), null, null, null, null, null, null));
	}
	
	@SuppressWarnings("deprecation")
	private void handlePrice (CommandSender sender, String matName, int amount) {
		if (amount <= 0) {
			warnBadNumberFormat(sender);
			return;
		}
		if (matName.equalsIgnoreCase("hand") && sender instanceof Player) {
			Player p = (Player) sender;
			if (p.getInventory().getItemInMainHand() != null)
				matName = p.getInventory().getItemInMainHand().getType().name().toLowerCase();
		}
		
		if (aliasDatabase.containsKey(matName.toLowerCase())) {
			String parentName = aliasDatabase.get(matName.toLowerCase());
			boolean hasBuyPrice = false, hasSellPrice = false;
			double buyRatio = 0, sellRatio = 0;
			double buyPrice = 0, sellPrice = 0;
			double baseBuyPrice = 0, baseBuyAmount = 0;
			double baseSellPrice = 0, baseSellAmount = 0;
			if (buyPriceDatabase.containsKey(parentName)) {
				baseBuyPrice = buyPriceDatabase.get(parentName);
				baseBuyAmount = buyAmountDatabase.get(parentName);
				buyRatio = baseBuyPrice / baseBuyAmount;
				buyPrice = buyRatio * amount;
				hasBuyPrice = true;
			}
			if (sellPriceDatabase.containsKey(parentName)) {
				baseSellPrice = sellPriceDatabase.get(parentName);
				baseSellAmount = sellAmountDatabase.get(parentName);
				sellRatio = baseSellPrice / baseSellAmount;
				sellPrice = sellRatio * amount;
				hasSellPrice = true;
			}
			if (hasBuyPrice)
				sender.sendMessage(formatString(Bukkit.getOfflinePlayer(sender.getName()), messagesFile.getString("Price_BuyResponse"), buyPrice+"", amount+"", ((int) baseBuyPrice)+"", ((int) baseBuyAmount)+"", parentName.toUpperCase(), matName));
			if (hasSellPrice)
				sender.sendMessage(formatString(Bukkit.getOfflinePlayer(sender.getName()), messagesFile.getString("Price_SellResponse"), sellPrice+"", amount+"", ((int) baseSellPrice)+"", ((int) baseSellAmount)+"", parentName.toUpperCase(), matName));
		} else {
			sender.sendMessage(formatString(Bukkit.getOfflinePlayer(sender.getName()), messagesFile.getString("Price_NotBoughtOrSold"), null, amount+"", null, null, matName, null));
		}
	}

	@SuppressWarnings("deprecation")
	private void handleSetPrice (CommandSender sender, String priceType, String matName, int price, int amount) {
		if (price < 0 || amount <= 0) {
			warnBadNumberFormat(sender);
			return;
		}
		if (matName.equalsIgnoreCase("hand") && sender instanceof Player) {
			Player p = (Player) sender;
			if (p.getInventory().getItemInMainHand() != null)
				matName = p.getInventory().getItemInMainHand().getType().name().toLowerCase();
		}
		
		if (aliasDatabase.containsKey(matName.toLowerCase())) {
			if (!priceType.equalsIgnoreCase("buy") && !priceType.equalsIgnoreCase("sell"))
				return;
			
			String parentName = aliasDatabase.get(matName.toLowerCase());
			String configPrefix = priceType.equalsIgnoreCase("buy") ? "Buy." : "Sell.";
			if (price == 0) {
				priceFile.set(configPrefix + parentName, null);
				priceFile.save();
				
				if (priceType.equalsIgnoreCase("buy")) {
					if (buyPriceDatabase.containsKey(parentName)) {
						buyPriceDatabase.remove(parentName);
						buyAmountDatabase.remove(parentName);
					}
				} else if (priceType.equalsIgnoreCase("sell")) {
					if (sellPriceDatabase.containsKey(parentName)) {
						sellPriceDatabase.remove(parentName);
						sellAmountDatabase.remove(parentName);
					}
				}
			} else {
				int gcd = gcd(price, amount);
				//Now we reduce the ratio down to it's smallest amount
				while (gcd != 1) {
					price /= gcd;
					amount /= gcd;
					gcd = gcd(price, amount);
				}
				priceFile.set(configPrefix + parentName + ".price", price);
				priceFile.set(configPrefix + parentName + ".amount", amount);
				priceFile.save();
				
				if (priceType.equalsIgnoreCase("buy")) {
					buyPriceDatabase.put(parentName, price);
					buyAmountDatabase.put(parentName, amount);
				} else if (priceType.equalsIgnoreCase("sell")) {
					sellPriceDatabase.put(parentName, price);
					sellAmountDatabase.put(parentName, amount);
				}
				loadAliasAndMaterialDatabases();
			}
			
			sender.sendMessage(formatString(Bukkit.getOfflinePlayer(sender.getName()), messagesFile.getString("SetPrice_Success"), price+"", amount+"", price+"", amount+"", matName, parentName));
		} else {
			sender.sendMessage(formatString(Bukkit.getOfflinePlayer(sender.getName()), messagesFile.getString("SetPrice_MaterialDoesntExist"), price+"", amount+"", null, null, matName, null));
		}
	}
	
	private void handleBuy (Player p, String matName, String desiredAmountString) {
		if (matName.equalsIgnoreCase("hand")) {
			if (p.getInventory().getItemInMainHand() != null)
				matName = p.getInventory().getItemInMainHand().getType().name().toLowerCase();
		}
		int desiredAmount = 1;
		try {
			desiredAmount = Integer.parseInt(desiredAmountString);
		} catch (NumberFormatException e) {
			if (!desiredAmountString.equalsIgnoreCase("max")) {
				warnBadNumberFormat(p);
				return;
			}
		}
		if (!desiredAmountString.equalsIgnoreCase("max") && desiredAmount <= 0) {
			warnBadNumberFormat(p);
			return;
		}
		
		if (aliasDatabase.containsKey(matName.toLowerCase())) {
			String parentName = aliasDatabase.get(matName.toLowerCase());
			if (buyPriceDatabase.containsKey(parentName) && buyAmountDatabase.containsKey(parentName)) {
				int price = buyPriceDatabase.get(parentName);
				int amount = buyAmountDatabase.get(parentName);
				double ratio = (double) price / (double) amount;
				
				//Handle if "max" amount was wanted
				if (desiredAmountString.equalsIgnoreCase("max")) {
					double balance = econ.getBalance(p);
					desiredAmount = (int) Math.max(Math.floor(balance / ratio), 1);
					desiredAmount -= desiredAmount % amount;
					if (desiredAmount <= 0) {
						p.sendMessage(formatString(p, messagesFile.getString("Buy_NotEnoughMoney"), price+"", amount+"", price+"", amount+"", matName, parentName));
						return;
					}
				}
				
				//Check if desired amount is in bundle size
				if (desiredAmount % amount != 0) {
					p.sendMessage(formatString(p, messagesFile.getString("Buy_BundleAlert"), null, desiredAmount+"", price+"", amount+"", matName, parentName));
					return;
				}
				
				//Check econ balance
				int priceOwed = price * (desiredAmount / amount);
				if (econ.getBalance(p) < priceOwed) {
					p.sendMessage(formatString(p, messagesFile.getString("Buy_NotEnoughMoney"), priceOwed+"", desiredAmount+"", price+"", amount+"", matName, parentName));
					return;
				}
				
				//Mock give items to see how many slots we will need
				Inventory inv = Bukkit.createInventory(null, 54);
				ItemStack item = new ItemStack(Material.getMaterial(parentName.toUpperCase()), desiredAmount);
				inv.addItem(item.clone());
				int emptySlotsNecessary = 0;
				for (int i = 0;i < inv.getSize();i++) {
					if (inv.getItem(i) != null) {
						emptySlotsNecessary++;
					} else {
						i = inv.getSize();
					}
				}
				int emptySlotsFound = 0;
				for (int i = 0;i < 36;i++) {
					if (p.getInventory().getItem(i) == null || p.getInventory().getItem(i).getType().equals(Material.AIR)) {
						emptySlotsFound++;
					}
				}
				if (emptySlotsFound < emptySlotsNecessary) {
					p.sendMessage(formatString(p, messagesFile.getString("Buy_MissingInventorySpace"), priceOwed+"", desiredAmount+"", price+"", amount+"", matName, parentName));
					return;
				}
				
				//Withdraw and give item
				econ.withdrawPlayer(p, priceOwed);
				p.getInventory().addItem(item.clone());
				recordTransaction(p, "Buy", parentName, desiredAmount, priceOwed);
				p.sendMessage(formatString(p, messagesFile.getString("Buy_Success"), priceOwed+"", desiredAmount+"", price+"", amount+"", matName, parentName));
			} else {
				if (sellPriceDatabase.containsKey(parentName) && sellAmountDatabase.containsKey(parentName)) {
					p.sendMessage(formatString(p, messagesFile.getString("Buy_CanOnlySell"), null, desiredAmount+"", null, null, matName, parentName));
				} else {
					p.sendMessage(formatString(p, messagesFile.getString("Buy_MissingMaterial"), null, desiredAmount+"", null, null, matName, parentName));
				}
			}
		} else {
			p.sendMessage(formatString(p, messagesFile.getString("Buy_NotBoughtOrSold"), null, desiredAmount+"", null, null, matName, null));
		}
	}
	
	private void handleSell (Player p, String matName, String desiredAmountString) {
		if (matName.equalsIgnoreCase("hand")) {
			if (p.getInventory().getItemInMainHand() != null)
				matName = p.getInventory().getItemInMainHand().getType().name().toLowerCase();
		}
		int desiredAmount = 1;
		try {
			desiredAmount = Integer.parseInt(desiredAmountString);
		} catch (NumberFormatException e) {
			if (!desiredAmountString.equalsIgnoreCase("max")) {
				warnBadNumberFormat(p);
				return;
			}
		}
		if (!desiredAmountString.equalsIgnoreCase("max") && desiredAmount <= 0) {
			warnBadNumberFormat(p);
			return;
		}
		
		if (aliasDatabase.containsKey(matName.toLowerCase())) {
			String parentName = aliasDatabase.get(matName.toLowerCase());
			if (sellPriceDatabase.containsKey(parentName) && sellAmountDatabase.containsKey(parentName)) {
				int price = sellPriceDatabase.get(parentName);
				int amount = sellAmountDatabase.get(parentName);
				
				//Handle if "max" amount was wanted
				if (desiredAmountString.equalsIgnoreCase("max")) {
					int foundAmount = 0;
					ItemStack itemExample = new ItemStack(Material.getMaterial(parentName.toUpperCase()));
					ItemStack itemFound = null;
					for (int i = 0;i < 36;i++) {
						itemFound = p.getInventory().getItem(i);
						if (itemFound != null && itemFound.isSimilar(itemExample))
							foundAmount += itemFound.getAmount();
					}
					desiredAmount = Math.max(foundAmount, 1);
				}
				
				//Check if desired amount is in bundle size
				if (desiredAmount % amount != 0) {
					p.sendMessage(formatString(p, messagesFile.getString("Sell_BundleAlert"), null, desiredAmount+"", price+"", amount+"", matName, parentName));
					return;
				}
				
				//Calculate econ
				int priceOwed = price * (desiredAmount / amount);
				
				//Mock give items to see how many slots we will need
				ItemStack item = new ItemStack(Material.getMaterial(parentName.toUpperCase()), 1);
				int materialAmountFound = 0;
				for (int i = 0;i < 36;i++) {
					ItemStack otherItem = p.getInventory().getItem(i);
					if (otherItem != null && otherItem.isSimilar(item.clone())) {
						materialAmountFound += otherItem.getAmount();
					}
				}
				if (materialAmountFound < desiredAmount) {
					p.sendMessage(formatString(p, messagesFile.getString("Sell_NotEnoughItems"), priceOwed+"", desiredAmount+"", price+"", amount+"", matName, parentName));
					return;
				}
				
				//Withdraw and give item
				int stillOwedAmount = desiredAmount;
				for (int i = 0;i < 36;i++) {
					ItemStack curItem = p.getInventory().getItem(i);
					int curItemAmount = curItem != null && curItem.isSimilar(item.clone()) ? curItem.getAmount() : 0;
					if (curItemAmount > 0) {
						if (curItemAmount <= stillOwedAmount) {
							curItem.setAmount(0);
							stillOwedAmount -= curItemAmount;
						} else {
							curItem.setAmount(curItemAmount - stillOwedAmount);
							stillOwedAmount = 0;
						}
					}
					
					if (stillOwedAmount <= 0)
						i = 36;
				}
				econ.depositPlayer(p, priceOwed);
				recordTransaction(p, "Sell", parentName, desiredAmount, priceOwed);
				p.sendMessage(formatString(p, messagesFile.getString("Sell_Success"), priceOwed+"", desiredAmount+"", price+"", amount+"", matName, parentName));
			} else {
				if (buyPriceDatabase.containsKey(parentName) && buyAmountDatabase.containsKey(parentName)) {
					p.sendMessage(formatString(p, messagesFile.getString("Sell_CanOnlyBuy"), null, desiredAmount+"", null, null, matName, parentName));
				} else {
					p.sendMessage(formatString(p, messagesFile.getString("Sell_MissingMaterial"), null, desiredAmount+"", null, null, matName, parentName));
				}
			}
		} else {
			p.sendMessage(formatString(p, messagesFile.getString("Sell_NotBoughtOrSold"), null, desiredAmount+"", null, null, matName, null));
		}
	}

	public String resolveMaterialName(String matName) {
		if (matName == null)
			return null;
		return aliasDatabase.get(matName.toLowerCase());
	}

	public Integer getBuyBundlePrice(String matName) {
		String parentName = resolveMaterialName(matName);
		if (parentName == null)
			return null;
		return buyPriceDatabase.get(parentName);
	}

	public Integer getBuyBundleAmount(String matName) {
		String parentName = resolveMaterialName(matName);
		if (parentName == null)
			return null;
		return buyAmountDatabase.get(parentName);
	}

	public Integer getSellBundlePrice(String matName) {
		String parentName = resolveMaterialName(matName);
		if (parentName == null)
			return null;
		return sellPriceDatabase.get(parentName);
	}

	public Integer getSellBundleAmount(String matName) {
		String parentName = resolveMaterialName(matName);
		if (parentName == null)
			return null;
		return sellAmountDatabase.get(parentName);
	}

	public List<ShopStat> getTopStats(OfflinePlayer target, String transactionType, int limit) {
		return getTopStatsFromKey(getStatsPlayerKey(target), transactionType, limit);
	}

	public int getStatTotal(OfflinePlayer target, String transactionType, String fieldName) {
		String playerKey = getStatsPlayerKey(target);
		String normalizedType = normalizeTransactionType(transactionType);
		if (playerKey == null || normalizedType == null)
			return 0;

		synchronized (statsLock) {
			ConfigurationSection sec = statsFile.getConfigurationSection("Players." + playerKey + "." + normalizedType);
			if (sec == null)
				return 0;

			int total = 0;
			for (String matName : sec.getKeys(false))
				total += sec.getInt(matName + "." + fieldName);
			return total;
		}
	}

	public List<String> getKnownStatsPlayerNames() {
		List<String> playerNames = new ArrayList<String>();
		synchronized (statsLock) {
			ConfigurationSection playersSec = statsFile.getConfigurationSection("Players");
			if (playersSec == null)
				return playerNames;

			for (String key : playersSec.getKeys(false)) {
				String playerName = playersSec.getString(key + ".Name");
				if (playerName != null && !playerName.isEmpty())
					playerNames.add(playerName);
			}
		}
		return playerNames;
	}

	private List<ShopStat> getTopStatsFromKey(String playerKey, String transactionType, int limit) {
		List<ShopStat> stats = new ArrayList<ShopStat>();
		String normalizedType = normalizeTransactionType(transactionType);
		if (playerKey == null || normalizedType == null)
			return stats;

		synchronized (statsLock) {
			ConfigurationSection sec = statsFile.getConfigurationSection("Players." + playerKey + "." + normalizedType);
			if (sec == null)
				return stats;

			for (String matName : sec.getKeys(false)) {
				int amount = sec.getInt(matName + ".amount");
				int money = sec.getInt(matName + ".money");
				if (amount > 0 || money > 0)
					stats.add(new ShopStat(matName, amount, money));
			}
		}

		Collections.sort(stats, new Comparator<ShopStat>() {
			@Override
			public int compare(ShopStat statA, ShopStat statB) {
				int moneyCompare = Integer.compare(statB.getMoney(), statA.getMoney());
				if (moneyCompare != 0)
					return moneyCompare;

				int amountCompare = Integer.compare(statB.getAmount(), statA.getAmount());
				if (amountCompare != 0)
					return amountCompare;

				return statA.getMaterial().compareTo(statB.getMaterial());
			}
		});

		if (stats.size() > limit)
			return new ArrayList<ShopStat>(stats.subList(0, limit));
		return stats;
	}

	private void recordTransaction(Player p, String transactionType, String matName, int amount, int money) {
		String normalizedType = normalizeTransactionType(transactionType);
		if (normalizedType == null || amount <= 0 || money < 0)
			return;

		String uuid = p.getUniqueId().toString();
		String material = matName.toLowerCase();
		String basePath = "Players." + uuid;
		String statPath = basePath + "." + normalizedType + "." + material;

		synchronized (statsLock) {
			statsFile.set(basePath + ".Name", p.getName());
			statsFile.set(statPath + ".amount", statsFile.getInt(statPath + ".amount") + amount);
			statsFile.set(statPath + ".money", statsFile.getInt(statPath + ".money") + money);
			statsFile.save();
		}
	}

	private String getStatsPlayerKey(OfflinePlayer target) {
		if (target == null)
			return null;

		UUID uuid = target.getUniqueId();
		synchronized (statsLock) {
			if (uuid != null && statsFile.isConfigurationSection("Players." + uuid.toString()))
				return uuid.toString();

			return findStatsPlayerKey(target.getName());
		}
	}

	private String findStatsPlayerKey(String playerName) {
		if (playerName == null)
			return null;

		synchronized (statsLock) {
			ConfigurationSection playersSec = statsFile.getConfigurationSection("Players");
			if (playersSec == null)
				return null;

			for (String key : playersSec.getKeys(false)) {
				if (playerName.equalsIgnoreCase(playersSec.getString(key + ".Name")))
					return key;
			}
		}
		return null;
	}

	private String normalizeTransactionType(String transactionType) {
		if (transactionType == null)
			return null;
		if (transactionType.equalsIgnoreCase("buy"))
			return "Buy";
		if (transactionType.equalsIgnoreCase("sell"))
			return "Sell";
		return null;
	}
	
	private int gcd(int a, int b)
	{
	    while (b > 0)
	    {
	        int temp = b;
	        b = a % b; // % is remainder
	        a = temp;
	    }
	    return a;
	}
	
	private String formatString (OfflinePlayer p, String s, String price, String amount, String baseBundlePrice, String baseBundleAmount, String material, String baseMaterial) {
		Map<String, String> replacements = new HashMap<String, String>();
		if (price != null)
			replacements.put("%scoreboardchatshop_price%", price);
		if (amount != null)
			replacements.put("%scoreboardchatshop_amount%", amount);
		if (baseBundleAmount != null)
			replacements.put("%scoreboardchatshop_basebundleamount%", baseBundleAmount);
		if (baseBundlePrice != null)
			replacements.put("%scoreboardchatshop_basebundleprice%", baseBundlePrice);
		if (material != null)
			replacements.put("%scoreboardchatshop_material%", material);
		if (baseMaterial != null)
			replacements.put("%scoreboardchatshop_basematerial%", baseMaterial.toUpperCase());
		return formatString(p, s, replacements);
	}

	private String formatString (OfflinePlayer p, String s, Map<String, String> replacements) {
		if (s == null)
			return "";

		for (String replacer : replacements.keySet()) {
			String replacement = replacements.get(replacer);
			while (replacement != null && s.contains(replacer))
				s = s.replace(replacer, replacement);
		}
		
		s = StringUtils.makeColors(s);
		if (foundPlaceholderAPI)
			s = PlaceholderAPI.setPlaceholders(p, s);
		return s;
	}

	public static class ShopStat {
		private String material;
		private int amount;
		private int money;

		public ShopStat(String material, int amount, int money) {
			this.material = material;
			this.amount = amount;
			this.money = money;
		}

		public String getMaterial() {
			return material;
		}

		public int getAmount() {
			return amount;
		}

		public int getMoney() {
			return money;
		}
	}
	
}
