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

import org.eclipse.microprofile.reactive.streams.operators.*;
import org.reactivestreams.*;

public final class RxJavaPublisherFactory implements ReactiveStreamsFactory {

    @Override
    public <T> PublisherBuilder<T> fromPublisher(
            Publisher<? extends T> publisher) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> PublisherBuilder<T> of(T t) {
        // TODO Auto-generated method stub
        return null;
    }

    /* final: Java 8 workaround for safevarargs */
    @Override
    @SafeVarargs
    public final <T> PublisherBuilder<T> of(T... ts) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> PublisherBuilder<T> empty() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> PublisherBuilder<T> ofNullable(T t) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> PublisherBuilder<T> fromIterable(Iterable<? extends T> ts) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> PublisherBuilder<T> failed(Throwable t) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> ProcessorBuilder<T, T> builder() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T, R> ProcessorBuilder<T, R> fromProcessor(
            Processor<? super T, ? extends R> processor) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> SubscriberBuilder<T, Void> fromSubscriber(
            Subscriber<? extends T> subscriber) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> PublisherBuilder<T> iterate(T seed, UnaryOperator<T> f) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> PublisherBuilder<T> generate(Supplier<? extends T> s) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> PublisherBuilder<T> concat(PublisherBuilder<? extends T> a,
            PublisherBuilder<? extends T> b) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> PublisherBuilder<T> fromCompletionStage(
            CompletionStage<? extends T> completionStage) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> PublisherBuilder<T> fromCompletionStageNullable(
            CompletionStage<? extends T> completionStage) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T, R> ProcessorBuilder<T, R> coupled(
            SubscriberBuilder<? super T, ?> subscriber,
            PublisherBuilder<? extends R> publisher) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T, R> ProcessorBuilder<T, R> coupled(
            Subscriber<? super T> subscriber,
            Publisher<? extends R> publisher) {
        // TODO Auto-generated method stub
        return null;
    }

}
