package com.skycat.nohatfordaground.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import org.jetbrains.annotations.Contract;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemPlacementContext.class)
public abstract class ItemPlacementContextMixin extends ItemUsageContext {
    /**
     * Required by ItemUsageContext, but unused. Don't use it - mixins aren't meant to be constructed.
     * @deprecated Don't use this.
     */
    @Contract("_,_,_->fail")
    private ItemPlacementContextMixin(PlayerEntity player, Hand hand, BlockHitResult hit) {
        super(player, hand, hit);
        throw new UnsupportedOperationException("Seriously, don't use this. Like bro, did you ignore the javadoc, the deprecated warning, and static analysis? You're asking for it man.");
    }

    @ModifyReturnValue(method = "canPlace", at = @At("RETURN"))
    private boolean stopPlacingCustomModelData(boolean original) {
        // Adapted from Linguardium's method: https://github.com/Linguardium/no-hat-for-da-ground/commit/33ae0e396bf29bd07edae919b6765e0569bd3a7a
        // This is a lot less clean-looking, but it reduces it to one mixin and makes more sense in my head.
        if (!original) return false; // If it wasn't going to place in the first place, don't bother.
        ItemStack stack = this.getStack();
        if (stack.getNbt() == null || !stack.getNbt().contains("CustomModelData", NbtElement.NUMBER_TYPE)) { // if it has no NBT or if it has no custom model data
            return true; // Then we don't need to bother.
        }
        // We've decided to block it
        PlayerEntity player = this.getPlayer();
        if (player != null) { // If it was a player
            this.getPlayer().sendMessage(Text.of("You can't place that!"), true); // Let the player know we're blocking it
            if (player instanceof ServerPlayerEntity serverPlayer) { // If we're on the server side
                serverPlayer.playerScreenHandler.syncState(); // Make sure the client gets the update
            }
        }
        return false; // We decided we were going to block it earlier
    }

}
