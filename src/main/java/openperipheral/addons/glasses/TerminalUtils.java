package openperipheral.addons.glasses;

import java.util.Locale;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTBase.NBTPrimitive;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.nbt.NBTTagString;

import openmods.utils.StringUtils;

public class TerminalUtils {

    public static Long extractGuid(NBTTagCompound tag) {
        if (tag == null) return null;

        NBTBase guidTag = tag.getTag("guid");
        if (guidTag instanceof NBTTagString) {
            String value = ((NBTTagString) guidTag).func_150285_a_();
            if (value == null) return null;
            try {
                // Base-36 is case-insensitive, but String.toLowerCase() is locale-sensitive (e.g. Turkish 'I').
                return Long.parseLong(value.toLowerCase(Locale.ROOT), 36);
            } catch (NumberFormatException e) {
                return null;
            }
        } else if (guidTag instanceof NBTTagLong) return ((NBTPrimitive) guidTag).func_150291_c();

        return null;
    }

    public static String formatTerminalId(long terminalId) {
        return Long.toString(terminalId, 36)
            .toUpperCase();
    }

    static long generateGuid() {
        long result;
        do {
            result = Long.parseLong(StringUtils.randomString(8), 36);
        } while (result <= 0);
        return result;
    }

    public static final UUID GLOBAL_SURFACE_UUID = UUID.fromString("df66eff0-1cae-11e4-8c21-0800200c9a66");

    public static ItemStack getHeadSlot(EntityPlayer player) {
        return player.inventory.armorItemInSlot(3);
    }

}
