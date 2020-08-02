/*
 * Copyright (C) 2020  Florens Pauwels
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package curiousarmorstands.mixins.client;

import curiousarmorstands.client.ArmorStandCuriosFeatureRenderer;
import net.minecraft.client.render.entity.ArmorStandEntityRenderer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.entity.EntityType;
import net.minecraft.resource.ReloadableResourceManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(EntityRenderDispatcher.class)
public class MixinEntityRenderDispatcher {

	@Shadow @Final private Map<EntityType<?>, EntityRenderer<?>> renderers;

	@Inject(method = "registerRenderers", at = @At(value = "RETURN"), require = 1)
	public void registerFeature(ItemRenderer itemRenderer, ReloadableResourceManager manager, CallbackInfo info) {
		EntityRenderer<?> armorStandRenderer = renderers.get(EntityType.ARMOR_STAND);
		if (armorStandRenderer instanceof ArmorStandEntityRenderer) {
			//noinspection unchecked,rawtypes,rawtypes
			((LivingEntityRendererAccessor) armorStandRenderer).callAddFeature(new ArmorStandCuriosFeatureRenderer<>((ArmorStandEntityRenderer) armorStandRenderer));
		}
	}
}
