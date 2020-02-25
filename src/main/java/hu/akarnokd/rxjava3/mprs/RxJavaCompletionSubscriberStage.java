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

import org.eclipse.microprofile.reactive.streams.operators.CompletionSubscriber;
import org.reactivestreams.*;

class RxJavaCompletionSubscriberStage<T, R> implements CompletionSubscriber<T, R> {

    final Subscriber<? super T> subscriber;
    
    final CompletionStage<R> stage;

    RxJavaCompletionSubscriberStage(Subscriber<? super T> subscriber, CompletionStage<R> stage) {
        this.subscriber = subscriber;
        this.stage = stage;
    }
    
    @Override
    public final void onSubscribe(Subscription s) {
        subscriber.onSubscribe(s);
    }

    @Override
    public final void onNext(T t) {
        subscriber.onNext(t);
    }

    @Override
    public final void onError(Throwable t) {
        subscriber.onError(t);
    }

    @Override
    public final void onComplete() {
        subscriber.onComplete();
    }

    @Override
    public final CompletionStage<R> getCompletion() {
        return stage;
    }

}
