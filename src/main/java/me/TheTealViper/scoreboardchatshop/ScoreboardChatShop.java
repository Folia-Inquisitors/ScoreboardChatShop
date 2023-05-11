package me.TheTealViper.scoreboardchatshop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	PluginFile messagesFile;
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
		messagesFile = new PluginFile(this, "messages.yml", "messages.yml");
		
		//Load stuff in
		loadPriceDatabase();
		loadAliasAndMaterialDatabases();
		
		AH = new AutocompleteHandler(this);
		getCommand("price").setTabCompleter(AH);
		getCommand("setprice").setTabCompleter(AH);
		getCommand("buy").setTabCompleter(AH);
		getCommand("sell").setTabCompleter(AH);
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
		for (String parentMatName : sec.getKeys(false)) {
			for (String aliasName : sec.getStringList(parentMatName)) {
				aliasDatabase.put(aliasName.toLowerCase(), parentMatName.toLowerCase());
//				materialList.add(aliasName.toLowerCase());
				if (materialBuyList.contains(parentMatName))
					materialBuyList.add(aliasName);
				if (materialSellList.contains(parentMatName))
					materialSellList.add(aliasName);
				materialExpansiveList.add(aliasName.toLowerCase());
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
		baseMaterial = baseMaterial == null ? null : baseMaterial.toUpperCase();
		
		String replacer = "%scoreboardchatshop_price%";
		while (price != null && s.contains(replacer))
			s = s.replace(replacer, price);
		replacer = "%scoreboardchatshop_amount%";
		while (amount != null && s.contains(replacer))
			s = s.replace(replacer, amount);
		replacer = "%scoreboardchatshop_basebundleamount%";
		while (baseBundleAmount != null && s.contains(replacer))
			s = s.replace(replacer, baseBundleAmount);
		replacer = "%scoreboardchatshop_basebundleprice%";
		while (baseBundlePrice != null && s.contains(replacer))
			s = s.replace(replacer, baseBundlePrice);
		replacer = "%scoreboardchatshop_material%";
		while (material != null && s.contains(replacer))
			s = s.replace(replacer, material);
		replacer = "%scoreboardchatshop_basematerial%";
		while (baseMaterial != null && s.contains(replacer))
			s = s.replace(replacer, baseMaterial);
		
		s = StringUtils.makeColors(s);
		if (foundPlaceholderAPI)
			s = PlaceholderAPI.setPlaceholders(p, s);
		return s;
	}
	
}
