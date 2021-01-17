package emris.snakes.util;

import emris.snakes.util.ExceptionInterfaces.UnsafeRunnable;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

@FunctionalInterface
public interface Scheduler {

    @NotNull TimerTask schedule(
            final @NotNull UnsafeRunnable executable,
            final int periodMs);

    static @NotNull Scheduler fromTimer(final @NotNull Timer timer) {
        return (executable, period) -> {
            Logger.getLogger(Scheduler.class.getSimpleName()).info("Task scheduled");
            val task = new TimerTask() {

                @Override
                public void run() {
                    try {
                        executable.run();
                    } catch (final Exception e) {
                        Logger.getLogger(Scheduler.class.getSimpleName()).warning(
                                "Executable thrown an exception " + e.getClass().getSimpleName()
                                        + ": " + e.getMessage());
                        e.printStackTrace();
                        timer.cancel();
                    }
                }

                @Override
                public boolean cancel() {
                    Logger.getLogger(Scheduler.class.getSimpleName()).info("Task cancelled");
                    return super.cancel();
                }
            };
            timer.scheduleAtFixedRate(task, period, period);
            return task;
        };
    }
}


