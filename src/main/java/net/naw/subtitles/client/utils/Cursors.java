package net.naw.subtitles.client.utils;

import net.minecraft.client.gui.cursor.Cursor;
import org.lwjgl.glfw.GLFW;

/**
 * Cursors: This utility class defines custom mouse icons.
 * We use these to give the user visual feedback when they are
 * hovering over the drag handle or the resize corner.
 */
public class Cursors {

    // The standard Minecraft pointer arrow.
    public static final Cursor DEFAULT = Cursor.DEFAULT;

    /**
     * MOVE: This creates the 4-way arrow icon (Up, Down, Left, Right).
     * It shows up when the user is allowed to drag the box around the screen.
     */
    public static final Cursor MOVE = Cursor.createStandard(
            GLFW.GLFW_RESIZE_ALL_CURSOR, "all_resize", Cursor.DEFAULT
    );

    /**
     * RESIZE_NWSE: This creates the diagonal resize icon (North-West to South-East).
     * It shows up when the user hovers over the bottom-right corner to scale the box.
     */
    public static final Cursor RESIZE_NWSE = Cursor.createStandard(
            GLFW.GLFW_RESIZE_NWSE_CURSOR, "resize_nwse", Cursor.DEFAULT
    );
}