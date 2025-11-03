package io.github.md5sha256.chestshopdatabase.listener;

import com.Acrobot.Breeze.Utils.PriceUtil;
import com.Acrobot.ChestShop.Events.ChestShopReloadEvent;
import com.Acrobot.ChestShop.Events.ShopCreatedEvent;
import com.Acrobot.ChestShop.Events.ShopDestroyedEvent;
import com.Acrobot.ChestShop.Events.TransactionEvent;
import com.Acrobot.ChestShop.Signs.ChestShopSign;
import com.Acrobot.ChestShop.Utils.uBlock;
import io.github.md5sha256.chestshopdatabase.ChestShopState;
import io.github.md5sha256.chestshopdatabase.ItemDiscoverer;
import io.github.md5sha256.chestshopdatabase.model.ChestshopItem;
import io.github.md5sha256.chestshopdatabase.model.HydratedShop;
import io.github.md5sha256.chestshopdatabase.util.BlockPosition;
import io.github.md5sha256.chestshopdatabase.util.InventoryUtil;
import io.github.md5sha256.chestshopdatabase.util.UnsafeChestShopSign;
import org.bukkit.block.Container;
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

    private void toHydratedShop(@Nonnull Sign sign,
                                @Nonnull String[] lines,
                                @Nonnull Container container,
                                Consumer<HydratedShop> callback) {
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
        this.discoverer.discoverItemStackFromCode(itemCode, itemStack -> {
            if (itemStack == null || itemStack.isEmpty()) {
                // FIXME log warning
                return;
            }
            HydratedShop shop = new HydratedShop(
                    world,
                    posX,
                    posY,
                    posZ,
                    new ChestshopItem(itemStack, itemCode),
                    owner,
                    buyPrice,
                    sellPrice,
                    quantity,
                    InventoryUtil.countItems(itemStack, container.getInventory()),
                    InventoryUtil.remainingCapacity(itemStack, container.getInventory())
            );
            callback.accept(shop);
        });
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChestShopCreated(ShopCreatedEvent event) {
        Sign sign = event.getSign();
        String[] lines = event.getSignLines();
        Container container = uBlock.findConnectedContainer(sign);
        if (container == null) {
            return;
        }
        toHydratedShop(sign, lines, container, this.shopState::queueShopCreation);
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
        Container container = uBlock.findConnectedContainer(sign);
        if (container == null) {
            return;
        }
        UUID world = sign.getWorld().getUID();
        int posX = sign.getX();
        int posY = sign.getY();
        int posZ = sign.getZ();
        if (!this.shopState.cachedShopRegistered(new BlockPosition(world, posX, posY, posZ))) {
            toHydratedShop(sign, sign.getLines(), container, this.shopState::queueShopCreation);
        }
    }

}
