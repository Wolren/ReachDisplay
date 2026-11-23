package net.wolren.reach_display.filter;

import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.wolren.reach_display.config.DisplayConfig;
import net.wolren.reach_display.config.DisplayConfig.EntityFilterMode;

public class EntityFilterHelper {
    public static Category categorize(Entity entity) {
        if (entity instanceof Player) return Category.PLAYER;

        String className = entity.getClass().getName();
        if (className.contains("EnderDragon") || className.contains("Wither") || className.contains("Warden")) {
            return Category.BOSS;
        }

        if (entity instanceof Monster) return Category.HOSTILE;
        if (entity instanceof Animal || entity instanceof AgeableMob) return Category.PASSIVE;

        return Category.OTHER;
    }

    public static boolean shouldTrack(Entity entity) {
        if (!DisplayConfig.entityFilterEnable) return true;

        Category cat = categorize(entity);

        boolean categoryMatches;
        switch (cat) {
            case PLAYER:
                categoryMatches = DisplayConfig.entityFilterPlayers;
                break;
            case HOSTILE:
                categoryMatches = DisplayConfig.entityFilterHostile;
                break;
            case PASSIVE:
                categoryMatches = DisplayConfig.entityFilterPassive;
                break;
            case BOSS:
                categoryMatches = DisplayConfig.entityFilterBoss;
                break;
            default:
                categoryMatches = DisplayConfig.entityFilterOther;
                break;
        }

        boolean customMatch = false;
        if (!DisplayConfig.entityFilterCustomIDs.isEmpty()) {
            String entityId = EntityType.getKey(entity.getType()).toString();
            String[] customIds = DisplayConfig.entityFilterCustomIDs.split(",");
            for (String customId : customIds) {
                if (customId.trim().equalsIgnoreCase(entityId)) {
                    customMatch = true;
                    break;
                }
            }
        }

        if (DisplayConfig.entityFilterMode == EntityFilterMode.WHITELIST) {
            return categoryMatches || customMatch;
        } else {
            if (!categoryMatches && !customMatch) return false;
            return true;
        }
    }

    public enum Category {
        PLAYER, HOSTILE, PASSIVE, BOSS, OTHER
    }
}
