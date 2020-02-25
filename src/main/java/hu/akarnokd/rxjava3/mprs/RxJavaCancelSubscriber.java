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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.Subscription;

import io.reactivex.rxjava3.core.FlowableSubscriber;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

final class RxJavaCancelSubscriber<T> 
extends AtomicReference<Subscription>
implements FlowableSubscriber<T> {

    private static final long serialVersionUID = -1718297417587197143L;

    final CompletableFuture<T> completable = new CompletableFuture<>();
    
    @Override
    public void onSubscribe(Subscription s) {
        if (SubscriptionHelper.setOnce(this, s)) {
            SubscriptionHelper.cancel(this);
            completable.complete(null);
        }
    }

    @Override
    public void onNext(T t) {
        // never invoked
    }

    @Override
    public void onError(Throwable t) {
        RxJavaPlugins.onError(t);
    }

    @Override
    public void onComplete() {
        // ignored
    }

}
