package openperipheral.addons.glasses;

import net.minecraft.item.ItemStack;

import openperipheral.addons.api.ITerminalItem;
import openperipheral.api.helpers.ItemStackMetaProviderSimple;

public class ItemTerminalMetaProvider extends ItemStackMetaProviderSimple<ITerminalItem> {

    @Override
    public String getKey() {
        return "op_terminal";
    }

    @Override
    public Object getMeta(ITerminalItem target, ItemStack stack) {
        return TerminalUtils.createBridgeMeta(target.getTerminalGuid(stack));
    }
}
