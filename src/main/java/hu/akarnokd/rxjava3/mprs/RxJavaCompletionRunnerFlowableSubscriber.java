/*
 * Copyright 2020 David Karnok
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hu.akarnokd.rxjava3.mprs;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.microprofile.reactive.streams.operators.CompletionRunner;
import org.eclipse.microprofile.reactive.streams.operators.spi.ReactiveStreamsEngine;
import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;

final class RxJavaCompletionRunnerFlowableSubscriber<T> 
extends AtomicBoolean
implements CompletionRunner<Void>, FlowableSubscriber<T>, Subscription {

    private static final long serialVersionUID = 6640182020510123315L;

    final Flowable<T> source;

    final Subscriber<? super T> subscriber;

    CompletableFuture<Void> complete;

    Subscription upstream;

    RxJavaCompletionRunnerFlowableSubscriber(Flowable<T> source, Subscriber<? super T> subscriber) {
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
