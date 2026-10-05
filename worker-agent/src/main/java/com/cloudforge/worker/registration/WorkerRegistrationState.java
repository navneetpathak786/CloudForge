package com.cloudforge.worker.registration;

import org.springframework.stereotype.Component;

/**
 * Tracks whether this worker has successfully registered with the scheduler.
 * The heartbeat scheduler is started by Spring as soon as the context
 * refreshes, with no ordering guarantee relative to the registration
 * ApplicationRunner (which runs later, after startup, on the main thread) -
 * this flag lets heartbeat wait for registration to have actually succeeded.
 */
@Component
public class WorkerRegistrationState {

    private volatile boolean registered = false;

    public void markRegistered() {
        registered = true;
    }

    public boolean isRegistered() {
        return registered;
    }
}
