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

import java.util.Iterator;
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

    @SuppressWarnings("unchecked")
    @Override
    public <T> CompletionStage<T> buildCompletion(Graph graph)
            throws UnsupportedStageException {
        return (CompletionStage<T>)build(graph, Mode.COMPLETION);
    }

    /**
     * How should the graph be built?
     * <p>
     * Some graph building modes have shared stages to consider.
     */
    enum Mode {
        PUBLISHER,
        PROCESSOR,
        SUBSCRIBER,
        COMPLETION
    }

    static void requireNullSource(Object o, Stage stage) {
        if (o != null) {
            throw new IllegalArgumentException("Graph already has a source-like stage! Found " + stage.getClass().getSimpleName());
        }
    }

    static void requireNullFront(Object o, Stage stage) {
        if (o != null) {
            throw new IllegalArgumentException("Graph already has an inlet Subscriber! Found " + stage.getClass().getSimpleName());
        }
    }

    static void requireSource(Object o, Stage stage) {
        if (o == null) {
            throw new IllegalArgumentException("Graph is missing a source-like stage! Found " + stage.getClass().getSimpleName());
        }
    }

    static void requireNullTerminal(Object o, Stage stage) {
        if (o != null) {
            throw new IllegalArgumentException("Graph already has a terminal stage! Found " + stage.getClass().getSimpleName());
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    static Object build(Graph graph, Mode mode) throws UnsupportedStageException {
        Subscriber graphInlet = null;
        Flowable result = null;
        CompletionStage completion = null;
        
        Iterator<Stage> stages = graph.getStages().iterator();
        
        Stage stage = null;

        if (stages.hasNext()) {
            stage = stages.next();
        }
        
        // PublisherFactory.builder() is not allowed to start with an
        // identity processor stage apparently as it would have too many
        // nodes in the graph
        // we'll patch in an identity processor here
        if (mode == Mode.PROCESSOR || mode == Mode.SUBSCRIBER) {
            if (stage == null 
                    || !(
                            (stage instanceof Stage.ProcessorStage)
                            || (stage instanceof Stage.Coupled)
                        )) {
                Processor processor = new DeferredProcessor<>();
                graphInlet = processor;
                result = Flowable.fromPublisher(processor);
            }
        }
        
        if (stage != null) {
            boolean once = false;
            for (;;) {
                
                if (once) {
                    if (!stages.hasNext()) {
                        break;
                    }
                    stage = stages.next();
                }
                once = true;

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
                        requireNullFront(graphInlet, stage);
                        // act as a source
                        Processor processor = ((Stage.ProcessorStage)stage).getRsProcessor();
                        graphInlet = processor;
                        result = Flowable.fromPublisher(processor);
                    } else {
                        // act as a middle operator
                        Processor processor = ((Stage.ProcessorStage)stage).getRsProcessor();
                        // FIXME should this be deferred for when the downstream actually subscribes?
                        result = new RxJavaDeferredViaProcessor(result, processor);
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
                    result = new FlowableConcatCanceling<>((Publisher)build(g1, Mode.PUBLISHER), (Publisher)build(g2, Mode.PUBLISHER));
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
                    Stage.Coupled coupled = (Stage.Coupled) stage;
                    if (result == null) {
                        requireNullFront(graphInlet, stage);
                    }
    
                    Subscriber s = ((SubscriberWithCompletionStage)build(coupled.getSubscriber(), Mode.SUBSCRIBER)).getSubscriber();
                    Flowable f = Flowable.fromPublisher((Publisher)build(coupled.getPublisher(), Mode.PUBLISHER));
    
                    Processor processor = RxJavaPublisherFactory.coupledBuildProcessor(s, f);
                    if (result == null) {
                        graphInlet = processor;
                        result = Flowable.fromPublisher(processor);
                    } else {
                        result = new RxJavaDeferredViaProcessor(result, processor);
                    }
                    
                    continue;
                }
                
                // ------------------------------------------------------------------------------
    
                if (stage instanceof Stage.Map) {
                    requireSource(result, stage);
    
                    Function mapper = ((Stage.Map) stage).getMapper();
                    result = result.map(v -> mapper.apply(v));
                    continue;
                }
                if (stage instanceof Stage.Peek) {
                    requireSource(result, stage);
    
                    Consumer consumer = ((Stage.Peek) stage).getConsumer();
                    result = result.doOnNext(v -> consumer.accept(v));
                    continue;
                }
                if (stage instanceof Stage.Filter) {
                    requireSource(result, stage);
    
                    Predicate predicate = ((Stage.Filter)stage).getPredicate();
                    result = result.filter(v -> predicate.test(v));
                    continue;
                }
                if (stage instanceof Stage.DropWhile) {
                    requireSource(result, stage);
    
                    Predicate predicate = ((Stage.DropWhile)stage).getPredicate();
                    result = result.skipWhile(v -> predicate.test(v));
                    continue;
                }
                if (stage instanceof Stage.Skip) {
                    requireSource(result, stage);
    
                    long n = ((Stage.Skip)stage).getSkip();
                    result = result.skip(n);
                    continue;
                }
                if (stage instanceof Stage.Limit) {
                    requireSource(result, stage);
    
                    long n = ((Stage.Limit)stage).getLimit();
                    result = result.take(n);
                    continue;
                }
                if (stage instanceof Stage.Distinct) {
                    requireSource(result, stage);
    
                    result = result.distinct();
                    continue;
                }
                if (stage instanceof Stage.TakeWhile) {
                    requireSource(result, stage);
    
                    Predicate predicate = ((Stage.TakeWhile)stage).getPredicate();
                    result = result.takeWhile(v -> predicate.test(v));
                    continue;
                }
                if (stage instanceof Stage.FlatMap) {
                    requireSource(result, stage);
    
                    Function mapper = ((Stage.FlatMap) stage).getMapper();
                    result = result.concatMap(v -> new RxJavaInnerNullGuard<>(INSTANCE.buildPublisher((Graph)mapper.apply(v))));
                    continue;
                }
                if (stage instanceof Stage.FlatMapCompletionStage) {
                    requireSource(result, stage);
    
                    Function mapper = ((Stage.FlatMapCompletionStage) stage).getMapper();
                    result = result.concatMapSingle(v -> Single.fromCompletionStage((CompletionStage)mapper.apply(v)));
                    continue;
                }
                if (stage instanceof Stage.FlatMapIterable) {
                    requireSource(result, stage);
    
                    Function mapper = ((Stage.FlatMapIterable) stage).getMapper();
                    result = result.concatMapIterable(v -> (Iterable)mapper.apply(v));
                    continue;
                }
                if (stage instanceof Stage.OnError) {
                    requireSource(result, stage);
    
                    Consumer consumer = ((Stage.OnError) stage).getConsumer();
                    result = result.doOnError(v -> consumer.accept(v));
                    continue;
                }
                if (stage instanceof Stage.OnTerminate) {
                    requireSource(result, stage);
    
                    Runnable runnable = ((Stage.OnTerminate) stage).getAction();
                    result = new FlowableDoOnTerminateAndCancel<>(result, runnable);
                    continue;
                }
                if (stage instanceof Stage.OnComplete) {
                    requireSource(result, stage);
    
                    Runnable runnable = ((Stage.OnComplete) stage).getAction();
                    result = result.doOnComplete(() -> runnable.run());
                    continue;
                }
                if (stage instanceof Stage.OnErrorResume) {
                    requireSource(result, stage);
    
                    Function mapper = ((Stage.OnErrorResume) stage).getFunction();
                    result = result.onErrorResumeNext(e -> {
                        try {
                            return Flowable.just(mapper.apply(e));
                        } catch (Throwable ex) {
                            return Flowable.error(ex);
                        }
                    });
                    continue;
                }
                if (stage instanceof Stage.OnErrorResumeWith) {
                    requireSource(result, stage);
    
                    Function mapper = ((Stage.OnErrorResumeWith) stage).getFunction();
                    result = result.onErrorResumeNext(e -> {
                        Graph g;
                        try {
                            g = (Graph)mapper.apply(e);
                        } catch (Throwable ex) {
                            return Flowable.error(ex);
                        }
                        return INSTANCE.buildPublisher(g);
                    });
                    continue;
                }
                
                if (stage instanceof Stage.FindFirst) {
                    if (mode == Mode.SUBSCRIBER || mode == Mode.COMPLETION) {
                        if (graphInlet != null) {
                            requireSource(result, stage);
                            requireNullTerminal(completion, stage);
                        }
    
                        RxJavaFindFirstSubscriber cs = new RxJavaFindFirstSubscriber();
                        completion = cs.completable;
                        if (result != null) {
                            result.subscribe(cs);
                        } else {
                            graphInlet = cs;
                        }
    
                        continue;
                    }
                    throw new IllegalArgumentException("Stage.FindFirst is only supported when building via buildSubscriber or buildCompletion");
                }
                if (stage instanceof Stage.SubscriberStage) {
                    if (mode == Mode.SUBSCRIBER || mode == Mode.COMPLETION) {
                        if (graphInlet != null) {
                            requireSource(result, stage);
                            requireNullTerminal(completion, stage);
                        }
                        
                        Subscriber s = ((Stage.SubscriberStage) stage).getRsSubscriber();
                        RxJavaCompletionSubscriber cs;
                        if (s instanceof FlowableSubscriber) {
                            cs = new RxJavaCompletionFlowableSubscriber(s);
                        } else {
                            cs = new RxJavaCompletionSubscriber(s);
                        }
                        completion = cs.getCompletion();
                        if (result != null) {
                            result.subscribe(cs);
                        } else {
                            graphInlet = cs;
                        }
                        continue;
                    }
                    throw new IllegalArgumentException("Stage.FindFirst is only supported when building via buildSubscriber or buildCompletion");
                }
                if (stage instanceof Stage.Collect) {
                    if (mode == Mode.SUBSCRIBER || mode == Mode.COMPLETION) {
                        if (graphInlet != null) {
                            requireSource(result, stage);
                            requireNullTerminal(completion, stage);
                        }
                        
                        Stage.Collect collect = (Stage.Collect) stage;
                        RxJavaCollectSubscriber cs = new RxJavaCollectSubscriber(collect.getCollector());
                        completion = cs.completable;
                        if (result != null) {
                            result.subscribe(cs);
                        } else {
                            graphInlet = cs;
                        }
                        continue;
                    }
                    throw new IllegalArgumentException("Stage.FindFirst is only supported when building via buildSubscriber or buildCompletion");
                }
                if (stage instanceof Stage.Cancel) {
                    if (mode == Mode.SUBSCRIBER || mode == Mode.COMPLETION) {
                        if (graphInlet != null) {
                            requireSource(result, stage);
                            requireNullTerminal(completion, stage);
                        }
    
                        RxJavaCancelSubscriber cs = new RxJavaCancelSubscriber();
                        completion = cs.completable;
                        if (result != null) {
                            result.subscribe(cs);
                        } else {
                            graphInlet = cs;
                        }
                        
                        continue;
                    }
                    throw new IllegalArgumentException("Stage.FindFirst is only supported when building via buildSubscriber or buildCompletion");
                }
    
                throw new UnsupportedStageException(stage);
            }
        }
        
        if (mode == Mode.PUBLISHER) {
            if (result == null) {
                throw new IllegalArgumentException("The graph had no usable stages for builing a Publisher.");
            }
            return result;
        }
        if (mode == Mode.PROCESSOR) {
            if (graphInlet == null || result == null) {
                throw new IllegalArgumentException("The graph had no usable stages for builing a Processor.");
            }
            return new FlowableProcessorBridge(graphInlet, result);
        }
        if (mode == Mode.COMPLETION) {
            if (completion == null) {
                throw new IllegalArgumentException("The graph had no usable stages for builing a CompletionStage.");
            }
            return completion;
        }
        if (graphInlet == null || completion == null) {
            throw new IllegalArgumentException("The graph had no usable stages for builing a Subscriber.");
        }
        return new InnerSubscriberWithCompletionStage(graphInlet, completion);
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
