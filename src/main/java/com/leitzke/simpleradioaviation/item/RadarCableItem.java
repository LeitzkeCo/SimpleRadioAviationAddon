package com.leitzke.simpleradioaviation.item;

import com.leitzke.simpleradioaviation.radar.RadarCableNetwork;
import com.leitzke.simpleradioaviation.radar.RadarCableNode;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RadarCableItem extends Item {
    public static final double MAX_SEGMENT_LENGTH = 10.0D;
    private static final Map<UUID, PendingEndpoint> PENDING = new ConcurrentHashMap<>();

    public RadarCableItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        RadarCableNode endpoint = RadarCableNetwork.findNode(level, context.getClickedPos());
        if (player == null || endpoint == null) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;

        UUID playerId = player.getUUID();
        if (player.isShiftKeyDown()) {
            RadarCableNetwork.disconnectAll(endpoint);
            PENDING.remove(playerId);
            show(player, "message.simpleradio_aviation.radar_cable.disconnected");
            return InteractionResult.CONSUME;
        }

        PendingEndpoint pending = PENDING.get(playerId);
        if (pending == null) {
            PENDING.put(playerId, new PendingEndpoint(level.dimension(), endpoint.getCableNodePosition()));
            show(player, "message.simpleradio_aviation.radar_cable.first");
            return InteractionResult.CONSUME;
        }
        if (!pending.dimension().equals(level.dimension())) {
            PENDING.remove(playerId);
            show(player, "message.simpleradio_aviation.radar_cable.dimension");
            return InteractionResult.FAIL;
        }
        if (pending.position().equals(endpoint.getCableNodePosition())) {
            PENDING.remove(playerId);
            show(player, "message.simpleradio_aviation.radar_cable.cancelled");
            return InteractionResult.CONSUME;
        }

        RadarCableNode first = RadarCableNetwork.findNode(level, pending.position());
        if (first == null) {
            PENDING.remove(playerId);
            show(player, "message.simpleradio_aviation.radar_cable.invalid");
            return InteractionResult.FAIL;
        }
        if (RadarCableNetwork.anchor(first).distanceToSqr(RadarCableNetwork.anchor(endpoint))
                > MAX_SEGMENT_LENGTH * MAX_SEGMENT_LENGTH) {
            player.displayClientMessage(Component.translatable(
                    "message.simpleradio_aviation.radar_cable.too_long", (int) MAX_SEGMENT_LENGTH), true);
            return InteractionResult.FAIL;
        }
        ItemStack stack = context.getItemInHand();
        if (!RadarCableNetwork.connect(first, endpoint)) {
            show(player, "message.simpleradio_aviation.radar_cable.invalid");
            return InteractionResult.FAIL;
        }
        PENDING.remove(playerId);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        show(player, "message.simpleradio_aviation.radar_cable.connected");
        return InteractionResult.CONSUME;
    }

    public static boolean isSegmentLengthValid(BlockPos first, BlockPos second) {
        return first.distSqr(second) <= MAX_SEGMENT_LENGTH * MAX_SEGMENT_LENGTH;
    }

    private static void show(Player player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }

    private record PendingEndpoint(ResourceKey<Level> dimension, BlockPos position) {}
}
