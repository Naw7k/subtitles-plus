package net.naw.subtitles.client.utils;

import com.mojang.blaze3d.platform.cursor.CursorType;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import org.lwjgl.glfw.GLFW;

/**
 * Cursors: This utility class defines custom mouse icons.
 * We use these to give the user visual feedback when they are
 * hovering over the drag handle or the resize corner.
 */
public class Cursors {

    // The standard Minecraft pointer arrow.
    public static final CursorType DEFAULT = CursorTypes.ARROW;

    /**
     * MOVE: The 4-way arrow icon.
     * Shows up when the user is allowed to drag the box around the screen.
     */
    public static final CursorType MOVE = CursorTypes.RESIZE_ALL;

    /**
     * RESIZE_NWSE: The diagonal resize icon (North-West to South-East).
     * Shows up when the user hovers over the bottom-right corner to scale the box.
     * Created directly via GLFW since Mojang didn't expose this one in CursorTypes.
     */
    public static final CursorType RESIZE_NWSE = CursorType.createStandardCursor(
            GLFW.GLFW_RESIZE_NWSE_CURSOR, "resize_nwse", CursorTypes.RESIZE_ALL
    );
}
