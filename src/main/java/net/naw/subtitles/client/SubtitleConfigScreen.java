package net.naw.subtitles.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

/**
 * SubtitleConfigScreen: The main UI class.
 * This handles all the buttons you see and the "Ghost Box" preview logic.
 */
public class SubtitleConfigScreen extends SubtitleConfigScreenBase {
    private boolean showHelpPopup = false;
    @SuppressWarnings("FieldMayBeFinal")
    private long[] limitLastChanged = {0};


    public SubtitleConfigScreen() {
        super(Component.literal("Subtitles+ Config"));
    }

    /**
     * init() runs every time the screen opens or is resized.
     * This is where we define our buttons and sliders.
     */
    @SuppressWarnings("ExtractMethodRecommender")
    @Override
    protected void init() {
        super.init(); // Sets up the box position from the Base class

        // --- UI TOGGLE (SHOW/HIDE) ---
        // This tiny button in the corner lets you hide the menu to see the preview clearly.
        this.addRenderableWidget(Button.builder(Component.literal(config.hideButtons ? "SHOW" : "HIDE"), (ignored) -> {
            config.hideButtons = !config.hideButtons;
            if (config.hideButtons) showHelpPopup = false;
            config.save();
            this.rebuildWidgets(); // Refresh the screen to show/hide the other buttons
        }).bounds(this.width - 5, 0, 5, 20).build());

        // --- MAIN SETTINGS GROUP ---
        if (!config.hideButtons) {
            // Help Button: Toggles the text overlay guide.
            this.addRenderableWidget(Button.builder(Component.literal("?"), (ignored) -> showHelpPopup = !showHelpPopup)
                    .bounds(this.width - 27, 0, 20, 20).build());



            // Opacity Slider: Controls how "see-through" the subtitle background is.
            // Disabled when No BG is selected since there is no background to control.
            float defaultOpacity = 0.8f;
            String opacityLabel = (Math.abs(config.boxOpacity - defaultOpacity) < 0.005f) ? "BG Opacity: Default" : "BG Opacity: " + (int)(config.boxOpacity * 100) + "%";
            AbstractSliderButton opacitySlider = new AbstractSliderButton(10, 85, 100, 20, Component.literal(opacityLabel), config.boxOpacity) {
                @Override
                protected void updateMessage() {
                    float defaultOpacity = 0.8f;
                    String label = (Math.abs(config.boxOpacity - defaultOpacity) < 0.005f) ? "BG Opacity: Default" : "BG Opacity: " + (int)(config.boxOpacity * 100) + "%";
                    this.setMessage(Component.literal(label));
                }
                @Override
                protected void applyValue() {
                    float defaultOpacity = 0.8f;
                    float val = (float)this.value;
                    // Snap to default if close enough
                    if (Math.abs(val - defaultOpacity) < 0.02f) {
                        val = defaultOpacity;
                        this.value = defaultOpacity;
                    }
                    config.boxOpacity = val;
                    config.save();
                }
            };
            opacitySlider.active = config.subtitleBackgroundMode != 0;
            this.addRenderableWidget(opacitySlider);



            // Toggle Preview Button: Turns the "Ghost" subtitle on or off
            this.addRenderableWidget(Button.builder(
                            Component.literal(config.previewMode == 0 ? "Preview: OFF" : config.previewMode == 1 ? "Preview: OUTLINE" : "Preview: ON"),
                            (btn) -> {
                                config.previewMode = (config.previewMode + 1) % 3;
                                btn.setMessage(Component.literal(config.previewMode == 0 ? "Preview: OFF" : config.previewMode == 1 ? "Preview: OUTLINE" : "Preview: ON"));
                                config.save();
                            })
                    .bounds(115, 85, 100, 20)
                    .build());

            // Direction & Dim Toggles: Handles list flow and background darkening.
            this.addRenderableWidget(Button.builder(Component.literal("Flip Direction"), (ignored) -> {
                config.isFlipped = !config.isFlipped;
                config.save();
            }).bounds(10, 10, 100, 20).build());

            this.addRenderableWidget(Button.builder(Component.literal("Screen Dim: " + (config.showBackground ? "ON" : "OFF")), (btn) -> { config.showBackground = !config.showBackground; btn.setMessage(Component.literal("Screen Dim: " + (config.showBackground ? "ON" : "OFF"))); config.save(); }).bounds(115, 10, 100, 20).build());

            // Subtitle Background Cycle: Switches between Off, Modded (tight), and Vanilla (wide).
            String bgName = config.subtitleBackgroundMode == 0 ? "OFF" : (config.subtitleBackgroundMode == 1 ? "MODDED" : "VANILLA");
            this.addRenderableWidget(Button.builder(Component.literal("Sub BG: " + bgName), (btn) -> {
                config.subtitleBackgroundMode = (config.subtitleBackgroundMode + 1) % 3;
                String newName = config.subtitleBackgroundMode == 0 ? "OFF" : (config.subtitleBackgroundMode == 1 ? "MODDED" : "VANILLA");
                btn.setMessage(Component.literal("Sub BG: " + newName));
                config.save();
                this.rebuildWidgets();
            }).bounds(10, 35, 100, 20).build());

            // Appearance & Utility: Shadow, Grid Snapping, and Resetting.
            this.addRenderableWidget(Button.builder(Component.literal("Shadow: " + (config.showShadow ? "ON" : "OFF")), (btn) -> { config.showShadow = !config.showShadow; btn.setMessage(Component.literal("Shadow: " + (config.showShadow ? "ON" : "OFF"))); config.save(); }).bounds(115, 35, 100, 20).build());
            this.addRenderableWidget(Button.builder(Component.literal("Guides: " + (config.showGuides ? "ON" : "OFF")), (btn) -> { config.showGuides = !config.showGuides; btn.setMessage(Component.literal("Guides: " + (config.showGuides ? "ON" : "OFF"))); config.save(); }).bounds(10, 60, 100, 20).build());

            this.addRenderableWidget(Button.builder(Component.literal("RESET POSITION"), (ignored) -> {
                int guiHeight = this.height;
                int guiWidth = this.width;
                config.relativeX = 1.0 - (2.0 / guiWidth);
                config.relativeY = (guiHeight - 35.0) / guiHeight;
                config.scale = 1.0f;
                config.save();
                this.rebuildWidgets();
            }).bounds(115, 60, 100, 20).build());

            // Feature Toggles: Color-coded categories and custom sound icons.
            this.addRenderableWidget(Button.builder(Component.literal("Colors: " + (config.useCategoryColors ? "ON" : "OFF")), (btn) -> { config.useCategoryColors = !config.useCategoryColors; btn.setMessage(Component.literal("Colors: " + (config.useCategoryColors ? "ON" : "OFF"))); config.save(); }).bounds(10, 110, 100, 20).build());
            this.addRenderableWidget(Button.builder(Component.literal("Icons: " + (config.showIcons ? "ON" : "OFF")), (btn) -> { config.showIcons = !config.showIcons; btn.setMessage(Component.literal("Icons: " + (config.showIcons ? "ON" : "OFF"))); config.save(); }).bounds(115, 110, 100, 20).build());

            this.addRenderableWidget(Button.builder(Component.literal("Align: " + (config.subtitleAlignment ? "AUTO" : "CENTER")), (btn) -> { config.subtitleAlignment = !config.subtitleAlignment; btn.setMessage(Component.literal("Align: " + (config.subtitleAlignment ? "AUTO" : "CENTER"))); config.save(); }).bounds(10, 135, 100, 20).build());

            String blacklistLabel = config.blacklist.isEmpty() ? "Blacklist" : "Blacklist (" + config.blacklist.size() + ")";
            this.addRenderableWidget(Button.builder(Component.literal(blacklistLabel), (ignored) -> this.minecraft.setScreen(new BlacklistScreen(this))).bounds(115, 135, 100, 20).build());

            // Subtitle Limit: Controls how many subtitles show at once. 0 = no limit. Click label to reset.
            this.addRenderableWidget(Button.builder(Component.literal("-"), (ignored) -> {
                if (config.subtitleLimit > 0) { config.subtitleLimit--; config.save(); this.rebuildWidgets(); limitLastChanged[0] = System.currentTimeMillis(); }
            }).bounds(10, 160, 20, 20).build());

            this.addRenderableWidget(Button.builder(Component.literal(config.subtitleLimit == 0 ? "Limit: OFF" : "Limit: " + config.subtitleLimit), (ignored) -> {
                config.subtitleLimit = 0;
                config.save();
                this.rebuildWidgets();
            }).bounds(35, 160, 155, 20).build());

            this.addRenderableWidget(Button.builder(Component.literal("+"), (ignored) -> {
                if (config.subtitleLimit < 10) { config.subtitleLimit++; config.save(); this.rebuildWidgets(); limitLastChanged[0] = System.currentTimeMillis(); }
            }).bounds(195, 160, 20, 20).build());
        }
    }

