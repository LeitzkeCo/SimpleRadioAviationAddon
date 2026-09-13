package com.leitzke.simpleradioaviation.item;

import com.codinglitch.simpleradio.SimpleRadioApi;
import com.codinglitch.simpleradio.central.Frequency;
import com.codinglitch.simpleradio.core.Frequencies;
import com.codinglitch.simpleradio.core.registry.items.TransceiverItem;
import com.codinglitch.simpleradio.routers.Receiver;
import com.codinglitch.simpleradio.routers.Transmitter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

import static com.codinglitch.simpleradio.core.SimpleRadioComponents.FREQUENCY;
import static com.codinglitch.simpleradio.core.SimpleRadioComponents.MODULATION;
import static com.codinglitch.simpleradio.core.SimpleRadioComponents.REFERENCE;
import static com.codinglitch.simpleradio.core.SimpleRadioComponents.USING;

public class AviationHeadsetItem extends TransceiverItem {
    private static final int CLIENT_ACTIVATION_DELAY_TICKS = 40;
    private static final Map<Level, Set<UUID>> CLIENT_ACTIVATIONS =
            Collections.synchronizedMap(new WeakHashMap<>());

    public AviationHeadsetItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public void entityTick(ItemStack stack, Entity entity) {
        if (!(entity instanceof LivingEntity livingEntity)) {
            deactivateRouters(stack, entity.level());
            return;
        }

        ItemStack equippedHeadset = livingEntity.getItemBySlot(EquipmentSlot.HEAD);
        if (equippedHeadset != stack) {
            // A copied stack may temporarily carry the UUID of the equipped one.
            // Clear only the copy so it cannot remove the live headset routers.
            if (
                    stack.has(REFERENCE)
                            && equippedHeadset.has(REFERENCE)
                            && stack.get(REFERENCE).equals(equippedHeadset.get(REFERENCE))
            ) {
                stack.remove(REFERENCE);
                stack.set(USING, false);
            } else {
                deactivateRouters(stack, entity.level());
            }
            return;
        }

        if (entity.level().isClientSide && stack.has(REFERENCE)) {
            // Let the initial chunk packets render before Simple Radio creates its
            // client routers. If identifier negotiation fails, never create a new
            // router set every frame; Simple Radio already retries the first set.
            if (entity.tickCount < CLIENT_ACTIVATION_DELAY_TICKS) return;
            UUID reference = stack.get(REFERENCE);
            if (SimpleRadioApi.getRouterSided(reference, true) == null) {
                Set<UUID> activated = CLIENT_ACTIVATIONS.computeIfAbsent(
                        entity.level(), ignored -> new HashSet<>());
                if (!activated.add(reference)) return;
            }
        }

        super.entityTick(stack, entity);
        synchronizeRouterFrequency(stack, entity.level());
    }

    private void deactivateRouters(ItemStack stack, Level level) {
        if (!stack.has(REFERENCE)) {
            return;
        }

        UUID reference = stack.get(REFERENCE);
        String frequencyName = stack.get(FREQUENCY);
        Frequency.Modulation modulation = stack.get(MODULATION);
        boolean clientSide = level.isClientSide;
        if (clientSide) {
            Set<UUID> activated = CLIENT_ACTIVATIONS.get(level);
            if (activated != null) activated.remove(reference);
        }

        stopListening(reference, clientSide);
        stopSpeaking(reference, clientSide);

        if (frequencyName != null && modulation != null) {
            stopReceiving(frequencyName, modulation, reference, clientSide);
            stopTransmitting(frequencyName, modulation, reference, clientSide);
        }

        stack.remove(REFERENCE);
        stack.set(USING, false);
    }

    private void synchronizeRouterFrequency(ItemStack stack, Level level) {
        if (!stack.has(REFERENCE) || !stack.has(FREQUENCY) || !stack.has(MODULATION)) {
            return;
        }

        UUID reference = stack.get(REFERENCE);
        String frequencyName = stack.get(FREQUENCY);
        Frequency.Modulation modulation = stack.get(MODULATION);
        Frequencies frequencies = SimpleRadioApi.getInstance(level.isClientSide).frequencies();
        Frequency targetFrequency = frequencies.getOrCreate(frequencyName, modulation);

        // The router pair is already tuned correctly.
        if (
                targetFrequency.getReceiver(reference) != null
                        && targetFrequency.getTransmitter(reference) != null
        ) {
            return;
        }

        // Locate the live pair without destroying it. Keeping these router objects
        // also keeps their server/client identifiers and every unrelated wire stable.
        Frequency sourceFrequency = frequencies.get().stream()
                .filter(frequency -> frequency != targetFrequency)
                .filter(frequency -> frequency.getReceiver(reference) != null)
                .filter(frequency -> frequency.getTransmitter(reference) != null)
                .findFirst()
                .orElse(null);

        if (sourceFrequency == null) {
            return;
        }

        Receiver receiver = sourceFrequency.getReceiver(reference);
        Transmitter transmitter = sourceFrequency.getTransmitter(reference);

        // FrequencyChannel exposes its backing lists here. Adding directly is
        // intentional: registerReceiver/registerTransmitter would register the
        // same router globally again and replace its network identifier.
        targetFrequency.getReceivers().add(receiver.frequency(targetFrequency));
        targetFrequency.getTransmitters().add(transmitter.frequency(targetFrequency));
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
