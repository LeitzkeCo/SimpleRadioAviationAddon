package com.leitzke.simpleradioaviation.client;

import com.leitzke.simpleradioaviation.block.RadarDeskBlock;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.InteractionResult;

public final class RadarDeskInteraction {

    private static boolean openRequested;

    private RadarDeskInteraction() {
    }

    public static void initialize() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!(world.getBlockState(hitResult.getBlockPos()).getBlock() instanceof RadarDeskBlock)) {
                return InteractionResult.PASS;
            }

            openRequested = true;
            return InteractionResult.SUCCESS;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!openRequested) {
                return;
            }

            openRequested = false;

            if (client.player != null && client.screen == null) {
                client.setScreen(new RadarDeskScreen());
            }
        });
    }
}