package com.leitzke.simpleradioaviation.client;

import com.leitzke.simpleradioaviation.client.render.AviationHeadsetRenderer;
import com.leitzke.simpleradioaviation.client.render.AviationRadarRenderer;
import com.leitzke.simpleradioaviation.client.render.RadarDeskCableRenderer;
import com.leitzke.simpleradioaviation.client.render.RadarCablePulleyRenderer;
import com.leitzke.simpleradioaviation.registry.ModBlocks;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.RenderType;

public class SimpleRadioAviationAddonClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.RADAR_DESK, RenderType.cutout());
        AviationRadarRenderer.initialize();
        RadarDeskCableRenderer.initialize();
        RadarCablePulleyRenderer.initialize();

        ClientPttKeybind.initialize();
        AviationHud.initialize();
        RadarDeskInteraction.initialize();
        DeviceConfigInteraction.initialize();
        AviationHeadsetRenderer.initialize();
    }
}
