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

import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.*;
import java.util.function.BiConsumer;

import org.reactivestreams.*;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava3.internal.util.*;

final class FlowableTakeUntilCompletionStage<T> extends Flowable<T> {

    final Flowable<T> source;

    final CompletionStage<?> completionStage;

    FlowableTakeUntilCompletionStage(Flowable<T> source, CompletionStage<?> completionStage) {
        this.source = source;
        this.completionStage = completionStage;
    }

    @Override
    protected void subscribeActual(
            @NonNull Subscriber<? super T> subscriber) {
        TakeUntilMainSubscriber<T> parent = new TakeUntilMainSubscriber<>(subscriber);
        AtomicReference<BiConsumer<Object, Throwable>> callback = parent.callback;

        subscriber.onSubscribe(parent);

        completionStage.whenComplete((v, e) -> {
            BiConsumer<Object, Throwable> h = callback.getAndSet(null);
            if (h != null) {
                h.accept(v, e);
            }
        });

        source.subscribe(parent);
    }

    static final class TakeUntilMainSubscriber<T> extends AtomicReference<Subscription>
    implements FlowableSubscriber<T>, Subscription, BiConsumer<Object, Throwable> {
     
        private static final long serialVersionUID = 5550970011608114920L;

        final Subscriber<? super T> downstream;

        final AtomicInteger wip;

        final AtomicThrowable error;

        final AtomicLong requested;
        
        final AtomicReference<BiConsumer<Object, Throwable>> callback;

        TakeUntilMainSubscriber(Subscriber<? super T> downstream) {
            this.downstream = downstream;
            this.wip = new AtomicInteger();
            this.error = new AtomicThrowable();
            this.requested = new AtomicLong();
            this.callback = new AtomicReference<>(this);
        }

        @Override
        public void onSubscribe(@NonNull Subscription s) {
            SubscriptionHelper.deferredSetOnce(this, requested, s);
        }

        @Override
        public void onNext(@NonNull T t) {
            HalfSerializer.onNext(downstream, t, wip, error);
        }

        @Override
        public void onError(Throwable t) {
            lazySet(SubscriptionHelper.CANCELLED);
            callback.getAndSet(null);
            HalfSerializer.onError(downstream, t, wip, error);
        }

        @Override
        public void onComplete() {
            lazySet(SubscriptionHelper.CANCELLED);
            callback.getAndSet(null);
            HalfSerializer.onComplete(downstream, wip, error);
        }

        @Override
        public void request(long n) {
            SubscriptionHelper.deferredRequest(this, requested, n);
        }

        @Override
        public void cancel() {
            callback.getAndSet(null);
            SubscriptionHelper.cancel(this);
        }

        @Override
        public void accept(Object t, Throwable u) {
            cancel();
            if (u != null) {
                HalfSerializer.onError(downstream, u, wip, error);
            } else {
                HalfSerializer.onComplete(downstream, wip, error);
            }
        }
    }
}
