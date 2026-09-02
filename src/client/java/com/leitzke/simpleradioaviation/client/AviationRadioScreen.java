package com.leitzke.simpleradioaviation.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class AviationRadioScreen extends Screen {

    public AviationRadioScreen() {
        super(Component.literal("Aviation Radio"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(
                this.font,
                "AVIATION RADIO",
                this.width / 2,
                this.height / 2 - 20,
                0xFFFFFF
        );

        graphics.drawCenteredString(
                this.font,
                "Frequência: carregando...",
                this.width / 2,
                this.height / 2,
                0x55FFFF
        );

        graphics.drawCenteredString(
                this.font,
                "Use + e - para ajustar",
                this.width / 2,
                this.height / 2 + 20,
                0xAAAAAA
        );

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}