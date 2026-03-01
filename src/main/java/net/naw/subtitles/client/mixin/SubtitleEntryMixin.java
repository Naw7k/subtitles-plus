package net.naw.subtitles.client.mixin;

import net.minecraft.client.sound.SoundInstance;
import net.naw.subtitles.client.SubtitleColorData;
import net.naw.subtitles.client.colors.SubtitleCategoryMapper; // Our new Portal Connection
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * SubtitleEntryMixin: The "Color Painter."
 * This mixin implements our SubtitleColorData interface. It looks at a sound
 * and decides what color its text should be by asking the SubtitleCategoryMapper.
 */
@Mixin(targets = "net.minecraft.client.gui.hud.SubtitlesHud$SubtitleEntry")
public class SubtitleEntryMixin implements SubtitleColorData {

    // The "Memory" for this specific subtitle's color. Default is White (0xFFFFFF).
    @Unique
    private int customColor = 0xFFFFFF;

    /**
     * Logic to determine the color of the subtitle text.
     */
    @Override
    @Unique
    public void subtitles$setCategoryColor(SoundInstance sound) {
        // --- FLICKER PROTECTION ---
        // We only set the color if it's currently the default white.
        // This prevents the Mixin from constantly overwriting the Word-based colors
        // (from SubtitleColorMapper), which is what causes flickering!
        if (this.customColor == 0xFFFFFF) {
            // We ask the Category Mapper through the portal to get our starting color
            this.customColor = SubtitleCategoryMapper.getCategoryColor(sound.getCategory());
        }
    }

    /**
     * Retrieves the color we saved so the renderer knows which color to use.
     */
    @Override
    @Unique
    public int subtitles$getSavedColor() {
        return this.customColor;
    }
}