package net.wolren.reach_display.data;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.wolren.reach_display.config.DisplayConfig;

import java.io.*;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.Queue;

public class SharedData {
    private static final String GLOBAL_AVERAGE_FILE_NAME = "global_average_hits.txt";
    private static final String GLOBAL_AVERAGE_HEADER = "#reachdisplay-global-average-v1";
    private static SharedData instance;
    private double localAverageDistance = 0;
    private int localAverageHitCount = 0;
    private Queue<Double> lastHitsDistance = new LinkedList<>();
    private double averageDistance = 0;
    private double distance;
    private Entity entity;
    private double globalSum = 0;
    private int globalCount = 0;
    private boolean globalCacheLoaded = false;
    private long lastHitTimestamp = 0;

    public static SharedData getInstance() {
        if (instance == null) {
            instance = new SharedData();
        }
        return instance;
    }

    public void setDistanceAndTarget(double distance, Entity entity) {
        this.distance = distance;
        this.entity = entity;
    }

    public void addDistanceToAverage(double distance) {
        switch (DisplayConfig.averageHitMode) {
            case LOCAL_AVERAGE -> {
                localAverageDistance += distance;
                localAverageHitCount++;
                averageDistance = localAverageDistance / localAverageHitCount;
            }
            case GLOBAL_AVERAGE -> {
                if (!globalCacheLoaded) {
                    loadGlobalAverage();
                    globalCacheLoaded = true;
                }

                globalSum += distance;
                globalCount++;
                averageDistance = globalSum / globalCount;
                saveGlobalAverage();
            }
            case LAST_HITS -> {
                this.lastHitsDistance.add(distance);
                while (this.lastHitsDistance.size() > DisplayConfig.averageNumberOfHitsCounted) {
                    this.lastHitsDistance.poll();
                }
                averageDistance = calculateAverageLastHitsDistance();
            }
        }
    }

    private double calculateAverageLastHitsDistance() {
        if (lastHitsDistance.isEmpty()) {
            return 0;
        }
        double sum = 0;
        for (double distance : lastHitsDistance) {
            sum += distance;
        }
        return sum / lastHitsDistance.size();
    }

    public double getAverageDistance() {
        return averageDistance;
    }

    public double getDistance() {
        return distance;
    }

    public Entity getEntity() {
        return entity;
    }

    public void close() {
        // No long-lived handle: the global average is written through on every recorded hit.
    }

    public void setLastHitTimestamp(long timestamp) {
        this.lastHitTimestamp = timestamp;
    }

    public boolean isLastHitExpired(long timeoutMs) {
        if (lastHitTimestamp == 0) return true;
        return System.currentTimeMillis() - lastHitTimestamp > timeoutMs;
    }

    private File getGlobalAverageFile() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        return configDir.resolve(GLOBAL_AVERAGE_FILE_NAME).toFile();
    }

    private double parseOrZero(String[] parts, int index) {
        if (index >= parts.length) {
            return 0;
        }
        try {
            return Double.parseDouble(parts[index]);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void accumulateLegacyLine(String line) {
        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        for (String value : trimmed.split("[,;\\s]+")) {
            if (value.isEmpty()) {
                continue;
            }
            try {
                globalSum += Double.parseDouble(value);
                globalCount++;
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private void loadGlobalAverage() {
        File globalAverageFile = getGlobalAverageFile();
        if (!globalAverageFile.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(globalAverageFile))) {
            String first = reader.readLine();
            if (first != null && GLOBAL_AVERAGE_HEADER.equals(first.trim())) {
                String data = reader.readLine();
                if (data == null) {
                    return;
                }
                String[] parts = data.trim().split("[,;\\s]+");
                globalSum = parseOrZero(parts, 0);
                globalCount = (int) Math.round(parseOrZero(parts, 1));
                return;
            }
            if (first == null) {
                return;
            }

            // The old file was an append-only list of every hit ever recorded. It is now
            // folded into sum/count and rewritten, so the file no longer grows without
            // bound and is never re-parsed in full again on a later session.
            String line = first;
            do {
                accumulateLegacyLine(line);
                line = reader.readLine();
            } while (line != null);
            saveGlobalAverage();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveGlobalAverage() {
        File globalAverageFile = getGlobalAverageFile();
        try (PrintWriter writer = new PrintWriter(new FileWriter(globalAverageFile, false))) {
            writer.println(GLOBAL_AVERAGE_HEADER);
            writer.println(globalSum + "," + globalCount);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }}

