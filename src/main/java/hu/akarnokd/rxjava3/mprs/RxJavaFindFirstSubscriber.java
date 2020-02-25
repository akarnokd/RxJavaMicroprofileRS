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

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.Subscription;

import io.reactivex.rxjava3.core.FlowableSubscriber;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

final class RxJavaFindFirstSubscriber<T> 
extends AtomicReference<Subscription>
implements FlowableSubscriber<T> {

    private static final long serialVersionUID = -1718297417587197143L;

    final CompletableFuture<Optional<T>> completable = new CompletableFuture<>();

    @Override
    public void onSubscribe(Subscription s) {
        if (SubscriptionHelper.setOnce(this, s)) {
            s.request(1);
        }
    }

    @Override
    public void onNext(T t) {
        if (SubscriptionHelper.cancel(this)) {
            completable.complete(Optional.of(t));
        }
    }

    @Override
    public void onError(Throwable t) {
        if (get() == SubscriptionHelper.CANCELLED) {
            lazySet(SubscriptionHelper.CANCELLED);
            completable.completeExceptionally(t);
        } else {
            RxJavaPlugins.onError(t);
        }
    }

    @Override
    public void onComplete() {
        if (get() == SubscriptionHelper.CANCELLED) {
            lazySet(SubscriptionHelper.CANCELLED);
            completable.complete(Optional.empty());
        }
    }

}
