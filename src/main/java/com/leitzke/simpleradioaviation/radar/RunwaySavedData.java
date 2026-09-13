package com.leitzke.simpleradioaviation.radar;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.Map;

final class RunwaySavedData extends SavedData {
    static final SavedData.Factory<RunwaySavedData> FACTORY = new SavedData.Factory<>(
            RunwaySavedData::new, RunwaySavedData::load, null);

    private final Map<String, PairData> pairs = new LinkedHashMap<>();

    PairData pair(String code) {
        return pairs.get(code);
    }

    PairData pairOrCreate(String code, int width) {
        return pairs.computeIfAbsent(code, ignored -> new PairData(width));
    }

    void removeIfEmpty(String code) {
        PairData pair = pairs.get(code);
        if (pair != null && pair.endpoints.isEmpty()) pairs.remove(code);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag pairTags = new ListTag();
        for (Map.Entry<String, PairData> entry : pairs.entrySet()) {
            CompoundTag pairTag = new CompoundTag();
            pairTag.putString("Code", entry.getKey());
            pairTag.putInt("Width", entry.getValue().width);
            ListTag endpointTags = new ListTag();
            for (Endpoint endpoint : entry.getValue().endpoints.values()) {
                CompoundTag endpointTag = new CompoundTag();
                endpointTag.putLong("Position", endpoint.position.asLong());
                endpointTag.putString("Name", endpoint.name);
                endpointTags.add(endpointTag);
            }
            pairTag.put("Endpoints", endpointTags);
            pairTags.add(pairTag);
        }
        tag.put("Pairs", pairTags);
        return tag;
    }

    private static RunwaySavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        RunwaySavedData data = new RunwaySavedData();
        ListTag pairTags = tag.getList("Pairs", Tag.TAG_COMPOUND);
        for (int index = 0; index < pairTags.size(); index++) {
            CompoundTag pairTag = pairTags.getCompound(index);
            String code = RunwayNetwork.normalizeCode(pairTag.getString("Code"));
            if (code.isBlank()) continue;
            PairData pair = new PairData(Math.max(1, Math.min(64, pairTag.getInt("Width"))));
            ListTag endpointTags = pairTag.getList("Endpoints", Tag.TAG_COMPOUND);
            for (int endpointIndex = 0; endpointIndex < endpointTags.size()
                    && endpointIndex < 2; endpointIndex++) {
                CompoundTag endpointTag = endpointTags.getCompound(endpointIndex);
                BlockPos position = BlockPos.of(endpointTag.getLong("Position"));
                pair.endpoints.put(position, new Endpoint(position,
                        RunwayNetwork.normalizeName(endpointTag.getString("Name"))));
            }
            if (!pair.endpoints.isEmpty()) data.pairs.put(code, pair);
        }
        return data;
    }

    static final class PairData {
        int width;
        final LinkedHashMap<BlockPos, Endpoint> endpoints = new LinkedHashMap<>();

        PairData(int width) {
            this.width = width;
        }
    }

    record Endpoint(BlockPos position, String name) {}
}
