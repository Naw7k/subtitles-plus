package net.naw.subtitles.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * SubtitleConfig: The "Brain" of the mod.
 * This file handles storing your settings and writing them to a file (subtitles_plus.json)
 * so your settings don't reset when you close Minecraft.
 */
public class SubtitleConfig {
    // --- INTERNAL SYSTEM SETTINGS ---
    private static final Logger LOGGER = LoggerFactory.getLogger("Subtitles+");

    // GSON is a library that converts Java code into a text file (.json) and back.
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // This tells the mod where to create the config file on your computer.
    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "subtitles_plus.json");

    // --- POSITION & SCALE ---
    public double relativeX = 0.5; // Horizontal position (0.0 to 1.0)
    public double relativeY = 0.5; // Vertical position (0.0 to 1.0)
    public float scale = 1.0f;      // Size of the subtitles
    public float boxOpacity = 0.5f; // Transparency of the background

    // --- VISUAL APPEARANCE ---
    public boolean isFlipped = false;        // Grow up or down?
    public boolean showBackground = true;    // Darken screen background?
    public int subtitleBackgroundMode = 1;   // 0: OFF, 1: MODDED, 2: VANILLA
    public boolean showShadow = true;        // Text shadow

    // @SuppressWarnings("unused") tells the IDE: "Don't worry that I'm not using this yet."
    @SuppressWarnings("unused")
    public boolean useCustomFont = false;

    public boolean hideButtons = false;

    @SuppressWarnings("unused")
    public boolean enabled = true;

    public boolean useCategoryColors = true;
    public boolean renderSubtitles = true;
    public boolean showGuides = true;
    public boolean showIcons = true;
    public boolean showPreview = true;

    // --- SINGLETON INSTANCE ---
    // This makes sure the whole mod uses the same "Brain" (one single instance).
    public static SubtitleConfig INSTANCE = load();

    /**
     * save(): Takes the current settings and writes them into the .json file.
     */
    public void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save config!", e);
        }
    }

    /**
     * load(): Reads the .json file when Minecraft starts.
     * If the file doesn't exist yet, it creates a new one with default settings.
     */
    public static SubtitleConfig load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                SubtitleConfig config = GSON.fromJson(reader, SubtitleConfig.class);
                return (config != null) ? config : new SubtitleConfig();
            } catch (IOException e) {
                LOGGER.error("Failed to load config!", e);
            }
        }
        return new SubtitleConfig();
    }
}