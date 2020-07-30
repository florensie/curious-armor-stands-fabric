package curiousarmorstands;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.Map;
import java.util.Set;

@Mod("curious_armor_stands")
@SuppressWarnings("unused")
public class CuriousArmorStands {

    public static final String MODID = "curious_armor_stands";

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class RegistryEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            EntityRenderer<?> renderer = Minecraft.getInstance().getRenderManager().renderers.get(EntityType.ARMOR_STAND);
            if (renderer instanceof ArmorStandRenderer) {
                ((ArmorStandRenderer) renderer).addLayer(new CuriosLayer<>((ArmorStandRenderer) renderer));
            }
        }
    }

    @Mod.EventBusSubscriber(modid = CuriousArmorStands.MODID)
    public static class Events {

        @SubscribeEvent
        public static void onEntityTick(LivingEvent.LivingUpdateEvent event) {
            if (event.getEntityLiving() instanceof ArmorStandEntity) {
                CuriosApi.getCuriosHelper().getCuriosHandler((ArmorStandEntity) event.getEntity()).ifPresent(handler -> {
                    if (handler instanceof CurioInventoryCapability.CurioInventoryWrapper) {
                        ((CurioInventoryCapability.CurioInventoryWrapper) handler).dropInvalidStacks();
                    }
                });
            }
        }

        @SubscribeEvent
        public static void attachEntitiesCapabilities(AttachCapabilitiesEvent<Entity> event) {
            if (event.getObject() instanceof ArmorStandEntity) {
                event.addCapability(CuriosCapability.ID_INVENTORY, CurioInventoryCapability.createProvider((ArmorStandEntity) event.getObject()));
            }
        }

        @SubscribeEvent
        public static void onEntityInteract(PlayerInteractEvent.EntityInteractSpecific event) {
            if (!(event.getTarget() instanceof ArmorStandEntity)) {
                return;
            }
            ArmorStandEntity entity = (ArmorStandEntity) event.getTarget();
            ItemStack stack = event.getItemStack();

            if (!stack.isEmpty()) {
                Item item = stack.getItem();
                CuriosApi.getCuriosHelper().getCurio(stack).ifPresent(curio -> CuriosApi.getCuriosHelper().getCuriosHandler(entity).ifPresent(handler -> {
                    if (!entity.world.isRemote) {
                        Map<String, ICurioStacksHandler> curios = handler.getCurios();
                        for (Map.Entry<String, ICurioStacksHandler> entry : curios.entrySet()) {
                            IDynamicStackHandler stackHandler = entry.getValue().getCosmeticStacks();
                            for (int i = 0; i < stackHandler.getSlots(); i++) {
                                ItemStack present = stackHandler.getStackInSlot(i);
                                Set<String> tags = CuriosApi.getCuriosHelper().getCurioTags(stack.getItem());
                                String id = entry.getKey();
                                if (present.isEmpty() && (tags.contains(id) || tags.contains("curio")) && curio.canEquip(id, entity) && curio.canRender(id, i, entity)) {
                                    stackHandler.setStackInSlot(i, stack.copy());
                                    curio.playRightClickEquipSound(entity);
                                    enableArmorStandArms(entity, item);
                                    if (!event.getPlayer().isCreative()) {
                                        int count = stack.getCount();
                                        stack.shrink(count);
                                    }
                                    event.setCancellationResult(ActionResultType.SUCCESS);
                                    event.setCanceled(true);
                                    return;
                                }
                            }
                        }
                    } else {
                        event.setCancellationResult(ActionResultType.CONSUME);
                        event.setCanceled(true);
                    }
                }));
            } else if (canUnEquipCurio(event.getLocalPos(), entity)){
                CuriosApi.getCuriosHelper().getCuriosHandler(entity).ifPresent(handler -> {
                    Map<String, ICurioStacksHandler> curios = handler.getCurios();
                    for (Map.Entry<String, ICurioStacksHandler> entry : curios.entrySet()) {
                        IDynamicStackHandler stackHandler = entry.getValue().getCosmeticStacks();
                        for (int i = 0; i < stackHandler.getSlots(); i++) {
                            ItemStack present = stackHandler.getStackInSlot(i);
                            Set<String> tags = CuriosApi.getCuriosHelper().getCurioTags(stack.getItem());
                            String id = entry.getKey();
                            if (!present.isEmpty()) {
                                if (!entity.world.isRemote()) {
                                    event.getPlayer().setHeldItem(event.getHand(), present);
                                    stackHandler.setStackInSlot(i, ItemStack.EMPTY);
                                }
                                event.setCancellationResult(ActionResultType.SUCCESS);
                                event.setCanceled(true);
                                return;
                            }
                        }
                    }
                });
            }
        }

        private static void enableArmorStandArms(ArmorStandEntity entity, Item curioItem) {
            if (CuriosApi.getCuriosHelper().getCurioTags(curioItem).contains("hands") || CuriosApi.getCuriosHelper().getCurioTags(curioItem).contains("ring") || CuriosApi.getCuriosHelper().getCurioTags(curioItem).contains("bracelet")) {
                CompoundNBT nbt = entity.writeWithoutTypeId(new CompoundNBT());
                nbt.putBoolean("ShowArms", true);
                entity.read(nbt);
            }
        }

        private static boolean canUnEquipCurio(Vector3d localPos, ArmorStandEntity entity) {
            boolean isSmall = entity.isSmall();
            double y = isSmall ? localPos.y * 2 : localPos.y;
            return !(entity.hasItemInSlot(EquipmentSlotType.FEET) && y >= 0.1 && y < 0.1 + (isSmall ? 0.8 : 0.45))
                    && !(entity.hasItemInSlot(EquipmentSlotType.CHEST) && y >= 0.9 + (isSmall ? 0.3 : 0) && y < 0.9 + (isSmall ? 1 : 0.7))
                    && !(entity.hasItemInSlot(EquipmentSlotType.LEGS) && y >= 0.4 && y < 0.4 + (isSmall ? 1.0 : 0.8))
                    && !(entity.hasItemInSlot(EquipmentSlotType.HEAD) && y >= 1.6)
                    && !entity.hasItemInSlot(EquipmentSlotType.MAINHAND)
                    && !entity.hasItemInSlot(EquipmentSlotType.OFFHAND);
        }
    }
}