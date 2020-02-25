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

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.processors.PublishProcessor;

/**
 * Constructs RxJava-based PublisherBuilders.
 */
public final class RxJavaPublisherFactory implements ReactiveStreamsFactory {

    @Override
    public <T> PublisherBuilder<T> fromPublisher(
            Publisher<? extends T> publisher) {
        return new RxJavaPublisherBuilder<>(Flowable.fromPublisher(publisher));
    }

    @Override
    public <T> PublisherBuilder<T> of(T t) {
        return new RxJavaPublisherBuilder<>(Flowable.just(t));
    }

    /* final: Java 8 workaround for safevarargs */
    @Override
    @SafeVarargs
    public final <T> PublisherBuilder<T> of(T... ts) {
        return new RxJavaPublisherBuilder<>(Flowable.fromArray(ts));
    }

    @Override
    public <T> PublisherBuilder<T> empty() {
        return new RxJavaPublisherBuilder<>(Flowable.empty());
    }

    @Override
    public <T> PublisherBuilder<T> ofNullable(T t) {
        return t != null ? of(t) : empty();
    }

    @Override
    public <T> PublisherBuilder<T> fromIterable(Iterable<? extends T> ts) {
        return new RxJavaPublisherBuilder<>(Flowable.fromIterable(ts));
    }

    @Override
    public <T> PublisherBuilder<T> failed(Throwable t) {
        return new RxJavaPublisherBuilder<>(Flowable.error(t));
    }

    @Override
    public <T> ProcessorBuilder<T, T> builder() {
        DeferredProcessor<T> processor = new DeferredProcessor<>();
        return new RxJavaProcessorBuilder<>(processor, processor);
    }

    @Override
    public <T, R> ProcessorBuilder<T, R> fromProcessor(
            Processor<? super T, ? extends R> processor) {
        return new RxJavaProcessorBuilder<>(processor, Flowable.fromPublisher(processor));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> SubscriberBuilder<T, Void> fromSubscriber(
            Subscriber<? extends T> subscriber) {
        // FIXME the signature is wrong, at least let it compile
        return new RxJavaSubscriberBuilder<>((Subscriber<T>)subscriber);
    }

    @Override
    public <T> PublisherBuilder<T> iterate(T seed, UnaryOperator<T> f) {
        return new RxJavaPublisherBuilder<>(Flowable.generate(() -> seed, (s, e) -> {
            e.onNext(s);
            return f.apply(s);
        }));
    }

    @Override
    public <T> PublisherBuilder<T> generate(Supplier<? extends T> s) {
        return new RxJavaPublisherBuilder<>(Flowable.generate(e -> e.onNext(s.get())));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> PublisherBuilder<T> concat(PublisherBuilder<? extends T> a,
            PublisherBuilder<? extends T> b) {
        Flowable<T> source1;
        Flowable<T> source2;
        if (a instanceof RxJavaPublisherBuilder) {
            source1 = ((RxJavaPublisherBuilder<T>)a).current;
        } else {
            source1 = Flowable.fromPublisher(a.buildRs());
        }
        if (b instanceof RxJavaPublisherBuilder) {
            source2 = ((RxJavaPublisherBuilder<T>)b).current;
        } else {
            source2 = Flowable.fromPublisher(b.buildRs());
        }
        return new RxJavaPublisherBuilder<>(
                Flowable.concat(source1, source2));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> PublisherBuilder<T> fromCompletionStage(
            CompletionStage<? extends T> completionStage) {
        return new RxJavaPublisherBuilder<>(Flowable.fromCompletionStage((CompletionStage<T>)completionStage));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> PublisherBuilder<T> fromCompletionStageNullable(
            CompletionStage<? extends T> completionStage) {
        return new RxJavaPublisherBuilder<>(Maybe.fromCompletionStage((CompletionStage<T>)completionStage).toFlowable());
    }

    @Override
    public <T, R> ProcessorBuilder<T, R> coupled(
            SubscriberBuilder<? super T, ?> subscriber,
            PublisherBuilder<? extends R> publisher) {
        return coupled(subscriber.build(), publisher.buildRs());
    }

    @Override
    public <T, R> ProcessorBuilder<T, R> coupled(
            Subscriber<? super T> subscriber,
            Publisher<? extends R> publisher) {
        
        BasicProcessor<T> inlet = new BasicProcessor<>();
        PublishProcessor<Void> publisherActivity = PublishProcessor.create();
        PublishProcessor<Void> subscriberActivity = PublishProcessor.create();
        
        inlet
                .doOnComplete(() -> subscriberActivity.onComplete())
                .doOnError(e -> subscriberActivity.onError(e))
                .takeUntil(publisherActivity)
                .doOnCancel(() -> subscriberActivity.onComplete())
                .subscribe(subscriber);
        
        Flowable<? extends R> outlet = Flowable.fromPublisher(publisher)
                .doOnComplete(() -> publisherActivity.onComplete())
                .doOnError(e -> publisherActivity.onError(e))
                .takeUntil(subscriberActivity)
                .doOnCancel(() -> publisherActivity.onComplete())
                ;

        return new RxJavaProcessorBuilder<>(inlet, outlet);
    }

}
