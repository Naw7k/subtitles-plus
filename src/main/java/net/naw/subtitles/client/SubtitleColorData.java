package net.naw.subtitles.client;

import net.minecraft.client.resources.sounds.SoundInstance;

/**
 * SubtitleColorData: A "Bridge" interface.
 * This allows us to "attach" custom color data to Minecraft's existing sound objects
 * without breaking the game's original code.
 */
public interface SubtitleColorData {

    /**
     * This "Sets" the color.
     * It looks at the sound (like a Creeper hiss) and decides which color it should be.
     */
    void subtitles$setCategoryColor(SoundInstance sound);

    /**
     * This "Gets" the color.
     * When it's time to draw the text on screen, we use this to find out which color to use.
     */
    int subtitles$getSavedColor();
}
