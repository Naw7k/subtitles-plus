package net.naw.subtitles.client.colors;

/*
 * NOTE: THIS IS A "STATIC HELPER" OR "LOGIC MAPPING" METHOD.
 * * WHY THIS IS BETTER THAN PUTTING IT IN THE MIXIN:

 * 1. THE "PORTAL" SYSTEM: This file acts like a shortcut. The SubtitlesMixin
 * is the "Engine" (it draws the text), but this file is the "Brain"
 * (it decides the color). The Mixin "asks" this file for a color.

 * * 2. CLEAN CODE: Instead of having a 500-line Mixin file that is hard to read,
 * we keep the "If Lava, If Portal" logic here. You can add 100 colors
 * here and the Mixin stays short and clean.

 * * 3. NO FLICKER: Because we check the word "Lava" right before it's painted
 * on the screen, it bypasses the save-file lag that causes flickering.

 * * 4. SAFETY: You can edit this file safely. If you mess up a bracket here,
 * it's easy to fix. If you mess up a bracket in a Mixin, the whole game crashes.
 */

public class SubtitleColorMapper {
    public static int getCustomColor(String text, int categoryColor) {
        // Add new colors below! Just follow the pattern:
        // if (text.contains("Word")) return 0xHEXCODE;

        if (text.contains("Footsteps")) return 0x00AA00; // Vibrant Green
        //if (text.contains("Lava"))      return 0xFF4500; // Orange Red
        if (text.contains("Portal"))    return 0xBD00FF; // A bright "Glow" Purple
        if (text.contains("Explosion") || text.contains("TNT")) return 0xFFAA00; // Gold
        if (text.contains("Water") || text.contains("Splash") || text.contains("Bucket")) return 0x55FFFF; // Cyan
        if (text.contains("Chest")) return 0xD4A017; // Metallic Gold


        if (text.contains("Creeper"))   return 0x55FF55; // Acid Green
        if (text.contains("Warden"))    return 0x004B4B; // Deep Dark Teal
        if (text.contains("Arrow"))     return 0xAAAAAA; // Iron Gray
        if (text.contains("Fire"))      return 0xFFAA00; // Orange
        if (text.contains("Lightning")) return 0xFFFFFF; // Pure White


        if (text.contains("Enchanting")) {
            float transition = (float) (Math.sin(System.currentTimeMillis() / 100.0) + 1.0) / 2.0f;
            int red = (int) (0x9B * transition + 0x8E * (1.0f - transition));
            int green = (int) (0x59 * transition + 0x44 * (1.0f - transition));
            int blue = (int) (0xB6 * transition + 0xAD * (1.0f - transition));
            return (red << 16) | (green << 8) | blue;
        }



         if (text.contains("Lava")) {
            float transition = (float) (Math.sin(System.currentTimeMillis() / 150.0) + 1.0) / 2.0f;
            // Fades between Pure Orange and Slightly Darker Orange
            int red = (int) (0xFF * transition + 0xE6 * (1.0f - transition));
            int green = (int) (0x66 * transition + 0x5C * (1.0f - transition));
            int blue = (int) (0x00 * transition + 0x00 * (1.0f - transition));
            return (red << 16) | (green << 8) | blue;
        }
                    //OTHER: /**/

        //if (text.contains("Block breaking")) return 0xBDC3C7;
        //if (text.contains("Block broken"))   return 0xBDC3C7;
        //0xFF0000 , //0xFFD37F


        /* 1. The Pulsing Effect (Animated) Feels "alive" and heavy.
        if (text.contains("Lava")) {
            float transition = (float) (Math.sin(System.currentTimeMillis() / 500.0) + 1.0) / 2.0f;
            int red = (int) (0xFF * transition + 0xAA * (1.0f - transition));
            int green = (int) (0x45 * transition + 0x00 * (1.0f - transition));
            return (red << 16) | (green << 8) | 0x00;
        } */


        /* 2. The "Shadow" Trick (High Contrast) Clean, professional, and easy to read.
        if (text.contains("Lava")) return 0xD35400; // Deep Burnt Orange (Looks "thick" like lava) */


        /* 3. The "Flicker" Effect (Rapid Change) High energy, feels like a real fire.
        if (text.contains("Lava")) {
            long time = System.currentTimeMillis() / 100; // Changes every 0.1 seconds
            return (time % 2 == 0) ? 0xFF4500 : 0xFF8C00; // Swaps between Lava Red and Safety Orange
        } */


        return categoryColor; // If no words match, use the default Minecraft color
    }
}
