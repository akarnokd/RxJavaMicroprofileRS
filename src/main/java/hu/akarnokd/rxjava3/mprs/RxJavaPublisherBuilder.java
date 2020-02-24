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
import java.util.stream.Collector;

import org.eclipse.microprofile.reactive.streams.operators.*;
import org.eclipse.microprofile.reactive.streams.operators.spi.ReactiveStreamsEngine;
import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;

/**
 * Builds a Flowable-based sequence by applying operators one after the other.
 * @param <T> the element type of the sequence at a specific stage
 */
@SuppressWarnings("rawtypes")
public final class RxJavaPublisherBuilder<T> implements PublisherBuilder<T> {

    Flowable<T> current;

    /**
     * Create a builder with the given Flowable as the source.
     * @param source the source Flowable to start chaining on
     */
    public RxJavaPublisherBuilder(Flowable<T> source) {
        this.current = source;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <R> PublisherBuilder<R> map(
            Function<? super T, ? extends R> mapper) {
        current = (Flowable)current.map(v -> mapper.apply(v));
        return (PublisherBuilder<R>)this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S> PublisherBuilder<S> flatMap(
            Function<? super T, ? extends PublisherBuilder<? extends S>> mapper) {
        current = current.concatMap(v -> {
            PublisherBuilder<? extends S> pb = mapper.apply(v);
            if (pb instanceof RxJavaPublisherBuilder) {
                return ((RxJavaPublisherBuilder)pb).current;
            }
            return pb.buildRs();
        });
        return (PublisherBuilder<S>)this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S> PublisherBuilder<S> flatMapRsPublisher(
            Function<? super T, ? extends Publisher<? extends S>> mapper) {
        current = (Flowable)current.concatMap(v -> mapper.apply(v), 1);
        return (PublisherBuilder<S>)this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S> PublisherBuilder<S> flatMapCompletionStage(
            Function<? super T, ? extends CompletionStage<? extends S>> mapper) {
        current = (Flowable)current.concatMapSingle(v -> Single.fromCompletionStage((CompletionStage<S>)mapper.apply(v)));
        return (PublisherBuilder<S>)this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S> PublisherBuilder<S> flatMapIterable(
            Function<? super T, ? extends Iterable<? extends S>> mapper) {
        current = (Flowable)current.concatMapIterable(v -> mapper.apply(v));
        return (PublisherBuilder<S>)this;
    }

    @Override
    public PublisherBuilder<T> filter(Predicate<? super T> predicate) {
        current = current.filter(v -> predicate.test(v));
        return this;
    }

    @Override
    public PublisherBuilder<T> distinct() {
        current = current.distinct();
        return this;
    }

    @Override
    public PublisherBuilder<T> limit(long maxSize) {
        current = current.take(maxSize);
        return this;
    }

    @Override
    public PublisherBuilder<T> skip(long n) {
        current = current.skip(n);
        return this;
    }

    @Override
    public PublisherBuilder<T> takeWhile(Predicate<? super T> predicate) {
        current = current.takeWhile(v -> predicate.test(v));
        return this;
    }

    @Override
    public PublisherBuilder<T> dropWhile(Predicate<? super T> predicate) {
        current = current.skipWhile(v -> predicate.test(v));
        return this;
    }

    @Override
    public PublisherBuilder<T> peek(Consumer<? super T> consumer) {
        current = current.doOnNext(v -> consumer.accept(v));
        return this;
    }

    @Override
    public PublisherBuilder<T> onError(Consumer<Throwable> errorHandler) {
        current = current.doOnError(v -> errorHandler.accept(v));
        return this;
    }

    @Override
    public PublisherBuilder<T> onTerminate(Runnable action) {
        current = current.doOnTerminate(() -> action.run());
        return this;
    }

    @Override
    public PublisherBuilder<T> onComplete(Runnable action) {
        current = current.doOnComplete(() -> action.run());
        return this;
    }

    @Override
    public CompletionRunner<Void> forEach(Consumer<? super T> action) {
        return new RxJavaCompletionRunner<>(
                current.doOnNext(v -> action.accept(v)),
                f -> f.ignoreElements().toCompletionStage(null));
    }

    @Override
    public CompletionRunner<Void> ignore() {
        return new RxJavaCompletionRunner<>(current, 
                f -> f.ignoreElements().toCompletionStage(null));
    }

    @Override
    public CompletionRunner<Void> cancel() {
        return new RxJavaCompletionRunner<>(current.take(0L), 
                f -> f.ignoreElements().toCompletionStage(null));
    }

    @Override
    public CompletionRunner<T> reduce(T identity,
            BinaryOperator<T> accumulator) {
        return new RxJavaCompletionRunner<>(
                current.reduce(identity, (a, b) -> accumulator.apply(a, b)),
                s -> s.toCompletionStage());
    }

    @Override
    public CompletionRunner<Optional<T>> reduce(BinaryOperator<T> accumulator) {
        return new RxJavaCompletionRunner<>(
                current.reduce((a, b) -> accumulator.apply(a, b))
                    .map(Optional::of).defaultIfEmpty(Optional.empty()),
                s -> s.toCompletionStage());
    }

    @Override
    public CompletionRunner<Optional<T>> findFirst() {
        return new RxJavaCompletionRunner<>(
                current.firstElement()
                    .map(Optional::of).defaultIfEmpty(Optional.empty()),
                s -> s.toCompletionStage());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R, A> CompletionRunner<R> collect(
            Collector<? super T, A, R> collector) {
        return new RxJavaCompletionRunner<>(
                current.collect((Collector<T, A, R>)collector),
                s -> s.toCompletionStage()
                );
    }

    @Override
    public <R> CompletionRunner<R> collect(Supplier<R> supplier,
            BiConsumer<R, ? super T> accumulator) {
        return new RxJavaCompletionRunner<>(
                current.collect(() -> supplier.get(), (a, b) -> accumulator.accept(a, b)),
                s -> s.toCompletionStage()
                );
    }

    @Override
    public CompletionRunner<List<T>> toList() {
        return new RxJavaCompletionRunner<>(
                current.toList(),
                s -> s.toCompletionStage()
                );
    }

    @Override
    public PublisherBuilder<T> onErrorResume(
            Function<Throwable, ? extends T> errorHandler) {
        current = current.onErrorReturn(e -> errorHandler.apply(e));
        return this;
    }

    @SuppressWarnings("unchecked")
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
        return this;
    }

    @Override
    public PublisherBuilder<T> onErrorResumeWithRsPublisher(
            Function<Throwable, ? extends Publisher<? extends T>> errorHandler) {
        current = current.onErrorResumeNext(e -> errorHandler.apply(e));
        return this;
    }

    @Override
    public CompletionRunner<Void> to(Subscriber<? super T> subscriber) {
        return new RxJavaCompletionRunnerSubscriber<>(current, subscriber);
    }

    @Override
    public <R> CompletionRunner<R> to(
            SubscriberBuilder<? super T, ? extends R> subscriber) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> PublisherBuilder<R> via(
            ProcessorBuilder<? super T, ? extends R> processor) {
        Flowable<T> c = current;
        Processor<? super T, ? extends R> p;
        if (processor instanceof RxJavaProcessorBuilder) {
            RxJavaProcessorBuilder<T, R> rx = (RxJavaProcessorBuilder<T, R>)processor;
            c.subscribe(rx.front);
            current = (Flowable)rx.tail;
        } else {
            p = processor.buildRs();
            c.subscribe(p);
            current = (Flowable)Flowable.fromPublisher(p);
        }
        return (PublisherBuilder<R>)this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> PublisherBuilder<R> via(
            Processor<? super T, ? extends R> processor) {
        Flowable<T> c = current;
        c.subscribe(processor);
        current = (Flowable)Flowable.fromPublisher(processor);
        return (PublisherBuilder<R>)this;
    }

    @Override
    public Publisher<T> buildRs() {
        return current;
    }

    @Override
    public Publisher<T> buildRs(ReactiveStreamsEngine engine) {
        // FIXME should we unroll the chain?
        return current;
    }

}
