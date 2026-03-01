package net.naw.subtitles.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.SubtitlesHud;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundListenerTransform;
import net.minecraft.client.sound.WeightedSoundSet;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.naw.subtitles.client.SubtitleColorData;
import net.naw.subtitles.client.SubtitleConfig;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.naw.subtitles.client.colors.SubtitleColorMapper;

import java.util.Iterator;
import java.util.List;

/**
 * SubtitlesMixin: The "Drawing Engine."
 * This file is responsible for taking all the sound data from the game and
 * physically painting it onto your screen. It handles the math for where
 * subtitles go, how big they are, and what colors they use.
 */
@Mixin(SubtitlesHud.class)
public abstract class SubtitlesMixin {

    // These @Shadows let us "borrow" variables that already exist inside Minecraft.
    @Shadow @Final private MinecraftClient client;
    @Shadow @Final private List<?> entries;

    /**
     * onSoundPlayed: This runs every single time a sound is triggered in the world.
     * We use it to "tag" the subtitle with its category color immediately.
     */
    @Inject(method = "onSoundPlayed", at = @At("RETURN"))
    private void onSoundPlayed(SoundInstance sound, WeightedSoundSet soundSet, float range, CallbackInfo ci) {
        if (this.entries.isEmpty()) return;
        Object lastEntry = this.entries.getLast();
        // Uses our 'Portal' (SubtitleColorData) to store the color in the entry's memory.
        ((SubtitleColorData) lastEntry).subtitles$setCategoryColor(sound);
    }

