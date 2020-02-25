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

import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.function.*;
import java.util.stream.*;

import org.eclipse.microprofile.reactive.streams.operators.*;
import org.eclipse.microprofile.reactive.streams.operators.spi.*;
import org.reactivestreams.*;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.*;

/**
 * Builds a Flowable-based sequence by applying operators one after the other.
 * @param <T> the input type of the sequence
 * @param <R> the output value type
 */
public final class RxJavaProcessorBuilder<T, R> implements ProcessorBuilder<T, R>, ToGraphable {

    final List<FlowableTransformer<?, ?>> transformers;

    final RxJavaGraphBuilder graph;

    public RxJavaProcessorBuilder() {
        this.transformers = new ArrayList<>();
        this.graph = RxJavaMicroprofilePlugins.buildGraph() ? new RxJavaListGraphBuilder() : RxJavaNoopGraphBuilder.INSTANCE;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public RxJavaProcessorBuilder(Processor<? super T, ? extends R> processor) {
        this();
        this.transformers.add(source -> {
            source.subscribe((Processor)processor);
            return (Processor)processor;
        });
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Flowable transform(Flowable input) {
        for (FlowableTransformer ft : transformers) {
            input = Flowable.fromPublisher(ft.apply(input));
        }
        return input;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S> ProcessorBuilder<T, S> map(
            Function<? super R, ? extends S> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        this.transformers.add(current -> current.map(v -> mapper.apply((R)v)));
        if (graph.isEnabled()) {
            graph.add((Stage.Map)() -> mapper);
        }
        return (ProcessorBuilder<T, S>)this;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <S> ProcessorBuilder<T, S> flatMap(
            Function<? super R, ? extends PublisherBuilder<? extends S>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        this.transformers.add(current -> current.concatMap(v -> {
            PublisherBuilder<? extends S> pb = mapper.apply((R)v);
            if (pb instanceof RxJavaPublisherBuilder) {
                return new RxJavaInnerNullGuard<>(((RxJavaPublisherBuilder)pb).current);
            }
            return new RxJavaInnerNullGuard<>(pb.buildRs());
        }));
        if (graph.isEnabled()) {
            graph.add((Stage.FlatMap)() -> v -> 
                RxJavaGraphCaptureEngine.capture(mapper.apply((R)v))
            );
        }
        return (ProcessorBuilder<T, S>)this;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <S> ProcessorBuilder<T, S> flatMapRsPublisher(
            Function<? super R, ? extends Publisher<? extends S>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        this.transformers.add(current -> current.concatMap(v -> mapper.apply((R)v)));
        if (graph.isEnabled()) {
            graph.add((Stage.FlatMap)() -> v -> {
                Publisher p = new RxJavaInnerNullGuard<>(mapper.apply((R)v));
                Stage.PublisherStage ps = () -> p;
                Collection<Stage> coll = Collections.singletonList(ps);
                return (Graph)() -> coll;
            });
        }
        return (ProcessorBuilder<T, S>)this;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <S> ProcessorBuilder<T, S> flatMapCompletionStage(
            Function<? super R, ? extends CompletionStage<? extends S>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        this.transformers.add(current -> current.concatMapSingle(v -> Single.fromCompletionStage((CompletionStage<S>)mapper.apply((R)v))));
        if (graph.isEnabled()) {
            graph.add((Stage.FlatMapCompletionStage)() -> (Function)mapper);
        }
        return (ProcessorBuilder<T, S>)this;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <S> ProcessorBuilder<T, S> flatMapIterable(
            Function<? super R, ? extends Iterable<? extends S>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        this.transformers.add(current -> current.concatMapIterable(v -> mapper.apply((R)v)));
        if (graph.isEnabled()) {
            graph.add((Stage.FlatMapIterable)() -> (Function)mapper);
        }
        return (ProcessorBuilder<T, S>)this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ProcessorBuilder<T, R> filter(Predicate<? super R> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        this.transformers.add(current -> current.filter(v -> predicate.test((R)v)));
        if (graph.isEnabled()) {
            graph.add((Stage.Filter)() -> predicate);
        }
        return this;
    }

    @Override
    public ProcessorBuilder<T, R> distinct() {
        this.transformers.add(current -> current.distinct());
        if (graph.isEnabled()) {
            graph.add(RxJavaStageDistinct.INSTANCE);
        }
        return this;
    }

    @Override
    public ProcessorBuilder<T, R> limit(long maxSize) {
        this.transformers.add(current -> current.take(maxSize));
        if (graph.isEnabled()) {
            graph.add((Stage.Limit)() -> maxSize);
        }
        return this;
    }

    @Override
    public ProcessorBuilder<T, R> skip(long n) {
        this.transformers.add(current -> current.skip(n));
        if (graph.isEnabled()) {
            graph.add((Stage.Skip)() -> n);
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ProcessorBuilder<T, R> takeWhile(Predicate<? super R> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        this.transformers.add(current -> current.takeWhile(v -> predicate.test((R)v)));
        if (graph.isEnabled()) {
            graph.add((Stage.TakeWhile)() -> predicate);
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ProcessorBuilder<T, R> dropWhile(Predicate<? super R> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        this.transformers.add(current -> current.skipWhile(v -> predicate.test((R)v)));
        if (graph.isEnabled()) {
            graph.add((Stage.DropWhile)() -> predicate);
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ProcessorBuilder<T, R> peek(Consumer<? super R> consumer) {
        Objects.requireNonNull(consumer, "consumer is null");
        this.transformers.add(current -> current.doOnNext(v -> consumer.accept((R)v)));
        if (graph.isEnabled()) {
            graph.add((Stage.Peek)() -> consumer);
        }
        return this;
    }

    @Override
    public ProcessorBuilder<T, R> onError(Consumer<Throwable> errorHandler) {
        Objects.requireNonNull(errorHandler, "errorHandler is null");
        this.transformers.add(current -> current.doOnError(v -> errorHandler.accept(v)));
        if (graph.isEnabled()) {
            graph.add((Stage.OnError)() -> errorHandler);
        }
        return this;
    }

    @Override
    public ProcessorBuilder<T, R> onTerminate(Runnable action) {
        Objects.requireNonNull(action, "action is null");
        this.transformers.add(current -> new FlowableDoOnTerminateAndCancel<>(current, action));
        if (graph.isEnabled()) {
            graph.add((Stage.OnTerminate)() -> action);
        }
        return this;
    }

    @Override
    public ProcessorBuilder<T, R> onComplete(Runnable action) {
        Objects.requireNonNull(action, "action is null");
        this.transformers.add(current -> current.doOnComplete(() -> action.run()));
        if (graph.isEnabled()) {
            graph.add((Stage.OnComplete)() -> action);
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public SubscriberBuilder<T, Void> forEach(Consumer<? super R> action) {
        Objects.requireNonNull(action, "action is null");
        if (graph.isEnabled()) {
            // FIXME there is no Stage.ForEach
        }
        return new RxJavaSubscriberForProcessorBuilder<>(transformers, 
                current -> current.doOnNext(v -> action.accept((R)v))
                                  .ignoreElements().toCompletionStage(null));
    }

    @Override
    public SubscriberBuilder<T, Void> ignore() {
        if (graph.isEnabled()) {
            // FIXME there is no Stage.Ignore
        }
        return new RxJavaSubscriberForProcessorBuilder<>(transformers, 
                current -> current.ignoreElements().toCompletionStage(null));
    }

    @Override
    public SubscriberBuilder<T, Void> cancel() {
        RxJavaSubscriberForProcessorBuilder<T, @NonNull Completable, Void> result = 
                new RxJavaSubscriberForProcessorBuilder<>(transformers, 
                current -> current.take(0L).ignoreElements().toCompletionStage(null));
        if (result.graph.isEnabled()) {
            result.graph.addAll(graph);
            result.graph.add(RxJavaStageCancel.INSTANCE);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public SubscriberBuilder<T, R> reduce(R identity,
            BinaryOperator<R> accumulator) {
        Objects.requireNonNull(accumulator, "accumulator is null");
        if (graph.isEnabled()) {
            // FIXME there is no Stage.Reduce
        }
        return new RxJavaSubscriberForProcessorBuilder<>(transformers, 
                current -> current.reduce(identity, (a, b) -> accumulator.apply(a, (R)b))
                                  .toCompletionStage());
    }

    @Override
    public SubscriberBuilder<T, Optional<R>> reduce(
            BinaryOperator<R> accumulator) {
        Objects.requireNonNull(accumulator, "accumulator is null");
        if (graph.isEnabled()) {
            // FIXME there is no Stage.Reduce
        }
        return new RxJavaSubscriberForProcessorBuilder<T, R, Optional<R>>(transformers, 
                current -> current.reduce((a, b) -> accumulator.apply(a, b)).map(Optional::of)
                                  .toCompletionStage(Optional.empty()));
    }

    @Override
    public SubscriberBuilder<T, Optional<R>> findFirst() {
        RxJavaSubscriberForProcessorBuilder<T, R, Optional<R>> result =
            new RxJavaSubscriberForProcessorBuilder<>(transformers, 
                    current -> current.firstElement().map(Optional::of).toCompletionStage(Optional.empty()));
        if (result.graph.isEnabled()) {
            result.graph.addAll(graph);
            result.graph.add(RxJavaStageFindFirst.INSTANCE);
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S, A> SubscriberBuilder<T, S> collect(
            Collector<? super R, A, S> collector) {
        Objects.requireNonNull(collector, "collector is null");
        RxJavaSubscriberForProcessorBuilder<T, R, S> result = 
                new RxJavaSubscriberForProcessorBuilder<>(transformers, 
                        current -> new FlowableCollectCollectorDeferred<>(current, (Collector<R, A, S>)collector)
                                   .toCompletionStage());
        if (result.graph.isEnabled()) {
            result.graph.addAll(graph);
            result.graph.add((Stage.Collect)() -> collector);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <S> SubscriberBuilder<T, S> collect(Supplier<S> supplier,
            BiConsumer<S, ? super R> accumulator) {
        Objects.requireNonNull(supplier, "predicate is null");
        Objects.requireNonNull(accumulator, "predicate is null");
        if (graph.isEnabled()) {
            // FIXME there is no Stage.Collect with supplier+lambda
        }
        return new RxJavaSubscriberForProcessorBuilder<>(transformers, 
                current -> current.collect(() -> supplier.get(), (a, b) -> accumulator.accept(a, (R)b))
                           .toCompletionStage());
    }

    @Override
    public SubscriberBuilder<T, List<R>> toList() {
        RxJavaSubscriberForProcessorBuilder<T, R, List<R>> result = 
                new RxJavaSubscriberForProcessorBuilder<>(transformers, 
                current -> current.toList().toCompletionStage());
        if (result.graph.isEnabled()) {
            // TODO there is no Stage.ToList
            Collector<?, ?, ?> coll = Collectors.toList();
            result.graph.add((Stage.Collect)() -> coll);
        }
        return result;
    }

    @Override
    public ProcessorBuilder<T, R> onErrorResume(
            Function<Throwable, ? extends R> errorHandler) {
        Objects.requireNonNull(errorHandler, "predicate is null");
        this.transformers.add(current -> current.onErrorResumeNext(e -> {
            try {
                return Flowable.just(errorHandler.apply(e));
            } catch (Throwable ex) {
                return Flowable.error(ex);
            }
        }));
        if (graph.isEnabled()) {
            graph.add((Stage.OnErrorResume)() -> errorHandler);
        }
        return this;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ProcessorBuilder<T, R> onErrorResumeWith(
            Function<Throwable, ? extends PublisherBuilder<? extends R>> errorHandler) {
        Objects.requireNonNull(errorHandler, "errorHandler is null");
        this.transformers.add(current -> current.onErrorResumeNext(e -> {
            PublisherBuilder<? extends R> pb;
            try {
                pb = errorHandler.apply(e);
            } catch (Throwable ex) {
                return Flowable.error(ex);
            }
            if (pb instanceof RxJavaPublisherBuilder) {
                return ((RxJavaPublisherBuilder)pb).current;
            }
            return pb.buildRs();
        }));
        if (graph.isEnabled()) {
            graph.add((Stage.OnErrorResumeWith)() -> v -> RxJavaGraphCaptureEngine.capture(errorHandler.apply(v)));
        }
        return this;
    }

    @Override
    public ProcessorBuilder<T, R> onErrorResumeWithRsPublisher(
            Function<Throwable, ? extends Publisher<? extends R>> errorHandler) {
        Objects.requireNonNull(errorHandler, "errorHandler is null");
        this.transformers.add(current -> current.onErrorResumeNext(e -> {
            try {
                return errorHandler.apply(e);
            } catch (Throwable ex) {
                return Flowable.error(ex);
            }
        }));
        graph.add((Stage.OnErrorResumeWith)() -> v -> {
            Publisher<?> p;
            try {
                p = errorHandler.apply(v);
            } catch (Throwable ex) {
                p = Flowable.error(ex);
            }
            Publisher<?> p1 = p;
            Stage.PublisherStage ps = () -> p1;
            Collection<Stage> coll = Collections.singletonList(ps);
            return (Graph)() -> coll;
        });
        return this;
    }

    @Override
    public SubscriberBuilder<T, Void> to(Subscriber<? super R> subscriber) {
        Objects.requireNonNull(subscriber, "subscriber is null");
        // FIXME pass along the graph?
        RxJavaSubscriberForProcessorBuilder<T, R, Void> result = 
                new RxJavaSubscriberForProcessorBuilder<>(transformers, f -> {
            RxJavaCompletionSubscriber<R> cs;
            if (subscriber instanceof FlowableSubscriber) {
                cs = new RxJavaCompletionFlowableSubscriber<>(subscriber);
            } else {
                cs = new RxJavaCompletionSubscriber<>(subscriber);
            }
            f.subscribe(cs);
            return cs.getCompletion();
        });
        if (result.graph.isEnabled()) {
            result.graph.add((Stage.SubscriberStage)() -> subscriber);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <S> SubscriberBuilder<T, S> to(
            SubscriberBuilder<? super R, ? extends S> subscriber) {
        Objects.requireNonNull(subscriber, "subscriber is null");
        // FIXME pass along the graph?
        RxJavaSubscriberForProcessorBuilder<T, R, S> result = 
                new RxJavaSubscriberForProcessorBuilder<>(transformers, f -> {
            CompletionSubscriber<? super R, ? extends S> cs = subscriber.build();
            f.subscribe(cs);
            return (CompletionStage<S>)cs.getCompletion();
        });
        if (result.graph.isEnabled()) {
            // TODO is this supposed to work like this?
            Subscriber<?> s = subscriber.build();
            result.graph.add((Stage.SubscriberStage)() -> s);
        }
        return result;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <S> ProcessorBuilder<T, S> via(
            ProcessorBuilder<? super R, ? extends S> processor) {
        Objects.requireNonNull(processor, "processor is null");
        this.transformers.add(current -> {
            Flowable c = current;
            if (processor instanceof RxJavaProcessorBuilder) {
                RxJavaProcessorBuilder<R, S> rx = (RxJavaProcessorBuilder<R, S>)processor;
                current = rx.transform(current);
            } else {
                Processor p = processor.buildRs();
                c.subscribe(p);
                current = Flowable.fromPublisher(p);
            }
            return current;
        });
        if (graph.isEnabled()) {
            // TODO is this supposed to work like this?
            Processor<?, ?> p1 = processor.buildRs();
            graph.add((Stage.ProcessorStage)() -> p1);
        }
        return (ProcessorBuilder<T, S>)this;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <S> ProcessorBuilder<T, S> via(
            Processor<? super R, ? extends S> processor) {
        Objects.requireNonNull(processor, "processor is null");
        this.transformers.add(current -> {
            Flowable c = current;
            c.subscribe(processor);
            return Flowable.fromPublisher(processor);
        });
        if (graph.isEnabled()) {
            graph.add((Stage.ProcessorStage)() -> processor);
        }
        return (ProcessorBuilder<T, S>)this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Processor<T, R> buildRs() {
        DeferredProcessor<T> dp = new DeferredProcessor<>();
        return new FlowableProcessorBridge<>(dp, transform(dp));
    }

    @Override
    public Processor<T, R> buildRs(ReactiveStreamsEngine engine) {
        if (engine instanceof RxJavaEngine) {
            return buildRs();
        }
        // FIXME should we unroll the chain?
        return engine.buildProcessor(graph);
    }

    @Override
    public Graph toGraph() {
        return graph;
    }
}
