package emris.snakes.util;

import org.jetbrains.annotations.NotNull;

import java.util.Timer;
import java.util.logging.Logger;


public class LoggedTimer extends Timer {

    private static final @NotNull Logger logger = Logger.getLogger(LoggedTimer.class.getSimpleName());

    public LoggedTimer() {
        super();
    }

    public LoggedTimer(final boolean isDaemon) {
        super(isDaemon);
    }

    @Override
    public void cancel() {
        logger.info(Thread.currentThread().getName() + " cancelled the timer");
        super.cancel();
    }
}
