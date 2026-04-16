package net.naw.subtitles.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * HelpScreen: A dedicated full-screen guide.
 * This screen is opened from the config screen via the "?" button.
 * It gives the user a large, clear view of what every setting does.
 */
@SuppressWarnings("unused") // Instantiated via SubtitleConfigScreen — IDE can't detect this
public class HelpScreen extends Screen {
    private final Screen parent; // Remembers which screen opened this one (usually the Config Screen)

    public HelpScreen(Screen parent) {
        super(Component.literal("Subtitles+ Help Guide"));
        this.parent = parent;
    }

    /**
     * init() handles the setup.
     * We only need one button here: the "Back" button to return to the config.
     */
    @Override
    protected void init() {
        this.addRenderableWidget(Button.builder(Component.literal("Back"), (ignored) -> {
            // minecraft is never null here — this screen can only open while the game is running
            this.minecraft.setScreen(this.parent);
        }).bounds(this.width / 2 - 50, this.height - 30, 100, 20).build());
    }

    /**
     * extractRenderState() draws the help text on the screen.
     * Each setting gets a yellow title and a gray description below it.
     */
    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        // Note: renderBackground is omitted here to prevent potential flickering/crashes
        // as noted in your original comments.

        context.centeredText(this.font, this.title, this.width / 2, 20, 0xFFFF55);

        // Positioning for the list
        int y = 50;
        int x = this.width / 2 - 120; // Moved slightly left to fit the longer text

        // All 7 descriptions to match your SubtitleConfigScreen perfectly
        drawHelpLine(context, "Box Opacity", "Sets the transparency level of the Preview Box background.", x, y);
        drawHelpLine(context, "Flip Direction", "Determines if the subtitle list expands upwards or downwards.", x, y + 25);
        drawHelpLine(context, "Subtitle BG", "Switches between No Background, Modded style, or Vanilla look.", x, y + 50);
        drawHelpLine(context, "Guides", "Displays magnetic alignment lines for precise UI positioning.", x, y + 75);
        drawHelpLine(context, "Icons", "Displays specialized sound category icons next to the text.", x, y + 100);
        drawHelpLine(context, "Colors", "Assigns unique colors to sound categories (e.g. Players, Blocks).", x, y + 125);
        drawHelpLine(context, "Reset Pos", "Restores the Preview Box/Ghost Subtitle to the center of the screen.", x, y + 150);

        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    /**
     * Helper method to draw a setting name in Yellow (§e) and its description in Gray (§7).
     * Each entry takes up 20px of vertical space (10px title + 10px description).
     */
    private void drawHelpLine(GuiGraphicsExtractor context, String title, String desc, int x, int y) {
        context.text(this.font, "§e" + title + ":", x, y, -1);
        context.text(this.font, "§7" + desc, x, y + 10, -1);
    }
}
