/*
 * SillyCars
 * meow moew mod
 *
 * Copyright (c) 2026 SillyCars Authors
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program. If not, see
 * <https://www.gnu.org/licenses/>.
 */

package io.github.sillycarsmc.patch.mixins;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.moulberry.mixinconstraints.annotations.IfModLoaded;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.spongepowered.asm.mixin.Mixin;
import techreborn.TechReborn;
import techreborn.items.armor.AttributeModifierBuilder;
@IfModLoaded("techreborn")
@Mixin(AttributeModifierBuilder.class)
public class TechRebornNullPointerExceptionRRV {
	@WrapMethod(
		method = "equals"
	)
	private static boolean catchNullPointer(ItemAttributeModifiers attributes, ItemAttributeModifiers target, Operation<Boolean> original) {
		try {
			return original.call(attributes, target);
		} catch (NullPointerException e) {
			TechReborn.LOGGER.warn("Caught NullPointerException in AttributeModifierBuilder#equals", e);
			return false;
		}
	}
}
