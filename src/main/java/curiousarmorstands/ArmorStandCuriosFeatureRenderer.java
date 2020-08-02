package curiousarmorstands;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

@Environment(EnvType.CLIENT)
public class ArmorStandCuriosFeatureRenderer<M extends EntityModel<ArmorStandEntity>> extends FeatureRenderer<ArmorStandEntity, M> {

    public ArmorStandCuriosFeatureRenderer(FeatureRendererContext<ArmorStandEntity, M> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrixStack, VertexConsumerProvider vertexConsumers, int light, ArmorStandEntity armorStandEntity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
        matrixStack.push();
        CuriosApi.getCuriosHelper().getCuriosHandler(armorStandEntity).ifPresent(handler -> handler.getCurios().forEach((id, stacksHandler) -> {
            IDynamicStackHandler stackHandler = stacksHandler.getStacks();
            IDynamicStackHandler cosmeticStackHandler = stacksHandler.getCosmeticStacks();

            for (int i = 0; i < stackHandler.size(); i++) {
                ItemStack stack = cosmeticStackHandler.getStack(i);

                if (stack.isEmpty() && stacksHandler.getRenders().get(i)) {
                    stack = stackHandler.getStack(i);
                }

                if (!stack.isEmpty()) {
                    int index = i;

                    CuriosApi.getCuriosHelper().getRenderableCurio(stack).ifPresent(curio -> {
                        matrixStack.push();
                        curio.render(id, index, matrixStack, vertexConsumers, light, armorStandEntity, limbAngle, limbDistance, tickDelta, animationProgress, headYaw, headPitch);
                        matrixStack.pop();
                    });
                }
            }
        }));
        matrixStack.pop();
    }
}
