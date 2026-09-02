package com.leitzke.simpleradioaviation.registry;

import com.leitzke.simpleradioaviation.SimpleRadioAviationAddon;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.EquipmentSlot;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.world.item.CreativeModeTabs;

import com.leitzke.simpleradioaviation.item.AviationHeadsetItem;

public final class ModItems {
    public static final Item AVIATION_HEADSET = Registry.register(
            BuiltInRegistries.ITEM,
            SimpleRadioAviationAddon.id("aviation_headset"),
            new AviationHeadsetItem(
                    new Item.Properties().equipmentSlot((entity, stack) -> EquipmentSlot.HEAD)
            )
    );

    public static final Item MILITARY_AVIATION_HEADSET = Registry.register(
            BuiltInRegistries.ITEM,
            SimpleRadioAviationAddon.id("military_aviation_headset"),
            new AviationHeadsetItem(
                    new Item.Properties().equipmentSlot((entity, stack) -> EquipmentSlot.HEAD)
            )
    );

    private ModItems() {
    }

    public static void initialize() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(entries -> {
                    entries.accept(AVIATION_HEADSET);
                    entries.accept(MILITARY_AVIATION_HEADSET);
                });
        SimpleRadioAviationAddon.LOGGER.info("Registering addon items.");
    }
}
