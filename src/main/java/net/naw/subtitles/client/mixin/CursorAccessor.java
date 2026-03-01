package net.naw.subtitles.client.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.cursor.Cursor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * CursorAccessor: The "Mouse Controller."
 * This Mixin targets the DrawContext (the thing that draws everything on your screen)
 * to let us change what the mouse pointer looks like.
 */
@Mixin(DrawContext.class)
public interface CursorAccessor {

    /**
     * This is a "Setter" Accessor.
     * It allows us to overwrite the current mouse icon with our custom ones
     * (like the 4-way arrow for dragging or the diagonal arrow for resizing).
     */
    @Accessor("cursor")
    void subtitles$setCursor(Cursor cursor);
}