/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.api.crafting;

import blusunrize.immersiveengineering.api.crafting.cache.CachedRecipeList;
import com.google.common.collect.Lists;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * @author BluSunrize - 23.03.2015
 * <br>
 * The recipe for the arc furnace
 */
public class ArcFurnaceRecipe extends MultiblockRecipe
{
	public static RecipeType<ArcFurnaceRecipe> TYPE;
	public static RegistryObject<IERecipeSerializer<ArcFurnaceRecipe>> SERIALIZER;

	public final IngredientWithSize input;
	public final IngredientWithSize[] additives;
	public final Lazy<NonNullList<ItemStack>> output;
	public final List<StackWithChance> secondaryOutputs;
	@Nonnull
	public final Lazy<ItemStack> slag;

	public String specialRecipeType;
	public static List<String> specialRecipeTypes = new ArrayList<>();
	public static final CachedRecipeList<ArcFurnaceRecipe> RECIPES = new CachedRecipeList<>(() -> TYPE, ArcFurnaceRecipe.class);

	public ArcFurnaceRecipe(
			ResourceLocation id,
			List<Lazy<ItemStack>> output, @Nonnull Lazy<ItemStack> slag, List<StackWithChance> secondaryOutputs,
			int time, int energy,
			IngredientWithSize input, IngredientWithSize... additives
	)
	{
		super(output.get(0), TYPE, id);
		this.output = Lazy.of(() -> output.stream()
				.map(Lazy::get)
				.collect(Collectors.toCollection(NonNullList::create))
		);
		this.secondaryOutputs = secondaryOutputs;
		this.input = input;
		this.slag = slag;
		setTimeAndEnergy(time, energy);
		this.additives = additives;

		List<IngredientWithSize> inputList = Lists.newArrayList(this.input);
		if(this.additives.length > 0)
			inputList.addAll(Lists.newArrayList(this.additives));
		setInputListWithSizes(inputList);
		this.outputList = this.output;
	}

	@Override
	protected IERecipeSerializer<ArcFurnaceRecipe> getIESerializer()
	{
		return SERIALIZER.get();
	}

	@Override
	public int getMultipleProcessTicks()
	{
		return 0;
	}

	public NonNullList<ItemStack> getBaseOutputs()
	{
		return this.output.get();
	}

	public NonNullList<ItemStack> generateActualOutput(ItemStack input, NonNullList<ItemStack> additives, long seed)
	{
		Random random = new Random(seed);
		var output = this.output.get();
		NonNullList<ItemStack> actualOutput = NonNullList.withSize(output.size(), ItemStack.EMPTY);
		for(int i = 0; i < output.size(); ++i)
			actualOutput.set(i, output.get(i).copy());
		for(StackWithChance secondary : secondaryOutputs)
		{
			if(secondary.chance() > random.nextFloat())
				continue;
			ItemStack remaining = secondary.stack().get();
			for(ItemStack existing : actualOutput)
				if(ItemHandlerHelper.canItemStacksStack(remaining, existing))
				{
					existing.grow(remaining.getCount());
					remaining = ItemStack.EMPTY;
					break;
				}
			if(!remaining.isEmpty())
				actualOutput.add(remaining);
		}
		return actualOutput;
	}

	public boolean matches(ItemStack input, NonNullList<ItemStack> additives)
	{
		if(this.input!=null&&this.input.test(input))
		{
			int[] consumed = getConsumedAdditives(additives, false);
			return consumed!=null;
		}

		return false;
	}

	public int[] getConsumedAdditives(NonNullList<ItemStack> additives, boolean consume)
	{
		int[] consumed = new int[additives.size()];
		for(IngredientWithSize add : this.additives)
			if(add!=null)
			{
				int addAmount = add.getCount();
				Iterator<ItemStack> it = additives.iterator();
				int i = 0;
				while(it.hasNext())
				{
					ItemStack query = it.next();
					if(!query.isEmpty())
					{
						if(add.test(query))
						{
							if(query.getCount() > addAmount)
							{
								query.shrink(addAmount);
								consumed[i] = addAmount;
								addAmount = 0;
							}
							else
							{
								addAmount -= query.getCount();
								consumed[i] = query.getCount();
								query.setCount(0);
							}
						}
						if(addAmount <= 0)
							break;
					}
					i++;
				}

				if(addAmount > 0)
				{
					for(int j = 0; j < consumed.length; j++)
						additives.get(j).grow(consumed[j]);
					return null;
				}
			}
		if(!consume)
			for(int j = 0; j < consumed.length; j++)
				additives.get(j).grow(consumed[j]);
		return consumed;
	}


	public boolean isValidInput(ItemStack stack)
	{
		return this.input!=null&&this.input.test(stack);
	}

	public boolean isValidAdditive(ItemStack stack)
	{
		for(IngredientWithSize add : additives)
			if(add!=null&&add.test(stack))
				return true;
		return false;
	}

	public ArcFurnaceRecipe setSpecialRecipeType(String type)
	{
		this.specialRecipeType = type;
		if(!specialRecipeTypes.contains(type))
			specialRecipeTypes.add(type);
		return this;
	}

	public static ArcFurnaceRecipe findRecipe(Level level, ItemStack input, NonNullList<ItemStack> additives)
	{
		for(ArcFurnaceRecipe recipe : RECIPES.getRecipes(level))
			if(recipe!=null&&recipe.matches(input, additives))
				return recipe;
		return null;
	}

	public static boolean isValidRecipeInput(Level level, ItemStack stack)
	{
		for(ArcFurnaceRecipe recipe : RECIPES.getRecipes(level))
			if(recipe!=null&&recipe.isValidInput(stack))
				return true;
		return false;
	}

	public static boolean isValidRecipeAdditive(Level level, ItemStack stack)
	{
		for(ArcFurnaceRecipe recipe : RECIPES.getRecipes(level))
			if(recipe!=null&&recipe.isValidAdditive(stack))
				return true;
		return false;
	}
}
