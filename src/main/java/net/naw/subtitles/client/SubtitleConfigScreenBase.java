package net.naw.subtitles.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.naw.subtitles.client.mixin.CursorAccessor;
import net.naw.subtitles.client.utils.Cursors;
import org.jetbrains.annotations.NotNull;

public abstract class SubtitleConfigScreenBase extends Screen {
    // --- CONFIG & CONSTANTS ---
    protected final SubtitleConfig config = SubtitleConfig.INSTANCE;
    protected final int boxWidth = 100;
    protected final int boxHeight = 20;

    // --- STATE VARIABLES ---
    protected int boxX, boxY;
    protected int handleX, handleY; // Visual corner handle position — set each frame by renderSubtitlePreview
    protected boolean isDragging = false;
    protected boolean isResizing = false;
    protected boolean hasResizeMoved = false; // Only apply scale if mouse actually moved after click
    protected double dragOffsetX, dragOffsetY;
    protected boolean snappedX = false;
    protected boolean snappedY = false;

    protected SubtitleConfigScreenBase(Component title) {
        super(title);
    }

    @Override
    protected void init() {
        // Position boxX/boxY using the same formula as finalX/finalY in renderSubtitlePreview
        // so drag hitbox matches the visual ghost subtitle position
        int ghostWidth = boxWidth + (config.showIcons ? 20 : 5);
        float scaledHalfWidth = (ghostWidth * config.scale) / 2.0f;
        this.boxX = (int)((config.relativeX * (this.width - ghostWidth * config.scale)) + scaledHalfWidth);
        this.boxY = Mth.clamp((int)(config.relativeY * this.height), (int)(10 * config.scale), (int)(this.height - 10.0f * config.scale));
    }

    /**
     * Handles the movement, resizing, and cursor icons.
     * Called every frame from the render method of the child screen.
     */
    protected void updateDragAndResizeLogic(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        int ghostWidth = boxWidth + (config.showIcons ? 20 : 5);
        float sw = ghostWidth * config.scale;
        float sh = boxHeight * config.scale;

        // Handle Box Movement
        if (this.isDragging) {
            this.boxX = Mth.clamp((int)(mouseX - this.dragOffsetX), (int)(sw / 2), (int)(this.width - sw / 2));
            this.boxY = Mth.clamp((int)(mouseY - this.dragOffsetY), (int)((config.subtitleBackgroundMode == 1 ? 7 : 10) * config.scale), (int)(this.height - 10.0f * config.scale));
            updateRelativePos(ghostWidth * config.scale);
        }
        // Handle Scaling (Resizing) — only apply if mouse actually moved after initial click
        else if (this.isResizing && this.hasResizeMoved) {
            // Cast is intentional — mouseX is int  we need float division result
            float rawScale = (mouseX - (handleX - (boxWidth * config.scale))) / (float) boxWidth;
            config.scale = Mth.clamp((float)(Math.round(rawScale * 10.0) / 10.0), 0.5f, 3.0f);
            int newGhostWidth = boxWidth + (config.showIcons ? 20 : 5);
            updateRelativePos(newGhostWidth * config.scale);
        }

        // --- CURSOR FEEDBACK — uses handleX/handleY set by renderSubtitlePreview ---
        CursorAccessor ca = (CursorAccessor) context;
        int cornerHitbox = (int) Mth.clamp(8 * config.scale, 5, 5);
        if (this.isResizing) {
            ca.subtitles$setCursor(Cursors.RESIZE_NWSE);
        } else if (mouseX >= handleX - cornerHitbox && mouseX <= handleX && mouseY >= handleY - cornerHitbox && mouseY <= handleY) {
            ca.subtitles$setCursor(Cursors.RESIZE_NWSE);
        } else if (this.isDragging || (mouseX >= boxX - sw / 2 && mouseX <= boxX + sw / 2 && mouseY >= boxY - sh / 2 && mouseY <= boxY + sh / 2)) {
            ca.subtitles$setCursor(Cursors.MOVE);
        } else {
            ca.subtitles$setCursor(Cursors.DEFAULT);
        }
    }

    private void updateRelativePos(float ghostScaledWidth) {
        // Converts center-based pixel position to 0.0 - 1.0 percentage
        // so it stays put if resolution changes
        config.relativeX = (double)(boxX - ghostScaledWidth / 2) / Math.max(1, this.width - ghostScaledWidth);
        config.relativeY = (double) boxY / Math.max(1, this.height);
    }

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        int ghostWidth = boxWidth + (config.showIcons ? 20 : 5);
        float sw = ghostWidth * config.scale;
        float sh = boxHeight * config.scale;

        // Check for resize handle — uses handleX/handleY which match the visual handle position
        int cornerHitbox = (int) Mth.clamp(8 * config.scale, 5, 14);
        boolean overCorner = (mx >= handleX - cornerHitbox && mx <= handleX) &&
                (my >= handleY - cornerHitbox && my <= handleY);

        if (overCorner) {
            this.isResizing = true;
            this.hasResizeMoved = false; // Reset — wait for actual movement before applying scale
            return true;
        }

        // Check for drag area (the whole box)
        if (mx >= boxX - sw / 2 - 1 && mx <= boxX + sw / 2 + 1 && my >= boxY - sh / 2 - 1 && my <= boxY + sh / 2 + 1) {
            this.isDragging = true;
            this.dragOffsetX = mx - boxX;
            this.dragOffsetY = my - boxY;
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(@NotNull MouseButtonEvent event) {
        this.isDragging = false;
        this.isResizing = false;
        this.hasResizeMoved = false;

        // Snap to corner if dropped inside a corner zone
        if (config.showGuides) {
            int zoneSize = 30;
            int half = zoneSize / 2;
            if (boxX < half && boxY < half) { config.relativeX = 0.0; config.relativeY = 0.0; } // Top Left
            else if (boxX > this.width - half && boxY < half) { config.relativeX = 1.0; config.relativeY = 0.0; } // Top Right
            else if (boxX < half && boxY > this.height - half) { config.relativeX = 0.0; config.relativeY = 1.0; } // Bottom Left
            else if (boxX > this.width - half && boxY > this.height - half) { config.relativeX = 1.0; config.relativeY = 1.0; } // Bottom Right

            // Auto flip — only triggers when both X and Y guidelines are snapped (i.e. a true corner)
            if (snappedX && snappedY && (boxX < this.width / 3 || boxX > this.width * 2 / 3) && (boxY < this.height / 3 || boxY > this.height * 2 / 3)) {
                config.isFlipped = boxY < this.height / 2;
            }

            this.rebuildWidgets();
        }

        config.save(); // Save changes immediately when letting go of the mouse
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(@NotNull MouseButtonEvent event, double dx, double dy) {
        if (this.isResizing) {
            this.hasResizeMoved = true; // Mouse moved while resizing — now safe to apply scale
        }
        return super.mouseDragged(event, dx, dy);
    }
}
