package com.leitzke.simpleradioaviation.client;

import com.leitzke.simpleradioaviation.network.RunwayConfigurePayload;
import com.leitzke.simpleradioaviation.radar.RunwayNetwork;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class RunwayThresholdScreen extends Screen {
    private static final int VALID_COLOR = 0xFF77D8FF;
    private static final int INVALID_COLOR = 0xFFFF5555;

    private final BlockPos position;
    private final String currentCode;
    private final String currentName;
    private final int currentWidth;
    private EditBox codeBox;
    private EditBox nameBox;
    private EditBox widthBox;
    private Button saveButton;

    public RunwayThresholdScreen(BlockPos position, String code, String name, int width) {
        super(Component.translatable("interface.simpleradio_aviation.runway.config"));
        this.position = position.immutable();
        this.currentCode = code;
        this.currentName = name;
        this.currentWidth = width;
    }

    @Override
    protected void init() {
        int x = width / 2 - 100;
        int y = height / 2 - 54;
        codeBox = addRenderableWidget(new EditBox(font, x, y, 200, 20,
                Component.translatable("interface.simpleradio_aviation.runway.code")));
        codeBox.setMaxLength(RunwayNetwork.MAX_CODE_LENGTH);
        codeBox.setValue(currentCode);

        nameBox = addRenderableWidget(new EditBox(font, x, y + 38, 200, 20,
                Component.translatable("interface.simpleradio_aviation.runway.name")));
        nameBox.setMaxLength(RunwayNetwork.MAX_NAME_LENGTH);
        nameBox.setValue(currentName);

        widthBox = addRenderableWidget(new EditBox(font, x, y + 76, 200, 20,
                Component.translatable("interface.simpleradio_aviation.runway.width")));
        widthBox.setMaxLength(2);
        widthBox.setFilter(value -> value.isEmpty() || value.chars().allMatch(Character::isDigit));
        widthBox.setValue(Integer.toString(currentWidth));

        saveButton = addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> save())
                .bounds(x, y + 106, 96, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose())
                .bounds(x + 104, y + 106, 96, 20).build());

        codeBox.setResponder(ignored -> updateValidation());
        nameBox.setResponder(ignored -> updateValidation());
        widthBox.setResponder(ignored -> updateValidation());
        updateValidation();
        setInitialFocus(codeBox);
        codeBox.setFocused(true);
    }

    private void updateValidation() {
        boolean codeValid = !RunwayNetwork.normalizeCode(codeBox.getValue()).isBlank();
        boolean nameValid = !RunwayNetwork.normalizeName(nameBox.getValue()).isBlank();
        int parsedWidth = parseWidth();
        boolean widthValid = parsedWidth >= RunwayNetwork.MIN_WIDTH
                && parsedWidth <= RunwayNetwork.MAX_WIDTH;
        codeBox.setTextColor(codeValid ? VALID_COLOR : INVALID_COLOR);
        nameBox.setTextColor(nameValid ? VALID_COLOR : INVALID_COLOR);
        widthBox.setTextColor(widthValid ? VALID_COLOR : INVALID_COLOR);
        if (saveButton != null) saveButton.active = codeValid && nameValid && widthValid;
    }

    private int parseWidth() {
        try {
            return Integer.parseInt(widthBox.getValue());
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private void save() {
        if (!saveButton.active || !ClientPlayNetworking.canSend(RunwayConfigurePayload.TYPE)) return;
        ClientPlayNetworking.send(new RunwayConfigurePayload(position, codeBox.getValue(),
                nameBox.getValue(), parseWidth()));
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xAA000000);
        super.render(graphics, mouseX, mouseY, partialTick);
        int x = width / 2 - 100;
        int y = height / 2 - 54;
        graphics.drawCenteredString(font, title, width / 2, y - 28, 0xFFFFFFFF);
        graphics.drawString(font, Component.translatable("interface.simpleradio_aviation.runway.code"),
                x, y - 11, 0xFFBFC8C3, false);
        graphics.drawString(font, Component.translatable("interface.simpleradio_aviation.runway.name"),
                x, y + 27, 0xFFBFC8C3, false);
        graphics.drawString(font, Component.translatable("interface.simpleradio_aviation.runway.width"),
                x, y + 65, 0xFFBFC8C3, false);
    }

    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}
    @Override public boolean isPauseScreen() { return false; }
}
