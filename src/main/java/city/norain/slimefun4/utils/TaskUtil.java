package city.norain.slimefun4.utils;

import com.molean.folia.adapter.SchedulerContext;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;

@UtilityClass
public class TaskUtil {
    @SneakyThrows
    public void runSyncMethod(Runnable runnable, SchedulerContext context) {
        context.runTask(Slimefun.instance(), runnable);
    }

    @SneakyThrows
    public <T> T runSyncMethod(Callable<T> callable, SchedulerContext context) {
        if (Bukkit.isPrimaryThread()) {
            return callable.call();
        } else {
            try {
                CompletableFuture<T> future = new CompletableFuture<>();
                context.runTask(Slimefun.instance(), () -> {
                    try {
                        future.complete(callable.call());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
                return future.get(1, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                Slimefun.logger().log(Level.WARNING, "Timeout when executing sync method", e);
                return null;
            }
        }
    }
}
