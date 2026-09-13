package com.leitzke.simpleradioaviation.client;

import com.leitzke.simpleradioaviation.network.DeskRunwayPayload;
import com.leitzke.simpleradioaviation.radar.RunwayNetwork;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class RunwayDeskConfigScreen extends Screen {
    private final Screen parent;
    private final BlockPos deskPosition;
    private final Set<String> existingCodes;
    private EditBox codeBox;
    private Button addButton;
    private boolean duplicateCode;

    public RunwayDeskConfigScreen(Screen parent, BlockPos deskPosition,
                                  Collection<String> existingCodes) {
        super(Component.translatable("interface.simpleradio_aviation.runway.add"));
        this.parent = parent;
        this.deskPosition = deskPosition.immutable();
        Set<String> normalizedCodes = new HashSet<>();
        for (String code : existingCodes) {
            String normalized = RunwayNetwork.normalizeCode(code);
            if (!normalized.isBlank()) normalizedCodes.add(normalized);
        }
        this.existingCodes = Set.copyOf(normalizedCodes);
    }

    @Override
    protected void init() {
        int x = width / 2 - 100;
        int y = height / 2 - 10;
        codeBox = addRenderableWidget(new EditBox(font, x, y, 200, 20,
                Component.translatable("interface.simpleradio_aviation.runway.code")));
        codeBox.setMaxLength(RunwayNetwork.MAX_CODE_LENGTH);
        addButton = addRenderableWidget(Button.builder(Component.translatable(
                "interface.simpleradio_aviation.runway.add"), button -> add())
                .bounds(x, y + 30, 96, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose())
                .bounds(x + 104, y + 30, 96, 20).build());
        codeBox.setResponder(value -> {
            String normalized = RunwayNetwork.normalizeCode(value);
            boolean valid = !normalized.isBlank();
            duplicateCode = valid && existingCodes.contains(normalized);
            codeBox.setTextColor(valid && !duplicateCode ? 0xFF77D8FF : 0xFFFF5555);
            addButton.active = valid && !duplicateCode;
        });
        addButton.active = false;
        setInitialFocus(codeBox);
        codeBox.setFocused(true);
    }

    private void add() {
        if (!addButton.active || !ClientPlayNetworking.canSend(DeskRunwayPayload.TYPE)) return;
        ClientPlayNetworking.send(new DeskRunwayPayload(deskPosition,
                DeskRunwayPayload.Action.ADD, codeBox.getValue()));
        onClose();
    }

    @Override public void onClose() { if (minecraft != null) minecraft.setScreen(parent); }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xAA000000);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 42, 0xFFFFFFFF);
        graphics.drawString(font, Component.translatable("interface.simpleradio_aviation.runway.code"),
                width / 2 - 100, height / 2 - 24, 0xFFBFC8C3, false);
        if (duplicateCode) {
            graphics.drawCenteredString(font, Component.translatable(
                    "interface.simpleradio_aviation.runway.duplicate"),
                    width / 2, height / 2 + 45, 0xFFFF5555);
        }
    }

    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}
    @Override public boolean isPauseScreen() { return false; }
}
