package curiousarmorstands;

import curiousarmorstands.mixins.EntityRenderDispatcherAccessor;
import curiousarmorstands.mixins.LivingEntityRendererAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.ArmorStandEntityRenderer;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.EntityType;

public class CuriousArmorStandsClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		EntityRenderer<?> renderer = ((EntityRenderDispatcherAccessor)MinecraftClient.getInstance().getEntityRenderManager()).getRenderers().get(EntityType.ARMOR_STAND);
		if (renderer instanceof ArmorStandEntityRenderer) {
			//noinspection unchecked,rawtypes,rawtypes
			((LivingEntityRendererAccessor) renderer).callAddFeature(new CuriosLayer<>((ArmorStandEntityRenderer) renderer));
		}
	}
}
