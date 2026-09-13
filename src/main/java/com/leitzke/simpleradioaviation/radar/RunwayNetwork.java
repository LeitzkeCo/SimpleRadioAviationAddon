package com.leitzke.simpleradioaviation.radar;

import com.leitzke.simpleradioaviation.block.entity.RunwayThresholdBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class RunwayNetwork {
    public static final int MAX_CODE_LENGTH = 16;
    public static final int MAX_NAME_LENGTH = 5;
    public static final int MIN_WIDTH = 1;
    public static final int MAX_WIDTH = 64;

    private RunwayNetwork() {}

    public static ConfigureResult configure(RunwayThresholdBlockEntity threshold,
                                            String rawCode, String rawName, int width) {
        if (!(threshold.getLevel() instanceof ServerLevel level)) return ConfigureResult.INVALID;
        String code = normalizeCode(rawCode);
        String name = normalizeName(rawName);
        if (code.isBlank() || name.isBlank() || width < MIN_WIDTH || width > MAX_WIDTH) {
            return ConfigureResult.INVALID;
        }

        RunwaySavedData data = data(level);
        RunwaySavedData.PairData destination = data.pair(code);
        BlockPos position = threshold.getBlockPos().immutable();
        if (destination != null && !destination.endpoints.containsKey(position)
                && destination.endpoints.size() >= 2) {
            return ConfigureResult.CODE_FULL;
        }

        String oldCode = threshold.getCode();
        if (!oldCode.isBlank() && !oldCode.equals(code)) removeEndpoint(data, oldCode, position);

        destination = data.pairOrCreate(code, width);
        destination.width = width;
        destination.endpoints.put(position, new RunwaySavedData.Endpoint(position, name));
        threshold.applyConfiguration(code, name, width);
        synchronizeLoadedEndpoints(level, destination, code);
        data.setDirty();
        return destination.endpoints.size() == 2 ? ConfigureResult.PAIRED : ConfigureResult.SAVED;
    }

    public static void onLoaded(RunwayThresholdBlockEntity threshold) {
        if (!(threshold.getLevel() instanceof ServerLevel level) || threshold.getCode().isBlank()) return;
        RunwaySavedData data = data(level);
        RunwaySavedData.PairData pair = data.pair(threshold.getCode());
        BlockPos position = threshold.getBlockPos().immutable();
        if (pair == null) {
            pair = data.pairOrCreate(threshold.getCode(), threshold.getRunwayWidth());
            pair.endpoints.put(position, new RunwaySavedData.Endpoint(position, threshold.getThresholdName()));
            data.setDirty();
        } else if (pair.endpoints.containsKey(position)) {
            threshold.applyConfigurationOnLoad(threshold.getCode(),
                    pair.endpoints.get(position).name(), pair.width);
        } else if (pair.endpoints.size() < 2) {
            pair.endpoints.put(position, new RunwaySavedData.Endpoint(position, threshold.getThresholdName()));
            threshold.applyConfigurationOnLoad(threshold.getCode(), threshold.getThresholdName(), pair.width);
            data.setDirty();
        }
    }

    public static void remove(RunwayThresholdBlockEntity threshold) {
        if (!(threshold.getLevel() instanceof ServerLevel level) || threshold.getCode().isBlank()) return;
        RunwaySavedData data = data(level);
        removeEndpoint(data, threshold.getCode(), threshold.getBlockPos());
        data.setDirty();
    }

    public static boolean isComplete(ServerLevel level, String rawCode) {
        RunwaySavedData.PairData pair = data(level).pair(normalizeCode(rawCode));
        return pair != null && pair.endpoints.size() == 2;
    }

    public static List<RunwayInfo> describe(ServerLevel level, List<String> codes) {
        RunwaySavedData data = data(level);
        List<RunwayInfo> result = new ArrayList<>(Math.min(4, codes.size()));
        Set<String> seenCodes = new HashSet<>();
        for (String rawCode : codes) {
            String code = normalizeCode(rawCode);
            if (code.isBlank() || !seenCodes.add(code)) continue;
            RunwaySavedData.PairData pair = data.pair(code);
            if (pair == null || pair.endpoints.isEmpty()) {
                result.add(new RunwayInfo(code, "", "", 0, null, null));
            } else {
                List<RunwaySavedData.Endpoint> endpoints = new ArrayList<>(pair.endpoints.values());
                RunwaySavedData.Endpoint first = endpoints.getFirst();
                RunwaySavedData.Endpoint second = endpoints.size() > 1 ? endpoints.get(1) : null;
                result.add(new RunwayInfo(code, first.name(), second == null ? "" : second.name(),
                        pair.width, first.position(), second == null ? null : second.position()));
            }
            if (result.size() >= 4) break;
        }
        return List.copyOf(result);
    }

    public static String normalizeCode(String value) {
        return normalize(value, MAX_CODE_LENGTH);
    }

    public static String normalizeName(String value) {
        return normalize(value, MAX_NAME_LENGTH);
    }

    private static String normalize(String value, int length) {
        if (value == null) return "";
        String normalized = value.strip().toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9_-]", "");
        return normalized.substring(0, Math.min(length, normalized.length()));
    }

    private static RunwaySavedData data(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                RunwaySavedData.FACTORY, "simpleradio_aviation_runways");
    }

    private static void removeEndpoint(RunwaySavedData data, String code, BlockPos position) {
        RunwaySavedData.PairData pair = data.pair(code);
        if (pair == null) return;
        pair.endpoints.remove(position);
        data.removeIfEmpty(code);
    }

    private static void synchronizeLoadedEndpoints(ServerLevel level,
                                                    RunwaySavedData.PairData pair,
                                                    String code) {
        for (RunwaySavedData.Endpoint endpoint : pair.endpoints.values()) {
            if (!level.hasChunkAt(endpoint.position())) continue;
            if (level.getBlockEntity(endpoint.position()) instanceof RunwayThresholdBlockEntity loaded) {
                loaded.applyConfiguration(code, endpoint.name(), pair.width);
            }
        }
    }

    public enum ConfigureResult { SAVED, PAIRED, CODE_FULL, INVALID }
}
