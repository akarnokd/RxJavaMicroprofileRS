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

import java.util.Objects;
import java.util.function.*;
import java.util.stream.Collector;

import org.reactivestreams.Subscription;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Runs a {@link Collector} but invokes its factory methods upon
 * onSubscribe because the TCK expects the {@link Collector#supplier()}'s
 * failure to cancel the flow. 
 * @param <T> the upstream value type
 * @param <A> the accumulator type
 * @param <R> the result type
 */
final class FlowableCollectCollectorDeferred<T, A, R> extends Single<R> {

    final Flowable<T> source;

    final Collector<T, A, R> collector;

    FlowableCollectCollectorDeferred(Flowable<T> source, Collector<T, A, R> collector) {
        this.source = source;
        this.collector = collector;
    }
    
    @Override
    protected void subscribeActual(
            @NonNull SingleObserver<? super R> observer) {
        
        Supplier<A> supplier = collector.supplier();
        BiConsumer<A, T> accumulator = collector.accumulator();
        Function<A, R> finisher = collector.finisher();
        
        source.subscribe(new CollectorSubscriber<>(observer, supplier, accumulator, finisher));
    }

    static final class CollectorSubscriber<T, A, R> implements FlowableSubscriber<T>, Disposable {

        final SingleObserver<? super R> downstream;
        
        final Supplier<A> supplier;

        final BiConsumer<A, T> accumulator;

        final Function<A, R> finisher;

        A collection;
        
        Subscription upstream;

        CollectorSubscriber(SingleObserver<? super R> downstream, Supplier<A> supplier,
                BiConsumer<A, T> accumulator, Function<A, R> finisher) {
            this.downstream = downstream;
            this.supplier = supplier;
            this.accumulator = accumulator;
            this.finisher = finisher;
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(upstream, s)) {
                upstream = s;
                downstream.onSubscribe(this);
                try {
                    collection = supplier.get();
                } catch (Throwable ex) {
                    s.cancel();
                    onError(ex);
                    return;
                }
                s.request(Long.MAX_VALUE);
            }
        }

        @Override
        public void onNext(T t) {
            if (!isDisposed()) {
                try {
                    accumulator.accept(collection, t);
                } catch (Throwable ex) {
                    upstream.cancel();
                    onError(ex);
                    return;
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            if (!isDisposed()) {
                upstream = SubscriptionHelper.CANCELLED;
                collection = null;
                downstream.onError(t);
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public void onComplete() {
            if (!isDisposed()) {
                upstream = SubscriptionHelper.CANCELLED;
                A c = collection;
                collection = null;
                R r;
                try {
                    r = Objects.requireNonNull(finisher.apply(c), "The finisher returned a null value");
                } catch (Throwable ex) {
                    downstream.onError(ex);
                    return;
                }
                downstream.onSuccess(r);
            }
        }

        @Override
        public void dispose() {
            upstream.cancel();
            upstream = SubscriptionHelper.CANCELLED;
        }

        @Override
        public boolean isDisposed() {
            return upstream == SubscriptionHelper.CANCELLED;
        }
    }
}
