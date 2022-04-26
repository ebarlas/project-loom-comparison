package loomtest;

import org.microhttp.LogEntry;
import org.microhttp.Logger;

public class NoOpLogger implements Logger {

    @Override
    public boolean enabled() {
        return false;
    }

    @Override
    public void log(LogEntry... entries) {

    }

    @Override
    public void log(Exception e, LogEntry... entries) {

    }

}
