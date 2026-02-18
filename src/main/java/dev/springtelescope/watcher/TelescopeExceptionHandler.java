package dev.springtelescope.watcher;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Auto-configured {@link ControllerAdvice} that records all unhandled exceptions
 * via {@link TelescopeExceptionRecorder}. Uses lowest precedence so that
 * application-defined exception handlers take priority.
 * <p>
 * If this handler is reached, it records the exception and re-throws it
 * so that Spring's default error handling (BasicErrorController) processes
 * the response normally.
 * <p>
 * Override by registering your own {@code TelescopeExceptionHandler} bean.
 */
@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class TelescopeExceptionHandler {

    private final TelescopeExceptionRecorder recorder;

    public TelescopeExceptionHandler(TelescopeExceptionRecorder recorder) {
        this.recorder = recorder;
    }

    @ExceptionHandler(Exception.class)
    public void handleException(Exception ex) throws Exception {
        recorder.record(ex);
        throw ex;
    }
}
