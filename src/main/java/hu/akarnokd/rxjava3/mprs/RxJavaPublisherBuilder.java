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

public final class RxJavaPublisherBuilder<T> implements PublisherBuilder<T> {

    @Override
    public <R> PublisherBuilder<R> map(
            Function<? super T, ? extends R> mapper) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S> PublisherBuilder<S> flatMap(
            Function<? super T, ? extends PublisherBuilder<? extends S>> mapper) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S> PublisherBuilder<S> flatMapRsPublisher(
            Function<? super T, ? extends Publisher<? extends S>> mapper) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S> PublisherBuilder<S> flatMapCompletionStage(
            Function<? super T, ? extends CompletionStage<? extends S>> mapper) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S> PublisherBuilder<S> flatMapIterable(
            Function<? super T, ? extends Iterable<? extends S>> mapper) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PublisherBuilder<T> filter(Predicate<? super T> predicate) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PublisherBuilder<T> distinct() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PublisherBuilder<T> limit(long maxSize) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PublisherBuilder<T> skip(long n) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PublisherBuilder<T> takeWhile(Predicate<? super T> predicate) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PublisherBuilder<T> dropWhile(Predicate<? super T> predicate) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PublisherBuilder<T> peek(Consumer<? super T> consumer) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PublisherBuilder<T> onError(Consumer<Throwable> errorHandler) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PublisherBuilder<T> onTerminate(Runnable action) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PublisherBuilder<T> onComplete(Runnable action) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CompletionRunner<Void> forEach(Consumer<? super T> action) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CompletionRunner<Void> ignore() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CompletionRunner<Void> cancel() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CompletionRunner<T> reduce(T identity,
            BinaryOperator<T> accumulator) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CompletionRunner<Optional<T>> reduce(BinaryOperator<T> accumulator) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CompletionRunner<Optional<T>> findFirst() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <R, A> CompletionRunner<R> collect(
            Collector<? super T, A, R> collector) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <R> CompletionRunner<R> collect(Supplier<R> supplier,
            BiConsumer<R, ? super T> accumulator) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CompletionRunner<List<T>> toList() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PublisherBuilder<T> onErrorResume(
            Function<Throwable, ? extends T> errorHandler) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PublisherBuilder<T> onErrorResumeWith(
            Function<Throwable, ? extends PublisherBuilder<? extends T>> errorHandler) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PublisherBuilder<T> onErrorResumeWithRsPublisher(
            Function<Throwable, ? extends Publisher<? extends T>> errorHandler) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CompletionRunner<Void> to(Subscriber<? super T> subscriber) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <R> CompletionRunner<R> to(
            SubscriberBuilder<? super T, ? extends R> subscriber) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <R> PublisherBuilder<R> via(
            ProcessorBuilder<? super T, ? extends R> processor) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <R> PublisherBuilder<R> via(
            Processor<? super T, ? extends R> processor) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Publisher<T> buildRs() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Publisher<T> buildRs(ReactiveStreamsEngine engine) {
        // TODO Auto-generated method stub
        return null;
    }

}
