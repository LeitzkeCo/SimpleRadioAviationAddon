package com.leitzke.simpleradioaviation.client;

import com.leitzke.simpleradioaviation.block.AviationRadarBlock;
import com.leitzke.simpleradioaviation.block.DataLinkBlock;
import com.leitzke.simpleradioaviation.block.entity.AviationRadarBlockEntity;
import com.leitzke.simpleradioaviation.block.entity.DataLinkBlockEntity;
import com.leitzke.simpleradioaviation.block.RunwayThresholdBlock;
import com.leitzke.simpleradioaviation.block.entity.RunwayThresholdBlockEntity;
import com.leitzke.simpleradioaviation.item.RadarCableItem;
import com.leitzke.simpleradioaviation.item.PendriveLockItem;
import com.leitzke.simpleradioaviation.network.ConfigureDevicePayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;

public final class DeviceConfigInteraction {
    private static OpenRequest pending;
    private DeviceConfigInteraction() {}

    public static void initialize() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (!world.isClientSide || player.getItemInHand(hand).getItem() instanceof RadarCableItem
                    || player.getItemInHand(hand).getItem() instanceof PendriveLockItem
                    || player.getItemInHand(hand).getItem() instanceof BlockItem blockItem
                    && blockItem.getBlock() instanceof DataLinkBlock) {
                return InteractionResult.PASS;
            }
            BlockPos clicked = hit.getBlockPos();
            var state = world.getBlockState(clicked);
            if (state.getBlock() instanceof AviationRadarBlock) {
                BlockPos core = AviationRadarBlock.getCorePos(clicked, state);
                if (world.getBlockEntity(core) instanceof AviationRadarBlockEntity radar) {
                    pending = new OpenRequest(core, ConfigureDevicePayload.Target.RADAR,
                            radar.getRadarName(), DataLinkBlock.Role.TRANSMITTER,
                            Component.translatable("interface.simpleradio_aviation.config.radar"));
                    return InteractionResult.SUCCESS;
                }
            }
            if (state.getBlock() instanceof DataLinkBlock
                    && world.getBlockEntity(clicked) instanceof DataLinkBlockEntity device) {
                if (device.isLocked()) return InteractionResult.SUCCESS;
                pending = new OpenRequest(clicked, ConfigureDevicePayload.Target.DATA_LINK,
                        device.getChannel(), device.getRole(),
                        Component.translatable("interface.simpleradio_aviation.config.data_link"));
                return InteractionResult.SUCCESS;
            }
            if (state.getBlock() instanceof RunwayThresholdBlock
                    && world.getBlockEntity(clicked) instanceof RunwayThresholdBlockEntity threshold) {
                if (threshold.isLocked()) return InteractionResult.SUCCESS;
                pendingRunway = new RunwayRequest(clicked, threshold.getCode(),
                        threshold.getThresholdName(), threshold.getRunwayWidth());
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (pendingRunway != null && client.screen == null) {
                RunwayRequest request = pendingRunway;
                pendingRunway = null;
                client.setScreen(new RunwayThresholdScreen(request.position(), request.code(),
                        request.name(), request.width()));
                return;
            }
            if (pending == null || client.screen != null) return;
            OpenRequest request = pending;
            pending = null;
            client.setScreen(new DeviceConfigScreen(request.position(), request.target(),
                    request.value(), request.role(), request.title()));
        });
    }

    private record OpenRequest(BlockPos position, ConfigureDevicePayload.Target target,
                               String value, DataLinkBlock.Role role, Component title) {}
    private static RunwayRequest pendingRunway;
    private record RunwayRequest(BlockPos position, String code, String name, int width) {}
}
