package io.github.md5sha256.chestshopFinder.listener;

import com.Acrobot.Breeze.Utils.PriceUtil;
import com.Acrobot.ChestShop.Events.ChestShopReloadEvent;
import com.Acrobot.ChestShop.Events.ShopCreatedEvent;
import com.Acrobot.ChestShop.Events.ShopDestroyedEvent;
import com.Acrobot.ChestShop.Events.TransactionEvent;
import com.Acrobot.ChestShop.Signs.ChestShopSign;
import io.github.md5sha256.chestshopFinder.ChestShopState;
import io.github.md5sha256.chestshopFinder.ItemDiscoverer;
import io.github.md5sha256.chestshopFinder.model.ChestshopItem;
import io.github.md5sha256.chestshopFinder.model.HydratedShop;
import io.github.md5sha256.chestshopFinder.util.BlockPosition;
import io.github.md5sha256.chestshopFinder.util.UnsafeChestShopSign;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import java.util.function.Consumer;

public record ChestShopListener(
        @Nonnull ChestShopState shopState,
        @Nonnull ItemDiscoverer discoverer
) implements Listener {

    private double toDouble(BigDecimal decimal) {
        return decimal.setScale(4, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .doubleValue();
    }


    @EventHandler
    public void onChestShopReload(ChestShopReloadEvent event) {
        UnsafeChestShopSign.init();
    }

    private void toHydratedShop(@Nonnull Sign sign, Consumer<HydratedShop> callback) {
        String[] lines = sign.getLines();
        UUID world = sign.getWorld().getUID();
        int posX = sign.getX();
        int posY = sign.getY();
        int posZ = sign.getZ();
        String itemCode = ChestShopSign.getItem(lines);
        String owner = ChestShopSign.getOwner(lines);
        int quantity = ChestShopSign.getQuantity(lines);
        String priceLine = ChestShopSign.getPrice(lines);
        BigDecimal buyPriceDecimal = PriceUtil.getExactBuyPrice(priceLine);
        BigDecimal sellPriceDecimal = PriceUtil.getExactSellPrice(priceLine);
        Double buyPrice = buyPriceDecimal.equals(PriceUtil.NO_PRICE) ? null : toDouble(
                buyPriceDecimal);
        Double sellPrice = sellPriceDecimal.equals(PriceUtil.NO_PRICE) ? null : toDouble(
                sellPriceDecimal);
        this.discoverer.discoverItemCode(itemCode, itemStack -> {
            HydratedShop shop = new HydratedShop(
                    world,
                    posX,
                    posY,
                    posZ,
                    new ChestshopItem(itemStack, itemCode),
                    owner,
                    buyPrice,
                    sellPrice,
                    quantity
            );
            callback.accept(shop);
        });
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChestShopCreated(ShopCreatedEvent event) {
        toHydratedShop(event.getSign(), this.shopState::queueShopCreation);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChestShopDestroyed(ShopDestroyedEvent event) {
        Sign sign = event.getSign();
        UUID world = sign.getWorld().getUID();
        int posX = sign.getX();
        int posY = sign.getY();
        int posZ = sign.getZ();
        this.shopState.queueShopDeletion(new BlockPosition(world, posX, posY, posZ));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTransaction(TransactionEvent event) {
        Sign sign = event.getSign();
        UUID world = sign.getWorld().getUID();
        int posX = sign.getX();
        int posY = sign.getY();
        int posZ = sign.getZ();
        if (!this.shopState.cachedShopRegistered(new BlockPosition(world, posX, posY, posZ))) {
            toHydratedShop(event.getSign(), this.shopState::queueShopCreation);
        }
    }

}
