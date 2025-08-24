package io.github.md5sha256.chestshopFinder;

import com.Acrobot.ChestShop.Configuration.Properties;
import com.Acrobot.ChestShop.Signs.ChestShopSign;

import javax.annotation.Nonnull;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UnsafeChestShopSign {

    private static Pattern PATTERN_PLAYER_NAME;
    private static Pattern PATTERN_PLAYER_NAME_WITH_ID;

    public static void init() {
        PATTERN_PLAYER_NAME = Pattern.compile(Properties.VALID_PLAYERNAME_REGEXP);
        PATTERN_PLAYER_NAME_WITH_ID = Pattern.compile("^(.+):[A-Za-z0-9]+$");
    }

    public static boolean isValidChestShop(@Nonnull String[] lines) {
        if (!isValidPreparedSign(lines) || getOwner(lines).isEmpty()) {
            return false;
        }
        String priceLine = getPrice(lines);
        return (priceLine.indexOf('B') != -1 || priceLine.indexOf('b') != -1)
                && (priceLine.indexOf('S') != -1 || priceLine.indexOf('s') != -1);
    }

    public static String getQuantity(String[] lines) {
        return lines[ChestShopSign.QUANTITY_LINE];
    }

    public static String getOwner(String[] lines) {
        return lines[0];
    }

    public static String getPrice(String[] lines) {
        return lines[ChestShopSign.PRICE_LINE];
    }

    public static String getItem(String[] lines) {
        return lines[ChestShopSign.ITEM_LINE];
    }

    public static boolean isValidPreparedSign(String[] lines) {
        String playername = ChestShopSign.getOwner(lines);
        if (!ChestShopSign.isAdminShop(playername) && !playername.isEmpty()) {
            Matcher playernameWithIdMatcher = PATTERN_PLAYER_NAME_WITH_ID.matcher(playername);
            if (playernameWithIdMatcher.matches()) {
                playername = playernameWithIdMatcher.group(1);
            }
            if (!PATTERN_PLAYER_NAME.matcher(playername).matches()) {
                return false;
            }
        }

        for (int i = 0; i < 3; ++i) {
            boolean matches = false;

            for (Pattern pattern : ChestShopSign.SHOP_SIGN_PATTERN[i]) {
                if (pattern.matcher(lines[i + 1]).matches()) {
                    matches = true;
                    break;
                }
            }

            if (!matches) {
                return false;
            }
        }

        String priceLine = getPrice(lines);
        return priceLine.indexOf(58) == priceLine.lastIndexOf(58);
    }

}
