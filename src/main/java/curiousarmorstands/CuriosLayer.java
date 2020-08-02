package curiousarmorstands;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

@Environment(EnvType.CLIENT)
public class CuriosLayer<T extends LivingEntity, M extends EntityModel<T>> extends FeatureRenderer<T, M> {

    public CuriosLayer(FeatureRendererContext<T, M> renderer) {
        super(renderer);
    }

    @Override
    public void render(MatrixStack matrixStack, VertexConsumerProvider renderTypeBuffer, int light, T livingEntity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        matrixStack.push();
        CuriosApi.getCuriosHelper().getCuriosHandler(livingEntity).ifPresent(handler -> handler.getCurios().forEach((id, stacksHandler) -> {
            IDynamicStackHandler stackHandler = stacksHandler.getStacks();
            IDynamicStackHandler cosmeticStacksHandler = stacksHandler.getCosmeticStacks();

            for (int i = 0; i < stackHandler.size(); i++) {
                ItemStack stack = cosmeticStacksHandler.getStack(i);

                if (stack.isEmpty() && stacksHandler.getRenders().get(i)) {
                    stack = stackHandler.getStack(i);
                }

                if (!stack.isEmpty()) {
                    int index = i;

                    CuriosApi.getCuriosHelper().getRenderableCurio(stack).ifPresent(curio -> {
                        matrixStack.push();
                        curio.render(id, index, matrixStack, renderTypeBuffer, light, livingEntity, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch);
                        matrixStack.pop();
                    });
                }
            }
        }));
        matrixStack.pop();
    }
}
