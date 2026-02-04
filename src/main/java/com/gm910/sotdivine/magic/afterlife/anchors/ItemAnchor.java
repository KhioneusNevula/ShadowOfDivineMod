package com.gm910.sotdivine.magic.afterlife.anchors;

import java.util.Optional;

import com.gm910.sotdivine.magic.afterlife.Afterlife;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

public class ItemAnchor implements IMoveableAnchor<ItemStack> {

	@Override
	public void moveAnchor(EntityOrPosition eop) {
		// TODO Auto-generated method stub

	}

	@Override
	public IAfterlifeAnchor<EntityOrPosition, ItemStack> updateAfterlifeReference(Afterlife afterlife) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ItemStack storedObject() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Optional<EntityOrPosition> getAnchor(ServerLevel level) {
		// TODO Auto-generated method stub
		return Optional.empty();
	}

	@Override
	public AfterlifeAnchorType<?> getAnchorType() {
		// TODO Auto-generated method stub
		return null;
	}

}
