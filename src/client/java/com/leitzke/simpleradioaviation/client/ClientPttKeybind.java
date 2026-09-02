package com.leitzke.simpleradioaviation.client;

import com.leitzke.simpleradioaviation.SimpleRadioAviationAddon;
import com.leitzke.simpleradioaviation.network.FrequencyAdjustPayload;
import com.leitzke.simpleradioaviation.network.PttPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class ClientPttKeybind {

    private static final int REPEAT_DELAY_TICKS = 6;
    private static final int REPEAT_INTERVAL_TICKS = 2;

    private static final KeyMapping PTT_KEY = KeyBindingHelper.registerKeyBinding(
            new KeyMapping(
                    "key.simpleradio_aviation.ptt",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_R,
                    "key.categories.simpleradio_aviation"
            )
    );

    private static final KeyMapping HUD_KEY = KeyBindingHelper.registerKeyBinding(
            new KeyMapping(
                    "key.simpleradio_aviation.radio_hud",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_Z,
                    "key.categories.simpleradio_aviation"
            )
    );

    private static final KeyMapping FREQUENCY_UP_KEY = KeyBindingHelper.registerKeyBinding(
            new KeyMapping(
                    "key.simpleradio_aviation.frequency_up",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_UP,
                    "key.categories.simpleradio_aviation"
            )
    );

    private static final KeyMapping FREQUENCY_DOWN_KEY = KeyBindingHelper.registerKeyBinding(
            new KeyMapping(
                    "key.simpleradio_aviation.frequency_down",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_DOWN,
                    "key.categories.simpleradio_aviation"
            )
    );

    private static boolean wasDown;
    private static int upHeldTicks;
    private static int downHeldTicks;
    private static int rightHeldTicks;
    private static int leftHeldTicks;

    private ClientPttKeybind() {
    }

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (HUD_KEY.consumeClick() && AviationHud.hasEquippedHeadset(client)) {
                AviationHud.toggle();
            }
            if (AviationHud.isOpen()) {
                upHeldTicks = processFrequencyKey(FREQUENCY_UP_KEY, 1, upHeldTicks);
                downHeldTicks = processFrequencyKey(FREQUENCY_DOWN_KEY, -1, downHeldTicks);

                rightHeldTicks = processFrequencyKey(FREQUENCY_RIGHT_KEY, 1000, rightHeldTicks);
                leftHeldTicks = processFrequencyKey(FREQUENCY_LEFT_KEY, -1000, leftHeldTicks);
            } else {
                upHeldTicks = 0;
                downHeldTicks = 0;
                rightHeldTicks = 0;
                leftHeldTicks = 0;
            }

            boolean isDown = PTT_KEY.isDown();

            if (isDown && !wasDown) {
                ClientPlayNetworking.send(new PttPayload(true));
                SimpleRadioAviationAddon.LOGGER.info("PTT pressed.");
            }

            if (!isDown && wasDown) {
                ClientPlayNetworking.send(new PttPayload(false));
                SimpleRadioAviationAddon.LOGGER.info("PTT released.");
            }

            wasDown = isDown;
        });
    }
    private static final KeyMapping FREQUENCY_RIGHT_KEY = KeyBindingHelper.registerKeyBinding(
            new KeyMapping(
                    "key.simpleradio_aviation.frequency_right",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_RIGHT,
                    "key.categories.simpleradio_aviation"
            )
    );

    private static final KeyMapping FREQUENCY_LEFT_KEY = KeyBindingHelper.registerKeyBinding(
            new KeyMapping(
                    "key.simpleradio_aviation.frequency_left",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_LEFT,
                    "key.categories.simpleradio_aviation"
            )
    );

    private static int processFrequencyKey(
            KeyMapping key,
            int amount,
            int heldTicks
    ) {
        if (!key.isDown()) {
            return 0;
        }

        if (
                heldTicks == 0
                        || (
                        heldTicks >= REPEAT_DELAY_TICKS
                                && (heldTicks - REPEAT_DELAY_TICKS) % REPEAT_INTERVAL_TICKS == 0
                )
        ) {
            ClientPlayNetworking.send(new FrequencyAdjustPayload(amount));
        }

        return heldTicks + 1;
    }
}