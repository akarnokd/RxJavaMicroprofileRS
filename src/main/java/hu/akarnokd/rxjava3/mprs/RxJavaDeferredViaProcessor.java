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

import java.util.concurrent.atomic.AtomicBoolean;

import org.reactivestreams.*;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Flowable;

/**
 * Lets the dowsntream subscribe to the Processor first, then
 * the processor is subscribed to the source once.
 * @param <T> the element type of the upstream and the inlet of the Processor
 * @param <R> the outlet type of the Processor and the result type of the sequence
 */
final class RxJavaDeferredViaProcessor<T, R> extends Flowable<R> {

    final Flowable<T> source;
    
    final Processor<T, R> processor;
    
    final AtomicBoolean once;
    
    RxJavaDeferredViaProcessor(Flowable<T> source, Processor<T, R> processor) {
        this.source = source;
        this.processor = processor;
        this.once = new AtomicBoolean();
    }

    @Override
    protected void subscribeActual(
            @NonNull Subscriber<? super R> subscriber) {

        processor.subscribe(subscriber);
        
        if (!once.get() && once.compareAndSet(false, true)) {
            source.subscribe(processor);
        }
    }
}
