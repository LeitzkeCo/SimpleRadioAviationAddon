package com.leitzke.simpleradioaviation.client;

import com.leitzke.simpleradioaviation.SimpleRadioAviationAddon;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class RadarDeskScreen extends Screen {

    private static final ResourceLocation RADAR_DESK_TEXTURE =
            SimpleRadioAviationAddon.id("textures/gui/radar_desk.png");

    public RadarDeskScreen() {
        super(Component.literal("Radar Desk"));
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        int panelWidth = Math.min(600, this.width - 40);
        int panelHeight = panelWidth * 420 / 600;

        int x = (this.width - panelWidth) / 2;
        int y = (this.height - panelHeight) / 2;

        graphics.blit(
                RADAR_DESK_TEXTURE,
                x,
                y,
                0,
                0,
                panelWidth,
                panelHeight,
                600,
                420
        );
    }
}