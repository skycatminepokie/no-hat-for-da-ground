package com.skycat.nohatfordaground.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
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
    @Deprecated
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
        if (stack.getTag() == null || !stack.getTag().contains("CustomModelData", 99)) { // if it has no NBT or if it has no custom model data. Magic number 99 is the NBT NUMBER_TYPE tag.
            return true; // Then we don't need to bother.
        }
        // We've decided to block it
        PlayerEntity player = this.getPlayer();
        if (player != null) { // If it was a player
            if (player instanceof ServerPlayerEntity) { // If we're on the server side
                this.getPlayer().sendMessage(new LiteralText("You can't place that!"), true); // Let the player know we're blocking it
                int slot;
                if (hand.equals(Hand.MAIN_HAND)) {
                    slot = player.inventory.selectedSlot;
                } else {
                    slot = 40; // Offhand (found by trial and error lol)
                }
                ((ServerPlayerEntity) player).networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(-2, slot, stack)); // Send inventory update to the client. Magic number -2: The id that's used for /give and pick block. I don't know what it does.
            }
        }
        return false; // We decided we were going to block it earlier
    }

}
