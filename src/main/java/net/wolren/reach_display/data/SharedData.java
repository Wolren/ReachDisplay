package net.wolren.reach_display.data;

import net.minecraft.entity.Entity;

public class SharedData {
    private static SharedData instance;
    private double distance;
    private Entity entity;

    private SharedData() {}

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

    public double getDistance() {
        return distance;
    }

    public Entity getEntity() {
        return entity;
    }
}
