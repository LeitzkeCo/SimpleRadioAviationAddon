package com.leitzke.simpleradioaviation;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.ResourceLocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.leitzke.simpleradioaviation.registry.ModItems;

import com.leitzke.simpleradioaviation.network.PttPayload;
import com.leitzke.simpleradioaviation.network.RadarContactsPayload;
import com.leitzke.simpleradioaviation.network.RadarContactsRequestPayload;
import com.leitzke.simpleradioaviation.network.ConfigureDevicePayload;
import com.leitzke.simpleradioaviation.network.RadarSwitchPayload;
import com.leitzke.simpleradioaviation.network.RunwayConfigurePayload;
import com.leitzke.simpleradioaviation.network.DeskRunwayPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import com.leitzke.simpleradioaviation.item.AviationHeadsetItem;
import com.leitzke.simpleradioaviation.item.RadarCableItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import com.codinglitch.simpleradio.SimpleRadioApi;
import com.codinglitch.simpleradio.central.Frequency;
import com.codinglitch.simpleradio.core.Frequencies;
import com.leitzke.simpleradioaviation.network.FrequencyAdjustPayload;

import static com.codinglitch.simpleradio.core.SimpleRadioComponents.FREQUENCY;
import static com.codinglitch.simpleradio.core.SimpleRadioComponents.MODULATION;
import static com.codinglitch.simpleradio.core.SimpleRadioComponents.USING;

