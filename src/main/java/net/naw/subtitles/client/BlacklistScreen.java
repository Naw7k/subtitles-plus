package net.naw.subtitles.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

/**
 * BlacklistScreen: Manages the subtitle blacklist.
 * Shows all blacklisted entries in a scrollable list with delete buttons.
 * Use the input field at the bottom to add new entries.
 * Suggestions appear as you type, pulled from Minecraft's language file.
 * Press Enter or click Add to confirm. Scroll with mouse wheel.
 */
public class BlacklistScreen extends Screen {
    private final Screen parent;
    private final SubtitleConfig config = SubtitleConfig.INSTANCE;
    private EditBox inputField;
    private int scrollOffset = 0;
    private final List<String> allSuggestions = new ArrayList<>();
    private List<String> filteredSuggestions = new ArrayList<>();
    private int selectedSuggestion = -1;

    private static final int ENTRY_HEIGHT = 22;
    private static final int LIST_TOP = 35;
    private static final int LIST_BOTTOM_MARGIN = 80;
    private static final int SUGGESTION_HEIGHT = 12;
    private static final int MAX_SUGGESTIONS = 5;

    public BlacklistScreen(Screen parent) {
        super(Component.literal("Subtitle Blacklist"));
        this.parent = parent;
        loadSuggestions();
    }

    private void loadSuggestions() {
        if (!allSuggestions.isEmpty()) return;
        try (InputStream stream = Language.class.getResourceAsStream("/assets/minecraft/lang/en_us.json")) {
            if (stream != null) {
                Language.loadFromJson(stream, (key, value) -> {
                    if (key.startsWith("subtitles.")) {
                        allSuggestions.add(value);
                    }
                });
            }
        } catch (Exception e) {
            // Silently fail — suggestions just won't show
        }
    }

    private void updateSuggestions() {
        String query = inputField.getValue().trim().toLowerCase();
        if (query.isEmpty()) {
            filteredSuggestions = new ArrayList<>();
        } else {
            filteredSuggestions = allSuggestions.stream()
                    .filter(s -> s.toLowerCase().contains(query))
                    .filter(s -> !config.blacklist.contains(s))
                    .limit(MAX_SUGGESTIONS)
                    .collect(Collectors.toList());
        }
        selectedSuggestion = -1;
    }

    @Override
    protected void init() {
        int listBottom = this.height - LIST_BOTTOM_MARGIN;
        int visibleEntries = (listBottom - LIST_TOP) / ENTRY_HEIGHT;

        // Input field at the bottom
        inputField = new EditBox(this.font, this.width / 2 - 100, this.height - 55, 180, 20, Component.literal(""));
        inputField.setMaxLength(100);
        inputField.setHint(Component.literal("Type subtitle to blacklist..."));
        inputField.setResponder(ignored -> updateSuggestions());
        this.addRenderableWidget(inputField);

        // Add button
        this.addRenderableWidget(Button.builder(Component.literal("Add"), (ignored) -> addEntry())
                .bounds(this.width / 2 + 85, this.height - 55, 40, 20).build());

        // Back button
        this.addRenderableWidget(Button.builder(Component.literal("Back"), (ignored) -> this.minecraft.setScreen(this.parent))
                .bounds(this.width / 2 - 50, this.height - 30, 100, 20).build());

        // Delete buttons for visible entries
        for (int i = 0; i < visibleEntries && (i + scrollOffset) < config.blacklist.size(); i++) {
            final int index = i + scrollOffset;
            int entryY = LIST_TOP + i * ENTRY_HEIGHT;
            this.addRenderableWidget(Button.builder(Component.literal("X"), (ignored) -> {
                config.blacklist.remove(index);
                config.save();
                scrollOffset = Mth.clamp(scrollOffset, 0, Math.max(0, config.blacklist.size() - visibleEntries));
                this.rebuildWidgets();
            }).bounds(this.width / 2 + 85, entryY, 20, 20).build());
        }
    }

    private void addEntry() {
        String text = inputField.getValue().trim();
        if (!text.isEmpty() && !config.blacklist.contains(text)) {
            config.blacklist.add(text);
            config.save();
            inputField.setValue("");
            filteredSuggestions = new ArrayList<>();
            selectedSuggestion = -1;
            this.rebuildWidgets();
        }
    }

