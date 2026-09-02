package com.leitzke.simpleradioaviation.item;

import com.codinglitch.simpleradio.core.registry.items.TransceiverItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class AviationHeadsetItem extends TransceiverItem {

    public AviationHeadsetItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }
}