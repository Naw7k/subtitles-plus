package net.naw.subtitles.client.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import java.util.List;

/**
 * SubtitleEntryAccessor: The "Remote Control" for subtitle logic.
 * This lets us interact with the actual text and the expiration
 * logic of a subtitle entry.
 */
@Mixin(targets = "net.minecraft.client.gui.components.SubtitleOverlay$Subtitle")
public interface SubtitleEntryAccessor {

    // --- ACCESSORS (Reading Data) ---

    /**
     * Grabs the actual words of the subtitle (e.g., "Creeper Hisses").
     */
    @Accessor("text")
    Component getText();

    /**
     * Grabs the list of sound locations associated with this subtitle.
     */
    @Accessor("playedAt")
    List<?> getSounds();

    // --- INVOKERS (Running Functions) ---

    /**
     * Invokes Minecraft's distance check.
     * It asks the game: "Can the player actually hear this sound from where they are standing?"
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    @Invoker("isAudibleFrom")
    boolean invokeCanHearFrom(Vec3 pos);

    /**
     * Invokes the cleanup logic.
     * Tells the game to delete subtitles that have been on screen for too long.
     */
    @Invoker("purgeOldInstances")
    void invokeRemoveExpired(double expiry);
}
