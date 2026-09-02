package com.leitzke.simpleradioaviation;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.ResourceLocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.leitzke.simpleradioaviation.registry.ModItems;

import com.leitzke.simpleradioaviation.network.PttPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import com.leitzke.simpleradioaviation.item.AviationHeadsetItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import com.codinglitch.simpleradio.ServerSimpleRadioApi;
import com.codinglitch.simpleradio.SimpleRadioApi;
import com.codinglitch.simpleradio.central.Frequency;
import com.leitzke.simpleradioaviation.network.FrequencyAdjustPayload;

import static com.codinglitch.simpleradio.core.SimpleRadioComponents.FREQUENCY;
import static com.codinglitch.simpleradio.core.SimpleRadioComponents.MODULATION;
import static com.codinglitch.simpleradio.core.SimpleRadioComponents.REFERENCE;
import static com.codinglitch.simpleradio.core.SimpleRadioComponents.USING;

import com.leitzke.simpleradioaviation.registry.ModBlocks;

public class SimpleRadioAviationAddon implements ModInitializer {
	public static final String MOD_ID = "simpleradio_aviation";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.playC2S().register(PttPayload.TYPE, PttPayload.CODEC);

		PayloadTypeRegistry.playC2S().register(
				FrequencyAdjustPayload.TYPE,
				FrequencyAdjustPayload.CODEC
		);

		ServerPlayNetworking.registerGlobalReceiver(PttPayload.TYPE, (payload, context) -> {
			ItemStack headsetStack = context.player().getItemBySlot(EquipmentSlot.HEAD);

			if (!(headsetStack.getItem() instanceof AviationHeadsetItem headsetItem)) {
				return;
			}

			if (payload.pressed()) {
				headsetItem.entityTick(headsetStack, context.player());
				headsetItem.begin(headsetStack, context.player());

				LOGGER.info("{} iniciou a transmissão pelo headset.", context.player().getName().getString());
			} else {
				headsetItem.end(headsetStack, context.player());

				LOGGER.info("{} encerrou a transmissão pelo headset.", context.player().getName().getString());
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(FrequencyAdjustPayload.TYPE, (payload, context) -> {
			if (
					payload.amount() != 1
							&& payload.amount() != -1
							&& payload.amount() != 1000
							&& payload.amount() != -1000
			) {
				return;
			}

			ItemStack headsetStack = context.player().getItemBySlot(EquipmentSlot.HEAD);

			if (!(headsetStack.getItem() instanceof AviationHeadsetItem headsetItem)) {
				return;
			}

			if (headsetStack.has(USING) && headsetStack.get(USING)) {
				return;
			}

			String currentFrequency = headsetStack.get(FREQUENCY);
			Frequency.Modulation modulation = headsetStack.get(MODULATION);

			if (currentFrequency == null || modulation == null) {
				return;
			}

			String newFrequency = SimpleRadioApi.getInstance()
					.frequencies()
					.incrementFrequency(currentFrequency, payload.amount());

			if (!SimpleRadioApi.getInstance().frequencies().check(newFrequency)) {
				return;
			}

			if (headsetStack.has(REFERENCE)) {
				ServerSimpleRadioApi.getInstance().removeRouter(headsetStack.get(REFERENCE));
				headsetStack.remove(REFERENCE);
			}

			headsetItem.setFrequency(headsetStack, newFrequency, modulation);
			headsetItem.entityTick(headsetStack, context.player());

			LOGGER.info(
					"{} tuned headset to {}{}.",
					context.player().getName().getString(),
					newFrequency,
					modulation.shorthand
			);
		});
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		ModItems.initialize();
		ModBlocks.initialize();
		LOGGER.info("Simpleradio Aviation Addon Initialized!");
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
}
