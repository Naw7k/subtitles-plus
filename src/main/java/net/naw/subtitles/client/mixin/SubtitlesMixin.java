package net.naw.subtitles.client.mixin;

import com.mojang.blaze3d.audio.ListenerTransform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.SubtitleOverlay;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.phys.Vec3;
import net.naw.subtitles.client.SubtitleColorData;
import net.naw.subtitles.client.SubtitleConfig;
import net.naw.subtitles.client.colors.SubtitleColorMapper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;
import java.util.List;

/**
 * SubtitlesMixin: The "Drawing Engine."
 * This file is responsible for taking all the sound data from the game and
 * physically painting it onto your screen. It handles the math for where
 * subtitles go, how big they are, and what colors they use.
 */
@Mixin(SubtitleOverlay.class)
public abstract class SubtitlesMixin {

    // These @Shadows let us "borrow" variables that already exist inside Minecraft.
    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private List<?> subtitles;

    /**
     * onPlaySound: This runs every single time a sound is triggered in the world.
     * We use it to "tag" the subtitle with its category color immediately.
     */
    @Inject(method = "onPlaySound", at = @At("RETURN"))
    private void onPlaySound(SoundInstance sound, WeighedSoundEvents soundEvent, float range, CallbackInfo ci) {
        if (this.subtitles.isEmpty()) return;
        Object lastEntry = this.subtitles.getLast();
        // Uses our 'Portal' (SubtitleColorData) to store the color in the entry's memory.
        ((SubtitleColorData) lastEntry).subtitles$setCategoryColor(sound);
    }

