package emris.snakes.util.ExceptionInterfaces;

@FunctionalInterface
public interface UnsafeRunnable {

    void run() throws Exception;
}
