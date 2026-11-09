package openperipheral.addons.glasses.client;

import org.junit.Assert;
import org.junit.Test;

import openperipheral.addons.glasses.SurfaceType;

public class SurfaceClientTest {

    @Test
    public void publicAndPrivateSurfacesStartEmpty() {
        final SurfaceClient publicSurface = SurfaceClient.createPublicSurface(1L);
        final SurfaceClient privateSurface = SurfaceClient.createPrivateSurface(2L);

        Assert.assertNotNull(publicSurface.drawablesContainer);
        Assert.assertNotNull(privateSurface.drawablesContainer);
        Assert.assertTrue(
            publicSurface.getSortedDrawables()
                .isEmpty());
        Assert.assertTrue(
            privateSurface.getSortedDrawables()
                .isEmpty());
        Assert.assertEquals(
            "Public",
            publicSurface.drawablesContainer.getClass()
                .getSimpleName());
        Assert.assertEquals(
            "Private",
            privateSurface.drawablesContainer.getClass()
                .getSimpleName());
        Assert.assertNotNull(SurfaceType.GLOBAL.scriptValue);
        Assert.assertNotNull(SurfaceType.PRIVATE.scriptValue);
    }
}
