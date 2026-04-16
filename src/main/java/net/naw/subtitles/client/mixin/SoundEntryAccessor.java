package net.naw.subtitles.client.mixin;

import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * SoundEntryAccessor: The "Secret Key" for individual sound locations.
 * Minecraft's SoundPlayedAt class is private (locked). This interface
 * lets us reach inside and grab the position and timing of a sound.
 */
@Mixin(targets = "net.minecraft.client.gui.components.SubtitleOverlay$SoundPlayedAt")
public interface SoundEntryAccessor {

    /**
     * Grabs the 3D position (X, Y, Z) of where a sound happened.
     * We need this to draw the little '<' or '>' arrows pointing to the sound!
     */
    @Accessor("location")
    Vec3 getLocation();

    /**
     * Grabs the exact time the sound started.
     * We use this to figure out when the subtitle should start fading away.
     */
    @Accessor("time")
    long getTime();
}
