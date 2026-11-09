package openperipheral.addons;

import cpw.mods.fml.common.FMLCommonHandler;
import openmods.Log;

/**
 * Inert unless {@code -Dopenperipheraladdons.smokeTest=true} is set. CI uses it to boot a real client/server and let
 * FML reach the load-complete state without an operator having to close the game.
 */
public final class SmokeTestHook {

    public static final String ENABLE_PROPERTY = "openperipheraladdons.smokeTest";

    public static final String DELAY_PROPERTY = "openperipheraladdons.smokeTest.delay";

    public static final String SUCCESS_MARKER = "OPENPERIPHERAL_ADDONS_SMOKE_TEST_OK";

    private static volatile boolean scheduled;

    private SmokeTestHook() {}

    public static void scheduleExitIfRequested() {
        if (scheduled || !Boolean.getBoolean(ENABLE_PROPERTY)) return;

        scheduled = true;

        final long delay;
        try {
            delay = Long.parseLong(System.getProperty(DELAY_PROPERTY, "2000"));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid smoke test delay: " + System.getProperty(DELAY_PROPERTY), e);
        }

        final Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    Thread.sleep(Math.max(0, delay));
                } catch (InterruptedException e) {
                    Thread.currentThread()
                        .interrupt();
                    return;
                }

                Log.info(SUCCESS_MARKER);
                FMLCommonHandler.instance()
                    .exitJava(0, false);
            }
        }, "OpenPeripheralAddons-SmokeTest");
        thread.setDaemon(true);
        thread.start();
    }

    static boolean isScheduled() {
        return scheduled;
    }
}
