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

import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.spi.*;
import org.reactivestreams.*;

/**
 * A mocked {@link ReactiveStreamsEngine} which just stores the {@link Graph} object
 * sent to its methods.
 */
final class RxJavaGraphCaptureEngine implements ReactiveStreamsEngine {

    Graph graph;
    
    private RxJavaGraphCaptureEngine() {
        // use #capture
    }

    @Override
    public <T> Publisher<T> buildPublisher(Graph graph) throws UnsupportedStageException {
        this.graph = graph;
        return null;
    }

    @Override
    public <T, R> SubscriberWithCompletionStage<T, R> buildSubscriber(Graph graph) throws UnsupportedStageException {
        this.graph = graph;
        return null;
    }

    @Override
    public <T, R> Processor<T, R> buildProcessor(Graph graph) throws UnsupportedStageException {
        this.graph = graph;
        return null;
    }

    @Override
    public <T> CompletionStage<T> buildCompletion(Graph graph) throws UnsupportedStageException {
        this.graph = graph;
        return null;
    }

    public static Graph capture(PublisherBuilder<?> builder) {
        RxJavaGraphCaptureEngine e = new RxJavaGraphCaptureEngine();
        builder.buildRs(e);
        return e.graph;
    }
}
