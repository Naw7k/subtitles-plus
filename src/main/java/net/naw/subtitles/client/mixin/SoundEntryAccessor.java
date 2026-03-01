package net.naw.subtitles.client.mixin;

import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * SoundEntryAccessor: The "Secret Key" for individual sounds.
 * Minecraft's SoundEntry class is 'private' (locked). This interface
 * lets us reach inside and grab the position and timing of a sound.
 */
@Mixin(targets = "net.minecraft.client.gui.hud.SubtitlesHud$SoundEntry")
public interface SoundEntryAccessor {

    /**
     * This lets us grab the 3D position (X, Y, Z) of where a sound happened.
     * We need this to draw the little '<' or '>' arrows pointing to the sound!
     */
    @Accessor("location")
    Vec3d getLocation();

    /**
     * This lets us grab the exact time the sound started.
     * We use this to figure out when the subtitle should start fading away.
     */
    @Accessor("time")
    long getTime();
}