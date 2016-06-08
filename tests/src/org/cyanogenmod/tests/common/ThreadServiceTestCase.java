package org.cyanogenmod.tests.common;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.test.InstrumentationTestCase;
import android.test.ServiceTestCase;

/**
 * Tests a service in its own Thread.
 *
 *
 * <p>
 * The {@link ServiceTestCase} class creates the service in the same thread the
 * test is running. In consequence Handlers and other constructs that depend on
 * the fact that the service methods are always run on the <em>main thread</em>
 * won't work.
 * </p>
 *
 * <p>
 * To circumvent this, this test class creates a {@link HandlerThread} on setup
 * to simulate the main tread and provides helper constructs to ease the
 * communication between the Service and the test class :
 * </p>
 *
 * <ul>
 * <li>The {@link #runOnServiceThread(Runnable)} methods allows to run code on
 * the service pseudo-main thread.</li>
 * <li>The {@link #startService(boolean, ServiceRunnable)} mehod allows starting
 * the service in its own thread with some additional initialization code.</li>
 * </ul>
 *
 *
 * @author Antoine Martin
 *
 */
public abstract class ThreadServiceTestCase<T extends Service> extends ServiceTestCase<T> {

    /** Typical maximum wait time for something to happen on the service */
    public static final long WAIT_TIME = 5 * 1000;

    /*
     * This class provides final mutable values through indirection
     */
    static class Holder<H> {
        H value;
    }

    protected Handler serviceHandler;
    protected Looper serviceLooper;
    /*
     * Got to catch this again because of damn package visibility of
     * mServiceClass in base class.
     */
    protected Class<T> serviceClass;

    public ThreadServiceTestCase(Class<T> serviceClass) {
        super(serviceClass);
        this.serviceClass = serviceClass;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Setup service thread
        HandlerThread serviceThread = new HandlerThread("[" + serviceClass.getSimpleName() + "Thread]");
        serviceThread.start();
        serviceLooper = serviceThread.getLooper();
        serviceHandler = new Handler(serviceLooper);
    }

    @Override
    public void testServiceTestCaseSetUpProperly() throws Exception {
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        // teardown service thread
        if (serviceLooper != null)
            serviceLooper.quit();
        serviceHandler = null;
    }

    /**
     * Runs the specified runnable on the service tread and waits for its
     * completion.
     *
     * @see InstrumentationTestCase#runTestOnUiThread(Runnable)
     * @param r
     *            The runnable to run on the pseudo-main thread.
     */
    protected void runOnServiceThread(final Runnable r) {
        final CountDownLatch serviceSignal = new CountDownLatch(1);
        serviceHandler.post(new Runnable() {

            @Override
            public void run() {
                r.run();
                serviceSignal.countDown();
            }
        });

        try {
            serviceSignal.await();
        } catch (InterruptedException ie) {
            fail("The Service thread has been interrupted");
        }
    }

    /**
     * Runnable interface allowing service initialization personalization.
     *
     * @author Antoine Martin
     *
     */
    protected interface ServiceRunnable {
        public void run(Service service);
    }

    /**
     * Initialize the service in its own thread and returns it.
     *
     * @param bound
     *            if {@code true}, the service will be created as if it was
     *            bound by a client. if {@code false}, it will be created by a
     *            {@code startService} call.
     * @param r
     *            {@link ServiceRunnable} instance that will be called with the
     *            created service.
     * @return The created service.
     */
    protected T startService(final ServiceRunnable r) {
        final Holder<T> serviceHolder = new Holder<T>();

        // I want to create my service in its own 'Main thread'
        // So it can use its handler
        runOnServiceThread(new Runnable() {

            @Override
            public void run() {
                T service = null;
                startService(new Intent(getContext(), serviceClass));
                service = getService();
                if (r != null)
                    r.run(service);
                serviceHolder.value = service;
            }
        });

        return serviceHolder.value;
    }

    protected IBinder bindService(final ServiceRunnable r) {
        final Holder<IBinder> serviceHolder = new Holder<IBinder>();

        // I want to create my service in its own 'Main thread'
        // So it can use its handler
        runOnServiceThread(new Runnable() {

            @Override
            public void run() {
                T service = null;
                IBinder binder = bindService(new Intent(getContext(), serviceClass));
                service = getService();
                if (r != null)
                    r.run(service);
                serviceHolder.value = binder;
            }
        });

        return serviceHolder.value;
    }

    public static class ServiceSyncHelper {
        // The semaphore will wakeup clients
        protected final Semaphore semaphore = new Semaphore(0);

        /**
         * Waits for some response coming from the service.
         *
         * @param timeout
         *            The maximum time to wait.
         * @throws InterruptedException
         *             if the Thread is interrupted or reaches the timeout.
         */
        public synchronized void waitListener(long timeout) throws InterruptedException {
            if (!semaphore.tryAcquire(timeout, TimeUnit.MILLISECONDS))
                throw new InterruptedException();
        }
    }

}