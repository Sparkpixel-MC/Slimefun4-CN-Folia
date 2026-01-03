package io.github.thebusybiscuit.slimefun4.implementation.items.multiblocks;

import com.molean.folia.adapter.Folia;
import io.github.bakedlibs.dough.items.ItemUtils;
import io.github.thebusybiscuit.slimefun4.api.events.MultiBlockCraftEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.services.sounds.SoundEffect;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.backpacks.SlimefunBackpack;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import io.papermc.lib.PaperLib;
import java.util.List;
import javax.annotation.ParametersAreNonnullByDefault;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Dispenser;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class EnhancedCraftingTable extends AbstractCraftingTable {

    @ParametersAreNonnullByDefault
    public EnhancedCraftingTable(ItemGroup itemGroup, SlimefunItemStack item) {
        super(
                itemGroup,
                item,
                new ItemStack[] {
                    null,
                    null,
                    null,
                    null,
                    new ItemStack(Material.CRAFTING_TABLE),
                    null,
                    null,
                    new ItemStack(Material.DISPENSER),
                    null
                },
                BlockFace.SELF);
    }

    @Override
    public void onInteract(Player p, Block b) {
        Block possibleDispenser = b.getRelative(BlockFace.DOWN);
        BlockState state = PaperLib.getBlockState(possibleDispenser, false).getState();

        if (state instanceof Dispenser dispenser) {
            Inventory inv = dispenser.getInventory();
            List<ItemStack[]> inputs = RecipeType.getRecipeInputList(this);

            for (ItemStack[] input : inputs) {
                if (isCraftable(inv, input)) {
                    ItemStack output =
                            RecipeType.getRecipeOutputList(this, input).clone();
                    MultiBlockCraftEvent event = new MultiBlockCraftEvent(p, this, input, output);

                    Folia.getPluginManager().ce(event);
                    if (!event.isCancelled() && SlimefunUtils.canPlayerUseItem(p, output, true)) {
                        craft(inv, possibleDispenser, p, b, event.getOutput());
                    }

                    return;
                }
            }

            if (inv.isEmpty()) {
                Slimefun.getLocalization().sendMessage(p, "machines.inventory-empty", true);
            } else {
                Slimefun.getLocalization().sendMessage(p, "machines.pattern-not-found", true);
            }
        }
    }

    private void craft(Inventory inv, Block dispenser, Player p, Block b, ItemStack output) {
        Inventory fakeInv = createVirtualInventory(inv);
        Inventory outputInv = findOutputInventory(output, dispenser, inv, fakeInv);

        if (outputInv != null) {
            SlimefunItem sfItem = SlimefunItem.getByItem(output);

            var waitCallback = false;
            if (sfItem instanceof SlimefunBackpack backpack) {
                waitCallback = upgradeBackpack(p, inv, backpack, output, () -> {
                    SoundEffect.ENHANCED_CRAFTING_TABLE_CRAFT_SOUND.playAt(b);
                    outputInv.addItem(output);
                });
            }

            for (int j = 0; j < 9; j++) {
                ItemStack item = inv.getContents()[j];

                if (item != null && item.getType() != Material.AIR) {
                    ItemUtils.consumeItem(item, true);
                }
            }

            if (!waitCallback) {
                SoundEffect.ENHANCED_CRAFTING_TABLE_CRAFT_SOUND.playAt(b);
                outputInv.addItem(output);
            }
        } else {
            Slimefun.getLocalization().sendMessage(p, "machines.full-inventory", true);
        }
    }

    private boolean isCraftable(Inventory inv, ItemStack[] recipe) {
        for (int j = 0; j < inv.getContents().length; j++) {
            ItemStack item = inv.getContents()[j];
            ItemStack recipeItem = recipe[j];

            // 创建物品副本以移除消失诅咒和保险相关描述，避免影响配方匹配
            ItemStack itemToCompare = item;
            if (item != null && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    boolean needsClone = false;

                    // 检查是否有消失诅咒
                    if (meta.hasEnchant(org.bukkit.enchantments.Enchantment.VANISHING_CURSE)) {
                        needsClone = true;
                    }

                    // 检查是否有保险相关的描述
                    if (meta.hasLore()) {
                        for (String line : meta.getLore()) {
                            if (line.contains("保险等级") ||
                                line.contains("剩余次数") ||
                                line.contains("保险已失效") ||
                                line.contains("1级保险") ||
                                line.contains("2级保险") ||
                                line.contains("效果: 死亡时") ||
                                line.contains("保单号:") ||
                                line.contains("生效时间:") ||
                                line.contains("到期时间:") ||
                                line.contains("保单信息") ||
                                line.contains("此物品受保险系统保护") ||
                                line.contains("无保险保护")) {
                                needsClone = true;
                                break;
                            }
                        }
                    }

                    if (needsClone) {
                        itemToCompare = item.clone();
                        ItemMeta clonedMeta = itemToCompare.getItemMeta();

                        // 移除消失诅咒
                        if (clonedMeta.hasEnchant(org.bukkit.enchantments.Enchantment.VANISHING_CURSE)) {
                            clonedMeta.removeEnchant(org.bukkit.enchantments.Enchantment.VANISHING_CURSE);
                        }

                        // 移除保险相关的描述
                        if (clonedMeta.hasLore()) {
                            java.util.List<String> lore = new java.util.ArrayList<>(clonedMeta.getLore());
                            lore.removeIf(line ->
                                line.contains("保险等级") ||
                                line.contains("剩余次数") ||
                                line.contains("保险已失效") ||
                                line.contains("1级保险") ||
                                line.contains("2级保险") ||
                                line.contains("效果: 死亡时") ||
                                line.contains("保单号:") ||
                                line.contains("生效时间:") ||
                                line.contains("到期时间:") ||
                                line.contains("保单信息") ||
                                line.contains("此物品受保险系统保护") ||
                                line.contains("无保险保护")
                            );
                            if (lore.isEmpty()) {
                                clonedMeta.setLore(null);
                            } else {
                                clonedMeta.setLore(lore);
                            }
                        }

                        itemToCompare.setItemMeta(clonedMeta);
                    }
                }
            }

            if (!SlimefunUtils.isItemSimilar(itemToCompare, recipeItem, true, true, false, false)) {
                if (SlimefunItem.getByItem(recipeItem) instanceof SlimefunBackpack) {
                    if (!SlimefunUtils.isItemSimilar(itemToCompare, recipeItem, false, true, false, false)) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        }
        return true;
    }
}
