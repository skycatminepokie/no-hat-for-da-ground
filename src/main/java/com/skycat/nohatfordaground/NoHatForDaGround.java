package com.skycat.nohatfordaground;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoHatForDaGround implements ModInitializer, UseBlockCallback {
	public static boolean sendUpdateAfterBlockBreak = false; // Used to send an inventory update packet when the server rejects the placement of a block
	@Override
	public ActionResult interact(PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) {
		ItemStack stack = player.getStackInHand(hand);
		NbtCompound nbt = stack.getNbt();
		if (nbt != null && nbt.get("CustomModelData") != null) {
			player.sendMessage(Text.of("You can't place that!"), true);
			sendUpdateAfterBlockBreak = true;
			return ActionResult.FAIL;
		}
		return ActionResult.PASS;
	}

	@Override
	public void onInitialize() {
		UseBlockCallback.EVENT.register(this);
	}
}