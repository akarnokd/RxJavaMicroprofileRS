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
public final class RxJavaProcessorBuilder<T, R> implements ProcessorBuilder<T, R> {

    Subscriber<T> front;

    Flowable<R> current;

    final RxJavaGraphBuilder graph;

    @SuppressWarnings("unchecked")
    public RxJavaProcessorBuilder(Subscriber<? super T> front, Flowable<? extends R> tail) {
        this.front = (Subscriber<T>)front;
        this.current = (Flowable<R>)tail;
        this.graph = RxJavaMicroprofilePlugins.buildGraph() ? new RxJavaListGraphBuilder() : RxJavaNoopGraphBuilder.INSTANCE;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <S> ProcessorBuilder<T, S> map(
            Function<? super R, ? extends S> mapper) {
        current = (Flowable)current.map(v -> mapper.apply(v));
        if (graph.isEnabled()) {
            graph.add((Stage.Map)() -> mapper);
        }
        return (ProcessorBuilder<T, S>)this;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <S> ProcessorBuilder<T, S> flatMap(
            Function<? super R, ? extends PublisherBuilder<? extends S>> mapper) {
        current = current.concatMap(v -> {
            PublisherBuilder<? extends S> pb = mapper.apply(v);
            if (pb instanceof RxJavaPublisherBuilder) {
                return ((RxJavaPublisherBuilder)pb).current;
            }
            return pb.buildRs();
        });
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
        current = (Flowable)current.concatMap(v -> mapper.apply(v));
        if (graph.isEnabled()) {
            graph.add((Stage.FlatMap)() -> v -> {
                Publisher p = mapper.apply((R)v);
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
        current = (Flowable)current.concatMapSingle(v -> Single.fromCompletionStage((CompletionStage<S>)mapper.apply(v)));
        if (graph.isEnabled()) {
            graph.add((Stage.FlatMapCompletionStage)() -> (Function)mapper);
        }
        return (ProcessorBuilder<T, S>)this;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <S> ProcessorBuilder<T, S> flatMapIterable(
            Function<? super R, ? extends Iterable<? extends S>> mapper) {
        current = (Flowable)current.concatMapIterable(v -> mapper.apply(v));
        if (graph.isEnabled()) {
            graph.add((Stage.FlatMapIterable)() -> (Function)mapper);
        }
        return (ProcessorBuilder<T, S>)this;
    }

    @Override
    public ProcessorBuilder<T, R> filter(Predicate<? super R> predicate) {
        current = current.filter(v -> predicate.test(v));
        if (graph.isEnabled()) {
            graph.add((Stage.Filter)() -> predicate);
        }
        return this;
    }

    @Override
    public ProcessorBuilder<T, R> distinct() {
        current = current.distinct();
        if (graph.isEnabled()) {
            graph.add(RxJavaStageDistinct.INSTANCE);
        }
        return this;
    }

    @Override
    public ProcessorBuilder<T, R> limit(long maxSize) {
        current = current.take(maxSize);
        if (graph.isEnabled()) {
            graph.add((Stage.Limit)() -> maxSize);
        }
        return this;
    }

    @Override
    public ProcessorBuilder<T, R> skip(long n) {
        current = current.skip(n);
        if (graph.isEnabled()) {
            graph.add((Stage.Skip)() -> n);
        }
        return this;
    }

    @Override
    public ProcessorBuilder<T, R> takeWhile(Predicate<? super R> predicate) {
        current = current.takeWhile(v -> predicate.test(v));
        if (graph.isEnabled()) {
            graph.add((Stage.TakeWhile)() -> predicate);
        }
        return this;
    }

    @Override
    public ProcessorBuilder<T, R> dropWhile(Predicate<? super R> predicate) {
        current = current.skipWhile(v -> predicate.test(v));
        if (graph.isEnabled()) {
            graph.add((Stage.DropWhile)() -> predicate);
        }
        return this;
    }

    @Override
    public ProcessorBuilder<T, R> peek(Consumer<? super R> consumer) {
        current = current.doOnNext(v -> consumer.accept(v));
        if (graph.isEnabled()) {
            graph.add((Stage.Peek)() -> consumer);
        }
        return this;
    }

    @Override
    public ProcessorBuilder<T, R> onError(Consumer<Throwable> errorHandler) {
        current = current.doOnError(v -> errorHandler.accept(v));
        if (graph.isEnabled()) {
            graph.add((Stage.OnError)() -> errorHandler);
        }
        return this;
    }

    @Override
    public ProcessorBuilder<T, R> onTerminate(Runnable action) {
        current = current.doOnTerminate(() -> action.run());
        if (graph.isEnabled()) {
            graph.add((Stage.OnTerminate)() -> action);
        }
        return this;
    }

    @Override
    public ProcessorBuilder<T, R> onComplete(Runnable action) {
        current = current.doOnComplete(() -> action.run());
        if (graph.isEnabled()) {
            graph.add((Stage.OnComplete)() -> action);
        }
        return this;
    }

    @Override
    public SubscriberBuilder<T, Void> forEach(Consumer<? super R> action) {
        if (graph.isEnabled()) {
            // FIXME there is no Stage.ForEach
        }
        return new RxJavaSubscriberForProcessorBuilder<>(front, 
                current.doOnNext(v -> action.accept(v)).ignoreElements(),
                c -> c.toCompletionStage(null));
    }

    @Override
    public SubscriberBuilder<T, Void> ignore() {
        if (graph.isEnabled()) {
            // FIXME there is no Stage.Ignore
        }
        return new RxJavaSubscriberForProcessorBuilder<>(front, 
                current.ignoreElements(),
                c -> c.toCompletionStage(null));
    }

    @Override
    public SubscriberBuilder<T, Void> cancel() {
        RxJavaSubscriberForProcessorBuilder<T, @NonNull Completable, Void> result = new RxJavaSubscriberForProcessorBuilder<>(front, 
                current.take(0L).ignoreElements(),
                c -> c.toCompletionStage(null));
        if (result.graph.isEnabled()) {
            result.graph.add(RxJavaStageCancel.INSTANCE);
        }
        return result;
    }

    @Override
    public SubscriberBuilder<T, R> reduce(R identity,
            BinaryOperator<R> accumulator) {
        if (graph.isEnabled()) {
            // FIXME there is no Stage.Reduce
        }
        return new RxJavaSubscriberForProcessorBuilder<>(front, 
                current.reduce(identity, (a, b) -> accumulator.apply(a, b)),
                s -> s.toCompletionStage());
    }

    @Override
    public SubscriberBuilder<T, Optional<R>> reduce(
            BinaryOperator<R> accumulator) {
        if (graph.isEnabled()) {
            // FIXME there is no Stage.Reduce
        }
        return new RxJavaSubscriberForProcessorBuilder<>(front, 
                current.reduce((a, b) -> accumulator.apply(a, b)).map(Optional::of),
                m -> m.toCompletionStage(Optional.empty()));
    }

    @Override
    public SubscriberBuilder<T, Optional<R>> findFirst() {
        RxJavaSubscriberForProcessorBuilder<T, Maybe<Optional<R>>, Optional<R>> result = new RxJavaSubscriberForProcessorBuilder<>(front, 
                current.firstElement().map(Optional::of),
                m -> m.toCompletionStage(Optional.empty()));
        if (result.graph.isEnabled()) {
            result.graph.add(RxJavaStageFindFirst.INSTANCE);
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S, A> SubscriberBuilder<T, S> collect(
            Collector<? super R, A, S> collector) {
        RxJavaSubscriberForProcessorBuilder<T, Single<S>, S> result = new RxJavaSubscriberForProcessorBuilder<>(front, 
                        current.collect((Collector<R, A, S>)collector),
                        s -> s.toCompletionStage());
        if (result.graph.isEnabled()) {
            result.graph.add((Stage.Collect)() -> collector);
        }
        return result;
    }

    @Override
    public <S> SubscriberBuilder<T, S> collect(Supplier<S> supplier,
            BiConsumer<S, ? super R> accumulator) {
        if (graph.isEnabled()) {
            // FIXME there is no Stage.Collect with supplier+lambda
        }
        return new RxJavaSubscriberForProcessorBuilder<>(front, 
                current.collect(() -> supplier.get(), (a, b) -> accumulator.accept(a, b)),
                s -> s.toCompletionStage());
    }

    @Override
    public SubscriberBuilder<T, List<R>> toList() {
        RxJavaSubscriberForProcessorBuilder<T, Single<List<R>>, List<R>> result = new RxJavaSubscriberForProcessorBuilder<>(front, 
                current.toList(),
                s -> s.toCompletionStage());
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
        current = current.onErrorReturn(e -> errorHandler.apply(e));
        if (graph.isEnabled()) {
            graph.add((Stage.OnErrorResume)() -> errorHandler);
        }
        return this;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ProcessorBuilder<T, R> onErrorResumeWith(
            Function<Throwable, ? extends PublisherBuilder<? extends R>> errorHandler) {
        current = current.onErrorResumeNext(e -> {
            PublisherBuilder<? extends R> pb = errorHandler.apply(e);
            if (pb instanceof RxJavaPublisherBuilder) {
                return ((RxJavaPublisherBuilder)pb).current;
            }
            return pb.buildRs();
        });
        if (graph.isEnabled()) {
            graph.add((Stage.OnErrorResumeWith)() -> v -> RxJavaGraphCaptureEngine.capture(errorHandler.apply(v)));
        }
        return this;
    }

    @Override
    public ProcessorBuilder<T, R> onErrorResumeWithRsPublisher(
            Function<Throwable, ? extends Publisher<? extends R>> errorHandler) {
        current = current.onErrorResumeNext(e -> errorHandler.apply(e));
        graph.add((Stage.OnErrorResumeWith)() -> v -> {
            Publisher<?> p = errorHandler.apply(v);
            Stage.PublisherStage ps = () -> p;
            Collection<Stage> coll = Collections.singletonList(ps);
            return (Graph)() -> coll;
        });
        return this;
    }

    @Override
    public SubscriberBuilder<T, Void> to(Subscriber<? super R> subscriber) {
        // FIXME pass along the graph?
        RxJavaSubscriberForProcessorBuilder<T, Flowable<R>, Void> result = new RxJavaSubscriberForProcessorBuilder<>(front, current, f -> {
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
        // FIXME pass along the graph?
        RxJavaSubscriberForProcessorBuilder<T, Flowable<R>, S> result = new RxJavaSubscriberForProcessorBuilder<>(front, current, f -> {
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
        Flowable<R> c = current;
        Processor<? super R, ? extends S> p;
        if (processor instanceof RxJavaProcessorBuilder) {
            RxJavaProcessorBuilder<R, S> rx = (RxJavaProcessorBuilder<R, S>)processor;
            c.subscribe(rx.front);
            current = (Flowable)rx.current;
        } else {
            p = processor.buildRs();
            c.subscribe(p);
            current = (Flowable)Flowable.fromPublisher(p);
        }
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
        Flowable<R> c = current;
        c.subscribe(processor);
        current = (Flowable)Flowable.fromPublisher(processor);
        if (graph.isEnabled()) {
            graph.add((Stage.ProcessorStage)() -> processor);
        }
        return (ProcessorBuilder<T, S>)this;
    }

    @Override
    public Processor<T, R> buildRs() {
        return new FlowableProcessorBridge<>(front, current);
    }

    @Override
    public Processor<T, R> buildRs(ReactiveStreamsEngine engine) {
        if (engine instanceof RxJavaEngine) {
            return buildRs();
        }
        // FIXME should we unroll the chain?
        return engine.buildProcessor(graph);
    }

}
