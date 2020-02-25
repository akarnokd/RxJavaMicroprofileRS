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

import org.reactivestreams.*;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Flowable;

/**
 * Implements a Processor over the Flowable API that simply stores one
 * downstream subscriber and relays signals to it directly.
 * <p>
 * Basically it gives access to the Subscriber-operator chain for a synchronous
 * subscriber chained onto it.
 * @param <T> the element type of the input and output flows
 */
final class BasicProcessor<T> extends Flowable<T> implements Processor<T, T> {

    Subscriber<? super T> downstream;

    @Override
    public void onSubscribe(Subscription s) {
        downstream.onSubscribe(s);
    }

    @Override
    public void onNext(T t) {
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
    protected void subscribeActual(@NonNull Subscriber<@NonNull ? super @NonNull T> subscriber) {
        this.downstream = subscriber;
    }
}
