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

import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.*;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Execute an action before a terminal signal is emitted or before the
 * cancellation is routed to the upstream.
 * @param <T> the element type of the flow
 */
final class FlowableDoOnTerminateAndCancel<T> extends Flowable<T> {

    final Flowable<T> source;

    final Runnable action;
    
    FlowableDoOnTerminateAndCancel(Flowable<T> source, Runnable action) {
        this.source = source;
        this.action = action;
    }

    @Override
    protected void subscribeActual(
            @NonNull Subscriber<@NonNull ? super @NonNull T> subscriber) {
        source.subscribe(new OnTerminateSubscriber<>(subscriber, action));
    }
    
    static final class OnTerminateSubscriber<T> extends AtomicReference<Runnable>
    implements FlowableSubscriber<T>, Subscription {

        private static final long serialVersionUID = 661773972780071356L;

        final Subscriber<? super T> downstream;

        Subscription upstream;
        
        OnTerminateSubscriber(Subscriber<? super T> downstream, Runnable action) {
            super(action);
            this.downstream = downstream;
        }

        @Override
        public void onSubscribe(@NonNull Subscription s) {
            if (SubscriptionHelper.validate(upstream, s)) {
                upstream = s;
                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onNext(@NonNull T t) {
            downstream.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            Runnable run = getAndSet(Functions.EMPTY_RUNNABLE);
            try {
                run.run();
            } catch (Throwable ex) {
                downstream.onError(ex);
                return;
            }
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            Runnable run = getAndSet(Functions.EMPTY_RUNNABLE);
            try {
                run.run();
            } catch (Throwable ex) {
                downstream.onError(ex);
                return;
            }
            downstream.onComplete();
        }

        @Override
        public void request(long n) {
            upstream.request(n);
        }

        @Override
        public void cancel() {
            Runnable run = getAndSet(Functions.EMPTY_RUNNABLE);
            try {
                run.run();
            } catch (Throwable ex) {
                RxJavaPlugins.onError(ex);
                return;
            }
            upstream.cancel();
        }
        
    }
}
