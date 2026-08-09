package net.wolren.reach_display.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.wolren.reach_display.config.DisplayConfig;
import net.wolren.reach_display.filter.EntityFilterHelper;
import net.wolren.reach_display.utils.ReachCalculation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public abstract class PlayerAttackMixin {
    @Inject(method = "attack(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"))
    private void onAttack(Player player, Entity entity, CallbackInfo ci) {
        if (!DisplayConfig.enabled || entity == null) return;
        if (DisplayConfig.entityFilterEnable && !EntityFilterHelper.shouldTrack(entity)) return;

        ReachCalculation.recordHit(player, entity);
    }
}
