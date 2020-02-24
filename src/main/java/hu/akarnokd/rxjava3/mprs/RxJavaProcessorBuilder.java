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

import io.reactivex.rxjava3.core.Flowable;

/**
 * Builds a Flowable-based sequence by applying operators one after the other.
 * @param <T> the input type of the sequence
 * @param <R> the output value type
 */
public final class RxJavaProcessorBuilder<T, R> implements ProcessorBuilder<T, R> {

    Subscriber<T> front;

    Flowable<R> tail;

    @SuppressWarnings("unchecked")
    public RxJavaProcessorBuilder(Subscriber<? super T> front, Flowable<? extends R> tail) {
        this.front = (Subscriber<T>)front;
        this.tail = (Flowable<R>)tail;
    }

    @Override
    public <S> ProcessorBuilder<T, S> map(
            Function<? super R, ? extends S> mapper) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S> ProcessorBuilder<T, S> flatMap(
            Function<? super R, ? extends PublisherBuilder<? extends S>> mapper) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S> ProcessorBuilder<T, S> flatMapRsPublisher(
            Function<? super R, ? extends Publisher<? extends S>> mapper) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S> ProcessorBuilder<T, S> flatMapCompletionStage(
            Function<? super R, ? extends CompletionStage<? extends S>> mapper) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S> ProcessorBuilder<T, S> flatMapIterable(
            Function<? super R, ? extends Iterable<? extends S>> mapper) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ProcessorBuilder<T, R> filter(Predicate<? super R> predicate) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ProcessorBuilder<T, R> distinct() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ProcessorBuilder<T, R> limit(long maxSize) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ProcessorBuilder<T, R> skip(long n) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ProcessorBuilder<T, R> takeWhile(Predicate<? super R> predicate) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ProcessorBuilder<T, R> dropWhile(Predicate<? super R> predicate) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ProcessorBuilder<T, R> peek(Consumer<? super R> consumer) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ProcessorBuilder<T, R> onError(Consumer<Throwable> errorHandler) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ProcessorBuilder<T, R> onTerminate(Runnable action) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ProcessorBuilder<T, R> onComplete(Runnable action) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SubscriberBuilder<T, Void> forEach(Consumer<? super R> action) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SubscriberBuilder<T, Void> ignore() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SubscriberBuilder<T, Void> cancel() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SubscriberBuilder<T, R> reduce(R identity,
            BinaryOperator<R> accumulator) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SubscriberBuilder<T, Optional<R>> reduce(
            BinaryOperator<R> accumulator) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S, A> SubscriberBuilder<T, S> collect(
            Collector<? super R, A, S> collector) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S> SubscriberBuilder<T, S> collect(Supplier<S> supplier,
            BiConsumer<S, ? super R> accumulator) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SubscriberBuilder<T, List<R>> toList() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SubscriberBuilder<T, Optional<R>> findFirst() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ProcessorBuilder<T, R> onErrorResume(
            Function<Throwable, ? extends R> errorHandler) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ProcessorBuilder<T, R> onErrorResumeWith(
            Function<Throwable, ? extends PublisherBuilder<? extends R>> errorHandler) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ProcessorBuilder<T, R> onErrorResumeWithRsPublisher(
            Function<Throwable, ? extends Publisher<? extends R>> errorHandler) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SubscriberBuilder<T, Void> to(Subscriber<? super R> subscriber) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S> SubscriberBuilder<T, S> to(
            SubscriberBuilder<? super R, ? extends S> subscriber) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S> ProcessorBuilder<T, S> via(
            ProcessorBuilder<? super R, ? extends S> processor) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S> ProcessorBuilder<T, S> via(
            Processor<? super R, ? extends S> processor) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Processor<T, R> buildRs() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Processor<T, R> buildRs(ReactiveStreamsEngine engine) {
        // FIXME should we unroll the chain?
        return buildRs();
    }

}
