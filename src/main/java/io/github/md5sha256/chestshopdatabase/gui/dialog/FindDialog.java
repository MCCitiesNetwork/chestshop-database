package io.github.md5sha256.chestshopdatabase.gui.dialog;

import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import io.github.md5sha256.chestshopdatabase.database.FindTaskFactory;
import io.github.md5sha256.chestshopdatabase.gui.FindState;
import io.github.md5sha256.chestshopdatabase.gui.ShopResultsGUI;
import io.github.md5sha256.chestshopdatabase.model.ChestshopItem;
import io.github.md5sha256.chestshopdatabase.util.DialogUtil;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class FindDialog {

    @Nonnull
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

    @Nonnull
    public static Dialog createMainPageDialog(
            @Nonnull FindState findState,
            @Nonnull FindTaskFactory taskFactory,
            @Nonnull ShopResultsGUI resultsGUI
    ) {
        DialogAction submitAction = DialogAction.customClick((view, audience) -> {
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
                chestGui.show(player);
            });
        }, ClickCallback.Options.builder().uses(1).build());
        ActionButton submitButton = ActionButton.builder(Component.text("Search"))
                .action(submitAction)
                .build();
        ActionButton exitButton = ActionButton.builder(Component.text("Exit"))
                .action(DialogUtil.CLOSE_DIALOG_ACTION)
                .build();
        List<ActionButton> actions = List.of(
                ActionButton.builder(Component.text("Filters"))
                        .action(DialogUtil.openDialogAction(() -> FilterDialog.createFiltersDialog(
                                findState,
                                () -> createMainPageDialog(findState, taskFactory, resultsGUI))))
                        .build(),
                ActionButton.builder(Component.text("Sorting"))
                        .action(DialogUtil.openDialogAction(() -> SortDialog.createSortDialog(
                                findState,
                                () -> createMainPageDialog(findState, taskFactory, resultsGUI))))
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
