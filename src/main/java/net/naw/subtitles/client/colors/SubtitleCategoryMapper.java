package net.naw.subtitles.client.colors;

import net.minecraft.sounds.SoundSource;

/**
 * SubtitleCategoryMapper: The "General Painter."
 * This file handles the default colors for each Minecraft sound category.
 */
public class SubtitleCategoryMapper {

    public static int getCategoryColor(SoundSource category) {
        // Every category has its own identity here.
        return switch (category) {

            case MASTER  -> 0xFFFFFF; // White (Used for generic sounds or mods without categories).
            case UI      -> 0xAAAAAA; // Gray (Menu clicks).
            case VOICE   -> 0xFFFFFF; // White (Villagers/Speaking).
            case MUSIC   -> 0xFF55FF; // Light Purple/Pink (Background music).
            case RECORDS -> 0xFFAA00; // Gold (Jukeboxes/Discs).


            case PLAYERS -> 0x5555FF; // Blue (Other players)
            case HOSTILE -> 0xFF5555; // Red (Zombies/Creepers)
            case NEUTRAL -> 0xFFAA00; // Orange (Sheep/Cows/Pigs)
            case BLOCKS  -> 0xFFFF55; // Yellow (Doors/Chest/Breaking)
            case WEATHER -> 0x55FFFF; // Aqua (Rain/Thunder)
            case AMBIENT -> 0xAAAAAA; // Gray (Cave sounds/Bird chirps)


            /*
             * QUICK TEST COMMANDS:
             * Hostile (Red): /playsound minecraft:entity.zombie.ambient hostile @s
             * Players (Blue): /playsound minecraft:entity.player.burp player @s
             * Blocks (Yellow): /playsound minecraft:block.stone.break block @s
             * Weather (Aqua): /playsound minecraft:weather.rain weather @s
             * Ambient (Gray): /playsound minecraft:ambient.cave ambient @s
             */


            default      -> 0xFFFFFF; // Fallback (Safety net: If Minecraft adds a new category, use White)
        };
    }
}
