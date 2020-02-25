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
import java.util.function.*;

import org.eclipse.microprofile.reactive.streams.operators.spi.*;
import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;

/**
 * An RxJava-based {@link ReactiveStreamsEngine} that translates the
 * assembly instructions in a {@link Graph} into RxJava
 * {@link Flowable}-based flows and components.
 */
public enum RxJavaEngine implements ReactiveStreamsEngine {

    INSTANCE;

    private RxJavaEngine() {
        // singleton
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Publisher<T> buildPublisher(Graph graph)
            throws UnsupportedStageException {
        return (Publisher<T>)build(graph, Mode.PUBLISHER);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T, R> SubscriberWithCompletionStage<T, R> buildSubscriber(
            Graph graph) throws UnsupportedStageException {
        return (SubscriberWithCompletionStage<T, R>)build(graph, Mode.SUBSCRIBER);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T, R> Processor<T, R> buildProcessor(Graph graph)
            throws UnsupportedStageException {
        return (Processor<T, R>)build(graph, Mode.PROCESSOR);
    }

    @Override
    public <T> CompletionStage<T> buildCompletion(Graph graph)
            throws UnsupportedStageException {
        return this.<T, T>buildSubscriber(graph).getCompletion();
    }
    
    enum Mode {
        PUBLISHER,
        PROCESSOR,
        SUBSCRIBER
    }

    static void requireNullSource(Object o, Stage stage) {
        if (o != null) {
            throw new IllegalArgumentException("Graph already has a source-like stage! Found " + stage.getClass().getSimpleName());
        }
    }

    static void requireNullTerminal(Object o, Stage stage) {
        if (o != null) {
            throw new IllegalArgumentException("Graph already has a terminal stage! Found " + stage.getClass().getSimpleName());
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    static Object build(Graph graph, Mode mode) throws UnsupportedStageException {
        Subscriber front = null;
        Flowable result = null;
        CompletionStage completion = null;
        
        for (Stage stage : graph.getStages()) {

            if (stage instanceof Stage.PublisherStage) {
                requireNullSource(result, stage);
                
                Publisher publisher = ((Stage.PublisherStage)stage).getRsPublisher();
                result = Flowable.fromPublisher(publisher);
                continue;
            }
            if (stage instanceof Stage.Of) {
                requireNullSource(result, stage);

                Iterable iterable = ((Stage.Of)stage).getElements();
                result = Flowable.fromIterable(iterable);
                continue;
            }
            if (stage instanceof Stage.ProcessorStage) {
                if (result == null) {
                    // act as a source
                    Publisher publisher = ((Stage.ProcessorStage)stage).getRsProcessor();
                    result = Flowable.fromPublisher(publisher);
                } else {
                    // act as a middle operator
                    Processor processor = ((Stage.ProcessorStage)stage).getRsProcessor();
                    // FIXME should this be deferred for when the downstream actually subscribes?
                    result.subscribe(processor);
                    result = Flowable.fromPublisher(processor);
                }
                continue;
            }
            if (stage instanceof Stage.Failed) {
                requireNullSource(result, stage);

                Throwable throwable = ((Stage.Failed)stage).getError();
                result = Flowable.error(throwable);
                continue;
            }
            if (stage instanceof Stage.Concat) {
                requireNullSource(result, stage);
                Graph g1 = ((Stage.Concat)stage).getFirst();
                Graph g2 = ((Stage.Concat)stage).getSecond();
                result = Flowable.concat((Publisher)build(g1, Mode.PUBLISHER), (Publisher)build(g2, Mode.PUBLISHER));
                continue;
            }
            if (stage instanceof Stage.FromCompletionStage) {
                requireNullSource(result, stage);
                CompletionStage cs = ((Stage.FromCompletionStage) stage).getCompletionStage();
                result = Flowable.fromCompletionStage(cs);
                continue;
            }
            if (stage instanceof Stage.FromCompletionStageNullable) {
                requireNullSource(result, stage);
                CompletionStage cs = ((Stage.FromCompletionStageNullable) stage).getCompletionStage();
                result = Maybe.fromCompletionStage(cs).toFlowable();
                continue;
            }
            if (stage instanceof Stage.Coupled) {
                if (mode == Mode.PROCESSOR) {
                    requireNullSource(result, stage);
                    Stage.Coupled coupled = (Stage.Coupled) stage;
                    front = ((SubscriberWithCompletionStage)build(coupled.getSubscriber(), Mode.SUBSCRIBER)).getSubscriber();
                    result = Flowable.fromPublisher((Publisher)build(coupled.getPublisher(), Mode.PUBLISHER));
                    continue;
                }
                throw new IllegalArgumentException("Stage.Coupled is only supported when building via buildProcessor");
            }
            
            // ------------------------------------------------------------------------------
            if (result == null) {
                throw new IllegalArgumentException("Graph is missing a source-like stage! Found " + stage.getClass().getSimpleName());
            }
            if (stage instanceof Stage.Map) {
                Function mapper = ((Stage.Map) stage).getMapper();
                result = result.map(v -> mapper.apply(v));
                continue;
            }
            if (stage instanceof Stage.Peek) {
                Consumer consumer = ((Stage.Peek) stage).getConsumer();
                result = result.doOnNext(v -> consumer.accept(v));
                continue;
            }
            if (stage instanceof Stage.Filter) {
                Predicate predicate = ((Stage.Filter)stage).getPredicate();
                result = result.filter(v -> predicate.test(v));
                continue;
            }
            if (stage instanceof Stage.DropWhile) {
                Predicate predicate = ((Stage.DropWhile)stage).getPredicate();
                result = result.skipWhile(v -> predicate.test(v));
                continue;
            }
            if (stage instanceof Stage.Skip) {
                long n = ((Stage.Skip)stage).getSkip();
                result = result.skip(n);
                continue;
            }
            if (stage instanceof Stage.Limit) {
                long n = ((Stage.Limit)stage).getLimit();
                result = result.take(n);
                continue;
            }
            if (stage instanceof Stage.Distinct) {
                result = result.distinct();
                continue;
            }
            if (stage instanceof Stage.TakeWhile) {
                Predicate predicate = ((Stage.TakeWhile)stage).getPredicate();
                result = result.takeWhile(v -> predicate.test(v));
                continue;
            }
            if (stage instanceof Stage.FlatMap) {
                Function mapper = ((Stage.FlatMap) stage).getMapper();
                result = result.concatMap(v -> INSTANCE.buildPublisher((Graph)mapper.apply(v)));
                continue;
            }
            if (stage instanceof Stage.FlatMapCompletionStage) {
                Function mapper = ((Stage.FlatMapCompletionStage) stage).getMapper();
                result = result.concatMapSingle(v -> Single.fromCompletionStage((CompletionStage)mapper.apply(v)));
                continue;
            }
            if (stage instanceof Stage.FlatMapIterable) {
                Function mapper = ((Stage.FlatMapIterable) stage).getMapper();
                result = result.concatMapIterable(v -> (Iterable)mapper.apply(v));
                continue;
            }
            if (stage instanceof Stage.OnError) {
                Consumer consumer = ((Stage.OnError) stage).getConsumer();
                result = result.doOnError(v -> consumer.accept(v));
                continue;
            }
            if (stage instanceof Stage.OnTerminate) {
                Runnable runnable = ((Stage.OnTerminate) stage).getAction();
                result = new FlowableDoOnTerminateAndCancel<>(result, runnable);
                continue;
            }
            if (stage instanceof Stage.OnComplete) {
                Runnable runnable = ((Stage.OnComplete) stage).getAction();
                result = result.doOnComplete(() -> runnable.run());
                continue;
            }
            if (stage instanceof Stage.OnErrorResume) {
                Function mapper = ((Stage.OnErrorResume) stage).getFunction();
                result = result.onErrorReturn(e -> mapper.apply(e));
                continue;
            }
            if (stage instanceof Stage.OnErrorResumeWith) {
                Function mapper = ((Stage.OnErrorResumeWith) stage).getFunction();
                result = result.onErrorResumeNext(e -> INSTANCE.buildPublisher((Graph)mapper.apply(e)));
                continue;
            }
            if (stage instanceof Stage.FindFirst) {
                if (mode == Mode.SUBSCRIBER) {
                    requireNullTerminal(front, stage);
                    requireNullTerminal(completion, stage);

                    RxJavaFindFirstSubscriber cs = new RxJavaFindFirstSubscriber();
                    front = cs;
                    completion = cs.completable;

                    continue;
                }
                throw new IllegalArgumentException("Stage.FindFirst is only supported when building via buildSubscriber");
            }
            if (stage instanceof Stage.SubscriberStage) {
                if (mode == Mode.SUBSCRIBER) {
                    requireNullTerminal(front, stage);
                    requireNullTerminal(completion, stage);

                    Subscriber s = ((Stage.SubscriberStage) stage).getRsSubscriber();
                    RxJavaCompletionSubscriber cs;
                    if (s instanceof FlowableSubscriber) {
                        cs = new RxJavaCompletionFlowableSubscriber(s);
                    } else {
                        cs = new RxJavaCompletionSubscriber(s);
                    }
                    front = cs;
                    completion = cs.getCompletion();

                    continue;
                }
                throw new IllegalArgumentException("Stage.FindFirst is only supported when building via buildSubscriber");
            }
            if (stage instanceof Stage.Collect) {
                if (mode == Mode.SUBSCRIBER) {
                    requireNullTerminal(front, stage);
                    requireNullTerminal(completion, stage);

                    Stage.Collect collect = (Stage.Collect) stage;
                    
                    RxJavaCollectSubscriber cs = new RxJavaCollectSubscriber(collect.getCollector());
                    front = cs;
                    completion = cs.completable;
                    
                    continue;
                }
                throw new IllegalArgumentException("Stage.FindFirst is only supported when building via buildSubscriber");
            }
            if (stage instanceof Stage.Cancel) {
                if (mode == Mode.SUBSCRIBER) {
                    requireNullTerminal(front, stage);
                    requireNullTerminal(completion, stage);

                    RxJavaCancelSubscriber cs = new RxJavaCancelSubscriber();
                    front = cs;
                    completion = cs.completable;
                    
                    continue;
                }
                throw new IllegalArgumentException("Stage.FindFirst is only supported when building via buildSubscriber");
            }

            throw new UnsupportedStageException(stage);
        }
        
        if (mode == Mode.PUBLISHER) {
            if (result == null) {
                throw new IllegalArgumentException("The graph had no usable stages for builing a Publisher.");
            }
            return result;
        }
        if (mode == Mode.PROCESSOR) {
            if (front == null || result == null) {
                throw new IllegalArgumentException("The graph had no usable stages for builing a Processor.");
            }
            return new FlowableProcessorBridge(front, result);
        }
        if (front == null || completion == null) {
            throw new IllegalArgumentException("The graph had no usable stages for builing a Subscriber.");
        }
        return new InnerSubscriberWithCompletionStage(front, completion);
    }

    static final class InnerSubscriberWithCompletionStage<T, R> implements SubscriberWithCompletionStage<T, R>{
        
        final CompletionStage<R> completion;
        
        final Subscriber<T> front;
        
        InnerSubscriberWithCompletionStage(Subscriber<T> front, CompletionStage<R> completion) {
            this.front = front;
            this.completion = completion;
        }
        
        @Override
        public CompletionStage<R> getCompletion() {
            return completion;
        }

        @Override
        public Subscriber<T> getSubscriber() {
            return front;
        }
    }
}