    /**
     * onRender: This is the heart of the visual mod.
     * It runs 60+ times per second (every frame) to draw the subtitles.
     */
    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    @SuppressWarnings("all")
    public void onRender(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        // Safety check: If no subtitles exist or the player isn't in a world, stop here.
        if (this.subtitles.isEmpty() || this.minecraft.player == null) return;

        // ci.cancel() tells Minecraft: "Don't draw your boring subtitles, I'm drawing my own!"
        ci.cancel();
        SubtitleConfig config = SubtitleConfig.INSTANCE;

        if (!config.renderSubtitles) return;
        if (!(Boolean)this.minecraft.options.showSubtitles().get()) return;

        // Transform handles where the player is looking (important for the arrows!)
        ListenerTransform transform = this.minecraft.getSoundManager().getListenerTransform();
        Vec3 listenerPos = transform.position();
        Vec3 listenerForward = transform.forward();
        Vec3 listenerRight = transform.right();

        // --- SCREEN DIMENSIONS ---
        int screenW = this.minecraft.getWindow().getGuiScaledWidth();
        int screenH = this.minecraft.getWindow().getGuiScaledHeight();

        double displayTime = this.minecraft.options.notificationDisplayTime().get();

        // --- COLLECT ACTIVE SUBTITLES AND FIND MAX WIDTH ---
        // Pre-loop runs before the main drawing loop so we know the widest subtitle ahead of time.
        // This lets us set a consistent background width and position anchor before drawing anything.
        int activeCount = 0;
        int maxWidth = 0;

        Iterator<?> iterator = this.subtitles.iterator();
        while (iterator.hasNext()) {
            SubtitleEntryAccessor entry = (SubtitleEntryAccessor) iterator.next();
            entry.invokeRemoveExpired(3000.0 * displayTime);
            if (!entry.invokeCanHearFrom(listenerPos)) continue;

            // Skip blacklisted subtitles
            String subtitleText = entry.getText().getString();
            if (config.blacklist.stream().anyMatch(subtitleText::contains)) continue;

            List<?> sounds = entry.getSounds();
            if (sounds == null || sounds.isEmpty()) {
                iterator.remove();
                continue;
            }

            maxWidth = Math.max(maxWidth, this.minecraft.font.width(entry.getText()));
            activeCount++;
        }

        // Add arrow padding — more space when icons are on since icons take extra width
        maxWidth += config.showIcons ? 20 : 5;

        if (activeCount == 0) return;

        // --- POSITION CALCULATION ---
        // finalX: centers the subtitle block horizontally based on relativeX (0.0 to 1.0)
        // finalY: positions vertically based on relativeY, clamped so text never goes off screen
        // fixedWidth: fixed anchor width so subtitles don't shift when new ones appear (mode 0 and 1)
        // positionWidth: vanilla (mode 2) uses dynamic maxWidth
        int fixedWidth = 100 + (config.showIcons ? 20 : 5);
        int positionWidth = config.subtitleBackgroundMode == 2 ? maxWidth : fixedWidth;
        float scaledHalfWidth = (positionWidth * config.scale) / 2.0f;
        float finalX = (float)(config.relativeX * (screenW - positionWidth * config.scale)) + scaledHalfWidth;
        // Top clamp (5 * scale): minimum distance from top edge — calibrated for all scales
        // Bottom clamp: modded (mode 1) uses larger value to account for 2 entries height, vanilla/none use 5
        // Flip offset (5 * scale): shifts the visual block up or down so both flip directions appear symmetric
        float finalY = Mth.clamp((float)(config.relativeY * screenH), 2 * config.scale, config.subtitleBackgroundMode == 1 ? screenH - 14 * config.scale : screenH - 5 * config.scale
        ) + (config.isFlipped ? -5 * config.scale : 5 * config.scale);

        // --- THE MAIN SUBTITLE LOOP ---
        // Now we go through every active sound and draw its text.
        int currentRow = 0;
        for (Object entryObj : this.subtitles) {
            if (config.subtitleLimit > 0 && currentRow >= config.subtitleLimit) break;
            SubtitleEntryAccessor entry = (SubtitleEntryAccessor) entryObj;
            if (!entry.invokeCanHearFrom(listenerPos)) continue;

            String subtitleText = entry.getText().getString();
            if (config.blacklist.stream().anyMatch(subtitleText::contains)) continue;

            List<?> sounds = entry.getSounds();
            if (sounds == null || sounds.isEmpty()) continue;

            // Finding the closest sound source to point the arrows correctly.
            SoundEntryAccessor closestSound = null;
            double minDist = Double.MAX_VALUE;
            for (Object soundObj : sounds) {
                SoundEntryAccessor sound = (SoundEntryAccessor) soundObj;
                double dist = sound.getLocation().distanceToSqr(listenerPos);
                if (dist < minDist) {
                    minDist = dist;
                    closestSound = sound;
                }
            }

            if (closestSound == null) continue;

            long gameTimeNow = Util.getMillis();

            // --- SLIDE-IN ANIMATION ---
            // birthWeight = The "sliding in" animation. pulseWeight = The "jiggle" when a block breaks.
            SoundEntryAccessor oldestSound = (SoundEntryAccessor) sounds.getFirst();
            SoundEntryAccessor newestSound = (SoundEntryAccessor) sounds.getLast();
            long hitAge = gameTimeNow - newestSound.getTime();
            long slideAge = gameTimeNow - oldestSound.getTime();
            boolean isBlockSound = entry.getText().getString().contains("Block");
            float birthWeight = isBlockSound ? 1.0F : Mth.clamp(slideAge / 300.0F, 0.0F, 1.0F);
            float pulseWeight = isBlockSound ? Mth.clamp(0.82F + (hitAge / 150.0F), 0.82F, 1.0F) : 1.0F;

            // Transparency (Alpha) calculation — fades from 255 to 75 over display time
            int brightness = (int) Mth.clampedLerp(
                    (float)(gameTimeNow - closestSound.getTime()) / (float)(3000.0 * displayTime),
                    255.0F, 75.0F
            );
            brightness = (int)(brightness * birthWeight * pulseWeight);

            // --- 1. COLOR LOGIC ---
            int baseColor = 0xFFFFFF; // Default is White.

            // Get the Category Color (Hostile = Red, etc.) from our Mixin memory.
            SubtitleColorData data = (SubtitleColorData) entry;
            if (config.useCategoryColors) {
                baseColor = data.subtitles$getSavedColor();
            }

            // --- 2. WORD-BASED OVERRIDE ---
            // Reach through the portal to 'SubtitleColorMapper' to see if a specific word (like Lava)
            // should change the color.
            Component rawText = entry.getText();
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

            // --- 4. BUILD FINAL TEXT COMPONENT ---
            Component text = Component.literal(rawString);

            // --- 5. ULTIMATE ICON NUKE ---
            // If icons are disabled, this wipes every single possible icon character from the text.
            if (!config.showIcons) {
                String finalRaw = text.getString();
                finalRaw = finalRaw.replaceAll("§f[\\uE000-\\uF8FF]§6", "");
                finalRaw = finalRaw.replaceAll("[\\uE000-\\uF8FF\\u2100-\\u33FF\\uD83C-\\uDBFF\\uDC00-\\uDFFF]", "");
                finalRaw = finalRaw.trim().replaceAll(" +", " ");
                text = Component.literal(finalRaw);
            }

            // --- 6. FINAL COLOR COMBINATION ---
            // Use category color if enabled, otherwise use vanilla brightness
            int finalTextColor;
            if (config.useCategoryColors) {
                finalTextColor = (brightness << 24) | (baseColor & 0x00FFFFFF);
            } else {
                // Vanilla-style: all channels equal brightness for a white-to-gray fade
                finalTextColor = 0xFF000000 | (brightness << 16) | (brightness << 8) | brightness;
            }

            int halfWidth = maxWidth / 2;
            int halfHeight = 4;

            // Slide offset for smooth pop-up animation
            float slideOffset = (1.0F - birthWeight) * 5.0F;

            // Each subtitle gets its own matrix — exactly like vanilla
            graphics.pose().pushMatrix();
            graphics.pose().translate(
                    finalX,
                    config.isFlipped ?
                            finalY + (currentRow * (config.subtitleBackgroundMode == 1 ? 12 : 10)) * config.scale + slideOffset * config.scale :
                            finalY - (currentRow * (config.subtitleBackgroundMode == 1 ? 12 : 10)) * config.scale - slideOffset * config.scale
            );
            graphics.pose().scale(config.scale, config.scale);

            // --- DIRECTIONAL ARROWS ---
            // For mode 0 and 1: arrows appended directly to text (tight, natural spacing)
            // For mode 2 (vanilla): arrows drawn separately at fixed halfWidth positions
            Vec3 soundVec = closestSound.getLocation().subtract(listenerPos).normalize();
            double forward = listenerForward.dot(soundVec);
            double side = listenerRight.dot(soundVec);
            boolean inView = forward > 0.5;

            if (config.subtitleBackgroundMode != 2) {
                // Mode 0 and 1: append arrows directly to text string
                if (forward <= 0.5) {
                    if (side > 0.0) text = text.copy().append(" >");
                    else if (side < 0.0) text = Component.literal("< ").append(text);
                }
            }

            // Recalculate width after arrows may have been added
            int finalTextWidth = this.minecraft.font.width(text);

            // halfWidth for modded/none uses fixedWidth so short texts still hug the wall correctly
            int effectiveHalfWidth = config.subtitleBackgroundMode == 2 ? halfWidth : fixedWidth / 2;

            // Physically draw the text on the screen
            int textDrawY = config.subtitleBackgroundMode == 1 ? -halfHeight + 4 : -halfHeight;

            int drawX;
            if (config.subtitleAlignment) {
                // Auto align mode
                if (config.relativeX < 0.33) drawX = config.subtitleBackgroundMode == 2 ? -effectiveHalfWidth + 10 : -effectiveHalfWidth + 2;
                else if (config.relativeX > 0.66) drawX = config.subtitleBackgroundMode == 2 ? effectiveHalfWidth - finalTextWidth - 10 : effectiveHalfWidth - finalTextWidth - 2;
                else drawX = -finalTextWidth / 2;
            } else {
                // Center mode: always centered
                drawX = -finalTextWidth / 2;
            }

            // --- MODDED BACKGROUND (MODE 1) — tight per-line background based on actual text width ---
            if (config.subtitleBackgroundMode == 1) {
                int bgColor = (int)(brightness * Mth.clamp(config.boxOpacity, 0.3f, 1.0f)) << 24;
                graphics.fill(drawX - 2, -2, drawX + finalTextWidth + 2, 9, bgColor);
            }

            // --- VANILLA BACKGROUND (MODE 2) ---
            if (config.subtitleBackgroundMode == 2) {
                graphics.fill(-halfWidth - 1, -halfHeight - 1, halfWidth + 1, halfHeight + 1,
                        this.minecraft.options.getBackgroundColor(Mth.clamp(config.boxOpacity, 0.3f, 1.0f)));


                // Vanilla: arrows drawn separately at fixed positions
                if (!inView) {
                    if (side > 0.0) {
                        int arrowX = config.subtitleAlignment ? drawX + finalTextWidth + 3 : halfWidth - this.minecraft.font.width(">");
                        graphics.text(this.minecraft.font, ">", arrowX, -halfHeight, finalTextColor, config.showShadow);
                    } else if (side < 0.0) {
                        int arrowX = config.subtitleAlignment ? drawX - this.minecraft.font.width("<") - 3 : -halfWidth;
                        graphics.text(this.minecraft.font, "<", arrowX, -halfHeight, finalTextColor, config.showShadow);
                    }
                }
            }

            graphics.text(this.minecraft.font, text, drawX, textDrawY, finalTextColor, config.showShadow);

            graphics.pose().popMatrix();
            currentRow++;
        }
    }
}
