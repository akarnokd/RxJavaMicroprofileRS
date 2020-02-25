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
 * @param <T> the element type of the sequence at a specific stage
 */
public final class RxJavaPublisherBuilder<T> implements PublisherBuilder<T> {

    Flowable<T> current;

    final RxJavaGraphBuilder graph;

    /**
     * Create a builder with the given Flowable as the source.
     * @param source the source Flowable to start chaining on
     */
    public RxJavaPublisherBuilder(Flowable<T> source) {
        this.current = source;
        this.graph = RxJavaMicroprofilePlugins.buildGraph() ? new RxJavaListGraphBuilder() : RxJavaNoopGraphBuilder.INSTANCE;
    }


    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <R> PublisherBuilder<R> map(
            Function<? super T, ? extends R> mapper) {
        current = (Flowable)current.map(v -> mapper.apply(v));
        if (graph.isEnabled()) {
            graph.add((Stage.Map)() -> mapper);
        }
        return (PublisherBuilder<R>)this;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <S> PublisherBuilder<S> flatMap(
            Function<? super T, ? extends PublisherBuilder<? extends S>> mapper) {
        current = current.concatMap(v -> {
            PublisherBuilder<? extends S> pb = mapper.apply(v);
            if (pb instanceof RxJavaPublisherBuilder) {
                return ((RxJavaPublisherBuilder)pb).current;
            }
            return pb.buildRs();
        });
        if (graph.isEnabled()) {
            graph.add((Stage.FlatMap)() -> v -> 
                RxJavaGraphCaptureEngine.capture(mapper.apply((T)v))
            );
        }
        return (PublisherBuilder<S>)this;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <S> PublisherBuilder<S> flatMapRsPublisher(
            Function<? super T, ? extends Publisher<? extends S>> mapper) {
        current = (Flowable)current.concatMap(v -> mapper.apply(v));
        if (graph.isEnabled()) {
            graph.add((Stage.FlatMap)() -> v -> {
                Publisher p = mapper.apply((T)v);
                Stage.PublisherStage ps = () -> p;
                Collection<Stage> coll = Collections.singletonList(ps);
                return (Graph)() -> coll;
            });
        }
        return (PublisherBuilder<S>)this;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <S> PublisherBuilder<S> flatMapCompletionStage(
            Function<? super T, ? extends CompletionStage<? extends S>> mapper) {
        current = (Flowable)current.concatMapSingle(v -> Single.fromCompletionStage((CompletionStage<S>)mapper.apply(v)));
        if (graph.isEnabled()) {
            graph.add((Stage.FlatMapCompletionStage)() -> (Function)mapper);
        }
        return (PublisherBuilder<S>)this;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <S> PublisherBuilder<S> flatMapIterable(
            Function<? super T, ? extends Iterable<? extends S>> mapper) {
        current = (Flowable)current.concatMapIterable(v -> mapper.apply(v));
        if (graph.isEnabled()) {
            graph.add((Stage.FlatMapIterable)() -> (Function)mapper);
        }
        return (PublisherBuilder<S>)this;
    }

    @Override
    public PublisherBuilder<T> filter(Predicate<? super T> predicate) {
        current = current.filter(v -> predicate.test(v));
        if (graph.isEnabled()) {
            graph.add((Stage.Filter)() -> predicate);
        }
        return this;
    }

    @Override
    public PublisherBuilder<T> distinct() {
        current = current.distinct();
        if (graph.isEnabled()) {
            graph.add(RxJavaStageDistinct.INSTANCE);
        }
        return this;
    }

    @Override
    public PublisherBuilder<T> limit(long maxSize) {
        current = current.take(maxSize);
        if (graph.isEnabled()) {
            graph.add((Stage.Limit)() -> maxSize);
        }
        return this;
    }

    @Override
    public PublisherBuilder<T> skip(long n) {
        current = current.skip(n);
        if (graph.isEnabled()) {
            graph.add((Stage.Skip)() -> n);
        }
        return this;
    }

    @Override
    public PublisherBuilder<T> takeWhile(Predicate<? super T> predicate) {
        current = current.takeWhile(v -> predicate.test(v));
        if (graph.isEnabled()) {
            graph.add((Stage.TakeWhile)() -> predicate);
        }
        return this;
    }

    @Override
    public PublisherBuilder<T> dropWhile(Predicate<? super T> predicate) {
        current = current.skipWhile(v -> predicate.test(v));
        if (graph.isEnabled()) {
            graph.add((Stage.DropWhile)() -> predicate);
        }
        return this;
    }

    @Override
    public PublisherBuilder<T> peek(Consumer<? super T> consumer) {
        current = current.doOnNext(v -> consumer.accept(v));
        if (graph.isEnabled()) {
            graph.add((Stage.Peek)() -> consumer);
        }
        return this;
    }

    @Override
    public PublisherBuilder<T> onError(Consumer<Throwable> errorHandler) {
        current = current.doOnError(v -> errorHandler.accept(v));
        if (graph.isEnabled()) {
            graph.add((Stage.OnError)() -> errorHandler);
        }
        return this;
    }

    @Override
    public PublisherBuilder<T> onTerminate(Runnable action) {
        current = current.doOnTerminate(() -> action.run());
        if (graph.isEnabled()) {
            graph.add((Stage.OnTerminate)() -> action);
        }
        return this;
    }

    @Override
    public PublisherBuilder<T> onComplete(Runnable action) {
        current = current.doOnComplete(() -> action.run());
        if (graph.isEnabled()) {
            graph.add((Stage.OnComplete)() -> action);
        }
        return this;
    }

    @Override
    public CompletionRunner<Void> forEach(Consumer<? super T> action) {
        if (graph.isEnabled()) {
            // FIXME there is no Stage.ForEach
        }
        return new RxJavaCompletionRunner<>(
                current.doOnNext(v -> action.accept(v)).ignoreElements(),
                c -> c.toCompletionStage(null));
    }

    @Override
    public CompletionRunner<Void> ignore() {
        if (graph.isEnabled()) {
            // FIXME there is no Stage.Ignore
        }
        return new RxJavaCompletionRunner<>(current.ignoreElements(), 
                c -> c.toCompletionStage(null));
    }

    @Override
    public CompletionRunner<Void> cancel() {
        RxJavaCompletionRunner<@NonNull Completable, Void> result = new RxJavaCompletionRunner<>(current.take(0L).ignoreElements(), 
                c -> c.toCompletionStage(null));
        if (result.graph.isEnabled()) {
            result.graph.add(RxJavaStageCancel.INSTANCE);
        }
        return result;
    }

    @Override
    public CompletionRunner<T> reduce(T identity,
            BinaryOperator<T> accumulator) {
        if (graph.isEnabled()) {
            // FIXME there is no Stage.Reduce
        }
        return new RxJavaCompletionRunner<>(
                current.reduce(identity, (a, b) -> accumulator.apply(a, b)),
                s -> s.toCompletionStage());
    }

    @Override
    public CompletionRunner<Optional<T>> reduce(BinaryOperator<T> accumulator) {
        if (graph.isEnabled()) {
            // FIXME there is no Stage.Reduce
        }
        return new RxJavaCompletionRunner<>(
                current.reduce((a, b) -> accumulator.apply(a, b))
                    .map(Optional::of),
                m -> m.toCompletionStage(Optional.empty()));
    }

    @Override
    public CompletionRunner<Optional<T>> findFirst() {
        RxJavaCompletionRunner<Maybe<Optional<T>>, Optional<T>> result = new RxJavaCompletionRunner<>(
                current.firstElement().map(Optional::of),
                m -> m.toCompletionStage(Optional.empty()));
        if (result.graph.isEnabled()) {
            result.graph.add(RxJavaStageFindFirst.INSTANCE);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R, A> CompletionRunner<R> collect(
            Collector<? super T, A, R> collector) {
        RxJavaCompletionRunner<Single<R>, R> result = new RxJavaCompletionRunner<>(
                current.collect((Collector<T, A, R>)collector),
                s -> s.toCompletionStage()
                );
        if (result.graph.isEnabled()) {
            result.graph.add((Stage.Collect)() -> collector);
        }
        return result;
    }

    @Override
    public <R> CompletionRunner<R> collect(Supplier<R> supplier,
            BiConsumer<R, ? super T> accumulator) {
        if (graph.isEnabled()) {
            // FIXME there is no Stage.Collect with supplier+lambda
        }
        return new RxJavaCompletionRunner<>(
                current.collect(() -> supplier.get(), (a, b) -> accumulator.accept(a, b)),
                s -> s.toCompletionStage()
                );
    }

    @Override
    public CompletionRunner<List<T>> toList() {
        if (graph.isEnabled()) {
            // TODO there is no Stage.ToList
            Collector<?, ?, ?> coll = Collectors.toList();
            graph.add((Stage.Collect)() -> coll);
        }
        RxJavaCompletionRunner<Single<List<T>>, List<T>> result = new RxJavaCompletionRunner<>(
                current.toList(),
                s -> s.toCompletionStage()
                );
        
        if (result.graph.isEnabled()) {
            Collector<?, ?, ?> coll = Collectors.toList();
            result.graph.add((Stage.Collect)() -> coll);
        }
        return result;
    }

    @Override
    public PublisherBuilder<T> onErrorResume(
            Function<Throwable, ? extends T> errorHandler) {
        current = current.onErrorReturn(e -> errorHandler.apply(e));
        if (graph.isEnabled()) {
            graph.add((Stage.OnErrorResume)() -> errorHandler);
        }
        return this;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public PublisherBuilder<T> onErrorResumeWith(
            Function<Throwable, ? extends PublisherBuilder<? extends T>> errorHandler) {
        current = current.onErrorResumeNext(e -> {
            PublisherBuilder<? extends T> pb = errorHandler.apply(e);
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
    public PublisherBuilder<T> onErrorResumeWithRsPublisher(
            Function<Throwable, ? extends Publisher<? extends T>> errorHandler) {
        current = current.onErrorResumeNext(e -> errorHandler.apply(e));
        if (graph.isEnabled()) {
            graph.add((Stage.OnErrorResumeWith)() -> v -> {
                Publisher<?> p = errorHandler.apply(v);
                Stage.PublisherStage ps = () -> p;
                Collection<Stage> coll = Collections.singletonList(ps);
                return (Graph)() -> coll;
            });
        }
        return this;
    }

    @Override
    public CompletionRunner<Void> to(Subscriber<? super T> subscriber) {
        if (subscriber instanceof FlowableSubscriber) {
            return new RxJavaCompletionRunnerFlowableSubscriber<>(current, subscriber);
        }
        return new RxJavaCompletionRunnerSubscriber<>(current, subscriber);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> CompletionRunner<R> to(
            SubscriberBuilder<? super T, ? extends R> subscriber) {
        RxJavaCompletionRunner<Flowable<T>, R> result = new RxJavaCompletionRunner<>(current, f -> {
            CompletionSubscriber<? super T, ? extends R> cs = subscriber.build();
            f.subscribe(cs);
            return (CompletionStage<R>)cs.getCompletion();
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
    public <R> PublisherBuilder<R> via(
            ProcessorBuilder<? super T, ? extends R> processor) {
        Flowable<T> c = current;
        Processor<? super T, ? extends R> p;
        if (processor instanceof RxJavaProcessorBuilder) {
            RxJavaProcessorBuilder<T, R> rx = (RxJavaProcessorBuilder<T, R>)processor;
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
        return (PublisherBuilder<R>)this;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <R> PublisherBuilder<R> via(
            Processor<? super T, ? extends R> processor) {
        Flowable<T> c = current;
        c.subscribe(processor);
        current = (Flowable)Flowable.fromPublisher(processor);
        if (graph.isEnabled()) {
            graph.add((Stage.ProcessorStage)() -> processor);
        }
        return (PublisherBuilder<R>)this;
    }

    @Override
    public Publisher<T> buildRs() {
        return current;
    }

    @Override
    public Publisher<T> buildRs(ReactiveStreamsEngine engine) {
        if (engine instanceof RxJavaEngine) {
            return current;
        }
        return engine.buildPublisher(graph);
    }

}
