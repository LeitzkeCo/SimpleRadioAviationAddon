package com.leitzke.simpleradioaviation.item;

import com.leitzke.simpleradioaviation.block.DataLinkBlock;
import com.leitzke.simpleradioaviation.block.entity.DataLinkBlockEntity;
import com.leitzke.simpleradioaviation.block.RunwayThresholdBlock;
import com.leitzke.simpleradioaviation.block.entity.RunwayThresholdBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class PendriveLockItem extends Item {
    public PendriveLockItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        boolean dataLink = level.getBlockState(context.getClickedPos()).getBlock() instanceof DataLinkBlock
                && level.getBlockEntity(context.getClickedPos()) instanceof DataLinkBlockEntity;
        boolean runway = level.getBlockState(context.getClickedPos()).getBlock() instanceof RunwayThresholdBlock
                && level.getBlockEntity(context.getClickedPos()) instanceof RunwayThresholdBlockEntity;
        if (!dataLink && !runway) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;

        if (dataLink) {
            DataLinkBlockEntity device = (DataLinkBlockEntity) level.getBlockEntity(context.getClickedPos());
            if (device.isLocked()) {
                if (player != null) player.displayClientMessage(Component.translatable(
                        "message.simpleradio_aviation.data_link.already_locked"), true);
                return InteractionResult.CONSUME;
            }
            device.lock();
            if (player != null) player.displayClientMessage(Component.translatable(
                    "message.simpleradio_aviation.data_link.locked"), true);
            return InteractionResult.CONSUME;
        }

        RunwayThresholdBlockEntity threshold = (RunwayThresholdBlockEntity)
                level.getBlockEntity(context.getClickedPos());
        if (threshold.isLocked()) {
            if (player != null) player.displayClientMessage(Component.translatable(
                    "message.simpleradio_aviation.runway.already_locked"), true);
            return InteractionResult.CONSUME;
        }
        threshold.lock();
        if (player != null) player.displayClientMessage(Component.translatable(
                "message.simpleradio_aviation.runway.locked"), true);
        return InteractionResult.CONSUME;
    }
}
