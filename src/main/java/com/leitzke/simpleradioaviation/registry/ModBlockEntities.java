package com.leitzke.simpleradioaviation.registry;

import com.leitzke.simpleradioaviation.SimpleRadioAviationAddon;
import com.leitzke.simpleradioaviation.block.entity.AviationRadarBlockEntity;
import com.leitzke.simpleradioaviation.block.entity.RadarDeskBlockEntity;
import com.leitzke.simpleradioaviation.block.entity.RadarCablePulleyBlockEntity;
import com.leitzke.simpleradioaviation.block.entity.DataLinkBlockEntity;
import com.leitzke.simpleradioaviation.block.entity.RunwayThresholdBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Set;

public final class ModBlockEntities {
    public static final BlockEntityType<RadarDeskBlockEntity> RADAR_DESK = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            SimpleRadioAviationAddon.id("radar_desk"),
            new BlockEntityType<>(
                    RadarDeskBlockEntity::new,
                    Set.of(ModBlocks.RADAR_DESK),
                    null
            )
    );

    public static final BlockEntityType<AviationRadarBlockEntity> AVIATION_RADAR = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            SimpleRadioAviationAddon.id("aviation_radar"),
            new BlockEntityType<>(
                    AviationRadarBlockEntity::new,
                    Set.of(ModBlocks.AVIATION_RADAR),
                    null
            )
    );

    public static final BlockEntityType<RadarCablePulleyBlockEntity> RADAR_CABLE_PULLEY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            SimpleRadioAviationAddon.id("radar_cable_pulley"),
            new BlockEntityType<>(RadarCablePulleyBlockEntity::new,
                    Set.of(ModBlocks.RADAR_CABLE_PULLEY), null)
    );

    public static final BlockEntityType<DataLinkBlockEntity> DATA_LINK = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            SimpleRadioAviationAddon.id("data_link"),
            new BlockEntityType<>(DataLinkBlockEntity::new, Set.of(
                    ModBlocks.BASIC_DATA_TRANSCEIVER,
                    ModBlocks.INTERMEDIATE_DATA_TRANSCEIVER,
                    ModBlocks.ADVANCED_DATA_TRANSCEIVER), null)
    );

    public static final BlockEntityType<RunwayThresholdBlockEntity> RUNWAY_THRESHOLD = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            SimpleRadioAviationAddon.id("runway_threshold"),
            new BlockEntityType<>(RunwayThresholdBlockEntity::new,
                    Set.of(ModBlocks.RUNWAY_THRESHOLD), null)
    );

    private ModBlockEntities() {
    }

    public static void initialize() {
        SimpleRadioAviationAddon.LOGGER.info("Registering addon block entities.");
    }
}