    /**
     * extractBackground() handles the darkening of the screen behind the menu.
     * Only draws if the user has "Toggle Dim" enabled in config.
     */
    @Override
    public void extractBackground(@NotNull GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if (config.showBackground) context.fill(0, 0, this.width, this.height, 0x40000000);
    }

    /**
     * extractRenderState() is the "Heartbeat" of the screen. It draws everything 60+ times per second.
     * Order matters here — background first, then guides, then preview, then buttons on top.
     * Buttons are hidden while dragging so the user can see the preview clearly.
     */
    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        this.extractBackground(context, mouseX, mouseY, delta);

        // --- UPDATE LOGIC ---
        // We call the dragging/scaling logic from the Base class first.
        updateDragAndResizeLogic(context, mouseX, mouseY);

        // --- DRAW LAYERS ---
        if (config.showGuides) renderMagneticGuides(context);

        // Auto-hide buttons while dragging so you can see the preview clearly.
        // Preview always draws regardless — so you can see position while dragging.
        if (!isDragging) {
            this.renderSubtitlePreview(context, mouseX, mouseY);
            if (showHelpPopup) renderHelpOverlay(context); // Draws the guide box if active
            super.extractRenderState(context, mouseX, mouseY, delta); // Draws the buttons on top of everything
        } else {
            this.renderSubtitlePreview(context, mouseX, mouseY);
        }
    }

    /**
     * Draws the white/gray alignment lines and handles magnetic snapping math.
     * Guidelines brighten when the box snaps to them so the user gets clear visual feedback.
     * Snap threshold is 10px — close enough to a guideline = snap to it.
     * Corner snap zones are handled separately in mouseReleased() in the Base class.
     */
    private void renderMagneticGuides(GuiGraphicsExtractor context) {
        int snapThreshold = 10;
        int ghostWidth = boxWidth + (config.showIcons ? 20 : 5);
        float sw = ghostWidth * config.scale;

        // Snap points based on center-based boxX
        int leftX = (int)(sw / 2);
        int centerX = this.width / 2;
        int rightX = (int)(this.width - sw / 2);

        // Top/bottom snap points — top uses different value for modded BG to account for its tighter padding
        int topY = (int)((config.subtitleBackgroundMode == 1 ? 5 : 10) * config.scale);
        int centerY = this.height / 2;
        int bottomY = (int)(this.height - (config.subtitleBackgroundMode == 1 ? 11 : 10) * config.scale);
        snappedX = false;
        snappedY = false;

        if (this.isDragging) {
            // Horizontal Snapping — whichever guide is within threshold wins
            if (Math.abs(boxX - leftX) < snapThreshold) { boxX = leftX; snappedX = true; }
            else if (Math.abs(boxX - centerX) < snapThreshold) { boxX = centerX; snappedX = true; }
            else if (Math.abs(boxX - rightX) < snapThreshold) { boxX = rightX; snappedX = true; }

            // Vertical Snapping
            if (Math.abs(boxY - topY) < snapThreshold) { this.boxY = topY; snappedY = true; }
            else if (Math.abs(boxY - centerY) < snapThreshold) { this.boxY = centerY; snappedY = true; }
            else if (Math.abs(boxY - bottomY) < snapThreshold) { this.boxY = bottomY; snappedY = true; }

            // Update relativeX/Y so the real subtitle position matches the snapped position
            int newGhostWidth = boxWidth + (config.showIcons ? 20 : 5);
            config.relativeX = (double)(boxX - (newGhostWidth * config.scale) / 2) / Math.max(1, this.width - newGhostWidth * config.scale);
            config.relativeY = (double) boxY / Math.max(1, this.height);
        }

        // Draw the visual guidelines — snapped lines are brighter than non-snapped ones
        int gridColor = 0x15FFFFFF, snapColor = 0xA0FFFFFF;
        context.fill(0, 0, 1, this.height, (snappedX && boxX == leftX) ? snapColor : gridColor);
        context.fill(this.width / 2, 0, this.width / 2 + 1, this.height, (snappedX && boxX == centerX) ? snapColor : gridColor);
        context.fill(this.width - 1, 0, this.width, this.height, (snappedX && boxX == rightX) ? snapColor : gridColor);
        context.fill(0, 0, this.width, 1, (snappedY && boxY == topY) ? snapColor : gridColor);
        context.fill(0, this.height / 2, this.width, this.height / 2 + 1, (snappedY && boxY == centerY) ? snapColor : gridColor);
        context.fill(0, this.height - 1, this.width, this.height, (snappedY && boxY == bottomY) ? snapColor : gridColor);
    }

    /**
     * Draws the Ghost subtitles and the dragging box frame.
     * Uses the EXACT same position formula as SubtitlesMixin so the ghost always matches the real subtitle.
     * finalX and finalY are calculated identically to the real subtitle renderer.
     */
    private void renderSubtitlePreview(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        // --- CALCULATE POSITION — identical formula to SubtitlesMixin ---
        // ghostWidth estimates the subtitle width since no real sounds play during config screen
        int ghostWidth = boxWidth + (config.showIcons ? 20 : 5);
        // Vanilla mode uses ghostWidth - 1 to match the real subtitle's positionWidth offset
        float scaledHalfWidth = (ghostWidth * config.scale) / 2.0f;
        float finalX = (float)(config.relativeX * (this.width - ghostWidth * config.scale)) + scaledHalfWidth;
        float finalY = Mth.clamp(
                (float)(config.relativeY * this.height),
                5 * config.scale,
                config.subtitleBackgroundMode == 1 ? this.height - 15 * config.scale : this.height - 5 * config.scale
        ) + (config.isFlipped ? -(config.subtitleBackgroundMode == 1 ? 6 : 5) * config.scale : (config.subtitleBackgroundMode == 1 ? 6 : 5) * config.scale);

        // Label alpha fades out when box opacity is very low so labels don't look weird on transparent backgrounds
        int yellowColor = 0xFFFFFF00;
        int labelWhite = 0xFFFFFFFF;

        // isOutOfBounds turns the UP/DOWN label red when the subtitle would go off-screen
        boolean isOutOfBounds = (!config.isFlipped && finalY - 30 * config.scale < 0) || (config.isFlipped && finalY + 30 * config.scale > this.height);
        int cornerZoneX = (int)(70 * config.scale);
        int cornerZoneY = (int)(25 * config.scale);
        boolean nearCorner = config.showGuides && isDragging &&
                (boxX < cornerZoneX || boxX > this.width - cornerZoneX) &&
                (boxY < cornerZoneY || boxY > this.height - cornerZoneY) &&
                ((!config.isFlipped && boxY < cornerZoneY) || (config.isFlipped && boxY > this.height - cornerZoneY));

        int directionLabelColor;
        if (nearCorner) {
            float pulse = (float)(Math.sin(System.currentTimeMillis() / 200.0) + 1.0) / 2.0f;
            int r = 0xFF;
            int g = (int)(0xFF * pulse);
            int b = (int)(0xFF * pulse);
            directionLabelColor = 0xFF000000 | (r << 16) | (g << 8) | b;
        } else {
            directionLabelColor = isOutOfBounds ? 0xFFFF0000 : labelWhite;
        }

        if (config.previewMode == 0) return; // Nothing to draw if preview is off

        if (config.previewMode == 2) {


            // --- GHOST SUBTITLE ENTRIES ---
            // These mimic what real subtitles look like using the same drawing code as SubtitlesMixin
            String footstepText = config.showIcons ? "👣 Footsteps" : "Footsteps";
            String[] ghostEntries = {"< " + footstepText, config.showIcons ? "§f\uE427" + (config.useCategoryColors ? "§6" : "§r") + " Chest Opens >" : "Chest Opens >"};

            for (int i = 0; i < ghostEntries.length; i++) {
                Component text = Component.literal(ghostEntries[i]);
                int finalTextWidth = this.font.width(text);
                int halfWidth = ghostWidth / 2;
                int halfHeight = 4;

                float entryY = config.isFlipped ?
                        finalY + (i * (config.subtitleBackgroundMode == 1 ? 12 : 10)) * config.scale + (config.subtitleBackgroundMode == 1 ? 1 : 0) :
                        finalY - (i * (config.subtitleBackgroundMode == 1 ? 12 : 10)) * config.scale + (config.subtitleBackgroundMode == 1 ? 1 : 0);

                // Ghost entries use fixed colors matching their real subtitle categories
                // Entry 0 (Footsteps) = green, Entry 1 (Chest) = gold — matches SubtitleColorMapper
                int textColor = config.useCategoryColors ? ((i == 0) ? 0x00AA00 : 0xD4A017) : 0xFFFFFF;
                int finalTextColor = (0xFF << 24) | (textColor & 0xFFFFFF);

                context.pose().pushMatrix();
                context.pose().translate(finalX, entryY);
                context.pose().scale(config.scale, config.scale);

                // Calculate text draw position based on alignment setting
                int ghostDrawX;
                if (config.subtitleAlignment) {
                    int effectiveHalfWidth = ghostWidth / 2;
                    if (config.relativeX < 0.33) ghostDrawX = -effectiveHalfWidth + 3;
                    else if (config.relativeX > 0.66) ghostDrawX = effectiveHalfWidth - finalTextWidth - 3;
                    else ghostDrawX = -finalTextWidth / 2;
                } else {
                    ghostDrawX = -finalTextWidth / 2;
                }

                // --- MODDED BACKGROUND (MODE 1) — tight per-line background based on actual text width ---
                if (config.subtitleBackgroundMode == 1) {
                    int bgColor = (int)(Mth.clamp(config.boxOpacity, 0.3f, 1.0f) * 255) << 24;
                    context.fill(ghostDrawX - 2, -2, ghostDrawX + finalTextWidth + 2, 9, bgColor);
                }

                // --- VANILLA BACKGROUND (MODE 2) — fixed width box centered on the subtitle ---
                if (config.subtitleBackgroundMode == 2) {
                    int bgColor = (int)(Mth.clamp(config.boxOpacity, 0.3f, 1.0f) * 255) << 24;
                    context.fill(-halfWidth, -halfHeight - 1, halfWidth, halfHeight + 1, bgColor);
                }

                // Draw ghost text — centered exactly like real subtitle
                int textOffsetY = config.subtitleBackgroundMode == 1 ? (config.isFlipped ? 4 : 3) : 0;
                context.text(this.font, text, ghostDrawX, -halfHeight + textOffsetY, finalTextColor, config.showShadow);

                context.pose().popMatrix();
            }
        }

        // --- PLACEHOLDER ROWS — show briefly when limit buttons are clicked ---
        if (config.subtitleLimit > 0) {
            long timeSince = System.currentTimeMillis() - limitLastChanged[0];
            if (timeSince < 1500) {
                float alpha = timeSince > 1000 ? 1.0f - ((timeSince - 1000) / 500.0f) : 1.0f;
                int placeholderColor = ((int)(alpha * 0x33)) << 24 | 0xFFFFFF;
                for (int i = 0; i < config.subtitleLimit; i++) {
                    float placeholderY = config.isFlipped ?
                            finalY + (i * (config.subtitleBackgroundMode == 1 ? 12 : 10)) * config.scale + (config.subtitleBackgroundMode == 1 ? 5 : 0) :
                            finalY - (i * (config.subtitleBackgroundMode == 1 ? 12 : 10)) * config.scale + (config.subtitleBackgroundMode == 1 ? 5 : 0);
                    context.pose().pushMatrix();
                    context.pose().translate(finalX, placeholderY);
                    context.pose().scale(config.scale, config.scale);
                    context.fill(-ghostWidth / 2, -4, ghostWidth / 2, 5, placeholderColor);
                    context.pose().popMatrix();
                }
            }
        }

        // --- LABELS ---
        // Scale label (e.g. "1.0x") drawn at 80% scale bottom-left of the ghost box
        // UP/DOWN label drawn at 70% scale — moves up or down based on flip direction
        context.pose().pushMatrix();
        context.pose().translate(finalX, finalY);
        context.pose().scale(config.scale, config.scale);

        // --- SCALE LABEL (1.0x) ---
        context.pose().pushMatrix();
        context.pose().scale(0.8f, 0.8f);
        if (config.subtitleBackgroundMode == 1) {
            // Modded BG — mirrors to right side when on left of screen in AUTO mode
            int scaleYModded = config.isFlipped ? (int)((boxHeight / 2.0f - 8) / 0.11f) - 7 : (int)((boxHeight / 2.0f - 8) / 0.11f) - 22;
            int scaleXModded = config.relativeX < 0.4 && config.subtitleAlignment ? (int)((ghostWidth / 2.0f - 2) / 0.8f) - this.font.width(String.format("%.1fx", config.scale)) : (int)((-ghostWidth / 2.0f + 2) / 0.8f);
            context.text(this.font, String.format("%.1fx", config.scale), scaleXModded, scaleYModded, yellowColor, config.showShadow);
        } else {
            // Vanilla BG and No BG — moves based on flip direction, mirrors to right side when on left of screen
            int scaleY = config.isFlipped ? (int)((-boxHeight / 2.0f + 13f) / 0.4f) : (int)((boxHeight / 2.0f - 14) / 0.7f);
            int scaleX = config.relativeX < 0.4 && config.subtitleAlignment ? (int)((ghostWidth / 2.0f - 2) / 0.8f) - this.font.width(String.format("%.1fx", config.scale)) : (int)((-ghostWidth / 2.0f + 2) / 0.8f);
            context.text(this.font, String.format("%.1fx", config.scale), scaleX, scaleY, yellowColor, config.showShadow);
        }
        context.pose().popMatrix();

         // --- UP/DOWN LABEL ---
        context.pose().pushMatrix();
        context.pose().scale(0.7f, 0.7f);
        if (config.subtitleBackgroundMode == 1) {
            // Modded BG — mirrors to right side when on left of screen in AUTO mode
            int dirYModded = config.isFlipped ? (int)((-boxHeight / 2.0f + 11) / 0.75f) : (int)((-boxHeight / 2.0f - 1.5) / 0.7f);
            int dirXModded = config.relativeX < 0.4 && config.subtitleAlignment ? ghostWidth / 2 + 20 - this.font.width(config.isFlipped ? "DOWN" : "UP") : -(ghostWidth / 2 + 20);
            context.text(this.font, config.isFlipped ? "DOWN" : "UP", dirXModded, dirYModded, directionLabelColor, config.showShadow);
        } else {
            // Vanilla BG and No BG — moves based on flip direction, mirrors to right side when on left of screen
            int dirY = config.isFlipped ? (int)((boxHeight / 2.0f - 13) / 0.7f) : (int)((-boxHeight / 2.5f - 5) / 0.7f);
            int dirX = config.relativeX < 0.4 && config.subtitleAlignment ? ghostWidth / 2 + 20 - this.font.width(config.isFlipped ? "DOWN" : "UP") : -(ghostWidth / 2 + 20);
            context.text(this.font, config.isFlipped ? "DOWN" : "UP", dirX, dirY, directionLabelColor, config.showShadow);
        }
        context.pose().popMatrix();

        context.pose().popMatrix();

        // --- SUBTLE BOX OUTLINE ---
        // Outline bounds are calculated from actual entry positions so it always wraps the ghost entries exactly
        int outlineColor = 0x40FFFFFF;

        // entry1Y is one row above or below finalY depending on flip direction
        float entrySpacing = (config.subtitleBackgroundMode == 1 ? 12 : 10) * config.scale;
        float entry1Y = config.isFlipped ? finalY + entrySpacing : finalY - entrySpacing;

        // Outline left/right edges are centered on finalX at half the ghost width
        float left = finalX - (ghostWidth * config.scale) / 2;
        float right = finalX + (ghostWidth * config.scale) / 2;
        // Top/bottom padding differs between modded (tighter) and other modes
        float top = config.subtitleBackgroundMode == 1 ? Math.min(finalY, entry1Y) - 2 * config.scale : Math.min(finalY, entry1Y) - 5 * config.scale;
        float bottom = config.subtitleBackgroundMode == 1 ? Math.max(finalY, entry1Y) + 11 * config.scale : Math.max(finalY, entry1Y) + 5 * config.scale;

        context.fill((int)left, (int)top, (int)right, (int)top + 1, outlineColor); // top edge
        context.fill((int)left, (int)bottom - 1, (int)right, (int)bottom, outlineColor); // bottom edge
        context.fill((int)left, (int)top, (int)left + 1, (int)bottom, outlineColor); // left edge
        context.fill((int)right - 1, (int)top, (int)right, (int)bottom, outlineColor); // right edge

        // --- CORNER HANDLE ---
        // sw is the full scaled width of the ghost box — used to place the handle at the right edge
        // sh is the scaled height — kept for reference but handle Y comes from outline bottom
        float sw = ghostWidth * config.scale;
        // For vanilla mode, handle is at the right edge of the fixed-width box.
        // For modded/off, handle is at the right edge of the outline.
        this.handleX = config.subtitleBackgroundMode == 2 ? (int)(finalX + sw / 2) : (int)right;
        this.handleY = (int)bottom;

        int cornerHitbox = (int) Mth.clamp(8 * config.scale, 5, 14);
        boolean mouseOverCorner = mouseX >= handleX - cornerHitbox && mouseX <= handleX
                && mouseY >= handleY - cornerHitbox && mouseY <= handleY;

        int handleColor;
        if (isResizing) {
            handleColor = 0xFFFFAA00; // Active: solid gold
        } else if (mouseOverCorner) {
            // Hover: pulses between cyan and white using sine wave
            float pulse = (float)(Math.sin(System.currentTimeMillis() / 400.0) + 1.0) / 2.0f;
            int r = (int)(0xFF * pulse + 0xAA * (1.0f - pulse));
            int g = (int)(0xFF * pulse + 0xFF * (1.0f - pulse));
            int b = (int)(0xFF * pulse + 0xFF * (1.0f - pulse));
            handleColor = 0xFF000000 | (r << 16) | (g << 8) | b;
        } else {
            handleColor = 0xCCFFFFFF; // Idle: static subtle white
        }

        // L-shaped corner bracket: two thin rectangles forming ⌟
        // Size scales with box scale but stays within reasonable bounds
        int armLength = (int) Mth.clamp(8 * config.scale, 5, 14);
        int thickness = (int) Mth.clamp(1 * config.scale, 1, 3);

        // Horizontal arm (bottom edge of corner)
        context.fill(handleX - armLength, handleY - thickness, handleX, handleY, handleColor);
        // Vertical arm (right edge of corner)
        context.fill(handleX - thickness, handleY - armLength, handleX, handleY, handleColor);
    }

    /**
     * renderHelpOverlay(): Draws the in-screen help guide popup.
     * Appears when the user clicks the "?" button.
     * Drawn at 90% scale to fit more text without it looking cramped.
     */
    private void renderHelpOverlay(GuiGraphicsExtractor context) {
        int boxW = 220, boxH = 260, bX = this.width - boxW - 5, bY = 22;
        context.fill(bX, bY, bX + boxW, bY + boxH, 0xFF000000);
        // Draw a subtle gray border around the help box
        int brd = 0xFF555555;
        context.fill(bX, bY, bX + boxW, bY + 1, brd);
        context.fill(bX, bY + boxH - 1, bX + boxW, bY + boxH, brd);
        context.fill(bX, bY, bX + 1, bY + boxH, brd);
        context.fill(bX + boxW - 1, bY, bX + boxW, bY + boxH, brd);

        context.pose().pushMatrix();
        float hScale = 0.9f;
        context.pose().scale(hScale, hScale);
        float sX = (bX + 8) / hScale, sY = (bY + 8) / hScale;
        int wrapW = (int)((boxW - 16) / hScale);

        context.text(this.font, Component.literal("§6§lHelp Guide"), (int)sX, (int)sY, -1, true);

        // Each entry is a [title, description] pair — title in yellow, description in gray
        String[][] helpEntries = {
                {"BG Opacity", "Controls the darkness of the subtitle background."},
                {"Flip Direction", "Determines if the subtitle list expands upwards or downwards."},
                {"Subtitle BG", "Switches between No Background, Modded style, or Vanilla look."},
                {"Guides", "Displays magnetic alignment lines for precise UI positioning."},
                {"Icons", "Displays specialized sound category icons next to the text."},
                {"Colors", "Assigns unique colors to sound categories (e.g. Players, Blocks)."},
                {"Reset Pos", "Resets the subtitle position and scale to match vanilla Minecraft's default."},
                {"Preview", "Cycles between OFF, Outline only, and Full preview modes."},
                {"Align", "CENTER keeps subtitles fixed in place. AUTO shifts them toward the nearest screen edge."},
                {"Limit", "Sets the max number of subtitles shown at once. Click the label to reset to OFF."},
                {"Blacklist", "Blocks specific subtitles from appearing."}
        };

        float currentY = sY + 16;
        for (String[] entry : helpEntries) {
            String fullLine = "§e" + entry[0] + ": §7" + entry[1];
            var lines = this.font.split(Component.literal(fullLine), wrapW);
            for (net.minecraft.util.FormattedCharSequence line : lines) {
                context.text(this.font, line, (int)sX, (int)currentY, -1, true);
                currentY += 10;
            }
            currentY += 3;
        }

        context.pose().popMatrix();
    }
}
