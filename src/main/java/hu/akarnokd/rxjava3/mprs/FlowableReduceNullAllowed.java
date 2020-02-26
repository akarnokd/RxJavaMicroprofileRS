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

import java.util.function.BinaryOperator;

import org.reactivestreams.Subscription;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * A seeded reducer implementation that allows {@code null} as the identity and
 * signals {@code onComplete} if the accumulation results in {@code null}.
 * @param <T> the element type of the flow and the accumulation
 */
final class FlowableReduceNullAllowed<T> extends Maybe<T> {

    final Flowable<T> source;

    final T identity;

    final BinaryOperator<T> accumulator;

    FlowableReduceNullAllowed(Flowable<T> source, T identity, BinaryOperator<T> accumulator) {
        this.source = source;
        this.identity = identity;
        this.accumulator = accumulator;
    }

    @Override
    protected void subscribeActual(@NonNull MaybeObserver<? super T> observer) {
        source.subscribe(new ReduceNullAllowedSubscriber<>(observer, identity, accumulator));
    }

    static final class ReduceNullAllowedSubscriber<T> implements FlowableSubscriber<T>, Disposable {

        final MaybeObserver<? super T> downstream;

        final BinaryOperator<T> accumulator;

        T current;

        Subscription upstream;

        boolean once;

        ReduceNullAllowedSubscriber(MaybeObserver<? super T> downstream, T identity, BinaryOperator<T> accumulator) {
            this.downstream = downstream;
            this.current = identity;
            this.accumulator = accumulator;
        }

        @Override
        public void onSubscribe(@NonNull Subscription s) {
            if (SubscriptionHelper.validate(upstream, s)) {
                upstream = s;
                downstream.onSubscribe(this);
                s.request(Long.MAX_VALUE);
            }
        }

        @Override
        public void onNext(@NonNull T t) {
            if (isDisposed()) {
                try {
                    current = accumulator.apply(current, t);
                } catch (Throwable ex) {
                    upstream.cancel();
                    onError(ex);
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            if (isDisposed()) {
                upstream = SubscriptionHelper.CANCELLED;
                current = null;
                downstream.onError(t);
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public void onComplete() {
            if (isDisposed()) {
                upstream = SubscriptionHelper.CANCELLED;
                T c = current;
                current = null;
                if (c == null) {
                    downstream.onComplete();
                } else {
                    downstream.onSuccess(c);
                }
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
