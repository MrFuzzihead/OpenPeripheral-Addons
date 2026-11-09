package openperipheral.addons.glasses;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.google.common.base.Optional;

import openperipheral.api.meta.IItemStackCustomMetaProvider;

public class NbtTerminalMetaProvider implements IItemStackCustomMetaProvider<Item> {

    @Override
    public Class<? extends Item> getTargetClass() {
        return Item.class;
    }

    @Override
    public String getKey() {
        return "op_terminal_embedded";
    }

    @Override
    public boolean canApply(Item target, ItemStack stack) {
        return NbtGuidProviders.hasTerminalCapabilities(stack);
    }

    @Override
    public Object getMeta(Item target, ItemStack stack) {
        final Optional<Long> terminalGuid = NbtGuidProviders.getTerminalGuid(stack);
        return TerminalUtils.createBridgeMeta(terminalGuid.orNull());
    }

}
