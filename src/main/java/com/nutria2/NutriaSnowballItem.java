package com.nutria2;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class NutriaSnowballItem extends Item {
	public NutriaSnowballItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW,
				SoundSource.NEUTRAL, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
		if (!level.isClientSide) {
			NutriaSnowballEntity ball = new NutriaSnowballEntity(level, player);
			ball.setItem(stack);
			ball.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
			level.addFreshEntity(ball);
		}
		player.awardStat(Stats.ITEM_USED.get(this));
		stack.consume(1, player);
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
	}
}
