package curiousarmorstands;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class CuriousArmorStands implements ModInitializer {

    public static final String MOD_ID = "curious_armor_stands";

    @Override
    public void onInitialize() {
        UseEntityCallback.EVENT.register(CuriousArmorStands::onUseEntity);
    }

    @SubscribeEvent
    public static void onEntityTick(LivingEvent.LivingUpdateEvent event) {
        if (event.getEntityLiving() instanceof ArmorStandEntity) {
            CuriosApi.getCuriosHelper().getCuriosHandler((ArmorStandEntity) event.getEntity()).ifPresent(handler -> {
                if (handler instanceof CurioInventoryComponent) {
                    ((CurioInventoryComponent) handler).dropInvalidStacks();
                }
            });
        }
    }

    @SubscribeEvent
    public static void attachEntitiesCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof ArmorStandEntity) {
            event.addCapability(CuriosCapability.ID_INVENTORY, CurioInventoryCapability((ArmorStandEntity) event.getObject()));
        }
    }

    public static ActionResult onUseEntity(PlayerEntity player, World world, Hand hand, Entity target, EntityHitResult hitResult) {
        if (!(target instanceof ArmorStandEntity)) {
            return ActionResult.PASS;
        }
        ArmorStandEntity entity = (ArmorStandEntity) target;
        ItemStack stack = player.getStackInHand(hand);
        AtomicReference<ActionResult> isSuccess = new AtomicReference<>(ActionResult.PASS);

        if (!stack.isEmpty()) {
            Item item = stack.getItem();
            CuriosApi.getCuriosHelper().getCurio(stack).ifPresent(curio -> CuriosApi.getCuriosHelper().getCuriosHandler(entity).ifPresent(handler -> {

                if (!entity.world.isClient) {
                    Map<String, ICurioStacksHandler> curios = handler.getCurios();

                    for (Map.Entry<String, ICurioStacksHandler> entry : curios.entrySet()) {
                        IDynamicStackHandler stackHandler = entry.getValue().getCosmeticStacks();

                        for (int i = 0; i < stackHandler.size(); i++) {
                            ItemStack present = stackHandler.getStack(i);
                            Set<String> tags = CuriosApi.getCuriosHelper().getCurioTags(stack.getItem());
                            String id = entry.getKey();

                            if (present.isEmpty() && (tags.contains(id) || tags.contains("curio")) && curio.canEquip(id, entity)) {
                                stackHandler.setStack(i, stack.copy());
                                curio.playRightClickEquipSound(entity);
                                enableArmorStandArms(entity, item);

                                if (!player.isCreative()) {
                                    int count = stack.getCount();
                                    stack.decrement(count);
                                }

                                isSuccess.set(ActionResult.SUCCESS);
                            }
                        }
                    }
                } else {
                    isSuccess.set(ActionResult.CONSUME);
                }
            }));
        } else if (canUnequipCurio(hitResult, entity)) { // Unequip curios with empty hand
            CuriosApi.getCuriosHelper().getCuriosHandler(entity).ifPresent(handler -> {
                Map<String, ICurioStacksHandler> curios = handler.getCurios();

                for (Map.Entry<String, ICurioStacksHandler> entry : curios.entrySet()) {
                    IDynamicStackHandler stackHandler = entry.getValue().getCosmeticStacks();

                    for (int i = 0; i < stackHandler.size(); i++) {
                        ItemStack present = stackHandler.getStack(i);

                        if (!present.isEmpty()) {

                            if (!entity.world.isClient()) {
                                player.setStackInHand(hand, present);
                                stackHandler.setStack(i, ItemStack.EMPTY);
                            }
                            isSuccess.set(ActionResult.SUCCESS);
                        }
                    }
                }
            });
        }
        return isSuccess.get();
    }

    private static void enableArmorStandArms(ArmorStandEntity entity, Item curioItem) {
        if (CuriosApi.getCuriosHelper().getCurioTags(curioItem).contains("hands") || CuriosApi.getCuriosHelper().getCurioTags(curioItem).contains("ring") || CuriosApi.getCuriosHelper().getCurioTags(curioItem).contains("bracelet")) {
            CompoundTag compoundTag = entity.toTag(new CompoundTag());
            compoundTag.putBoolean("ShowArms", true);
            entity.fromTag(compoundTag);
        }
    }

    private static boolean canUnequipCurio(HitResult hitResult, ArmorStandEntity entity) {
        boolean isSmall = entity.isSmall();
        Vec3d localPos = hitResult.getPos().subtract(entity.getPos());
        double y = isSmall ? localPos.y * 2 : localPos.y;

        return !(entity.hasStackEquipped(EquipmentSlot.FEET) && y >= 0.1 && y < 0.1 + (isSmall ? 0.8 : 0.45))
                && !(entity.hasStackEquipped(EquipmentSlot.CHEST) && y >= 0.9 + (isSmall ? 0.3 : 0) && y < 0.9 + (isSmall ? 1 : 0.7))
                && !(entity.hasStackEquipped(EquipmentSlot.LEGS) && y >= 0.4 && y < 0.4 + (isSmall ? 1.0 : 0.8))
                && !(entity.hasStackEquipped(EquipmentSlot.HEAD) && y >= 1.6)
                && !entity.hasStackEquipped(EquipmentSlot.MAINHAND)
                && !entity.hasStackEquipped(EquipmentSlot.OFFHAND);
    }
}
