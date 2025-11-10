package io.github.md5sha256.chestshopdatabase.gui.dialog;

import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import io.github.md5sha256.chestshopdatabase.database.task.FindTaskFactory;
import io.github.md5sha256.chestshopdatabase.gui.FindState;
import io.github.md5sha256.chestshopdatabase.gui.ShopResultsGUI;
import io.github.md5sha256.chestshopdatabase.model.ChestshopItem;
import io.github.md5sha256.chestshopdatabase.model.ShopAttribute;
import io.github.md5sha256.chestshopdatabase.model.ShopType;
import io.github.md5sha256.chestshopdatabase.util.DialogUtil;
import io.github.md5sha256.chestshopdatabase.util.SortDirection;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FindDialog {

    @NotNull
    private static DialogBase createMainPageBase(@Nullable ChestshopItem item) {
        if (item == null) {
            return DialogBase.builder(Component.text("Find ChestShops"))
                    .canCloseWithEscape(true).build();
        }
        ItemStack itemStack = item.itemStack();
        Component name = itemStack.getDataOrDefault(DataComponentTypes.CUSTOM_NAME,
                itemStack.effectiveName());
        var builder = DialogBase.builder(Component.text("Find ChestShops for ").append(name))
                .canCloseWithEscape(true);

        var nameBody = DialogBody.plainMessage(name);
        var itemBody = DialogBody.item(item.itemStack()).build();
        return builder.body(List.of(itemBody, nameBody)).build();
    }

    private static Dialog waitScreen() {
        return Dialog.create(factory -> factory
                .empty()
                .base(waitScreenBase())
                .type(DialogType.notice())
        );
    }

    private static DialogBase waitScreenBase() {
        return DialogBase.builder(Component.text("Chest Shop Query"))
                .afterAction(DialogBase.DialogAfterAction.CLOSE)
                .canCloseWithEscape(true)
                .body(List.of(DialogBody.plainMessage(Component.text("Querying..."))))
                .build();
    }

    private static void submit(
            @NotNull DialogResponseView view,
            @NotNull Audience audience,
            @NotNull FindState findState,
            @NotNull FindTaskFactory taskFactory,
            @NotNull ShopResultsGUI resultsGUI,
            @NotNull Plugin plugin) {
        audience.showDialog(waitScreen());
        if (!(audience instanceof Player player)) {
            return;
        }
        taskFactory.findTask(findState).whenComplete((res, ex) -> {
            audience.closeDialog();
            if (ex != null) {
                ex.printStackTrace();
                audience.sendMessage(Component.text("Internal error when querying shops!",
                        NamedTextColor.RED));
                return;
            }
            if (res.isEmpty()) {
                audience.sendMessage(Component.text("No shops found!", NamedTextColor.RED));
                return;
            }
            Component title = Component.text("Shop results for " + findState.item().itemCode());
            ChestGui chestGui = resultsGUI.createGui(title, res, findState.item().itemStack(), findState.queryPosition());
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> chestGui.show(player), 1);
        });
    }

    @NotNull
    public static Dialog createMainPageDialog(
            @NotNull FindState findState,
            @NotNull FindTaskFactory taskFactory,
            @NotNull ShopResultsGUI resultsGUI,
            @NotNull Plugin plugin
    ) {
        DialogAction submitAction = DialogAction.customClick((view, audience) -> {
            submit(view, audience, findState, taskFactory, resultsGUI, plugin);
        }, ClickCallback.Options.builder().uses(1).build());
        DialogAction buyCheapAction = DialogAction.customClick((view, audience) -> {
            findState.setShopTypes(List.of(ShopType.BUY, ShopType.BOTH));
            findState.setSortPriority(ShopAttribute.UNIT_BUY_PRICE, 100);
            findState.setSortPriority(ShopAttribute.DISTANCE, 99);
            findState.setSortDirection(ShopAttribute.UNIT_BUY_PRICE, SortDirection.ASCENDING);
            findState.setSortDirection(ShopAttribute.DISTANCE, SortDirection.ASCENDING);
            findState.setHideEmptyShops(true);
            submit(view, audience, findState, taskFactory, resultsGUI, plugin);
        }, ClickCallback.Options.builder().uses(1).build());
        DialogAction buyNearbyAction = DialogAction.customClick((view, audience) -> {
            findState.setShopTypes(List.of(ShopType.BUY, ShopType.BOTH));
            findState.setSortPriority(ShopAttribute.DISTANCE, 100);
            findState.setSortPriority(ShopAttribute.UNIT_BUY_PRICE, 99);
            findState.setSortDirection(ShopAttribute.DISTANCE, SortDirection.ASCENDING);
            findState.setSortDirection(ShopAttribute.UNIT_BUY_PRICE, SortDirection.ASCENDING);
            findState.setHideEmptyShops(true);
            submit(view, audience, findState, taskFactory, resultsGUI, plugin);
        }, ClickCallback.Options.builder().uses(1).build());
        DialogAction sellBestPriceAction = DialogAction.customClick((view, audience) -> {
            findState.setShopTypes(List.of(ShopType.SELL, ShopType.BOTH));
            findState.setSortPriority(ShopAttribute.UNIT_SELL_PRICE, 100);
            findState.setSortPriority(ShopAttribute.REMAINING_CAPACITY, 99);
            findState.setSortPriority(ShopAttribute.DISTANCE, 98);
            findState.setSortDirection(ShopAttribute.UNIT_SELL_PRICE, SortDirection.DESCENDING);
            findState.setSortDirection(ShopAttribute.REMAINING_CAPACITY, SortDirection.DESCENDING);
            findState.setSortDirection(ShopAttribute.DISTANCE, SortDirection.ASCENDING);
            findState.setHideFullShops(true);
            submit(view, audience, findState, taskFactory, resultsGUI, plugin);
        }, ClickCallback.Options.builder().uses(1).build());

        ActionButton submitButton = ActionButton.builder(Component.text("Search", NamedTextColor.GREEN))
                .action(submitAction)
                .build();
        ActionButton exitButton = ActionButton.builder(Component.text("Exit"))
                .action(DialogUtil.CLOSE_DIALOG_ACTION)
                .build();
        ActionButton spacerButton = ActionButton.builder(Component.text(""))
                .action(DialogUtil.CLOSE_DIALOG_ACTION)
                .width(1)
                .build();

        List<ActionButton> actions = List.of(
                ActionButton.builder(Component.text("Buy Cheap"))
                        .action(buyCheapAction)
                        .build(),
                ActionButton.builder(Component.text("Buy Nearby"))
                        .action(buyNearbyAction)
                        .build(),
                ActionButton.builder(Component.text("Sell for Best Price"))
                        .action(sellBestPriceAction).build(),
                spacerButton,
                ActionButton.builder(Component.text("Filters"))
                        .action(DialogUtil.openDialogAction(() -> FilterDialog.createFiltersDialog(
                                findState,
                                () -> createMainPageDialog(findState, taskFactory, resultsGUI, plugin))))
                        .build(),
                ActionButton.builder(Component.text("Sorting"))
                        .action(DialogUtil.openDialogAction(() -> SortDialog.createSortDialog(
                                findState,
                                () -> createMainPageDialog(findState, taskFactory, resultsGUI, plugin))))
                        .build(),
                submitButton
        );

        return Dialog.create(factory ->
                factory.empty()
                        .base(createMainPageBase(findState.item()))
                        .type(DialogType.multiAction(actions)
                                .exitAction(exitButton)
                                .columns(1)
                                .build()));
    }

}
