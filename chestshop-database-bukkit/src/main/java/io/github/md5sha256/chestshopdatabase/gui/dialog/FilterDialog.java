package io.github.md5sha256.chestshopdatabase.gui.dialog;

import io.github.md5sha256.chestshopdatabase.gui.FindState;
import io.github.md5sha256.chestshopdatabase.model.ShopType;
import io.github.md5sha256.chestshopdatabase.settings.MessageContainer;
import io.github.md5sha256.chestshopdatabase.util.DialogUtil;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class FilterDialog {

    @NotNull
    private static DialogBase createShopFiltersBase(@NotNull Set<ShopType> includedTypes,
                                                    @NotNull MessageContainer messages) {
        var options = List.of(SingleOptionDialogInput.OptionEntry.create("enabled",
                        messages.messageFor("dialog.option.on"),
                        true),
                SingleOptionDialogInput.OptionEntry.create("disabled",
                        messages.messageFor("dialog.option.off"),
                        false));

        Stream<SingleOptionDialogInput> typeInputs = Arrays.stream(ShopType.values())
                .map(type -> DialogInput.singleOption(type.name(),
                                messages.messageFor(
                                        "find.filter.shop-type." + type.name().toLowerCase()),
                                options)
                        .build());

        Stream<SingleOptionDialogInput> emptyFullInputs = Stream.of(
                DialogInput.singleOption(
                        "show_empty",
                        messages.messageFor("find.filter.empty-shops"),
                        options).build(),
                DialogInput.singleOption(
                        "show_full",
                        messages.messageFor("find.filter.full-shops"),
                        options).build()
        );

        return DialogBase.builder(messages.messageFor("find.filter.title"))
                .canCloseWithEscape(true)
                .inputs(Stream.concat(typeInputs, emptyFullInputs).toList())
                .build();
    }

    @NotNull
    public static Dialog createFiltersDialog(@NotNull FindState state,
                                             @NotNull Supplier<Dialog> prevDialog,
                                             @NotNull MessageContainer messages) {
        Set<ShopType> includedTypes = state.shopTypes();
        ActionButton saveButton = ActionButton.builder(messages.messageFor("dialog.common.save"))
                .tooltip(messages.messageFor("dialog.common.tooltip.save-return"))
                .action(DialogAction.customClick(applyFilters(state, prevDialog),
                        DialogUtil.DEFAULT_CALLBACK_OPTIONS))
                .build();
        ActionButton backButton = ActionButton.builder(messages.messageFor("dialog.common.back"))
                .tooltip(messages.messageFor("dialog.common.tooltip.back"))
                .action(DialogUtil.openDialogAction(prevDialog))
                .build();
        return Dialog.create(factory ->
                factory.empty()
                        .base(createShopFiltersBase(includedTypes, messages))
                        .type(DialogType.confirmation(saveButton, backButton))
        );
    }

    private static DialogActionCallback applyFilters(@NotNull FindState findState,
                                                     @NotNull Supplier<Dialog> prevDialog) {
        return (view, audience) -> {
            Set<ShopType> included = EnumSet.noneOf(ShopType.class);
            for (ShopType shopType : ShopType.values()) {
                String value = view.getText(shopType.name());
                if (value != null && value.equals("enabled")) {
                    included.add(shopType);
                }
            }
            findState.setShopTypes(included);

            String show_empty = view.getText("show_empty");
            String show_full = view.getText("show_full");
            if (show_empty != null) { findState.setHideEmptyShops(show_empty.equals("disabled")); }
            if (show_full != null) { findState.setHideFullShops(show_full.equals("disabled")); }

            audience.showDialog(prevDialog.get());
        };
    }

}