    private void addSuggestion(String suggestion) {
        if (!config.blacklist.contains(suggestion)) {
            config.blacklist.add(suggestion);
            config.save();
            inputField.setValue("");
            filteredSuggestions = new ArrayList<>();
            selectedSuggestion = -1;
            this.rebuildWidgets();
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        // Dark background
        context.fill(0, 0, this.width, this.height, 0xAA000000);

        // Title
        context.centeredText(this.font, this.title, this.width / 2, 15, 0xFFFF55);

        int listBottom = this.height - LIST_BOTTOM_MARGIN;
        int visibleEntries = (listBottom - LIST_TOP) / ENTRY_HEIGHT;

        // Draw buttons first
        super.extractRenderState(context, mouseX, mouseY, delta);

        if (config.blacklist.isEmpty()) {
            context.centeredText(this.font, Component.literal("§7No entries yet. Type a subtitle name and click Add."), this.width / 2, LIST_TOP + 10, -1);
        } else {
            // Draw entries on top
            for (int i = 0; i < visibleEntries && (i + scrollOffset) < config.blacklist.size(); i++) {
                int entryY = LIST_TOP + i * ENTRY_HEIGHT;
                String entry = config.blacklist.get(i + scrollOffset);
                context.fill(this.width / 2 - 105, entryY, this.width / 2 + 83, entryY + 20, 0x88000000);
                context.text(this.font, entry, this.width / 2 - 100, entryY + 6, -1);
            }

            // Scroll indicator
            if (config.blacklist.size() > visibleEntries) {
                context.centeredText(this.font,
                        Component.literal("§7" + (scrollOffset + 1) + "-" + Math.min(scrollOffset + visibleEntries, config.blacklist.size()) + " of " + config.blacklist.size()),
                        this.width / 2, listBottom + 5, -1);
            }
        }

        // Scrollbar
        if (config.blacklist.size() > visibleEntries) {
            int scrollbarHeight = listBottom - LIST_TOP;
            int thumbHeight = Math.max(20, scrollbarHeight * visibleEntries / config.blacklist.size());
            int maxScroll = Math.max(1, config.blacklist.size() - visibleEntries);
            int thumbY = LIST_TOP + (scrollbarHeight - thumbHeight) * scrollOffset / maxScroll;
            context.fill(this.width / 2 + 108, LIST_TOP, this.width / 2 + 110, listBottom, 0x55FFFFFF);
            context.fill(this.width / 2 + 108, thumbY, this.width / 2 + 110, thumbY + thumbHeight, 0xFFFFFFFF);
        }

        // Divider line above input
        context.fill(this.width / 2 - 105, this.height - 62, this.width / 2 + 130, this.height - 61, 0x55FFFFFF);

        // --- SUGGESTIONS ---
        if (!filteredSuggestions.isEmpty()) {
            int suggBoxX = this.width / 2 - 100;
            int suggBoxY = this.height - 55 - (filteredSuggestions.size() * SUGGESTION_HEIGHT) - 4;
            int suggBoxW = 180;

            // Suggestion box background
            context.fill(suggBoxX, suggBoxY, suggBoxX + suggBoxW, this.height - 55 - 2, 0xFF222222);
            context.fill(suggBoxX, suggBoxY, suggBoxX + suggBoxW, suggBoxY + 1, 0xFFAAAAAA); // top border

            for (int i = 0; i < filteredSuggestions.size(); i++) {
                int sy = suggBoxY + i * SUGGESTION_HEIGHT + 2;
                boolean hovered = mouseX >= suggBoxX && mouseX <= suggBoxX + suggBoxW && mouseY >= sy && mouseY <= sy + SUGGESTION_HEIGHT;
                boolean selected = i == selectedSuggestion;
                if (hovered || selected) {
                    context.fill(suggBoxX, sy, suggBoxX + suggBoxW, sy + SUGGESTION_HEIGHT, 0xFF444444);
                }
                context.text(this.font, filteredSuggestions.get(i), suggBoxX + 3, sy + 2, hovered || selected ? 0xFFFFFF00 : 0xFFCCCCCC);
            }
        }
    }

    @Override
    public boolean mouseClicked(@NotNull net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        // Check if clicking on a suggestion
        if (!filteredSuggestions.isEmpty()) {
            int suggBoxX = this.width / 2 - 100;
            int suggBoxY = this.height - 55 - (filteredSuggestions.size() * SUGGESTION_HEIGHT) - 4;
            int suggBoxW = 180;
            double mx = event.x(), my = event.y();

            for (int i = 0; i < filteredSuggestions.size(); i++) {
                int sy = suggBoxY + i * SUGGESTION_HEIGHT + 2;
                if (mx >= suggBoxX && mx <= suggBoxX + suggBoxW && my >= sy && my <= sy + SUGGESTION_HEIGHT) {
                    addSuggestion(filteredSuggestions.get(i));
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        int listBottom = this.height - LIST_BOTTOM_MARGIN;
        int visibleEntries = (listBottom - LIST_TOP) / ENTRY_HEIGHT;
        int maxScroll = Math.max(0, config.blacklist.size() - visibleEntries);
        scrollOffset = Mth.clamp((int)(scrollOffset - scrollY), 0, maxScroll);
        this.rebuildWidgets();
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        // Enter to add
        if (event.key() == 257 && inputField != null && inputField.isFocused()) {
            if (selectedSuggestion >= 0 && selectedSuggestion < filteredSuggestions.size()) {
                addSuggestion(filteredSuggestions.get(selectedSuggestion));
            } else {
                addEntry();
            }
            return true;
        }
        // Arrow up/down to navigate suggestions
        if (event.key() == 265 && !filteredSuggestions.isEmpty()) { // up
            selectedSuggestion = Math.max(0, selectedSuggestion - 1);
            return true;
        }
        if (event.key() == 264 && !filteredSuggestions.isEmpty()) { // down
            selectedSuggestion = Math.min(filteredSuggestions.size() - 1, selectedSuggestion + 1);
            return true;
        }
        return super.keyPressed(event);
    }
}