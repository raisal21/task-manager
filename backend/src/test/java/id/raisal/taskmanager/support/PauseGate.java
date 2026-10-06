package id.raisal.taskmanager.support;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * A pause point for race tests (section 7.10). Test sources only: the production code has no test gate.
 * A repository wrapper calls reached() after a method returns. If the gate is armed for that method,
 * the calling thread stops there until the test calls release(). All waits have a time limit, and there are no sleeps.
 */
public final class PauseGate {

    private static final Duration MAXIMUM_PAUSE = Duration.ofSeconds(10);

    private volatile String armedMethod;
    private volatile CountDownLatch paused = new CountDownLatch(1);
    private volatile CountDownLatch released = new CountDownLatch(1);

    /** Stop the next call that returns from this repository method. The gate stops only one call. */
    public void arm(String methodName) {
        paused = new CountDownLatch(1);
        released = new CountDownLatch(1);
        armedMethod = methodName;
    }

    /** Wait until a call has reached the pause point. False if it did not come in time. */
    public boolean awaitPaused(Duration timeout) throws InterruptedException {
        return paused.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    /** Let the stopped call continue. It is safe to call this more than once, also in test cleanup. */
    public void release() {
        released.countDown();
    }

    private void reached(String methodName) {
        if (!methodName.equals(armedMethod)) {
            return;
        }
        armedMethod = null;
        paused.countDown();
        try {
            if (!released.await(MAXIMUM_PAUSE.toMillis(), TimeUnit.MILLISECONDS)) {
                throw new IllegalStateException("The pause gate was not released in time.");
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted at the pause gate.", interrupted);
        }
    }

    /** A wrapper that calls the target, and then stops at the gate if the gate is armed for that method. */
    public static <T> T wrap(Class<T> type, T target, PauseGate gate) {
        Object proxy = Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (instance, method, args) -> {
            Object result;
            try {
                result = method.invoke(target, args);
            } catch (InvocationTargetException exception) {
                throw exception.getCause();
            }
            gate.reached(method.getName());
            return result;
        });
        return type.cast(proxy);
    }
}
