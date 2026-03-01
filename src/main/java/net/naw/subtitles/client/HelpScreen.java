package net.naw.subtitles.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * HelpScreen: A dedicated full-screen guide.
 * This is useful if the user wants a large, clear view of what every setting does.
 */
public class HelpScreen extends Screen {
    private final Screen parent; // Remembers which screen opened this one (usually the Config Screen)

    public HelpScreen(Screen parent) {
        super(Text.literal("Subtitles+ Help Guide"));
        this.parent = parent;
    }

    /**
     * init() handles the setup.
     * We only need one button here: the "Back" button to return to the config.
     */
    @Override
    protected void init() {
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Back"), (btn) -> {
            if (this.client != null) this.client.setScreen(this.parent);
        }).dimensions(this.width / 2 - 50, this.height - 30, 100, 20).build());
    }

    /**
     * render() draws the text on the screen.
     */
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Note: renderBackground is omitted here to prevent potential flickering/crashes
        // as noted in your original comments.

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFF55);

        // Positioning for the list
        int y = 50;
        int x = this.width / 2 - 120; // Moved slightly left to fit the longer text

        // all 7 descriptions to match your SubtitleConfigScreen perfectly
        drawHelpLine(context, "Box Opacity", "Sets the transparency level of the Preview Box background.", x, y);
        drawHelpLine(context, "Flip Direction", "Determines if the subtitle list expands upwards or downwards.", x, y + 25);
        drawHelpLine(context, "Subtitle BG", "Switches between No Background, Modded style, or Vanilla look.", x, y + 50);
        drawHelpLine(context, "Guides", "Displays magnetic alignment lines for precise UI positioning.", x, y + 75);
        drawHelpLine(context, "Icons", "Displays specialized sound category icons next to the text.", x, y + 100);
        drawHelpLine(context, "Colors", "Assigns unique colors to sound categories (e.g. Players, Blocks).", x, y + 125);
        drawHelpLine(context, "Reset Pos", "Restores the Preview Box/Ghost Subtitle to the center of the screen.", x, y + 150);

        super.render(context, mouseX, mouseY, delta);
    }

    /**
     * Helper method to draw a title in Yellow (§e) and the description in Gray (§7).
     */
    private void drawHelpLine(DrawContext context, String title, String desc, int x, int y) {
        context.drawTextWithShadow(this.textRenderer, "§e" + title + ":", x, y, -1);
        context.drawTextWithShadow(this.textRenderer, "§7" + desc, x, y + 10, -1);
    }
}