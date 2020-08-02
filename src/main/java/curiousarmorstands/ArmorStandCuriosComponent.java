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

package curiousarmorstands;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.collection.DefaultedList;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.CuriosComponent;
import top.theillusivec4.curios.api.type.component.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.*;

public class ArmorStandCuriosComponent implements ICuriosItemHandler {

    Map<String, ICurioStacksHandler> curios = new LinkedHashMap<>();
    DefaultedList<ItemStack> invalidStacks = DefaultedList.of();
    LivingEntity wearer;

    ArmorStandCuriosComponent(LivingEntity entity) {
        wearer = entity;
        reset();
    }

    @Override
    public void reset() {
        if (!wearer.getEntityWorld().isClient()) {
            curios.clear();
            invalidStacks.clear();
            CuriosApi.getSlotHelper().createSlots().forEach((slotType, stacksHandler) -> curios.put(slotType.getIdentifier(), stacksHandler));
        }
    }

    @Override
    public int getSlots() {
        return curios.values().stream().mapToInt(ICurioStacksHandler::getSlots).sum();
    }

    @Override
    public Set<String> getLockedSlots() {
        return Collections.emptySet();
    }

    @Override
    public Optional<ICurioStacksHandler> getStacksHandler(String identifier) {
        return Optional.ofNullable(curios.get(identifier));
    }

    @Override
    public Map<String, ICurioStacksHandler> getCurios() {
        return Collections.unmodifiableMap(curios);
    }

    @Override
    public void setCurios(Map<String, ICurioStacksHandler> curios) {
        this.curios = curios;
    }

    @Override
    public void unlockSlotType(String identifier, int amount, boolean visible, boolean cosmetic) { }

    @Override
    public void lockSlotType(String identifier) { }

    @Override
    public void growSlotType(String identifier, int amount) {
        if (amount > 0) {
            getStacksHandler(identifier).ifPresent(stackHandler -> stackHandler.grow(amount));
        }
    }

    @Override
    public void shrinkSlotType(String identifier, int amount) {
        if (amount > 0) {
            this.getStacksHandler(identifier).ifPresent(stackHandler -> {
                int toShrink = Math.min(stackHandler.getSlots() - 1, amount);
                loseStacks(stackHandler.getStacks(), identifier, toShrink);
                stackHandler.shrink(amount);
            });
        }
    }

    @Override
    public LivingEntity getWearer() {
        return wearer;
    }

    @Override
    public void loseInvalidStack(ItemStack stack) {
        invalidStacks.add(stack);
    }

    @Override
    public void handleInvalidStacks() {
        if (this.wearer != null && !this.invalidStacks.isEmpty()) {
            this.invalidStacks.forEach(drop -> dropStack(wearer, drop));
            this.invalidStacks = DefaultedList.of();
        }
    }

    private void loseStacks(IDynamicStackHandler stackHandler, String identifier, int amount) {
        if (wearer != null && !wearer.getEntityWorld().isClient()) {
            List<ItemStack> drops = new ArrayList<>();

            for (int i = stackHandler.size() - amount; i < stackHandler.size(); i++) {
                ItemStack stack = stackHandler.getStack(i);
                drops.add(stackHandler.getStack(i));

                if (!stack.isEmpty()) {
                    wearer.getAttributes().removeModifiers(CuriosApi.getCuriosHelper().getAttributeModifiers(identifier, stack));
                    int index = i;
                    CuriosApi.getCuriosHelper().getCurio(stack).ifPresent(curio -> curio.onUnequip(identifier, index, wearer));
                }
                stackHandler.setStack(i, ItemStack.EMPTY);
            }
            drops.forEach(drop -> dropStack(wearer, drop));
        }
    }

    private void dropStack(LivingEntity entity, ItemStack stack) {
        if (!entity.world.isClient) {
            ItemEntity itemEntity = new ItemEntity(entity.world, entity.getX(), entity.getY() + 0.5, entity.getZ(), stack);
            entity.world.spawnEntity(itemEntity);
        }
    }

    @Override
    @NotNull
    public Entity getEntity() {
        return wearer;
    }

    @Override
    // TODO: Does this work?
    public void fromTag(CompoundTag compoundTag) {
        CuriosComponent.INVENTORY.get(this).fromTag(compoundTag);
    }

    @Override
    @NotNull
    // TODO: Does this work?
    public CompoundTag toTag(CompoundTag compoundTag) {
        return CuriosComponent.INVENTORY.get(this).toTag(compoundTag);
    }
}
