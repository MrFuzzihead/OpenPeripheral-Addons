package openperipheral.addons.glasses;

import java.util.Locale;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagString;

import org.junit.Assert;
import org.junit.Test;

public class TerminalUtilsTest {

    @Test
    public void formatsAndParsesLongGuid() {
        final long guid = 0x1234ABCDEFL;
        final String formatted = TerminalUtils.formatTerminalId(guid);
        Assert.assertEquals(formatted.toUpperCase(Locale.ROOT), formatted);

        final NBTTagCompound tag = new NBTTagCompound();
        tag.setLong("guid", guid);
        Assert.assertEquals(Long.valueOf(guid), TerminalUtils.extractGuid(tag));
    }

    @Test
    public void parsesLegacyBase36String() {
        final long guid = 54L; // base-36 "1I"
        final NBTTagCompound tag = new NBTTagCompound();
        tag.setString("guid", TerminalUtils.formatTerminalId(guid));
        Assert.assertEquals(Long.valueOf(guid), TerminalUtils.extractGuid(tag));
    }

    @Test
    public void legacyParsingIsLocaleIndependent() {
        final Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr", "TR"));
            final NBTTagCompound tag = new NBTTagCompound();
            tag.setString("guid", "1I");
            Assert.assertEquals(Long.valueOf(54L), TerminalUtils.extractGuid(tag));
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    public void rejectsMissingWrongTypeAndMalformedTags() {
        Assert.assertNull(TerminalUtils.extractGuid(null));
        Assert.assertNull(TerminalUtils.extractGuid(new NBTTagCompound()));
        Assert.assertNull(TerminalUtils.extractGuid(tagWith("guid", new NBTTagInt(1))));
        Assert.assertNull(TerminalUtils.extractGuid(tagWith("guid", new NBTTagString("not base 36!"))));
        Assert.assertNull(TerminalUtils.extractGuid(tagWith("guid", new NBTTagString(""))));
    }

    @Test
    public void generatedGuidsArePositive() {
        for (int i = 0; i < 200; i++) {
            final long guid = TerminalUtils.generateGuid();
            Assert.assertTrue("Generated a non-positive guid: " + guid, guid > 0);
        }
    }

    private static NBTTagCompound tagWith(String key, net.minecraft.nbt.NBTBase value) {
        final NBTTagCompound tag = new NBTTagCompound();
        tag.setTag(key, value);
        return tag;
    }
}
