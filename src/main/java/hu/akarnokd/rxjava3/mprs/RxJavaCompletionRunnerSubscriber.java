package hu.akarnokd.rxjava3.mprs;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.microprofile.reactive.streams.operators.CompletionRunner;
import org.eclipse.microprofile.reactive.streams.operators.spi.ReactiveStreamsEngine;
import org.reactivestreams.*;

import io.reactivex.rxjava3.core.Flowable;

final class RxJavaCompletionRunnerSubscriber<T> 
extends AtomicBoolean
implements CompletionRunner<Void>, Subscriber<T>, Subscription {

    private static final long serialVersionUID = 6640182020510123315L;

    final Flowable<T> source;

    final Subscriber<? super T> subscriber;

    CompletableFuture<Void> complete;

    Subscription upstream;

    RxJavaCompletionRunnerSubscriber(Flowable<T> source, Subscriber<? super T> subscriber) {
        this.source = source;
        this.subscriber = subscriber;
    }
    
    @Override
    public CompletionStage<Void> run() {
        CompletableFuture<Void> cf = new CompletableFuture<>();
        if (compareAndSet(false, true)) {
            this.complete = cf;
            source.subscribe(this);
        } else {
            cf.completeExceptionally(new IllegalStateException("This runner can be run only once"));
        }
        return cf;
    }

    @Override
    public CompletionStage<Void> run(ReactiveStreamsEngine engine) {
        // FIXME should we unroll?
        return run();
    }

    @Override
    public void request(long n) {
        Subscription s = upstream;
        if (s != null) {
            s.request(n);
        }
    }

    @Override
    public void cancel() {
        Subscription s = upstream;
        if (s != null) {
            upstream = null;
            s.cancel();
            complete.cancel(true);
        }
    }

    @Override
    public void onSubscribe(Subscription s) {
        upstream = s;
        subscriber.onSubscribe(this);
    }

    @Override
    public void onNext(T t) {
        subscriber.onNext(t);
    }

    @Override
    public void onError(Throwable t) {
        subscriber.onError(t);
        complete.completeExceptionally(t);
    }

    @Override
    public void onComplete() {
        subscriber.onComplete();
        complete.complete(null);
    }

}
