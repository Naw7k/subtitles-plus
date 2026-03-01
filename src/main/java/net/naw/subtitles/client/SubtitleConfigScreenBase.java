package net.naw.subtitles.client;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.naw.subtitles.client.mixin.CursorAccessor;
import net.naw.subtitles.client.utils.Cursors;

public abstract class SubtitleConfigScreenBase extends Screen {
    // --- CONFIG & CONSTANTS ---
    protected final SubtitleConfig config = SubtitleConfig.INSTANCE;
    protected final int boxWidth = 100;
    protected final int boxHeight = 20;
    private final int BOTTOM_MARGIN = 1;
    private final int TOP_MARGIN = 3;

    // --- STATE VARIABLES ---
    protected int boxX, boxY;
    protected boolean isDragging = false;
    protected boolean isResizing = false;
    protected double dragOffsetX, dragOffsetY;

    protected SubtitleConfigScreenBase(Text title) {
        super(title);
    }

    @Override
    protected void init() {
        // Calculate current scaled dimensions
        float sw = boxWidth * config.scale;
        float sh = boxHeight * config.scale;

        // Position the box based on saved relative percentages
        this.boxX = (int) (config.relativeX * (this.width - sw));
        this.boxY = (int) (TOP_MARGIN + (config.relativeY * (this.height - sh - BOTTOM_MARGIN - TOP_MARGIN)));
    }

    /* * Handles the movement, resizing, and cursor icons.
     * Called every frame from the render method of the child screen.
     */
    protected void updateDragAndResizeLogic(DrawContext context, int mouseX, int mouseY) {
        float sw = boxWidth * config.scale;
        float sh = boxHeight * config.scale;

        // Handle Box Movement
        if (this.isDragging) {
            this.boxX = MathHelper.clamp((int)(mouseX - this.dragOffsetX), 0, (int)(this.width - sw));
            this.boxY = MathHelper.clamp((int)(mouseY - this.dragOffsetY), TOP_MARGIN, (int)(this.height - sh - BOTTOM_MARGIN));
            updateRelativePos(sw, sh);
        }
        // Handle Scaling (Resizing)
        else if (this.isResizing) {
            float rawScale = (float) (mouseX - boxX) / boxWidth;
            config.scale = MathHelper.clamp((float) (Math.round(rawScale * 10.0) / 10.0), 0.5f, 3.0f);
            updateRelativePos(boxWidth * config.scale, boxHeight * config.scale);
        }

        // --- CURSOR FEEDBACK ---
        CursorAccessor ca = (CursorAccessor) context;
        if (mouseX >= boxX + sw - 10 && mouseX <= boxX + sw && mouseY >= boxY + sh - 10 && mouseY <= boxY + sh) {
            ca.subtitles$setCursor(Cursors.RESIZE_NWSE);
        } else if (mouseX >= boxX && mouseX <= boxX + sw && mouseY >= boxY && mouseY <= boxY + sh) {
            ca.subtitles$setCursor(Cursors.MOVE);
        } else {
            ca.subtitles$setCursor(Cursors.DEFAULT);
        }
    }

    private void updateRelativePos(float sw, float sh) {
        // Converts pixel position to 0.0 - 1.0 percentage so it stays put if resolution changes
        config.relativeX = (double) boxX / Math.max(1, this.width - (int)sw);
        config.relativeY = (double) (boxY - TOP_MARGIN) / Math.max(1, this.height - (int)sh - BOTTOM_MARGIN - TOP_MARGIN);
    }

    @Override
    public boolean mouseClicked(Click c, boolean bl) {
        double mx = c.x(), my = c.y();
        float sw = boxWidth * config.scale;
        float sh = boxHeight * config.scale;

        // Check for resize handle (bottom-right corner)
        boolean overCorner = (mx >= boxX + sw - 12 && mx <= boxX + sw) &&
                (my >= boxY + sh - 12 && my <= boxY + sh);

        if (overCorner) {
            this.isResizing = true;
            return true;
        }

        // Check for drag area (the whole box)
        if (mx >= boxX - 1 && mx <= boxX + sw + 1 && my >= boxY - 1 && my <= boxY + sh + 1) {
            this.isDragging = true;
            this.dragOffsetX = mx - boxX;
            this.dragOffsetY = my - boxY;
            return true;
        }

        return super.mouseClicked(c, bl);
    }

    @Override
    public boolean mouseReleased(Click c) {
        this.isDragging = false;
        this.isResizing = false;
        config.save(); // Save changes immediately when letting go of the mouse
        return super.mouseReleased(c);
    }
}