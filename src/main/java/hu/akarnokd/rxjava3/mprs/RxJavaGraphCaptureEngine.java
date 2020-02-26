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
import java.util.concurrent.*;

import org.eclipse.microprofile.reactive.streams.operators.*;
import org.eclipse.microprofile.reactive.streams.operators.spi.*;
import org.reactivestreams.*;

import io.reactivex.rxjava3.core.Flowable;

/**
 * A mocked {@link ReactiveStreamsEngine} which just stores the {@link Graph} object
 * sent to its methods.
 */
final class RxJavaGraphCaptureEngine implements ReactiveStreamsEngine {

    Graph graph;
    
    static final CompletionStage<?> MOCK_STAGE = CompletableFuture.completedFuture(null);
    
    private RxJavaGraphCaptureEngine() {
        // use #capture
    }

    @Override
    public <T> Publisher<T> buildPublisher(Graph graph) throws UnsupportedStageException {
        this.graph = graph;
        return Flowable.never();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public <T, R> SubscriberWithCompletionStage<T, R> buildSubscriber(Graph graph) throws UnsupportedStageException {
        this.graph = graph;
        return new SubscriberWithCompletionStage() {

            @Override
            public CompletionStage getCompletion() {
                return MOCK_STAGE;
            }

            @Override
            public Subscriber getSubscriber() {
                return MockProcessor.INSTANCE;
            }
            
        };
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T, R> Processor<T, R> buildProcessor(Graph graph) throws UnsupportedStageException {
        this.graph = graph;
        return (Processor<T, R>)MockProcessor.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> CompletionStage<T> buildCompletion(Graph graph) throws UnsupportedStageException {
        this.graph = graph;
        return (CompletionStage<T>)MOCK_STAGE;
    }

    public static Graph capture(PublisherBuilder<?> builder) {
        if (builder instanceof ToGraphable) {
            return ((ToGraphable)builder).toGraph();
        }
        RxJavaGraphCaptureEngine e = new RxJavaGraphCaptureEngine();
        builder.buildRs(e);
        return e.graph;
    }

    public static Graph capture(SubscriberBuilder<?, ?> builder) {
        if (builder instanceof ToGraphable) {
            return ((ToGraphable)builder).toGraph();
        }
        RxJavaGraphCaptureEngine e = new RxJavaGraphCaptureEngine();
        builder.build(e);
        return e.graph;
    }
    
    enum MockProcessor implements Processor<Object, Object> {
        INSTANCE;

        @Override
        public void onSubscribe(Subscription s) {
            Objects.requireNonNull(s, "s is null");
        }

        @Override
        public void onNext(Object t) {
            Objects.requireNonNull(t, "t is null");
        }

        @Override
        public void onError(Throwable t) {
            Objects.requireNonNull(t, "t is null");
        }

        @Override
        public void onComplete() {
        }

        @Override
        public void subscribe(Subscriber<? super Object> s) {
            Objects.requireNonNull(s, "s is null");
        }
    }
}
