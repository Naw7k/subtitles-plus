package net.naw.subtitles.client.mixin;

import com.mojang.blaze3d.platform.cursor.CursorType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * CursorAccessor: The "Mouse Controller."
 * This Mixin targets GuiGraphicsExtractor (the thing that draws everything on your screen)
 * to let us change what the mouse pointer looks like.
 */
@Mixin(GuiGraphicsExtractor.class)
public interface CursorAccessor {

    /**
     * Invokes requestCursor to change the current mouse icon to our custom ones
     * (like the 4-way arrow for dragging or the diagonal arrow for resizing).
     */
    @Invoker("requestCursor")
    void subtitles$setCursor(CursorType cursorType);
}
