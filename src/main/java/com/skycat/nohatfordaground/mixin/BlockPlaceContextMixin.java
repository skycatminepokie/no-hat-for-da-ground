package com.skycat.nohatfordaground.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Contract;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockPlaceContext.class)
public abstract class BlockPlaceContextMixin extends UseOnContext {
    /**
     * Required by ItemUsageContext, but unused. Don't use it - mixins aren't meant to be constructed.
     * @deprecated Don't use this.
     */
    @Contract("_,_,_->fail")
    @Deprecated
    private BlockPlaceContextMixin(Player player, InteractionHand hand, BlockHitResult hit) {
        super(player, hand, hit);
        throw new UnsupportedOperationException("Seriously, don't use this. Like bro, did you ignore the javadoc, the deprecated warning, and static analysis? You're asking for it man.");
    }

    @ModifyReturnValue(method = "canPlace", at = @At("RETURN"))
    private boolean stopPlacingCustomModelData(boolean original) {
        // Adapted from Linguardium's method: https://github.com/Linguardium/no-hat-for-da-ground/commit/33ae0e396bf29bd07edae919b6765e0569bd3a7a
        // This is a lot less clean-looking, but it reduces it to one mixin and makes more sense in my head.
        if (!original) return false; // If it wasn't going to place in the first place, don't bother.
        ItemStack stack = this.getItemInHand();
        if (!stack.getComponents().has(DataComponents.CUSTOM_MODEL_DATA)) { // if it has no custom model data
            return true; // Then we don't need to bother.
        }
        // We've decided to block it
        Player player = this.getPlayer();
        if (player != null) { // If it was a player
            player.sendOverlayMessage(Component.nullToEmpty("You can't place that!")); // Let the player know we're blocking it
            if (player instanceof ServerPlayer serverPlayer) { // If we're on the server side
                serverPlayer.inventoryMenu.sendAllDataToRemote(); // Make sure the client gets the update
            }
        }
        return false; // We decided we were going to block it earlier
    }

}
