package hu.akarnokd.rxjava3.mprs;

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.rxjava3.annotations.*;
import io.reactivex.rxjava3.internal.subscriptions.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.processors.FlowableProcessor;

/**
 * A processor that once subscribed by the downstream and connected to the upstream,
 * relays signals between the two.
 * @param <T> the element type of the flow
 */
final class DeferredProcessor<@NonNull T> extends FlowableProcessor<T> implements Subscription {

    final AtomicBoolean once = new AtomicBoolean();

    final AtomicReference<Subscription> upstreamDeferred = new AtomicReference<>();

    final AtomicReference<Subscription> upstreamActual = new AtomicReference<>();

    final AtomicLong requested = new AtomicLong();

    final AtomicReference<Subscriber<? super T>> downstream = new AtomicReference<>();

    final AtomicInteger wip = new AtomicInteger();

    Throwable error;

    @Override
    public void onSubscribe(Subscription s) {
        if (downstream.get() != null) {
            upstreamActual.lazySet(SubscriptionHelper.CANCELLED);
            SubscriptionHelper.deferredSetOnce(upstreamDeferred, requested, s);
        } else {
            if (SubscriptionHelper.setOnce(upstreamActual, s)) {
                if (downstream.get() != null) {
                    if (upstreamActual.compareAndSet(s, SubscriptionHelper.CANCELLED)) {
                        SubscriptionHelper.deferredSetOnce(upstreamDeferred, requested, s);
                    }
                }
            }
        }
    }

    @Override
    public void onNext(@NonNull T t) {
        downstream.get().onNext(t);
    }

    @Override
    public void onError(Throwable t) {
        error = t;
        // null -> DONE: deliver onError later
        // s -> DONE: deliver error now
        // CANCELED -> DONE_CANCELED: RxJavaPlugins.onError
        for (;;) {
            Subscriber<? super T> s = downstream.get();
            Subscriber<? super T> u;
            if (s == TerminatedSubscriber.CANCELED) {
                u = TerminatedSubscriber.DONE_CANCELED;
            } else
            if (s == TerminatedSubscriber.DONE_CANCELED) {
                break;
            } else {
                u = TerminatedSubscriber.DONE;
            }
            if (downstream.compareAndSet(s, u)) {
                if (s == TerminatedSubscriber.CANCELED) {
                    RxJavaPlugins.onError(t);
                } else
                if (s != null) {
                    s.onError(t);
                }
                break;
            }
        }
    }

    @Override
    public void onComplete() {
        // null -> DONE: deliver onComplete later
        // s -> DONE: deliver onComplete now
        // CANCELED -> DONE_CANCELED -> ignore onComplete
        for (;;) {
            Subscriber<? super T> s = downstream.get();
            Subscriber<? super T> u;
            if (s == TerminatedSubscriber.CANCELED) {
                u = TerminatedSubscriber.DONE_CANCELED;
            } else
            if (s == TerminatedSubscriber.DONE_CANCELED) {
                break;
            } else {
                u = TerminatedSubscriber.DONE;
            }
            if (downstream.compareAndSet(s, u)) {
                if (s != null && s != TerminatedSubscriber.CANCELED) {
                    s.onComplete();
                }
                break;
            }
        }
    }

    @Override
    public boolean hasSubscribers() {
        return downstream.get() != null;
    }

    @Override
    public boolean hasThrowable() {
        return isDone() && error != null;
    }

    @Override
    public boolean hasComplete() {
        return isDone() && error == null;
    }

    @Override
    public @Nullable Throwable getThrowable() {
        return isDone() ? error : null;
    }
    
    boolean isDone() {
        Subscriber<? super T> s = downstream.get();
        return s == TerminatedSubscriber.DONE || s == TerminatedSubscriber.DONE_CANCELED;
    }

    @Override
    protected void subscribeActual(
            @NonNull Subscriber<@NonNull ? super T> subscriber) {
        if (once.compareAndSet(false, true)) {
            subscriber.onSubscribe(this);
            if (downstream.compareAndSet(null, subscriber)) {
                Subscription s = upstreamActual.get();
                if (s != null 
                        && s != SubscriptionHelper.CANCELLED 
                        && upstreamActual.compareAndSet(s, SubscriptionHelper.CANCELLED)) {
                    SubscriptionHelper.deferredSetOnce(upstreamDeferred, requested, s);
                }
            } else {
                // CANCELED || DONE_CANCELED : ignore
                // DONE -> DONE_CANCELED : signal terminal event
                for (;;) {
                    Subscriber<? super T> s = downstream.get();
                    if (s == TerminatedSubscriber.CANCELED || s == TerminatedSubscriber.DONE_CANCELED) {
                        break;
                    }
                    if (downstream.compareAndSet(s, TerminatedSubscriber.DONE_CANCELED)) {
                        Throwable ex = error;
                        if (ex != null) {
                            subscriber.onError(ex);
                        } else {
                            subscriber.onComplete();
                        }
                        break;
                    }
                }
            }
        } else {
            EmptySubscription.error(new IllegalStateException("Only one Subscriber allowed"), subscriber);
        }
    }

    @Override
    public void request(long n) {
        SubscriptionHelper.deferredRequest(upstreamDeferred, requested, n);
    }

    @Override
    public void cancel() {
        // null -> CANCEL : do nothing
        // s -> CANCEL : do nothing
        // DONE -> DONE_CANCEL : RxJavaPlugins.onError if error != null
        for (;;) {
            Subscriber<? super T> s = downstream.get();
            Subscriber<? super T> u;
            if (s == TerminatedSubscriber.CANCELED || s == TerminatedSubscriber.DONE_CANCELED) {
                break;
            }
            if (s == TerminatedSubscriber.DONE) {
                u = TerminatedSubscriber.DONE_CANCELED;
            } else {
                u = TerminatedSubscriber.CANCELED;
            }
            if (downstream.compareAndSet(s, u)) {
                if (s == TerminatedSubscriber.DONE) {
                    Throwable ex = error;
                    if (ex != null) {
                        RxJavaPlugins.onError(ex);
                    }
                }
                break;
            }
        }
        
        SubscriptionHelper.cancel(upstreamActual);
        SubscriptionHelper.cancel(upstreamDeferred);
    }

    enum TerminatedSubscriber implements Subscriber<Object> {
        DONE, CANCELED, DONE_CANCELED;

        @Override
        public void onSubscribe(Subscription s) {
            // deliberately no-op
        }

        @Override
        public void onNext(Object t) {
            // deliberately no-op
        }

        @Override
        public void onError(Throwable t) {
            // deliberately no-op
        }

        @Override
        public void onComplete() {
            // deliberately no-op
        }
    }
}
