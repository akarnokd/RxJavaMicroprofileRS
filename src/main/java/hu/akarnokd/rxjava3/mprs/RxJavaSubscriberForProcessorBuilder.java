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
import java.util.function.Function;

import org.eclipse.microprofile.reactive.streams.operators.*;
import org.eclipse.microprofile.reactive.streams.operators.spi.ReactiveStreamsEngine;
import org.reactivestreams.Subscriber;

import io.reactivex.rxjava3.core.FlowableSubscriber;

public final class RxJavaSubscriberForProcessorBuilder<T, U, R> implements SubscriberBuilder<T, R> {

    final Subscriber<T> front;
    
    final U source;
    
    final Function<U, CompletionStage<R>> toStage;

    public RxJavaSubscriberForProcessorBuilder(Subscriber<T> front, U source, 
            Function<U, CompletionStage<R>> toStage) {
        this.front = front;
        this.source = source;
        this.toStage = toStage;
    }
    
    @Override
    public CompletionSubscriber<T, R> build() {
        if (front instanceof FlowableSubscriber) {
            return new RxJavaCompletionFlowableSubscriberStage<>(front, toStage.apply(source));
        }
        return new RxJavaCompletionSubscriberStage<>(front, toStage.apply(source));
    }

    @Override
    public CompletionSubscriber<T, R> build(ReactiveStreamsEngine engine) {
        return build();
    }

}
