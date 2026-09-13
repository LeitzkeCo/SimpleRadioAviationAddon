package com.leitzke.simpleradioaviation.registry;

import com.leitzke.simpleradioaviation.SimpleRadioAviationAddon;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.EquipmentSlot;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;

import com.leitzke.simpleradioaviation.item.AviationHeadsetItem;
import com.leitzke.simpleradioaviation.item.RadarCableItem;
import com.leitzke.simpleradioaviation.item.PendriveLockItem;

public final class ModItems {
    public static final Item AVIATION_HEADSET = Registry.register(
            BuiltInRegistries.ITEM,
            SimpleRadioAviationAddon.id("aviation_headset"),
            new AviationHeadsetItem(
                    new Item.Properties()
                            .stacksTo(1)
                            .equipmentSlot((entity, stack) -> EquipmentSlot.HEAD)
            )
    );

    public static final Item MILITARY_AVIATION_HEADSET = Registry.register(
            BuiltInRegistries.ITEM,
            SimpleRadioAviationAddon.id("military_aviation_headset"),
            new AviationHeadsetItem(
                    new Item.Properties()
                            .stacksTo(1)
                            .equipmentSlot((entity, stack) -> EquipmentSlot.HEAD)
            )
    );

    public static final Item RADAR_CABLE = Registry.register(
            BuiltInRegistries.ITEM,
            SimpleRadioAviationAddon.id("radar_cable"),
            new RadarCableItem(new Item.Properties().stacksTo(64))
    );

    public static final Item PENDRIVE_LOCK = Registry.register(
            BuiltInRegistries.ITEM,
            SimpleRadioAviationAddon.id("pendrive_lock"),
            new PendriveLockItem(new Item.Properties().stacksTo(1))
    );

    private ModItems() {
    }

    public static void initialize() {
        ItemGroupEvents.modifyEntriesEvent(ModCreativeTabs.SIMPLE_RADIO)
                .register(entries -> {
                    entries.accept(AVIATION_HEADSET);
                    entries.accept(MILITARY_AVIATION_HEADSET);
                    entries.accept(RADAR_CABLE);
                    entries.accept(PENDRIVE_LOCK);
                });
        SimpleRadioAviationAddon.LOGGER.info("Registering addon items.");
    }
}
