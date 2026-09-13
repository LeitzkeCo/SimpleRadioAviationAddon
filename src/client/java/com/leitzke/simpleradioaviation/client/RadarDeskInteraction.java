package com.leitzke.simpleradioaviation.client;

import com.leitzke.simpleradioaviation.block.RadarDeskBlock;
import com.leitzke.simpleradioaviation.item.RadarCableItem;
import com.leitzke.simpleradioaviation.network.RadarContactsPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import com.leitzke.simpleradioaviation.block.DataLinkBlock;

public final class RadarDeskInteraction {

    private static BlockPos requestedDeskPosition;

    private RadarDeskInteraction() {
    }

    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(
                RadarContactsPayload.TYPE,
                (payload, context) -> context.client().execute(() -> {
                    if (context.client().screen instanceof RadarDeskScreen screen
                            && screen.accepts(payload)) {
                        screen.updateContacts(payload);
                    }
                })
        );

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClientSide) {
                return InteractionResult.PASS;
            }

            // Let the cable item receive the click instead of opening the desk screen.
            if (player.getItemInHand(hand).getItem() instanceof RadarCableItem
                    || player.getItemInHand(hand).getItem() instanceof BlockItem blockItem
                    && blockItem.getBlock() instanceof DataLinkBlock) {
                return InteractionResult.PASS;
            }

            BlockPos clickedPosition = hitResult.getBlockPos();
            var state = world.getBlockState(clickedPosition);
            if (!(state.getBlock() instanceof RadarDeskBlock)) {
                return InteractionResult.PASS;
            }

            requestedDeskPosition = RadarDeskBlock.getControllerPos(clickedPosition, state);
            return InteractionResult.SUCCESS;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (requestedDeskPosition == null) {
                return;
            }

            BlockPos deskPosition = requestedDeskPosition;
            requestedDeskPosition = null;

            if (client.player != null && client.screen == null) {
                client.setScreen(new RadarDeskScreen(deskPosition));
            }
        });
    }
}