    /**
     * onRender: This is the heart of the visual mod.
     * It runs 60+ times per second (every frame) to draw the subtitles.
     */
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void onRender(DrawContext drawContext, CallbackInfo ci) {
        // Safety check: If no subtitles exist or the player isn't in a world, stop here.
        if (this.entries.isEmpty() || this.client.player == null) return;

        // ci.cancel() tells Minecraft: "Don't draw your boring subtitles, I'm drawing my own!"
        ci.cancel();
        SubtitleConfig config = SubtitleConfig.INSTANCE;

        // --- PREPARING THE CANVAS ---
        drawContext.createNewRootLayer();
        Matrix3x2fStack matrices = drawContext.getMatrices(); // 'Matrices' handle the scaling and moving.

        // Transform handles where the player is looking (important for the arrows!)
        SoundListenerTransform transform = this.client.getSoundManager().getListenerTransform();
        Vec3d listenerPos = transform.position();
        Vec3d listenerForward = transform.forward();
        Vec3d listenerRight = transform.right();

        matrices.pushMatrix();

        // --- MATH: WHERE ON THE SCREEN? ---
        int screenW = this.client.getWindow().getScaledWidth();
        int screenH = this.client.getWindow().getScaledHeight();

        // We multiply the screen size by your 0.0-1.0 config values to find the exact pixel.
        float finalX = (float)(screenW * config.relativeX);
        float boxHeightScaled = 20 * config.scale;
        float safeZoneHeight = screenH - boxHeightScaled - 4; // Keeps them from going off the bottom.
        float finalY = (float)(3 + (config.relativeY * safeZoneHeight));

        matrices.translate(finalX, finalY); // Move the "Pen" to the right spot.
        matrices.scale(config.scale, config.scale); // Make everything bigger or smaller.

        // --- VANILLA BACKGROUND CALCULATION (MODE 2) ---
        // This 'pre-loop' finds the widest subtitle so the big grey box fits them all perfectly.
        int maxTextWidth = 0;
        int activeSubtitlesCount = 0;
        for (Object entryObj : this.entries) {
            SubtitleEntryAccessor entry = (SubtitleEntryAccessor) entryObj;
            if (entry.invokeCanHearFrom(listenerPos)) {
                int textWidth = this.client.textRenderer.getWidth(entry.getText());
                if (textWidth > maxTextWidth) maxTextWidth = textWidth;
                activeSubtitlesCount++;
            }
        }
        int totalVanillaWidth = maxTextWidth + 3;

        // This draws the classic giant Minecraft box if 'Vanilla' is selected in your menu.
        if (config.subtitleBackgroundMode == 2 && activeSubtitlesCount > 0 && config.renderSubtitles) {
            int halfVW = totalVanillaWidth / 2;
            int bgX1, bgX2;
            // Logic to make sure the box stays attached to the left, right, or center.
            if (config.relativeX < 0.33) { bgX1 = -2; bgX2 = totalVanillaWidth + 2; }
            else if (config.relativeX > 0.66) { bgX1 = -totalVanillaWidth - 2; bgX2 = 2; }
            else { bgX1 = -halfVW - 2; bgX2 = halfVW + 2; }

            int totalHeight = activeSubtitlesCount * 10;
            if (config.isFlipped) {
                drawContext.fill(bgX1, -3, bgX2, totalHeight, 0x90000000); // 0x90 is the transparency level.
            } else {
                int bottomY = 21;
                int topY = bottomY - totalHeight - 1;
                drawContext.fill(bgX1, topY, bgX2, bottomY, 0x90000000);
            }
        }

        // --- THE MAIN SUBTITLE LOOP ---
        // Now we go through every active sound and draw its text.
        int subtitleIndex = 0;
        double displayTime = this.client.options.getNotificationDisplayTime().getValue();
        Iterator<?> iterator = this.entries.iterator();

        while (iterator.hasNext()) {
            SubtitleEntryAccessor entry = (SubtitleEntryAccessor) iterator.next();
            entry.invokeRemoveExpired(3000.0 * displayTime); // Delete if too old.

            if (!entry.invokeCanHearFrom(listenerPos)) continue;

            List<?> sounds = entry.getSounds();
            if (sounds == null || sounds.isEmpty()) {
                iterator.remove();
                continue;
            }

            // Get the timing (used for animations and fading out).
            SoundEntryAccessor newestSound = (SoundEntryAccessor) sounds.getLast();
            SoundEntryAccessor oldestSound = (SoundEntryAccessor) sounds.getFirst();
            long gameTimeNow = Util.getMeasuringTimeMs();
            long hitAge = gameTimeNow - newestSound.getTime();
            long slideAge = gameTimeNow - oldestSound.getTime();

            // Check if it's a specific type of sound for special animations.
            boolean isBlockSound = entry.getText().getString().contains("Block");


            // birthWeight = The "sliding in" animation. pulseWeight = The "jiggle" when a block breaks.
            float birthWeight = isBlockSound ? 1.0F : MathHelper.clamp(slideAge / 300.0F, 0.0F, 1.0F);
            float pulseWeight = isBlockSound ? MathHelper.clamp(0.82F + (hitAge / 150.0F), 0.82F, 1.0F) : 1.0F;

            // Transparency (Alpha) calculation so subtitles fade away slowly.
            float f = (float)(gameTimeNow - newestSound.getTime()) / (float)(3000.0 * displayTime);
            int alpha = (int)((1.0F - f) * 180.0F + 75.0F);
            alpha = (int)(alpha * birthWeight * pulseWeight);

            // If the user turned off subtitles in the menu, we just make them invisible.
            if (!config.renderSubtitles) alpha = 0;

            // Finding the closest sound source to point the arrows correctly.
            Vec3d nearestLocation = null;
            double minDistanceSq = Double.MAX_VALUE;
            for (Object soundObj : sounds) {
                SoundEntryAccessor sound = (SoundEntryAccessor) soundObj;
                Vec3d loc = sound.getLocation();
                double distSq = loc.squaredDistanceTo(listenerPos);
                if (distSq < minDistanceSq) {
                    minDistanceSq = distSq;
                    nearestLocation = loc;
                }
            }

            // --- 1. COLOR LOGIC ---
            SubtitleColorData data = (SubtitleColorData) entry;
            int baseColor = 0xFFFFFF; // Default is White.

            // Get the Category Color (Hostile = Red, etc.) from our Mixin memory.
            if (config.useCategoryColors) {
                baseColor = data.subtitles$getSavedColor();
            }

            // --- 2. WORD-BASED OVERRIDE ---
            // Reach through the portal to 'SubtitleColorMapper' to see if a specific word (like Lava)
            // should change the color.
            Text rawText = entry.getText();
            String rawString = rawText.getString();

            if (config.useCategoryColors) {
                baseColor = SubtitleColorMapper.getCustomColor(rawString, baseColor);
            }

            // --- 3. ICON & FORMATTING CLEANUP ---
            if (config.showIcons) {
                // This magic code ensures icons stay white even if the text is red or blue.
                rawString = rawString.replaceAll("([\\uE000-\\uF8FF\\u2000-\\u33FF])", "§r§f$1§r");
            }

            // Remove all standard Minecraft color codes so they don't break our custom colors.
            rawString = rawString.replaceAll("§[0-9a-fA-Fk-orK-OR]", "");

            if (config.showIcons) {
                rawString = rawString.replaceAll("([\\uE000-\\uF8FF\\u2000-\\u33FF])", "§r§f$1§r");
            }

            Text text = Text.literal(rawString);

            // --- 4. DIRECTIONAL ARROWS ---
            // Compares player rotation to sound position to put a < or > on the text.
            if (nearestLocation != null) {
                Vec3d soundVec = nearestLocation.subtract(listenerPos).normalize();
                double forward = listenerForward.dotProduct(soundVec);
                double side = listenerRight.dotProduct(soundVec);

                if (forward <= 0.5) {
                    if (side > 0.0) text = text.copy().append(" >");
                    else if (side < 0.0) text = Text.literal("< ").append(text);
                }
            }

            // --- 5. ULTIMATE ICON NUKE ---
            // If icons are disabled, this wipes every single possible icon character from the text.
            if (!config.showIcons) {
                String finalRaw = text.getString();
                finalRaw = finalRaw.replaceAll("§f[\\uE000-\\uF8FF]§6", "");
                finalRaw = finalRaw.replaceAll("[\\uE000-\\uF8FF\\u2100-\\u33FF\\uD83C-\\uDBFF\\uDC00-\\uDFFF]", "");
                finalRaw = finalRaw.trim().replaceAll(" +", " ");
                text = Text.literal(finalRaw);
            }

            // --- 6. FINAL COLOR COMBINATION ---
            // Combines the transparency (Alpha) with the RGB color into one single code.
            int color = (alpha << 24) | (baseColor & 0x00FFFFFF);

            // --- FINAL DRAWING ---
            int width = this.client.textRenderer.getWidth(text);
            int drawX;
            // Alignment logic: Should the text grow from the left, right, or center?
            if (config.relativeX < 0.33) drawX = 0;
            else if (config.relativeX > 0.66) drawX = -width;
            else drawX = -width / 2;

            int drawY;
            // Spacing logic: Vanilla mode is tighter (10px) than Modded mode (12px).
            int spacing = (config.subtitleBackgroundMode == 2) ? 10 : 12;
            drawY = config.isFlipped ? (subtitleIndex * spacing) : (12 - (subtitleIndex * spacing));

            // Adds that smooth "pop-up" sliding effect.
            float slideOffset = (1.0F - birthWeight) * 5.0F;
            drawY += (int) (config.isFlipped ? slideOffset : -slideOffset);

            // Draw the custom 'Modded' background (Mode 1).
            if (config.subtitleBackgroundMode == 1) {
                int bgColor = (int)(alpha * 0.6F) << 24;
                drawContext.fill(drawX - 2, drawY - 2, drawX + width + 2, drawY + 9, bgColor);
            }

            // Physically draw the text on the screen!
            if (config.showShadow) {
                drawContext.drawTextWithShadow(this.client.textRenderer, text, drawX, drawY, color);
            } else {
                drawContext.drawText(this.client.textRenderer, text, drawX, drawY, color, false);
            }

            subtitleIndex++;
        }
        matrices.popMatrix();
    }
}