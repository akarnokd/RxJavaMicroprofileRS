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

import java.util.concurrent.*;

import org.eclipse.microprofile.reactive.streams.operators.CompletionSubscriber;
import org.reactivestreams.*;

class RxJavaCompletionSubscriber<T> 
implements CompletionSubscriber<T, Void>, Subscription {

    final Subscriber<? super T> subscriber;

    final CompletableFuture<Void> complete;

    Subscription upstream;

    RxJavaCompletionSubscriber(Subscriber<? super T> subscriber) {
        this.subscriber = subscriber;
        this.complete = new CompletableFuture<>();
    }

    @Override
    public final CompletionStage<Void> getCompletion() {
        return complete;
    }

    @Override
    public final void request(long n) {
        Subscription s = upstream;
        if (s != null) {
            s.request(n);
        }
    }

    @Override
    public final void cancel() {
        Subscription s = upstream;
        if (s != null) {
            upstream = null;
            s.cancel();
            complete.cancel(true);
        }
    }

    @Override
    public final void onSubscribe(Subscription s) {
        upstream = s;
        subscriber.onSubscribe(this);
    }

    @Override
    public final void onNext(T t) {
        subscriber.onNext(t);
    }

    @Override
    public final void onError(Throwable t) {
        subscriber.onError(t);
        complete.completeExceptionally(t);
    }

    @Override
    public final void onComplete() {
        subscriber.onComplete();
        complete.complete(null);
    }

}
