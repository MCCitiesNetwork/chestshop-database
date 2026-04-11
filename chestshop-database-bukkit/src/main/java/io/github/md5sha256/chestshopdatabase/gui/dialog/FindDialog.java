package io.github.md5sha256.chestshopdatabase.gui.dialog;

import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import io.github.md5sha256.chestshopdatabase.database.task.FindTaskFactory;
import io.github.md5sha256.chestshopdatabase.gui.FindState;
import io.github.md5sha256.chestshopdatabase.gui.ShopResultsGUI;
import io.github.md5sha256.chestshopdatabase.model.ChestshopItem;
import io.github.md5sha256.chestshopdatabase.model.ShopAttribute;
import io.github.md5sha256.chestshopdatabase.model.ShopType;
import io.github.md5sha256.chestshopdatabase.settings.MessageContainer;
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
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

public class FindDialog {

    @NotNull
    private static DialogBase createMainPageBase(@Nullable ChestshopItem item,
                                                 @NotNull MessageContainer messages) {
        if (item == null) {
            return DialogBase.builder(messages.messageFor("find.dialog.title"))
                    .canCloseWithEscape(true).build();
        }
        ItemStack itemStack = item.itemStack();
        Component name = itemStack.getDataOrDefault(DataComponentTypes.CUSTOM_NAME,
                itemStack.effectiveName());

        var builder = DialogBase.builder(
                        messages.messageResolving("find.dialog.title-with-item",
                                Placeholder.component("item", name)))
                .canCloseWithEscape(true);

        var nameBody = DialogBody.plainMessage(name);
        var itemBody = DialogBody.item(item.itemStack()).build();
        return builder.body(List.of(itemBody, nameBody)).build();
    }

    private static Dialog waitScreen(@NotNull MessageContainer messages) {
        return Dialog.create(factory -> factory
                .empty()
                .base(waitScreenBase(messages))
                .type(DialogType.notice())
        );
    }

    private static DialogBase waitScreenBase(@NotNull MessageContainer messages) {
        return DialogBase.builder(messages.messageFor("find.dialog.query-title"))
                .afterAction(DialogBase.DialogAfterAction.CLOSE)
                .canCloseWithEscape(true)
                .body(List.of(DialogBody.plainMessage(messages.messageFor("find.dialog.query-body"))))
                .build();
    }

