package com.leitzke.simpleradioaviation.client;

import com.leitzke.simpleradioaviation.block.AviationRadarBlock;
import com.leitzke.simpleradioaviation.block.DataLinkBlock;
import com.leitzke.simpleradioaviation.network.ConfigureDevicePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class DeviceConfigScreen extends Screen {
    private final BlockPos position;
    private final ConfigureDevicePayload.Target target;
    private final String currentValue;
    private DataLinkBlock.Role dataLinkRole;
    private EditBox valueBox;
    private Button roleButton;

    public DeviceConfigScreen(BlockPos position, ConfigureDevicePayload.Target target,
                              String currentValue, DataLinkBlock.Role dataLinkRole,
                              Component title) {
        super(title);
        this.position = position.immutable();
        this.target = target;
        this.currentValue = currentValue;
        this.dataLinkRole = dataLinkRole;
    }

    @Override
    protected void init() {
        int x = width / 2 - 100;
        int y = height / 2 - 10;
        valueBox = addRenderableWidget(new EditBox(font, x, y, 200, 20,
                Component.translatable(target == ConfigureDevicePayload.Target.RADAR
                        ? "interface.simpleradio_aviation.config.radar_name"
                        : "interface.simpleradio_aviation.config.channel")));
        valueBox.setMaxLength(target == ConfigureDevicePayload.Target.RADAR ? 32 : 16);
        valueBox.setValue(currentValue);
        int buttonY = y + 30;
        if (target == ConfigureDevicePayload.Target.DATA_LINK) {
            roleButton = addRenderableWidget(Button.builder(roleLabel(), button -> toggleRole())
                    .bounds(x, buttonY, 200, 20).build());
            buttonY += 28;
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> save())
                .bounds(x, buttonY, 96, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose())
                .bounds(x + 104, buttonY, 96, 20).build());

        // Assign focus after every widget exists. Otherwise the first button can
        // steal the initial focus during screen initialization.
        valueBox.setCanLoseFocus(false);
        setInitialFocus(valueBox);
        valueBox.setFocused(true);
    }

    private void save() {
        if (ClientPlayNetworking.canSend(ConfigureDevicePayload.TYPE)) {
            ClientPlayNetworking.send(new ConfigureDevicePayload(position, target, valueBox.getValue(),
                    dataLinkRole == DataLinkBlock.Role.RECEIVER));
        }
        onClose();
    }

    private void toggleRole() {
        dataLinkRole = dataLinkRole == DataLinkBlock.Role.TRANSMITTER
                ? DataLinkBlock.Role.RECEIVER : DataLinkBlock.Role.TRANSMITTER;
        if (roleButton != null) roleButton.setMessage(roleLabel());
        valueBox.setFocused(true);
    }

    private Component roleLabel() {
        return Component.translatable(dataLinkRole == DataLinkBlock.Role.RECEIVER
                ? "interface.simpleradio_aviation.config.role_receiver"
                : "interface.simpleradio_aviation.config.role_transmitter");
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xAA000000);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 52, 0xFFFFFFFF);
        if (target == ConfigureDevicePayload.Target.RADAR && minecraft != null
                && minecraft.level != null) {
            var state = minecraft.level.getBlockState(position);
            boolean powered = state.getBlock() instanceof AviationRadarBlock
                    && state.getValue(AviationRadarBlock.POWERED);
            graphics.drawCenteredString(font, Component.translatable(powered
                            ? "interface.simpleradio_aviation.config.radar_energized"
                            : "interface.simpleradio_aviation.config.radar_off"),
                    width / 2, height / 2 - 38,
                    powered ? 0xFF7CFF9B : 0xFFFF5555);
        }
        graphics.drawString(font, Component.translatable(target == ConfigureDevicePayload.Target.RADAR
                        ? "interface.simpleradio_aviation.config.radar_name"
                        : "interface.simpleradio_aviation.config.channel"),
                width / 2 - 100, height / 2 - 24, 0xFFBFC8C3, false);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Intentionally empty: menu blur also softens text drawn before widgets.
    }

    @Override public boolean isPauseScreen() { return false; }
}
