package openperipheral.addons.api;

import net.minecraft.entity.player.EntityPlayerMP;

import cpw.mods.fml.common.eventhandler.Event;

/**
 * Extension hook for third-party mods that manage terminal/glasses binding themselves.
 * <p>
 * OpenPeripheral-Addons normally discovers terminals from the player's head slot each tick. Posting this event on
 * {@code MinecraftForge.EVENT_BUS} asks the bridge identified by {@link #terminalId} to register {@link #player}
 * immediately. The event is server-side only and has no internal producer in this mod; it is intended for addons that
 * equip or transfer terminal items outside the vanilla inventory/equipment flow.
 */
public class TerminalRegisterEvent extends Event {

    public final EntityPlayerMP player;
    public final long terminalId;

    public TerminalRegisterEvent(EntityPlayerMP player, long terminalId) {
        this.player = player;
        this.terminalId = terminalId;
    }
}
