package com.leitzke.simpleradioaviation.client;

import com.codinglitch.simpleradio.central.Frequency;
import com.codinglitch.simpleradio.core.SimpleRadioComponents;
import com.leitzke.simpleradioaviation.item.AviationHeadsetItem;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public final class AviationHud {

    private static boolean open;

    private AviationHud() {
    }

    public static void initialize() {
        HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
            if (!open) {
                return;
            }

            Minecraft client = Minecraft.getInstance();

            if (!hasEquippedHeadset(client)) {
                open = false;
                return;
            }

            ItemStack headset = client.player.getItemBySlot(EquipmentSlot.HEAD);

            String frequencyText = "No headset equipped";

            if (headset.getItem() instanceof AviationHeadsetItem) {
                String frequency = headset.get(SimpleRadioComponents.FREQUENCY);
                Frequency.Modulation modulation = headset.get(SimpleRadioComponents.MODULATION);

                if (frequency != null && modulation != null) {
                    frequencyText = frequency + modulation.shorthand;
                } else {
                    frequencyText = "No signal";
                }
            }

            int x = 10;
            int y = 10;

            //graphics.fill(x - 4, y - 4, x + 180, y + 42, 0xA0000000);

            graphics.drawString(client.font, "Aviation radio", x, y, 0x39FF14);
            graphics.drawString(client.font, frequencyText, x, y + 13, 0x39FF14);
        });
    }

    public static void toggle() {
        open = !open;
    }

    public static boolean isOpen() {
        return open;
    }
    public static boolean hasEquippedHeadset(Minecraft client) {
        return client.player != null
                && client.player.getItemBySlot(EquipmentSlot.HEAD).getItem()
                instanceof AviationHeadsetItem;
    }
}