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

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava3.internal.util.BackpressureHelper;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Concatenates two sequences and still subscribes to the second
 * source if the first fails or the whole sequence gets canceled.
 * @param <T> the 
 */
final class FlowableConcatCanceling<T> extends Flowable<T> {

    final Publisher<T> source1;

    final Publisher<T> source2;

    FlowableConcatCanceling(Publisher<T> source1, Publisher<T> source2) {
        this.source1 = source1;
        this.source2 = source2;
    }

    @Override
    protected void subscribeActual(@NonNull Subscriber<? super T> subscriber) {
        ConcatCancelingSubscription<T> parent = new ConcatCancelingSubscription<>(subscriber, source1, source2);
        subscriber.onSubscribe(parent);
        parent.drain();
    }

    static final class ConcatCancelingSubscription<T> 
    extends AtomicInteger implements Subscription {

        private static final long serialVersionUID = -1593224722447706944L;

        final InnerSubscriber<T> inner1;
        
        final InnerSubscriber<T> inner2;
        
        final AtomicBoolean canceled;
        
        Publisher<T> source1;

        Publisher<T> source2;

        int index;

        ConcatCancelingSubscription(Subscriber<? super T> subscriber, 
                Publisher<T> source1, Publisher<T> source2) {
            this.inner1 = new InnerSubscriber<>(subscriber, this);
            this.inner2 = new InnerSubscriber<>(subscriber, this);
            this.canceled = new AtomicBoolean();
            this.source1 = source1;
            this.source2 = source2;
        }

        @Override
        public void request(long n) {
            SubscriptionHelper.deferredRequest(inner2, inner2.requested, n);
            SubscriptionHelper.deferredRequest(inner1, inner1.requested, n);
        }

        @Override
        public void cancel() {
            if (canceled.compareAndSet(false, true)) {
                SubscriptionHelper.cancel(inner1);
                SubscriptionHelper.cancel(inner2);
                drain();
            }
        }

        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }
            
            int missed = 1;
            for (;;) {

                if (index == 0) {
                    index = 1;
                    Publisher<T> source = source1;
                    source1 = null;
                    source.subscribe(inner1);
                } else if (index == 1) {
                    index = 2;
                    Publisher<T> source = source2;
                    source2 = null;
                    if (inner1.produced != 0L) {
                        BackpressureHelper.produced(inner2.requested, inner1.produced);
                    }
                    source.subscribe(inner2);
                } else if (index == 2) {
                    index = 3;
                    if (!canceled.get()) {
                        inner1.downstream.onComplete();
                    }
                }

                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }

        static final class InnerSubscriber<T> extends AtomicReference<Subscription>
        implements FlowableSubscriber<T> {

            private static final long serialVersionUID = 3029954591185720794L;

            final Subscriber<? super T> downstream;

            final ConcatCancelingSubscription<T> parent;

            final AtomicLong requested;

            long produced;

            InnerSubscriber(Subscriber<? super T> downstream, ConcatCancelingSubscription<T> parent) {
                this.downstream = downstream;
                this.parent = parent;
                this.requested = new AtomicLong();
            }

            @Override
            public void onSubscribe(Subscription s) {
                SubscriptionHelper.deferredSetOnce(this, requested, s);
            }

            @Override
            public void onNext(T t) {
                if (get() != SubscriptionHelper.CANCELLED) {
                    produced++;
                    downstream.onNext(t);
                }
            }

            @Override
            public void onError(Throwable t) {
                if (get() != SubscriptionHelper.CANCELLED) {
                    lazySet(SubscriptionHelper.CANCELLED);
                    downstream.onError(t);

                    parent.cancel();
                } else {
                    RxJavaPlugins.onError(t);
                }
            }

            @Override
            public void onComplete() {
                if (get() != SubscriptionHelper.CANCELLED) {
                    lazySet(SubscriptionHelper.CANCELLED);
                    parent.drain();
                }
            }
        }
    }
}
