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

import io.reactivex.rxjava3.core.Flowable;

final class FlowableProcessorBridge<T, R> extends Flowable<R> implements Processor<T, R> {

    final Subscriber<? super T> front;
    
    final Flowable<? extends R> tail;
    
    FlowableProcessorBridge(Subscriber<? super T> front, Flowable<? extends R> tail) {
        this.front = front;
        this.tail = tail;
    }

    @Override
    public void onSubscribe(Subscription s) {
        Objects.requireNonNull(s, "s is null");
        front.onSubscribe(s);
    }

    @Override
    public void onNext(T t) {
        Objects.requireNonNull(t, "t is null");
        front.onNext(t);
    }

    @Override
    public void onError(Throwable t) {
        Objects.requireNonNull(t, "t is null");
        front.onError(t);
    }

    @Override
    public void onComplete() {
        front.onComplete();
    }

    @Override
    public void subscribeActual(Subscriber<? super R> s) {
        tail.subscribe(s);
    }

}
