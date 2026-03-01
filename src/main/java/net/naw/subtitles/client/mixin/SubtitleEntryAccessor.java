package net.naw.subtitles.client.mixin;

import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import java.util.List;

/**
 * SubtitleEntryAccessor: The "Remote Control" for subtitle logic.
 * This lets us interact with the actual text and the 'expiration'
 * logic of a subtitle entry.
 */
@Mixin(targets = "net.minecraft.client.gui.hud.SubtitlesHud$SubtitleEntry")
public interface SubtitleEntryAccessor {

    // --- ACCESSORS (Reading Data) ---

    /**
     * Grabs the actual words of the subtitle (e.g., "Creeper Hisses").
     */
    @Accessor("text")
    Text getText();

    /**
     * Grabs the list of sounds associated with this subtitle.
     */
    @Accessor("sounds")
    List<?> getSounds();

    // --- INVOKERS (Running Functions) ---

    /**
     * This "Invokes" (runs) Minecraft's distance check.
     * It asks the game: "Can the player actually hear this sound from where they are standing?"
     */
    @Invoker("canHearFrom")
    boolean invokeCanHearFrom(Vec3d pos);

    /**
     * This "Invokes" the cleanup logic.
     * It tells the game to delete subtitles that have been on screen for too long.
     */
    @Invoker("removeExpired")
    void invokeRemoveExpired(double expiry);
}