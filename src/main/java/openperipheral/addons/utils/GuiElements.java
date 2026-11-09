package openperipheral.addons.utils;

/**
 * Vanilla HUD elements that capture mode can toggle. Kept outside {@link GuiUtils} so common code (network events,
 * server-side capture control) never has to load the client-only {@code GuiIngameForge} class.
 */
public enum GuiElements {
    OVERLAY,
    PORTAL,
    HOTBAR,
    CROSSHAIRS,
    BOSS_HEALTH,
    HEALTH,
    ARMOR,
    FOOD,
    MOUNT_HEALTH,
    AIR,
    EXPERIENCE,
    JUMP_BAR,
    OBJECTIVES;
}
