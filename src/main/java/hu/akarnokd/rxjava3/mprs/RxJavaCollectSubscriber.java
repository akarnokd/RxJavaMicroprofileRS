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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;
import java.util.stream.Collector;

import org.reactivestreams.*;

import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

final class RxJavaCollectSubscriber<T, A, R> 
extends AtomicReference<Subscription>
implements Subscriber<T> {

    private static final long serialVersionUID = -1718297417587197143L;

    final CompletableFuture<R> completable = new CompletableFuture<>();

    final Supplier<A> supplier;

    final BiConsumer<A, T> accumulator;

    final Function<A, R> finisher;
    
    A collection;

    RxJavaCollectSubscriber(Collector<T, A, R> collector) {
        supplier = collector.supplier();
        accumulator = collector.accumulator();
        finisher = collector.finisher();
    }
    
    @Override
    public void onSubscribe(Subscription s) {
        if (SubscriptionHelper.setOnce(this, s)) {
            try {
                collection = supplier.get();
            } catch (Throwable ex) {
                SubscriptionHelper.cancel(this);
                completable.completeExceptionally(ex);
                return;
            }
            s.request(Long.MAX_VALUE);
        }
    }

    @Override
    public void onNext(T t) {
        try {
            accumulator.accept(collection, t);
        } catch (Throwable ex) {
            SubscriptionHelper.cancel(this);
            completable.completeExceptionally(ex);
            return;
        }
    }

    @Override
    public void onError(Throwable t) {
        if (get() != SubscriptionHelper.CANCELLED) {
            lazySet(SubscriptionHelper.CANCELLED);
            collection = null;
            completable.completeExceptionally(t);
        } else {
            RxJavaPlugins.onError(t);
        }
    }

    @Override
    public void onComplete() {
        if (get() != SubscriptionHelper.CANCELLED) {
            lazySet(SubscriptionHelper.CANCELLED);
            A c = collection;
            collection = null;
            R r;
            try {
                r = finisher.apply(c); // null is allowed here for the completable's sake
            } catch (Throwable ex) {
                completable.completeExceptionally(ex);
                return;
            }
            completable.complete(r);
        }
    }

}