    private static void submit(
            @NotNull DialogResponseView view,
            @NotNull Audience audience,
            @NotNull FindState findState,
            @NotNull FindTaskFactory taskFactory,
            @NotNull ShopResultsGUI resultsGUI,
            @NotNull Plugin plugin,
            @NotNull Predicate<Player> isBedrockPlayer,
            @NotNull MessageContainer messages) {
        if (!(audience instanceof Player player)) {
            audience.showDialog(waitScreen(messages));
            return;
        }
        if (isBedrockPlayer.test(player)) {
            player.sendMessage(messages.messageFor("find.querying"));
        } else {
            audience.showDialog(waitScreen(messages));
        }
        taskFactory.findTask(findState).whenComplete((res, ex) -> {
            audience.closeDialog();
            if (ex != null) {
                ex.printStackTrace();
                audience.sendMessage(messages.messageFor("find.error.query"));
                return;
            }
            if (res.isEmpty()) {
                audience.sendMessage(messages.messageFor("find.empty-results"));
                return;
            }
            Component title = messages.messageResolving("find.results-title",
                    Placeholder.unparsed("item_code", findState.item().itemCode()));
            ChestGui chestGui = resultsGUI.createGui(title, res, findState.item().itemStack(), findState.queryPosition());
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> chestGui.show(player), 1);
        });
    }

    @NotNull
    public static Dialog createMainPageDialog(
            @NotNull FindState findState,
            @NotNull FindTaskFactory taskFactory,
            @NotNull ShopResultsGUI resultsGUI,
            @NotNull Plugin plugin,
            @NotNull Predicate<Player> isBedrockPlayer,
            @NotNull MessageContainer messages
    ) {
        DialogAction submitAction = DialogAction.customClick((view, audience) -> {
            submit(view, audience, findState, taskFactory, resultsGUI, plugin, isBedrockPlayer, messages);
        }, ClickCallback.Options.builder().uses(1).build());
        DialogAction buyCheapAction = DialogAction.customClick((view, audience) -> {
            findState.setShopTypes(List.of(ShopType.BUY, ShopType.BOTH));
            findState.setSortPriority(ShopAttribute.UNIT_BUY_PRICE, 100);
            findState.setSortPriority(ShopAttribute.DISTANCE, 99);
            findState.setSortDirection(ShopAttribute.UNIT_BUY_PRICE, SortDirection.ASCENDING);
            findState.setSortDirection(ShopAttribute.DISTANCE, SortDirection.ASCENDING);
            findState.setHideEmptyShops(true);
            submit(view, audience, findState, taskFactory, resultsGUI, plugin, isBedrockPlayer, messages);
        }, ClickCallback.Options.builder().uses(1).build());
        DialogAction buyNearbyAction = DialogAction.customClick((view, audience) -> {
            findState.setShopTypes(List.of(ShopType.BUY, ShopType.BOTH));
            findState.setSortPriority(ShopAttribute.DISTANCE, 100);
            findState.setSortPriority(ShopAttribute.UNIT_BUY_PRICE, 99);
            findState.setSortDirection(ShopAttribute.DISTANCE, SortDirection.ASCENDING);
            findState.setSortDirection(ShopAttribute.UNIT_BUY_PRICE, SortDirection.ASCENDING);
            findState.setHideEmptyShops(true);
            submit(view, audience, findState, taskFactory, resultsGUI, plugin, isBedrockPlayer, messages);
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
            submit(view, audience, findState, taskFactory, resultsGUI, plugin, isBedrockPlayer, messages);
        }, ClickCallback.Options.builder().uses(1).build());

        ActionButton submitButton = ActionButton.builder(messages.messageFor("dialog.common.search"))
                .action(submitAction)
                .build();
        ActionButton exitButton = ActionButton.builder(messages.messageFor("dialog.common.exit"))
                .action(DialogUtil.CLOSE_DIALOG_ACTION)
                .build();
        ActionButton spacerButton = ActionButton.builder(Component.text(""))
                .action(DialogUtil.CLOSE_DIALOG_ACTION)
                .width(1)
                .build();

        List<ActionButton> actions = List.of(
                ActionButton.builder(messages.messageFor("find-main.buy-cheap"))
                        .action(buyCheapAction)
                        .build(),
                ActionButton.builder(messages.messageFor("find-main.buy-nearby"))
                        .action(buyNearbyAction)
                        .build(),
                ActionButton.builder(messages.messageFor("find-main.sell-best-price"))
                        .action(sellBestPriceAction).build(),
                spacerButton,
                ActionButton.builder(messages.messageFor("find-main.filters"))
                        .action(DialogUtil.openDialogAction(() -> FilterDialog.createFiltersDialog(
                                findState,
                                () -> createMainPageDialog(findState, taskFactory, resultsGUI, plugin,
                                        isBedrockPlayer, messages),
                                messages)))
                        .build(),
                ActionButton.builder(messages.messageFor("find-main.sorting"))
                        .action(DialogUtil.openDialogAction(() -> SortDialog.createSortDialog(
                                findState,
                                () -> createMainPageDialog(findState, taskFactory, resultsGUI, plugin,
                                        isBedrockPlayer, messages),
                                messages)))
                        .build(),
                submitButton
        );

        return Dialog.create(factory ->
                factory.empty()
                        .base(createMainPageBase(findState.item(), messages))
                        .type(DialogType.multiAction(actions)
                                .exitAction(exitButton)
                                .columns(1)
                                .build()));
    }

}
