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

import org.eclipse.microprofile.reactive.streams.operators.spi.*;
import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;

public final class RxJavaEngine implements ReactiveStreamsEngine {

    public static final ReactiveStreamsEngine INSTANCE = new RxJavaEngine();

    private RxJavaEngine() {
        // singleton
    }

    static void requireNull(Object o, Stage stage) {
        if (o != null) {
            throw new IllegalArgumentException("Graph already has a source-like stage! Found " + stage.getClass().getSimpleName());
        }
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked"})
    public <T> Publisher<T> buildPublisher(Graph graph)
            throws UnsupportedStageException {
        Flowable result = null;
        
        for (Stage stage : graph.getStages()) {

            if (stage instanceof Stage.PublisherStage) {
                requireNull(result, stage);
                
                Publisher publisher = ((Stage.PublisherStage)stage).getRsPublisher();
                result = Flowable.fromPublisher(publisher);
                continue;
            }
            if (stage instanceof Stage.Of) {
                requireNull(result, stage);

                Iterable iterable = ((Stage.Of)stage).getElements();
                result = Flowable.fromIterable(iterable);
                continue;
            }
            if (stage instanceof Stage.ProcessorStage) {
                requireNull(result, stage);
                
                Publisher publisher = ((Stage.ProcessorStage)stage).getRsProcessor();
                result = Flowable.fromPublisher(publisher);
                continue;
            }
            if (stage instanceof Stage.Failed) {
                requireNull(result, stage);

                Throwable throwable = ((Stage.Failed)stage).getError();
                result = Flowable.error(throwable);
                continue;
            }
            if (stage instanceof Stage.Concat) {
                requireNull(result, stage);
                Graph g1 = ((Stage.Concat)stage).getFirst();
                Graph g2 = ((Stage.Concat)stage).getSecond();
                result = Flowable.concat(buildPublisher(g1), buildPublisher(g2));
                continue;
            }
            if (stage instanceof Stage.FromCompletionStage) {
                requireNull(result, stage);
                CompletionStage cs = ((Stage.FromCompletionStage) stage).getCompletionStage();
                result = Flowable.fromCompletionStage(cs);
                continue;
            }
            if (stage instanceof Stage.FromCompletionStageNullable) {
                requireNull(result, stage);
                CompletionStage cs = ((Stage.FromCompletionStageNullable) stage).getCompletionStage();
                result = Maybe.fromCompletionStage(cs).toFlowable();
                continue;
            }
            // FIXME not sure how to handle this for a Publisher output
            if (stage instanceof Stage.Coupled) {
                requireNull(result, stage);
                throw new UnsupportedStageException(stage);
            }
            
            // ------------------------------------------------------------------------------
            if (result == null) {
                throw new IllegalArgumentException("Graph is missing a source-like stage! Found " + stage.getClass().getSimpleName());
            }
            if (stage instanceof Stage.Map) {
                Function mapper = ((Stage.Map) stage).getMapper();
                result = result.map(v -> mapper.apply(v));
                continue;
            }
            if (stage instanceof Stage.Peek) {
                Consumer consumer = ((Stage.Peek) stage).getConsumer();
                result = result.doOnNext(v -> consumer.accept(v));
                continue;
            }
            if (stage instanceof Stage.Filter) {
                Predicate predicate = ((Stage.Filter)stage).getPredicate();
                result = result.filter(v -> predicate.test(v));
                continue;
            }
            if (stage instanceof Stage.DropWhile) {
                Predicate predicate = ((Stage.DropWhile)stage).getPredicate();
                result = result.skipWhile(v -> predicate.test(v));
                continue;
            }
            if (stage instanceof Stage.Skip) {
                long n = ((Stage.Skip)stage).getSkip();
                result = result.skip(n);
                continue;
            }
            if (stage instanceof Stage.Limit) {
                long n = ((Stage.Limit)stage).getLimit();
                result = result.take(n);
                continue;
            }
            if (stage instanceof Stage.Distinct) {
                result = result.distinct();
                continue;
            }
            if (stage instanceof Stage.TakeWhile) {
                Predicate predicate = ((Stage.TakeWhile)stage).getPredicate();
                result = result.takeWhile(v -> predicate.test(v));
                continue;
            }
            if (stage instanceof Stage.FlatMap) {
                Function mapper = ((Stage.FlatMap) stage).getMapper();
                result = result.concatMap(v -> INSTANCE.buildPublisher((Graph)mapper.apply(v)));
                continue;
            }
            if (stage instanceof Stage.FlatMapCompletionStage) {
                Function mapper = ((Stage.FlatMapCompletionStage) stage).getMapper();
                result = result.concatMapSingle(v -> Single.fromCompletionStage((CompletionStage)mapper.apply(v)));
                continue;
            }
            if (stage instanceof Stage.FlatMapIterable) {
                Function mapper = ((Stage.FlatMapIterable) stage).getMapper();
                result = result.concatMapIterable(v -> (Iterable)mapper.apply(v));
                continue;
            }
            if (stage instanceof Stage.OnError) {
                Consumer consumer = ((Stage.OnError) stage).getConsumer();
                result = result.doOnError(v -> consumer.accept(v));
                continue;
            }
            if (stage instanceof Stage.OnTerminate) {
                Runnable runnable = ((Stage.OnTerminate) stage).getAction();
                result = result.doOnTerminate(() -> runnable.run());
                continue;
            }
            if (stage instanceof Stage.OnComplete) {
                Runnable runnable = ((Stage.OnComplete) stage).getAction();
                result = result.doOnComplete(() -> runnable.run());
                continue;
            }
            if (stage instanceof Stage.OnErrorResume) {
                Function mapper = ((Stage.OnErrorResume) stage).getFunction();
                result = result.onErrorReturn(e -> mapper.apply(e));
                continue;
            }
            if (stage instanceof Stage.OnErrorResumeWith) {
                Function mapper = ((Stage.OnErrorResumeWith) stage).getFunction();
                result = result.onErrorResumeNext(e -> INSTANCE.buildPublisher((Graph)mapper.apply(e)));
                continue;
            }
            // FIXME not sure how to handle this for a Publisher output
            if (stage instanceof Stage.FindFirst) {
            }
            // FIXME not sure how to handle this for a Publisher output
            if (stage instanceof Stage.SubscriberStage) {
            }
            // FIXME not sure how to handle this for a Publisher output
            if (stage instanceof Stage.Collect) {
            }
            // FIXME not sure how to handle this for a Publisher output
            if (stage instanceof Stage.Cancel) {
            }

            throw new UnsupportedStageException(stage);
        }
        
        if (result == null) {
            throw new IllegalArgumentException("The graph had no stages.");
        }
        return result;
    }

    @Override
    public <T, R> SubscriberWithCompletionStage<T, R> buildSubscriber(
            Graph graph) throws UnsupportedStageException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T, R> Processor<T, R> buildProcessor(Graph graph)
            throws UnsupportedStageException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> CompletionStage<T> buildCompletion(Graph graph)
            throws UnsupportedStageException {
        return this.<T, T>buildSubscriber(graph).getCompletion();
    }

}
