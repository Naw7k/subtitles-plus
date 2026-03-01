package net.naw.subtitles.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import org.joml.Matrix3x2fStack;

/**
 * SubtitleConfigScreen: The main UI class.
 * This handles all the buttons you see and the "Ghost Box" preview logic.
 */
public class SubtitleConfigScreen extends SubtitleConfigScreenBase {
    private boolean showHelpPopup = false;
    // Removed the local showPreview because we use config.showPreview now!

    public SubtitleConfigScreen() {
        super(Text.literal("Subtitles+ Config"));
    }

    /**
     * init() runs every time the screen opens or is resized.
     * This is where we define our buttons and sliders.
     */
    @Override
    protected void init() {
        super.init(); // Sets up the box position from the Base class

        // --- UI TOGGLE (SHOW/HIDE) ---
        // This tiny button in the corner lets you hide the menu to see the preview clearly.
        this.addDrawableChild(ButtonWidget.builder(Text.literal(config.hideButtons ? "SHOW" : "HIDE"), (btn) -> {
            config.hideButtons = !config.hideButtons;
            if (config.hideButtons) showHelpPopup = false;
            config.save();
            this.clearAndInit(); // Refresh the screen to show/hide the other buttons
        }).dimensions(this.width - 5, 0, 5, 20).build());

        // --- MAIN SETTINGS GROUP ---
        if (!config.hideButtons) {
            // Help Button: Toggles the text overlay guide.
            this.addDrawableChild(ButtonWidget.builder(Text.literal("?"), (btn) -> showHelpPopup = !showHelpPopup)
                    .dimensions(this.width - 27, 0, 20, 20).build());

            // Opacity Slider: Controls how "see-through" the background box is.
            this.addDrawableChild(new SliderWidget(10, 85, 100, 20, Text.literal("Box Opacity: " + (int)(config.boxOpacity * 100) + "%"), config.boxOpacity) {
                @Override
                protected void updateMessage() { this.setMessage(Text.literal("Box Opacity: " + (int)(config.boxOpacity * 100) + "%")); }
                @Override
                protected void applyValue() { config.boxOpacity = (float)this.value; config.save(); }
            });

            // Toggle Preview Button: Turns the "Ghost" subtitle on or off
            this.addDrawableChild(ButtonWidget.builder(
                            Text.literal(config.showPreview ? "Preview: ON" : "Preview: OFF"),
                            (btn) -> {
                                config.showPreview = !config.showPreview; // Saves it to the config!
                                btn.setMessage(Text.literal(config.showPreview ? "Preview: ON" : "Preview: OFF"));
                                config.save(); // Make sure it saves to file
                            })
                    .dimensions(115, 85, 100, 20)
                    .build());

            // Direction & Dim Toggles: Handles list flow and background darkening.
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Flip Direction"), (btn) -> { config.isFlipped = !config.isFlipped; config.save(); }).dimensions(10, 10, 100, 20).build());
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Toggle Dim"), (btn) -> { config.showBackground = !config.showBackground; config.save(); }).dimensions(115, 10, 100, 20).build());

            // Subtitle Background Cycle: Switches between Off, Modded (tight), and Vanilla (wide).
            String bgName = config.subtitleBackgroundMode == 0 ? "OFF" : (config.subtitleBackgroundMode == 1 ? "MODDED" : "VANILLA");
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Sub BG: " + bgName), (btn) -> {
                config.subtitleBackgroundMode = (config.subtitleBackgroundMode + 1) % 3;
                String newName = config.subtitleBackgroundMode == 0 ? "OFF" : (config.subtitleBackgroundMode == 1 ? "MODDED" : "VANILLA");
                btn.setMessage(Text.literal("Sub BG: " + newName));
                config.save();
            }).dimensions(10, 35, 100, 20).build());

            // Appearance & Utility: Shadow, Grid Snapping, and Resetting.
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Shadow: " + (config.showShadow ? "ON" : "OFF")), (btn) -> { config.showShadow = !config.showShadow; btn.setMessage(Text.literal("Shadow: " + (config.showShadow ? "ON" : "OFF"))); config.save(); }).dimensions(115, 35, 100, 20).build());
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Guides: " + (config.showGuides ? "ON" : "OFF")), (btn) -> { config.showGuides = !config.showGuides; btn.setMessage(Text.literal("Guides: " + (config.showGuides ? "ON" : "OFF"))); config.save(); }).dimensions(10, 60, 100, 20).build());
            this.addDrawableChild(ButtonWidget.builder(Text.literal("RESET POSITION"), (btn) -> { config.relativeX = 0.5; config.relativeY = 0.5; config.scale = 1.0f; config.save(); this.clearAndInit(); }).dimensions(115, 60, 100, 20).build());

            // Feature Toggles: Color-coded categories and custom sound icons.
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Colors: " + (config.useCategoryColors ? "ON" : "OFF")), (btn) -> { config.useCategoryColors = !config.useCategoryColors; btn.setMessage(Text.literal("Colors: " + (config.useCategoryColors ? "ON" : "OFF"))); config.save(); }).dimensions(10, 110, 100, 20).build());
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Icons: " + (config.showIcons ? "ON" : "OFF")), (btn) -> { config.showIcons = !config.showIcons; btn.setMessage(Text.literal("Icons: " + (config.showIcons ? "ON" : "OFF"))); config.save(); }).dimensions(115, 110, 100, 20).build());
        }
    }

    /**
     * renderBackground() handles the darkening of the screen behind the menu.
     */
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        if (config.showBackground) context.fill(0, 0, this.width, this.height, 0x40000000);
    }

    /**
     * render() is the "Heartbeat" of the screen. It draws everything 60+ times per second.
     */
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        // --- UPDATE LOGIC ---
        // We call the dragging/scaling logic from the Base class first.
        updateDragAndResizeLogic(context, mouseX, mouseY);

        // --- DRAW LAYERS ---
        if (config.showGuides) renderMagneticGuides(context);

        // This always draws so you can see the background box and labels while positioning
        this.renderSubtitlePreview(context);

        if (showHelpPopup) renderHelpOverlay(context); // Draws the guide box if active

        super.render(context, mouseX, mouseY, delta); // Draws the buttons on top of everything
    }

    /**
     * Draws the white/gray alignment lines and handles the "Snapping" math.
     */
    private void renderMagneticGuides(DrawContext context) {
        int snapThreshold = 10;
        float sw = boxWidth * config.scale;
        float sh = boxHeight * config.scale;
        int leftX = 0, centerX = this.width / 2 - (int)(sw / 2), rightX = this.width - (int)sw;
        int topY = 0, centerY = this.height / 2 - (int)(sh / 2), bottomY = this.height - (int)sh;
        boolean snappedX = false, snappedY = false;

        if (this.isDragging) {
            // Horizontal Snapping
            if (Math.abs(boxX - leftX) < snapThreshold) { boxX = leftX; snappedX = true; }
            else if (Math.abs(boxX - centerX) < snapThreshold) { boxX = centerX; snappedX = true; }
            else if (Math.abs(boxX - rightX) < snapThreshold) { boxX = rightX; snappedX = true; }

            // Vertical Snapping
            if (Math.abs(boxY - topY) < snapThreshold) { this.boxY = topY; snappedY = true; }
            else if (Math.abs(boxY - centerY) < snapThreshold) { this.boxY = centerY; snappedY = true; }
            else if (Math.abs(boxY - bottomY) < snapThreshold) { this.boxY = bottomY; snappedY = true; }

            config.relativeX = (double) boxX / Math.max(1, this.width - (int)sw);
            config.relativeY = (double) boxY / Math.max(1, this.height - (int)sh);
        }

        // Draw the visual lines
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
     */
    private void renderSubtitlePreview(DrawContext context) {
        int alpha = (int)(config.boxOpacity * 255);
        int boxBgColor = (alpha << 24);
        int labelAlpha = (config.boxOpacity >= 0.3f) ? 255 : (int)((config.boxOpacity / 0.3f) * 255);
        int yellowColor = (labelAlpha << 24) | 0xFFFF00;
        int labelWhite = (labelAlpha << 24) | 0xFFFFFF;

        boolean isOutOfBounds = (!config.isFlipped && (boxY - 12) < 0) || (config.isFlipped && (boxY + (boxHeight * config.scale) + 12) > this.height);

        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(boxX, boxY);
        matrices.scale(config.scale, config.scale);

        // --- LAYER 1: THE BOX (Always visible for opacity editing) ---
        if (config.boxOpacity >= 1.0f) context.fill(0, -2, boxWidth, boxHeight + 2, boxBgColor);

        // --- LAYER 2: THE GHOST TEXT (Only if showPreview is ON) ---
        if (config.showPreview) {
            // We define the preview strings here to check if icons should be shown
            String footstepText = config.showIcons ? "👣 Footsteps" : "Footsteps";
            String chestText = "Chest Opens";

            String[] events = {"< " + footstepText, chestText + " >"};

            for (int i = 0; i < events.length; i++) {
                Text text = Text.literal(events[i]);
                int tw = this.textRenderer.getWidth(text);
                int tx = (config.relativeX < 0.33) ? 0 : (config.relativeX > 0.66 ? boxWidth - tw : boxWidth / 2 - tw / 2);
                int ty = config.isFlipped ? (boxHeight - 20) + (i * 12) : 12 - (i * 12);
                int textColor = config.useCategoryColors ? ((i == 0) ? 0x00AA00 : 0xD4A017) : 0xFFFFFF;
                int finalTextColor = (0xFF << 24) | (textColor & 0xFFFFFF);

                if (config.subtitleBackgroundMode > 0) {
                    int padding = config.subtitleBackgroundMode == 1 ? 2 : 4;
                    context.fill(tx - padding, ty - 2, tx + tw + padding, ty + (config.subtitleBackgroundMode == 1 ? 9 : 10), 0x90000000);
                }
                context.drawText(this.textRenderer, text, tx, ty, finalTextColor, config.showShadow);
            }
        }

        if (config.boxOpacity < 1.0f) context.fill(0, -2, boxWidth, boxHeight + 2, boxBgColor);

        // --- LAYER 3: LABELS ---
        matrices.pushMatrix();
        matrices.scale(0.8f, 0.8f);
        context.drawText(this.textRenderer, String.format("%.1fx", config.scale), (int)(2 / 0.8f), (int)((boxHeight - 8) / 0.8f), yellowColor, config.showShadow);
        matrices.popMatrix();

        matrices.pushMatrix();
        matrices.scale(0.7f, 0.7f);
        context.drawText(this.textRenderer, config.isFlipped ? "DOWN" : "UP", (int)((boxWidth / 2.0f) / 0.7f) - (config.isFlipped ? 70 : 66), (int)(3 / 0.7f), isOutOfBounds ? 0xFFFF0000 : labelWhite, config.showShadow);
        matrices.popMatrix();

        context.fill(boxWidth - 4, boxHeight - 2, boxWidth, boxHeight + 2, isOutOfBounds ? 0xFFFF0000 : labelWhite);
        matrices.popMatrix();
    }

    /**
     * Draws the Help Guide box with the text you provided.
     */
    private void renderHelpOverlay(DrawContext context) {
        int boxW = 220, boxH = 190, bX = this.width - boxW - 5, bY = 22;
        context.fill(bX, bY, bX + boxW, bY + boxH, 0xFF000000);
        int brd = 0xFF555555;
        context.fill(bX, bY, bX + boxW, bY + 1, brd);
        context.fill(bX, bY + boxH - 1, bX + boxW, bY + boxH, brd);
        context.fill(bX, bY, bX + 1, bY + boxH, brd);
        context.fill(bX + boxW - 1, bY, bX + boxW, bY + boxH, brd);

        context.getMatrices().pushMatrix();
        float hScale = 0.9f;
        context.getMatrices().scale(hScale, hScale);
        float sX = (bX + 8) / hScale, sY = (bY + 8) / hScale;
        int wrapW = (int)((boxW - 16) / hScale);

        context.drawTextWithShadow(this.textRenderer, "§6§lHelp Guide", (int)sX, (int)sY, -1);

        String[][] helpEntries = {
                {"Box Opacity", "Sets the transparency level of the Preview Box background."},
                {"Flip Direction", "Determines if the subtitle list expands upwards or downwards."},
                {"Subtitle BG", "Switches between No Background, Modded style, or Vanilla look."},
                {"Guides", "Displays magnetic alignment lines for precise UI positioning."},
                {"Icons", "Displays specialized sound category icons next to the text."},
                {"Colors", "Assigns unique colors to sound categories (e.g. Players, Blocks)."},
                {"Reset Pos", "Restores the Preview Box/Ghost Subtitle to the center of the screen."},
                {"Preview", "Toggles the 'Ghost' subtitle on/off for a cleaner editing view."}
        };

        float currentY = sY + 16;
        for (String[] entry : helpEntries) {
            String fullLine = "§e" + entry[0] + ": §7" + entry[1];
            var lines = this.textRenderer.wrapLines(Text.literal(fullLine), wrapW);
            for (net.minecraft.text.OrderedText line : lines) {
                context.drawTextWithShadow(this.textRenderer, line, (int)sX, (int)currentY, -1);
                currentY += 10;
            }
            currentY += 3;
        }

        context.getMatrices().popMatrix();
    }
}