import com.leitzke.simpleradioaviation.registry.ModBlocks;
import com.leitzke.simpleradioaviation.registry.ModBlockEntities;
import com.leitzke.simpleradioaviation.block.AviationRadarBlock;
import com.leitzke.simpleradioaviation.block.RadarDeskBlock;
import com.leitzke.simpleradioaviation.block.entity.AviationRadarBlockEntity;
import com.leitzke.simpleradioaviation.block.entity.RadarDeskBlockEntity;
import com.leitzke.simpleradioaviation.block.entity.DataLinkBlockEntity;
import com.leitzke.simpleradioaviation.block.entity.RunwayThresholdBlockEntity;
import com.leitzke.simpleradioaviation.radar.DataLinkNetwork;
import com.leitzke.simpleradioaviation.radar.RadarCableNetwork;
import com.leitzke.simpleradioaviation.radar.RunwayNetwork;
import com.leitzke.simpleradioaviation.radar.RunwayInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SimpleRadioAviationAddon implements ModInitializer {
	public static final String MOD_ID = "simpleradio_aviation";
	private static final long RADAR_VIEW_SESSION_TIMEOUT_TICKS = 40L;
	private static final Map<UUID, RadarViewSession> RADAR_VIEW_SESSIONS = new HashMap<>();

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.playC2S().register(PttPayload.TYPE, PttPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(
				RadarContactsRequestPayload.TYPE,
				RadarContactsRequestPayload.CODEC
		);
		PayloadTypeRegistry.playS2C().register(
				RadarContactsPayload.TYPE,
				RadarContactsPayload.CODEC
		);

		PayloadTypeRegistry.playC2S().register(
				FrequencyAdjustPayload.TYPE,
				FrequencyAdjustPayload.CODEC
		);
		PayloadTypeRegistry.playC2S().register(ConfigureDevicePayload.TYPE, ConfigureDevicePayload.CODEC);
		PayloadTypeRegistry.playC2S().register(RadarSwitchPayload.TYPE, RadarSwitchPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(RunwayConfigurePayload.TYPE, RunwayConfigurePayload.CODEC);
		PayloadTypeRegistry.playC2S().register(DeskRunwayPayload.TYPE, DeskRunwayPayload.CODEC);

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

			Frequencies frequencies = SimpleRadioApi.getInstance().frequencies();
			String newFrequency = frequencies.incrementFrequency(currentFrequency, payload.amount());

			if (!frequencies.check(newFrequency) || newFrequency.equals(currentFrequency)) {
				return;
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
		ServerPlayNetworking.registerGlobalReceiver(
				RadarContactsRequestPayload.TYPE,
				(payload, context) -> context.server().execute(
						() -> sendRadarContacts(context.player(), payload.deskPosition())
				)
		);
		ServerPlayNetworking.registerGlobalReceiver(ConfigureDevicePayload.TYPE, (payload, context) ->
				context.server().execute(() -> configureDevice(context.player(), payload)));
		ServerPlayNetworking.registerGlobalReceiver(RadarSwitchPayload.TYPE, (payload, context) ->
				context.server().execute(() -> switchRadar(context.player(), payload.deskPosition())));
		ServerPlayNetworking.registerGlobalReceiver(RunwayConfigurePayload.TYPE, (payload, context) ->
				context.server().execute(() -> configureRunway(context.player(), payload)));
		ServerPlayNetworking.registerGlobalReceiver(DeskRunwayPayload.TYPE, (payload, context) ->
				context.server().execute(() -> configureDeskRunway(context.player(), payload)));
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		ModItems.initialize();
		ModBlocks.initialize();
		ModBlockEntities.initialize();
		LOGGER.info("Simpleradio Aviation Addon Initialized!");
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}

	private static void sendRadarContacts(ServerPlayer player, BlockPos requestedDeskPosition) {
		if (!player.level().hasChunkAt(requestedDeskPosition)) {
			return;
		}
		BlockState requestedState = player.level().getBlockState(requestedDeskPosition);
		if (!(requestedState.getBlock() instanceof RadarDeskBlock)) {
			return;
		}

		BlockPos deskPosition = RadarDeskBlock.getControllerPos(requestedDeskPosition, requestedState);
		if (!canContinueRadarView(player, deskPosition)) {
			return;
		}

		if (!(player.level().getBlockEntity(deskPosition) instanceof RadarDeskBlockEntity desk)) {
			return;
		}

		List<BlockPos> radarPositions = RadarCableNetwork.findConnectedRadars(player.level(), deskPosition);
		if (radarPositions.isEmpty()) {
			BlockPos wirelessRadar = DataLinkNetwork.resolveRadarForDesk(player.level(), deskPosition);
			if (wirelessRadar != null) radarPositions = List.of(wirelessRadar);
		}
		if (radarPositions.isEmpty()) { sendDisconnected(player, deskPosition); return; }

		BlockPos radarPosition = desk.getRadarPosition();
		if (radarPosition == null || !radarPositions.contains(radarPosition)) {
			radarPosition = radarPositions.getFirst();
			desk.setRadarPosition(radarPosition);
		}
		int radarIndex = radarPositions.indexOf(radarPosition);
		if (!player.level().hasChunkAt(radarPosition)) {
			sendOfflineRadar(player, deskPosition, radarPosition, "Aviation Radar", radarIndex, radarPositions.size());
			return;
		}

		BlockState radarState = player.level().getBlockState(radarPosition);
		if (!(radarState.getBlock() instanceof AviationRadarBlock)
				|| !AviationRadarBlock.isCore(radarState)
				|| !(player.level().getBlockEntity(radarPosition) instanceof AviationRadarBlockEntity radar)) {
			sendDisconnected(player, deskPosition); return;
		}

		if (!radarState.getValue(AviationRadarBlock.POWERED)) {
			sendOfflineRadar(player, deskPosition, radarPosition, radar.getRadarName(), radarIndex, radarPositions.size());
			return;
		}

		ServerPlayNetworking.send(player, new RadarContactsPayload(
				deskPosition,
				RadarContactsPayload.Status.ACTIVE,
				radarPosition,
				radar.getRadarName(), radarIndex, radarPositions.size(),
				AviationRadarBlock.getWorldRotorHeading(
						radarState,
						radar.getRotorRotation(1.0F)
				),
				radar.getRotorSpeed(),
				radar.getContacts(),
				RunwayNetwork.describe(player.serverLevel(), desk.getRunwayCodes())
		));
	}

	private static void sendOfflineRadar(ServerPlayer player, BlockPos deskPosition,
										BlockPos radarPosition, String name, int index, int count) {
		ServerPlayNetworking.send(player, new RadarContactsPayload(
				deskPosition,
				RadarContactsPayload.Status.RADAR_OFFLINE,
				radarPosition,
				name, index, count,
				0.0F,
				0.0F,
				List.of(),
				getDeskRunways(player, deskPosition)
		));
	}

	private static void sendDisconnected(ServerPlayer player, BlockPos deskPosition) {
		ServerPlayNetworking.send(player, new RadarContactsPayload(deskPosition,
				RadarContactsPayload.Status.DISCONNECTED, null, "", 0, 0, 0.0F, 0.0F,
				List.of(), getDeskRunways(player, deskPosition)));
	}

	private static void switchRadar(ServerPlayer player, BlockPos requestedPosition) {
		if (!player.level().hasChunkAt(requestedPosition)) return;
		BlockState state = player.level().getBlockState(requestedPosition);
		if (!(state.getBlock() instanceof RadarDeskBlock)) return;
		BlockPos deskPos = RadarDeskBlock.getControllerPos(requestedPosition, state);
		if (!canContinueRadarView(player, deskPos)
				|| !(player.level().getBlockEntity(deskPos) instanceof RadarDeskBlockEntity desk)) return;
		List<BlockPos> radars = RadarCableNetwork.findConnectedRadars(player.level(), deskPos);
		if (radars.size() < 2) return;
		int current = radars.indexOf(desk.getRadarPosition());
		desk.setRadarPosition(radars.get((current + 1 + radars.size()) % radars.size()));
		sendRadarContacts(player, deskPos);
	}

	private static boolean canContinueRadarView(ServerPlayer player, BlockPos deskPosition) {
		long currentTick = player.level().getGameTime();
		UUID playerId = player.getUUID();
		RadarViewSession session = RADAR_VIEW_SESSIONS.get(playerId);
		boolean closeEnoughToOpen = player.distanceToSqr(Vec3.atCenterOf(deskPosition)) <= 64.0D;
		boolean activeSession = session != null
				&& session.dimension().equals(player.level().dimension())
				&& session.deskPosition().equals(deskPosition)
				&& currentTick >= session.lastRequestTick()
				&& currentTick - session.lastRequestTick() <= RADAR_VIEW_SESSION_TIMEOUT_TICKS;

		if (!closeEnoughToOpen && !activeSession) {
			RADAR_VIEW_SESSIONS.remove(playerId);
			return false;
		}

		RADAR_VIEW_SESSIONS.put(playerId, new RadarViewSession(
				player.level().dimension(), deskPosition.immutable(), currentTick));
		return true;
	}

	private static void configureDevice(ServerPlayer player, ConfigureDevicePayload payload) {
		if (player.distanceToSqr(Vec3.atCenterOf(payload.position())) > 64.0D) return;
		if (payload.target() == ConfigureDevicePayload.Target.RADAR) {
			BlockState state = player.level().getBlockState(payload.position());
			if (state.getBlock() instanceof AviationRadarBlock) {
				BlockPos core = AviationRadarBlock.getCorePos(payload.position(), state);
				if (player.level().getBlockEntity(core) instanceof AviationRadarBlockEntity radar) {
					radar.setRadarName(payload.value());
					player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
							"message.simpleradio_aviation.radar.name_saved"), true);
				}
			}
		} else if (player.level().getBlockEntity(payload.position()) instanceof DataLinkBlockEntity device) {
			if (device.isLocked()) {
				player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
						"message.simpleradio_aviation.data_link.locked_access"), true);
				return;
			}
            DataLinkNetwork.BindResult result = DataLinkNetwork.configure(device, payload.value(),
                    payload.receiver() ? com.leitzke.simpleradioaviation.block.DataLinkBlock.Role.RECEIVER
                            : com.leitzke.simpleradioaviation.block.DataLinkBlock.Role.TRANSMITTER);
			if (result == DataLinkNetwork.BindResult.TRANSMITTER_FULL) {
				player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
						"message.simpleradio_aviation.data_link.transmitter_full"), false);
			} else {
				String resultKey = switch (result) {
					case CONNECTED -> "saved";
					case RECEIVER_NOT_ATTACHED -> "receiver_not_attached";
					case TRANSMITTER_NOT_ATTACHED -> "transmitter_not_attached";
					case TIER_MISMATCH -> "tier_mismatch";
					case OUT_OF_RANGE -> "out_of_range";
					default -> "no_transmitter";
				};
				player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
						"message.simpleradio_aviation.data_link." + resultKey), false);
			}
		}
	}

	private static void configureRunway(ServerPlayer player, RunwayConfigurePayload payload) {
		if (player.distanceToSqr(Vec3.atCenterOf(payload.position())) > 64.0D
				|| !(player.level().getBlockEntity(payload.position())
				instanceof RunwayThresholdBlockEntity threshold)) return;
		if (threshold.isLocked()) {
			player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
					"message.simpleradio_aviation.runway.locked_access"), true);
			return;
		}
		RunwayNetwork.ConfigureResult result = RunwayNetwork.configure(
				threshold, payload.code(), payload.name(), payload.width());
		String key = switch (result) {
			case PAIRED -> "paired";
			case SAVED -> "waiting";
			case CODE_FULL -> "code_full";
			case INVALID -> "invalid";
		};
		player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
				"message.simpleradio_aviation.runway." + key), false);
	}

	private static void configureDeskRunway(ServerPlayer player, DeskRunwayPayload payload) {
		if (!player.level().hasChunkAt(payload.deskPosition())) return;
		BlockState state = player.level().getBlockState(payload.deskPosition());
		if (!(state.getBlock() instanceof RadarDeskBlock)) return;
		BlockPos deskPos = RadarDeskBlock.getControllerPos(payload.deskPosition(), state);
		if (!canContinueRadarView(player, deskPos)
				|| !(player.level().getBlockEntity(deskPos) instanceof RadarDeskBlockEntity desk)) return;
		String code = RunwayNetwork.normalizeCode(payload.code());
		if (payload.action() == DeskRunwayPayload.Action.REMOVE) {
			if (desk.removeRunwayCode(code)) player.displayClientMessage(
					net.minecraft.network.chat.Component.translatable(
							"message.simpleradio_aviation.runway.removed", code), false);
			sendRadarContacts(player, deskPos);
			return;
		}
		if (!RunwayNetwork.isComplete(player.serverLevel(), code)) {
			player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
					"message.simpleradio_aviation.runway.not_paired"), false);
			return;
		}
		RadarDeskBlockEntity.AddRunwayResult result = desk.addRunwayCode(code);
		String key = switch (result) {
			case ADDED -> "desk_added";
			case DUPLICATE -> "desk_duplicate";
			case FULL -> "desk_full";
		};
		player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
				"message.simpleradio_aviation.runway." + key, code), false);
		sendRadarContacts(player, deskPos);
	}

	private static List<RunwayInfo> getDeskRunways(ServerPlayer player, BlockPos deskPosition) {
		return player.level().getBlockEntity(deskPosition) instanceof RadarDeskBlockEntity desk
				? RunwayNetwork.describe(player.serverLevel(), desk.getRunwayCodes()) : List.of();
	}

	private record RadarViewSession(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,
									BlockPos deskPosition, long lastRequestTick) {
	}
}
