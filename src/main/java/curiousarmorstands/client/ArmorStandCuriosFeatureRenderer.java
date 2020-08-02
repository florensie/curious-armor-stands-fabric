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

package curiousarmorstands.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.EntityHitResult;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class ArmorStandCuriosFeatureRenderer<M extends EntityModel<ArmorStandEntity>> extends FeatureRenderer<ArmorStandEntity, M> {

    public ArmorStandCuriosFeatureRenderer(FeatureRendererContext<ArmorStandEntity, M> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrixStack, VertexConsumerProvider vertexConsumers, int light, ArmorStandEntity armorStandEntity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
        matrixStack.push();
        List<ItemStack> stacks = new ArrayList<>();

        CuriosApi.getCuriosHelper().getCuriosHandler(armorStandEntity).ifPresent(handler -> handler.getCurios().forEach((id, stacksHandler) -> {
            IDynamicStackHandler stackHandler = stacksHandler.getStacks();
            IDynamicStackHandler cosmeticStackHandler = stacksHandler.getCosmeticStacks();

            for (int i = 0; i < stackHandler.size(); i++) {
                ItemStack stack = cosmeticStackHandler.getStack(i);

                if (stack.isEmpty() && stacksHandler.getRenders().get(i)) {
                    stack = stackHandler.getStack(i);
                }

                if (!stack.isEmpty()) {
                    stacks.add(stack);
                    int index = i;

                    CuriosApi.getCuriosHelper().getRenderableCurio(stack).ifPresent(curio -> {
                        matrixStack.push();
                        curio.render(id, index, matrixStack, vertexConsumers, light, armorStandEntity, limbAngle, limbDistance, tickDelta, animationProgress, headYaw, headPitch);
                        matrixStack.pop();
                    });
                }
            }
        }));

        if (MinecraftClient.getInstance().crosshairTarget instanceof EntityHitResult && ((EntityHitResult) MinecraftClient.getInstance().crosshairTarget).getEntity() == armorStandEntity) {
            matrixStack.scale(0.25F, 0.25F, 0.25F);
            matrixStack.translate((stacks.size() - 1) / 2F, -4, 0);
            matrixStack.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(180));

            for (ItemStack stack : stacks) {
                MinecraftClient.getInstance().getItemRenderer().renderItem(stack, ModelTransformation.Mode.FIXED, light, OverlayTexture.DEFAULT_UV, matrixStack, vertexConsumers);
                matrixStack.translate(-1, 0, 0);
            }
        }
        matrixStack.pop();
    }
}
