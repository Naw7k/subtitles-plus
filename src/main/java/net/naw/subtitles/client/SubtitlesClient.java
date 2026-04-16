package net.naw.subtitles.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class SubtitlesClient implements ClientModInitializer {

    public static KeyMapping configKey;
    public static KeyMapping toggleKey;

    @Override
    public void onInitializeClient() {
        // --- INITIALIZATION ---
        SubtitleConfig.load();

        // --- KEYBINDING REGISTRATION ---
        configKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.subtitles.open_menu",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F4,
                KeyMapping.Category.MISC
        ));

        toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.subtitles.toggle",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                KeyMapping.Category.MISC
        ));

        // --- TICK EVENTS ---
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Open the configuration menu
            while (configKey.consumeClick()) {
                client.setScreen(new SubtitleConfigScreen());
            }

            // Toggle subtitle visibility
            while (toggleKey.consumeClick()) {
                if (!(Boolean)client.options.showSubtitles().get()) {
                    if (client.player != null)
                        client.player.sendSystemMessage(Component.literal("§eEnable Closed Captions in Music & Sounds first!"));
                    continue;
                }
                SubtitleConfig.INSTANCE.renderSubtitles = !SubtitleConfig.INSTANCE.renderSubtitles;
                SubtitleConfig.INSTANCE.save();

                // Feedback message for the player
                if (client.player != null) {
                    Component status = SubtitleConfig.INSTANCE.renderSubtitles ?
                            Component.literal("§aSubtitles Shown") : Component.literal("§cSubtitles Hidden");
                    client.player.sendSystemMessage(status);
                }
            }
        });
    }
}
