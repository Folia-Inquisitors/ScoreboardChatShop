package me.TheTealViper.scoreboardchatshop;

import java.util.List;
import java.util.Locale;

import org.bukkit.OfflinePlayer;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

public class ScoreboardChatShopPlaceholderExpansion extends PlaceholderExpansion {
	private final ScoreboardChatShop plugin;

	public ScoreboardChatShopPlaceholderExpansion(ScoreboardChatShop plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean persist() {
		return true;
	}

	@Override
	public String getIdentifier() {
		return "scoreboardchatshop";
	}

	@Override
	public String getAuthor() {
		return String.join(", ", plugin.getDescription().getAuthors());
	}

	@Override
	public String getVersion() {
		return plugin.getDescription().getVersion();
	}

	@Override
	public String onRequest(OfflinePlayer player, String params) {
		if (params == null || params.isEmpty())
			return "";

		String lowerParams = params.toLowerCase();
		if (lowerParams.startsWith("buy_price_"))
			return numberOrEmpty(plugin.getBuyBundlePrice(params.substring("buy_price_".length())));
		if (lowerParams.startsWith("buy_amount_"))
			return numberOrEmpty(plugin.getBuyBundleAmount(params.substring("buy_amount_".length())));
		if (lowerParams.startsWith("buy_unit_price_"))
			return unitPrice(plugin.getBuyBundlePrice(params.substring("buy_unit_price_".length())),
					plugin.getBuyBundleAmount(params.substring("buy_unit_price_".length())));
		if (lowerParams.startsWith("sell_price_"))
			return numberOrEmpty(plugin.getSellBundlePrice(params.substring("sell_price_".length())));
		if (lowerParams.startsWith("sell_amount_"))
			return numberOrEmpty(plugin.getSellBundleAmount(params.substring("sell_amount_".length())));
		if (lowerParams.startsWith("sell_unit_price_"))
			return unitPrice(plugin.getSellBundlePrice(params.substring("sell_unit_price_".length())),
					plugin.getSellBundleAmount(params.substring("sell_unit_price_".length())));
		if (lowerParams.startsWith("stats_"))
			return statsPlaceholder(player, lowerParams);

		return null;
	}

	private String statsPlaceholder(OfflinePlayer player, String params) {
		if (player == null)
			return "";

		if (params.equals("stats_buy_total_amount"))
			return "" + plugin.getStatTotal(player, "Buy", "amount");
		if (params.equals("stats_buy_total_money") || params.equals("stats_buy_total_spent"))
			return "" + plugin.getStatTotal(player, "Buy", "money");
		if (params.equals("stats_sell_total_amount"))
			return "" + plugin.getStatTotal(player, "Sell", "amount");
		if (params.equals("stats_sell_total_money") || params.equals("stats_sell_total_earned"))
			return "" + plugin.getStatTotal(player, "Sell", "money");

		String[] pieces = params.split("_");
		if (pieces.length != 5 || !pieces[2].equals("top"))
			return null;

		int rank;
		try {
			rank = Integer.parseInt(pieces[3]);
		} catch (NumberFormatException e) {
			return "";
		}
		if (rank <= 0)
			return "";

		List<ScoreboardChatShop.ShopStat> stats = plugin.getTopStats(player, pieces[1], rank);
		if (stats.size() < rank)
			return "";

		ScoreboardChatShop.ShopStat stat = stats.get(rank - 1);
		if (pieces[4].equals("item"))
			return stat.getMaterial().toUpperCase();
		if (pieces[4].equals("amount"))
			return "" + stat.getAmount();
		if (pieces[4].equals("money") || pieces[4].equals("spent") || pieces[4].equals("earned"))
			return "" + stat.getMoney();
		return null;
	}

	private String numberOrEmpty(Integer value) {
		return value == null ? "" : "" + value;
	}

	private String unitPrice(Integer price, Integer amount) {
		if (price == null || amount == null || amount <= 0)
			return "";

		double unitPrice = (double) price / (double) amount;
		if (unitPrice == Math.floor(unitPrice))
			return "" + (int) unitPrice;
		return String.format(Locale.US, "%.2f", unitPrice);
	}
}
