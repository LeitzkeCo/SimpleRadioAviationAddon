package com.leitzke.simpleradioaviation.client;

import net.fabricmc.api.ClientModInitializer;
import com.leitzke.simpleradioaviation.client.render.AviationHeadsetRenderer;

public class SimpleRadioAviationAddonClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		ClientPttKeybind.initialize();
		AviationHud.initialize();
		RadarDeskInteraction.initialize();
		AviationHeadsetRenderer.initialize();
	}
}