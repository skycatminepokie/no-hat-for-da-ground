package com.skycat.nohatfordaground.mixin;

import com.bawnorton.mixinsquared.TargetHandler;
import com.skycat.nohatfordaground.NoHatForDaGround;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("InvalidMemberReference")
@Mixin(value = ServerPlayerInteractionManager.class, priority = 1500)
public abstract class UseBlockCallbackMixinServer {
    @Shadow @Final protected ServerPlayerEntity player;

    @Shadow @Final private static Logger LOGGER;

    @TargetHandler(
        mixin = "net.fabricmc.fabric.mixin.event.interaction.ServerPlayerInteractionManagerMixin",
            name = "interactBlock"
    )
    @Inject(
            method = "@MixinSquared:Handler",
            at = @At("RETURN")
    )
    private void checkAndSendUpdatePacket(CallbackInfo ci) {
        if (NoHatForDaGround.sendUpdateAfterBlockBreak) {
            player.playerScreenHandler.syncState();
            NoHatForDaGround.sendUpdateAfterBlockBreak = false;
        }
    }

}
