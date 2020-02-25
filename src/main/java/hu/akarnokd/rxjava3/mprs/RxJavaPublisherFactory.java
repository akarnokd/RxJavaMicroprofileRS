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

import org.eclipse.microprofile.reactive.streams.operators.*;
import org.eclipse.microprofile.reactive.streams.operators.spi.*;
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
        RxJavaPublisherBuilder<T> result = new RxJavaPublisherBuilder<>(Flowable.fromPublisher(publisher));
        if (result.graph.isEnabled()) {
            result.graph.add((Stage.PublisherStage)() -> publisher);
        }
        return result;
    }

    @Override
    public <T> PublisherBuilder<T> of(T t) {
        RxJavaPublisherBuilder<T> result = new RxJavaPublisherBuilder<>(Flowable.just(t));
        if (result.graph.isEnabled()) {
            Collection<T> coll = Collections.singletonList(t);
            result.graph.add((Stage.Of)() -> coll);
        }
        return result;
    }

    /* final: Java 8 workaround for safevarargs */
    @Override
    @SafeVarargs
    public final <T> PublisherBuilder<T> of(T... ts) {
        RxJavaPublisherBuilder<T> result = new RxJavaPublisherBuilder<>(Flowable.fromArray(ts));
        if (result.graph.isEnabled()) {
            Collection<T> coll = Arrays.asList(ts);
            result.graph.add((Stage.Of)() -> coll);
        }
        return result;
    }

    @Override
    public <T> PublisherBuilder<T> empty() {
        RxJavaPublisherBuilder<T> result = new RxJavaPublisherBuilder<>(Flowable.empty());
        if (result.graph.isEnabled()) {
            Collection<T> coll = Collections.emptyList();
            result.graph.add((Stage.Of)() -> coll);
        }
        return result;
    }

    @Override
    public <T> PublisherBuilder<T> ofNullable(T t) {
        return t != null ? of(t) : empty();
    }

    @Override
    public <T> PublisherBuilder<T> fromIterable(Iterable<? extends T> ts) {
        RxJavaPublisherBuilder<T> result = new RxJavaPublisherBuilder<>(Flowable.fromIterable(ts));
        if (result.graph.isEnabled()) {
            result.graph.add((Stage.Of)() -> ts);
        }
        return result;
    }

    @Override
    public <T> PublisherBuilder<T> failed(Throwable t) {
        RxJavaPublisherBuilder<T> result = new RxJavaPublisherBuilder<>(Flowable.error(t));
        if (result.graph.isEnabled()) {
            result.graph.add((Stage.Failed)() -> t);
        }
        return result;
    }

    @Override
    public <T> ProcessorBuilder<T, T> builder() {
        DeferredProcessor<T> processor = new DeferredProcessor<>();
        RxJavaProcessorBuilder<T, T> result = new RxJavaProcessorBuilder<>(processor, processor);
        if (result.graph.isEnabled()) {
            result.graph.add((Stage.ProcessorStage)() -> processor);
        }
        return result;
    }

    @Override
    public <T, R> ProcessorBuilder<T, R> fromProcessor(
            Processor<? super T, ? extends R> processor) {
        RxJavaProcessorBuilder<T, R> result = new RxJavaProcessorBuilder<>(processor, Flowable.fromPublisher(processor));
        if (result.graph.isEnabled()) {
            result.graph.add((Stage.ProcessorStage)() -> processor);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> SubscriberBuilder<T, Void> fromSubscriber(
            Subscriber<? extends T> subscriber) {
        // FIXME the signature is wrong, at least let it compile
        // TODO graph specificiation?
        return new RxJavaSubscriberBuilder<>((Subscriber<T>)subscriber);
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> PublisherBuilder<T> iterate(T seed, UnaryOperator<T> f) {
        RxJavaPublisherBuilder<T> result = new RxJavaPublisherBuilder<>(Flowable.generate(() -> seed, (s, e) -> {
            e.onNext(s);
            return f.apply(s);
        }));
        if (result.graph.isEnabled()) {
            // TODO there is no Stage.Iterate
            result.graph.add((Stage.Of)() -> () -> (Iterator)new Iterator<T>() {
                
                T last = seed;
                boolean once;
                
                @Override
                public boolean hasNext() {
                    return true;
                }
                
                @Override
                public T next() {
                    if (!once) {
                        once = true;
                        return last;
                    }
                    T o = f.apply(last);
                    last = o;
                    return o;
                    
                }
            });
        }
        return result;
    }

    @Override
    public <T> PublisherBuilder<T> generate(Supplier<? extends T> s) {
        RxJavaPublisherBuilder<T> result = new RxJavaPublisherBuilder<>(Flowable.generate(e -> e.onNext(s.get())));
        if (result.graph.isEnabled()) {
            // TODO there is no Stage.Generate
            result.graph.add((Stage.Of)() -> () -> new Iterator<Object>() {
                @Override
                public boolean hasNext() {
                    return true;
                }
                
                @Override
                public Object next() {
                    return s.get();
                }
            });
        }
        return result;
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
        RxJavaPublisherBuilder<T> result = new RxJavaPublisherBuilder<>(
                Flowable.concat(source1, source2));
        
        if (result.graph.isEnabled()) {
            result.graph.add(new Stage.Concat() {
                
                @Override
                public Graph getSecond() {
                    return RxJavaGraphCaptureEngine.capture(b);
                }
                
                @Override
                public Graph getFirst() {
                    return RxJavaGraphCaptureEngine.capture(a);
                }
            });
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> PublisherBuilder<T> fromCompletionStage(
            CompletionStage<? extends T> completionStage) {
        RxJavaPublisherBuilder<T> result = new RxJavaPublisherBuilder<>(
                Flowable.fromCompletionStage((CompletionStage<T>)completionStage));
        if (result.graph.isEnabled()) {
            result.graph.add((Stage.FromCompletionStage)() -> completionStage);
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> PublisherBuilder<T> fromCompletionStageNullable(
            CompletionStage<? extends T> completionStage) {
        RxJavaPublisherBuilder<T> result = new RxJavaPublisherBuilder<>(
                        Maybe.fromCompletionStage((CompletionStage<T>)completionStage).toFlowable());
        if (result.graph.isEnabled()) {
            result.graph.add((Stage.FromCompletionStageNullable)() -> completionStage);
        }
        return result;
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

        RxJavaProcessorBuilder<T, R> result = new RxJavaProcessorBuilder<>(inlet, outlet);
        if (result.graph.isEnabled()) {
            result.graph.add(new Stage.Coupled() {

                @Override
                public Graph getSubscriber() {
                    Collection<Stage> coll = Collections.singletonList((Stage.SubscriberStage)() -> subscriber);
                    return (Graph)() -> coll;
                }

                @Override
                public Graph getPublisher() {
                    Collection<Stage> coll = Collections.singletonList((Stage.PublisherStage)() -> publisher);
                    return (Graph)() -> coll;
                }
                
            });
        }
        return result;
    }

}
