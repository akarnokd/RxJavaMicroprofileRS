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
import java.util.concurrent.atomic.AtomicBoolean;

import org.reactivestreams.*;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.FlowableSubscriber;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;

final class RxJavaInnerNullGuard<T> implements Publisher<T> {

    final Publisher<T> source;
    
    RxJavaInnerNullGuard(Publisher<T> source) {
        this.source = source;
    }
    
    @Override
    public void subscribe(Subscriber<? super T> s) {
        source.subscribe(new NullGuard<>(s));
    }

    static final class NullGuard<T> extends AtomicBoolean implements FlowableSubscriber<T> {

        private static final long serialVersionUID = -5247348177689779682L;

        final Subscriber<? super T> downstream;

        NullGuard(Subscriber<? super T> downstream) {
            this.downstream = downstream;
        }

        @Override
        public void onNext(@NonNull T t) {
            Objects.requireNonNull(t, "t is null");
            downstream.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            Objects.requireNonNull(t, "t is null");
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            downstream.onComplete();
        }

        @Override
        public void onSubscribe(@NonNull Subscription s) {
            if (compareAndSet(false, true)) {
                downstream.onSubscribe(s);
            } else {
                s.cancel();
                SubscriptionHelper.reportSubscriptionSet();
            }
        }
        
    }
}
