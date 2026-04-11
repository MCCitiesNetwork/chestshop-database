package io.github.md5sha256.chestshopdatabase.gui.dialog;

import io.github.md5sha256.chestshopdatabase.gui.FindState;
import io.github.md5sha256.chestshopdatabase.model.ShopAttribute;
import io.github.md5sha256.chestshopdatabase.settings.MessageContainer;
import io.github.md5sha256.chestshopdatabase.util.DialogUtil;
import io.github.md5sha256.chestshopdatabase.util.SortDirection;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Supplier;

public class SortDialog {


    public static Dialog createSortDialog(@NotNull FindState findState,
                                          @NotNull Supplier<Dialog> prevDialog,
                                          @NotNull MessageContainer messages) {
        var buttons = List.of(
                ActionButton.builder(messages.messageFor("find.sort.direction-button"))
                        .action(DialogUtil.openDialogAction(() -> createSortDirectionDialog(
                                findState, () -> createSortDialog(findState, prevDialog, messages),
                                messages)))
                        .build(),
                ActionButton.builder(messages.messageFor("find.sort.priority-button"))
                        .action(DialogUtil.openDialogAction(() -> createSortPriorityDialog(
                                findState, () -> createSortDialog(findState, prevDialog, messages),
                                messages)))
                        .build()
        );

        var backButton = ActionButton.builder(messages.messageFor("dialog.common.back"))
                .tooltip(messages.messageFor("dialog.common.tooltip.back-sort"))
                .action(DialogUtil.openDialogAction(prevDialog)).build();

        return Dialog.create(factory -> factory.empty().base(sortingBase(messages))
                .type(DialogType.multiAction(buttons)
                        .columns(1)
                        .exitAction(backButton)
                        .build()));
    }

    private static Dialog createSortDirectionDialog(@NotNull FindState findState,
                                                    @NotNull Supplier<Dialog> prevDialog,
                                                    @NotNull MessageContainer messages) {

        var saveButton = ActionButton.builder(messages.messageFor("dialog.common.save"))
                .action(DialogAction.customClick(applyShopAttributeSortDirections(findState,
                                prevDialog),
                        DialogUtil.DEFAULT_CALLBACK_OPTIONS))
                .tooltip(messages.messageFor("dialog.common.tooltip.save-return-sort"))
                .build();

        var backButton = ActionButton.builder(messages.messageFor("dialog.common.back"))
                .tooltip(messages.messageFor("dialog.common.tooltip.back-sort"))
                .action(DialogUtil.openDialogAction(prevDialog)).build();
        var attributes = findState.selectedAttributes().stream().sorted().toList();

        return Dialog.create(factory ->
                factory.empty().base(setSortingDirectionBase(attributes, messages))
                        .type(DialogType.confirmation(saveButton, backButton))
        );
    }

    private static Dialog createSortPriorityDialog(@NotNull FindState findState,
                                                   @NotNull Supplier<Dialog> prevDialog,
                                                   @NotNull MessageContainer messages) {

        var saveButton = ActionButton.builder(messages.messageFor("dialog.common.save"))
                .action(DialogAction.customClick(applyShopAttributeSortPriority(findState,
                                prevDialog),
                        DialogUtil.DEFAULT_CALLBACK_OPTIONS))
                .tooltip(messages.messageFor("dialog.common.tooltip.save-return-sort"))
                .build();

        var backButton = ActionButton.builder(messages.messageFor("dialog.common.back"))
                .tooltip(messages.messageFor("dialog.common.tooltip.back-sort"))
                .action(DialogUtil.openDialogAction(prevDialog)).build();

        var attributes = findState.selectedAttributes().stream().sorted().toList();

        return Dialog.create(factory ->
                factory.empty().base(setPrioritiesBase(attributes, messages))
                        .type(DialogType.confirmation(saveButton, backButton))
        );
    }

    @NotNull
    private static DialogBase sortingBase(@NotNull MessageContainer messages) {
        Component description = messages.messageFor("find.sort.description");
        return DialogBase.builder(messages.messageFor("find.sort.title"))
                .body(List.of(DialogBody.plainMessage(description)))
                .build();
    }

    @NotNull
    private static DialogBase setSortingDirectionBase(@NotNull List<ShopAttribute> attributes,
                                                       @NotNull MessageContainer messages) {
        var options = List.of(SingleOptionDialogInput.OptionEntry.create("ascending",
                        messages.messageFor("find.sort.ascending"),
                        true),
                SingleOptionDialogInput.OptionEntry.create("descending",
                        messages.messageFor("find.sort.descending"),
                        false),
                SingleOptionDialogInput.OptionEntry.create("disabled",
                        messages.messageFor("find.sort.off"),
                        false));

        var directions = attributes.stream().map(attribute ->
                DialogInput.singleOption(attribute.name(),
                        messages.messageFor(attributeMessageKey(attribute)),
                        options
                ).build()
        ).toList();
        return DialogBase.builder(messages.messageFor("find.sort.directions-title"))
                .inputs(directions)
                .build();
    }

    private static DialogBase setPrioritiesBase(@NotNull List<ShopAttribute> attributes,
                                                @NotNull MessageContainer messages) {

        var directions = attributes.stream().map(attribute ->
                DialogInput.numberRange(attribute.name(),
                        messages.messageFor(attributeMessageKey(attribute)),
                        0, 100
                ).initial(0f).step(1f).build()
        ).toList();
        Component message = messages.messageFor("find.sort.priorities-description");
        return DialogBase.builder(messages.messageFor("find.sort.priorities-title"))
                .body(List.of(DialogBody.plainMessage(message)))
                .inputs(directions)
                .build();
    }

    @NotNull
    private static String attributeMessageKey(@NotNull ShopAttribute attribute) {
        return "find.sort.attribute." + attribute.name().toLowerCase();
    }

    @NotNull
    private static DialogActionCallback applyShopAttributeSortDirections(@NotNull FindState findState,
                                                                         @NotNull Supplier<Dialog> prevDialog) {
        return (view, audience) -> {
            for (ShopAttribute attribute : ShopAttribute.values()) {
                String option = view.getText(attribute.name());
                if (option == null || option.equals("disabled")) {
                    findState.clearShopAttributeMeta(attribute);
                } else if (option.equals("ascending")) {
                    findState.getOrCreate(attribute).sortDirection(SortDirection.ASCENDING);
                } else if (option.equals("descending")) {
                    findState.getOrCreate(attribute).sortDirection(SortDirection.DESCENDING);
                } else {
                    findState.clearShopAttributeMeta(attribute);
                }
            }
            audience.showDialog(prevDialog.get());
        };
    }

    @NotNull
    private static DialogActionCallback applyShopAttributeSortPriority(@NotNull FindState findState,
                                                                       @NotNull Supplier<Dialog> prevDialog) {
        return (view, audience) -> {

            for (ShopAttribute attribute : ShopAttribute.values()) {
                Float value = view.getFloat(attribute.name());
                if (value == null) {
                    continue;
                }
                findState.setSortPriority(attribute, Math.round(value));
            }
            audience.showDialog(prevDialog.get());
        };
    }

}
