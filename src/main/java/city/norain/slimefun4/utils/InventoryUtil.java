package city.norain.slimefun4.utils;

import com.molean.folia.adapter.Folia;
import java.util.LinkedList;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

@UtilityClass
public class InventoryUtil {
    public void openInventory(Player p, Inventory inventory) {
        if (p == null || inventory == null) {
            return;
        }

        if (Bukkit.isPrimaryThread()) {
            p.openInventory(inventory);
        } else {
            Folia.runSync(() -> p.openInventory(inventory), p);
        }
    }

    /**
     * Close inventory for all viewers.
     *
     * @param inventory {@link Inventory}
     */
    public void closeInventory(Inventory inventory) {
        if (inventory == null) {
            return;
        }

        if (Bukkit.isPrimaryThread()) {
            new LinkedList<>(inventory.getViewers()).forEach(HumanEntity::closeInventory);
        } else {
            Folia.runSync(
                    () -> new LinkedList<>(inventory.getViewers()).forEach(HumanEntity::closeInventory),
                    inventory.getLocation());
        }
    }

    public void closeInventory(Inventory inventory, Runnable callback) {
        closeInventory(inventory);

        if (Bukkit.isPrimaryThread()) {
            callback.run();
        } else {
            Folia.runSync(callback, inventory.getLocation());
        }
    }
}
