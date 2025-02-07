/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.common.util.inventory;

import blusunrize.immersiveengineering.api.utils.CapabilityUtils;
import blusunrize.immersiveengineering.common.items.InternalStorageItem;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class IEItemStackHandler extends ItemStackHandler implements ICapabilityProvider
{
	public IEItemStackHandler(ItemStack stack)
	{
		super();
		int idealSize = ((InternalStorageItem)stack.getItem()).getSlotCount();
		NonNullList<ItemStack> newList = NonNullList.withSize(idealSize, ItemStack.EMPTY);
		for(int i = 0; i < Math.min(stacks.size(), idealSize); i++)
			newList.set(i, stacks.get(i));
		stacks = newList;
	}

	@Nonnull
	private Runnable onChange = () -> {
	};

	public void setTile(BlockEntity tile)
	{
		if(tile!=null)
		{
			onChange = tile::setChanged;
		}
		else
		{
			onChange = () -> {
			};
		}
	}

	public void setInventoryForUpdate(Container inv)
	{
		if(inv!=null)
		{
			onChange = inv::setChanged;
		}
		else
		{
			onChange = () -> {
			};
		}
	}

	@Override
	protected void onContentsChanged(int slot)
	{
		super.onContentsChanged(slot);
		onChange.run();
	}

	//TODO invalidate the LazyOptional objects after use?
	private final LazyOptional<IItemHandler> thisOpt = CapabilityUtils.constantOptional(this);

	@Nonnull
	@Override
	public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction facing)
	{
		return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.orEmpty(capability, thisOpt);
	}

	public NonNullList<ItemStack> getContainedItems()
	{
		return stacks;
	}
}
