package com.leitzke.simpleradioaviation.registry;

import com.leitzke.simpleradioaviation.SimpleRadioAviationAddon;
import com.leitzke.simpleradioaviation.block.RadarDeskBlock;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

public final class ModBlocks {

    public static final Block RADAR_DESK = register(
            "radar_desk",
            new RadarDeskBlock(
                    Block.Properties.of()
                            .strength(2.5F)
                            .sound(SoundType.METAL)
            )
    );

    private static Block register(String name, Block block) {
        Registry.register(
                BuiltInRegistries.BLOCK,
                SimpleRadioAviationAddon.id(name),
                block
        );

        Registry.register(
                BuiltInRegistries.ITEM,
                SimpleRadioAviationAddon.id(name),
                new BlockItem(block, new Item.Properties())
        );

        return block;
    }

    private ModBlocks() {
    }

    public static void initialize() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
                .register(entries -> entries.accept(RADAR_DESK));

        SimpleRadioAviationAddon.LOGGER.info("Registering addon blocks.");
    }
}