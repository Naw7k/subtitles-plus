package net.naw.subtitles.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class SubtitlesClient implements ClientModInitializer {

    public static KeyBinding configKey;
    public static KeyBinding toggleKey;

    @Override
    public void onInitializeClient() {
        // --- INITIALIZATION ---
        SubtitleConfig.load();

        // --- KEYBINDING REGISTRATION ---
        configKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.subtitles.open_menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F4,
                KeyBinding.Category.MISC
        ));

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.subtitles.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                KeyBinding.Category.MISC
        ));

        // --- TICK EVENTS ---
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Open the configuration menu
            while (configKey.wasPressed()) {
                client.setScreen(new SubtitleConfigScreen());
            }

            // Toggle subtitle visibility
            while (toggleKey.wasPressed()) {
                SubtitleConfig.INSTANCE.renderSubtitles = !SubtitleConfig.INSTANCE.renderSubtitles;
                SubtitleConfig.INSTANCE.save();

                // Feedback message for the player
                if (client.player != null) {
                    Text status = SubtitleConfig.INSTANCE.renderSubtitles ?
                            Text.literal("§aSubtitles Shown") : Text.literal("§cSubtitles Hidden");
                    client.player.sendMessage(status, true);
                }
            }
        });
    }
}