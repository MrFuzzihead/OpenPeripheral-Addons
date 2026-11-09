package openperipheral.addons.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a static field (or a single-argument static method) in another mod as an OpenPeripheral-Addons API injection
 * point.
 * <p>
 * The addon processes these annotations with OpenMods' {@code ApiFactory} during pre-initialization. Every target type
 * must implement {@link IApiInterface}; the currently provided API is {@link ITerminalIdAccess}. Targets are populated
 * before init, so their values are undefined during pre-initialization.
 * <p>
 * This is an extension point for third-party mods. It does not register the API itself; the addon's API factory owns
 * that lifecycle.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiHolder {}
