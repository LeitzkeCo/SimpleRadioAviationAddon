package com.leitzke.simpleradioaviation.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;

public final class ModCreativeTabs {

    public static final ResourceKey<CreativeModeTab> SIMPLE_RADIO = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            ResourceLocation.fromNamespaceAndPath("simpleradio", "simple_radio_tab")
    );

    private ModCreativeTabs() {
    }
}
