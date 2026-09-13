package com.leitzke.simpleradioaviation.registry;

import com.leitzke.simpleradioaviation.SimpleRadioAviationAddon;
import com.leitzke.simpleradioaviation.block.AviationRadarBlock;
import com.leitzke.simpleradioaviation.block.RadarDeskBlock;
import com.leitzke.simpleradioaviation.block.RadarCablePulleyBlock;
import com.leitzke.simpleradioaviation.block.DataLinkBlock;
import com.leitzke.simpleradioaviation.block.RunwayThresholdBlock;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

public final class ModBlocks {

    public static final Block RADAR_DESK = register(
            "radar_desk",
            new RadarDeskBlock(
                    Block.Properties.of()
                            .strength(2.5F)
                            .noOcclusion()
                            .sound(SoundType.METAL)
            )
    );

    public static final Block AVIATION_RADAR = register(
            "aviation_radar",
            new AviationRadarBlock(
                    Block.Properties.of()
                            .strength(4.0F)
                            .noOcclusion()
                            .sound(SoundType.METAL)
            ),
            new Item.Properties().stacksTo(1)
    );

    public static final Block RADAR_CABLE_PULLEY = register(
            "radar_cable_pulley",
            new RadarCablePulleyBlock(
                    Block.Properties.of()
                            .strength(1.5F)
                            .sound(SoundType.COPPER)
            )
    );

    public static final Block BASIC_DATA_TRANSCEIVER = dataLink("basic_data_transmitter", DataLinkBlock.Tier.BASIC);
    public static final Block INTERMEDIATE_DATA_TRANSCEIVER = dataLink("intermediate_data_transmitter", DataLinkBlock.Tier.INTERMEDIATE);
    public static final Block ADVANCED_DATA_TRANSCEIVER = dataLink("advanced_data_transmitter", DataLinkBlock.Tier.ADVANCED);

    public static final Block RUNWAY_THRESHOLD = register(
            "runway_threshold",
            new RunwayThresholdBlock(Block.Properties.of()
                    .strength(2.5F).noOcclusion().sound(SoundType.METAL))
    );

    private static Block dataLink(String name, DataLinkBlock.Tier tier) {
        return register(name, new DataLinkBlock(Block.Properties.of()
                .strength(2.0F).noOcclusion().sound(SoundType.METAL), tier));
    }

    private static Block register(String name, Block block) {
        return register(name, block, new Item.Properties());
    }

    private static Block register(String name, Block block, Item.Properties itemProperties) {
        Registry.register(
                BuiltInRegistries.BLOCK,
                SimpleRadioAviationAddon.id(name),
                block
        );

        Registry.register(
                BuiltInRegistries.ITEM,
                SimpleRadioAviationAddon.id(name),
                new BlockItem(block, itemProperties)
        );

        return block;
    }

    private ModBlocks() {
    }

    public static void initialize() {
        ItemGroupEvents.modifyEntriesEvent(ModCreativeTabs.SIMPLE_RADIO)
                .register(entries -> {
                    entries.accept(RADAR_DESK);
                    entries.accept(AVIATION_RADAR);
                    entries.accept(RADAR_CABLE_PULLEY);
                    entries.accept(BASIC_DATA_TRANSCEIVER);
                    entries.accept(INTERMEDIATE_DATA_TRANSCEIVER);
                    entries.accept(ADVANCED_DATA_TRANSCEIVER);
                    entries.accept(RUNWAY_THRESHOLD);
                });

        SimpleRadioAviationAddon.LOGGER.info("Registering addon blocks.");
    }
}
