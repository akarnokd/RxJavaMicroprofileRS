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

import org.eclipse.microprofile.reactive.streams.operators.*;
import org.eclipse.microprofile.reactive.streams.operators.spi.*;
import org.reactivestreams.Subscriber;

import io.reactivex.rxjava3.core.FlowableSubscriber;

final class RxJavaSubscriberBuilder<T> implements SubscriberBuilder<T, Void>, ToGraphable {

    final Subscriber<? super T> subscriber;
    
    final RxJavaGraphBuilder graph;
    
    public RxJavaSubscriberBuilder(Subscriber<? super T> subscriber) {
        this.subscriber = subscriber;
        this.graph = RxJavaMicroprofilePlugins.buildGraph() ? new RxJavaListGraphBuilder() : RxJavaNoopGraphBuilder.INSTANCE;
    }
    
    @Override
    public CompletionSubscriber<T, Void> build() {
        if (subscriber instanceof FlowableSubscriber) {
            return new RxJavaCompletionFlowableSubscriber<>(subscriber);
        }
        return new RxJavaCompletionSubscriber<>(subscriber);
    }

    @Override
    public CompletionSubscriber<T, Void> build(ReactiveStreamsEngine engine) {
        if (engine instanceof RxJavaEngine) {
            return build();
        }
        SubscriberWithCompletionStage<T, Void> buildSubscriber = engine.buildSubscriber(graph);
        return CompletionSubscriber.of(buildSubscriber.getSubscriber(), buildSubscriber.getCompletion());
    }

    @Override
    public Graph toGraph() {
        return graph;
    }
}
