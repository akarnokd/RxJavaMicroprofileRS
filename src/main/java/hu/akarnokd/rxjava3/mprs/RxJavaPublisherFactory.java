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
import java.util.concurrent.*;
import java.util.function.*;

import org.eclipse.microprofile.reactive.streams.operators.*;
import org.eclipse.microprofile.reactive.streams.operators.spi.*;
import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;

/**
 * Constructs RxJava-based PublisherBuilders.
 * <p>
 * @apiNote this has to be with a public constructor for the ServiceLoader to work
 */
public final class RxJavaPublisherFactory implements ReactiveStreamsFactory {

    /** The (almost) singleton instance. */
    public static final ReactiveStreamsFactory INSTANCE = new RxJavaPublisherFactory();

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
        RxJavaProcessorBuilder<T, T> result = new RxJavaProcessorBuilder<>();
        /*
        if (result.graph.isEnabled()) {
            result.graph.add((Stage.ProcessorStage)() -> new DeferredProcessor<>());
        }
        */
        return result;
    }

    @Override
    public <T, R> ProcessorBuilder<T, R> fromProcessor(
            Processor<? super T, ? extends R> processor) {
        Objects.requireNonNull(processor, "processor is null");
        RxJavaProcessorBuilder<T, R> result = new RxJavaProcessorBuilder<>(processor);
        if (result.graph.isEnabled()) {
            result.graph.add((Stage.ProcessorStage)() -> processor);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> SubscriberBuilder<T, Void> fromSubscriber(
            Subscriber<? extends T> subscriber) {
        Objects.requireNonNull(subscriber, "processor is null");
        // FIXME the signature is wrong, at least let it compile
        RxJavaSubscriberBuilder<T> result = new RxJavaSubscriberBuilder<>((Subscriber<T>)subscriber);
        if (result.graph.isEnabled()) {
            result.graph.add((Stage.SubscriberStage)() -> subscriber);
        }
        return result;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> PublisherBuilder<T> iterate(T seed, UnaryOperator<T> f) {
        Objects.requireNonNull(f, "f is null");
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
        Objects.requireNonNull(s, "s is null");
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
                new FlowableConcatCanceling<>(source1, source2));
        
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

    @SuppressWarnings("unchecked")
    @Override
    public <T, R> ProcessorBuilder<T, R> coupled(
            SubscriberBuilder<? super T, ?> subscriber,
            PublisherBuilder<? extends R> publisher) {
        Objects.requireNonNull(subscriber, "subscriber is null");
        Objects.requireNonNull(publisher, "publisher is null");

        RxJavaProcessorBuilder<T, R> result = new RxJavaProcessorBuilder<>();
        
        result.transformers.add(f -> {
            @SuppressWarnings("rawtypes")
            Processor p = coupledBuildProcessor(subscriber.build(), publisher.buildRs());
            f.subscribe(p);
            return p;
        });
                
        if (result.graph.isEnabled()) {
            Graph subscriberGraph = RxJavaGraphCaptureEngine.capture(subscriber);
            Graph publisherGraph = RxJavaGraphCaptureEngine.capture(publisher);
            result.graph.add(new Stage.Coupled() {

                @Override
                public Graph getSubscriber() {
                    return subscriberGraph;
                }

                @Override
                public Graph getPublisher() {
                    return publisherGraph;
                }
                
            });
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T, R> ProcessorBuilder<T, R> coupled(
            Subscriber<? super T> subscriber,
            Publisher<? extends R> publisher) {
        Objects.requireNonNull(subscriber, "subscriber is null");
        Objects.requireNonNull(publisher, "publisher is null");

        RxJavaProcessorBuilder<T, R> result = new RxJavaProcessorBuilder<>();
        result.transformers.add(f -> {
            @SuppressWarnings("rawtypes")
            Processor p = coupledBuildProcessor(subscriber, publisher);
            f.subscribe(p);
            return p;
        });
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

    static <T, R> RxJavaProcessorBuilder<T, R> coupledBuild(Subscriber<? super T> subscriber,
            Publisher<? extends R> publisher) {
        return new RxJavaProcessorBuilder<>(coupledBuildProcessor(subscriber, publisher));
    }
    
    static <T, R> Processor<T, R> coupledBuildProcessor(Subscriber<? super T> subscriber,
            Publisher<? extends R> publisher) {
        
        BasicProcessor<T> inlet = new BasicProcessor<>();
        CompletableFuture<Object> publisherActivity = new CompletableFuture<>();
        CompletableFuture<Object> subscriberActivity = new CompletableFuture<>();
        
        inlet
                .doOnComplete(() -> complete(subscriberActivity))
                .doOnError(e -> fail(subscriberActivity, e))
                .compose(f -> new FlowableTakeUntilCompletionStage<>(f, publisherActivity))
                .doOnCancel(() -> complete(subscriberActivity))
                .subscribe(subscriber);
        
        Flowable<? extends R> outlet = Flowable.fromPublisher(publisher)
                .doOnComplete(() -> complete(publisherActivity))
                .doOnError(e -> fail(publisherActivity, e))
                .compose(f -> new FlowableTakeUntilCompletionStage<>(f, subscriberActivity))
                .doOnCancel(() -> complete(publisherActivity))
                ;

        return new FlowableProcessorBridge<>(inlet, outlet);
    }

    static void complete(CompletableFuture<Object> cf) {
        ForkJoinPool.commonPool().submit(() -> {
            cf.complete(null);
            return null;
        });
    }

    static void fail(CompletableFuture<Object> cf, Throwable ex) {
        ForkJoinPool.commonPool().submit(() -> {
            cf.completeExceptionally(ex);
            return null;
        });
    }
}
