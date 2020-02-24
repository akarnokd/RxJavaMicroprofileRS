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
import java.util.function.Function;

import org.eclipse.microprofile.reactive.streams.operators.CompletionRunner;
import org.eclipse.microprofile.reactive.streams.operators.spi.ReactiveStreamsEngine;

final class RxJavaCompletionRunner<T, R> implements CompletionRunner<R> {

    final T source;
    
    final Function<T, CompletionStage<R>> transform;

    RxJavaCompletionRunner(T source, Function<T, CompletionStage<R>> transform) {
        this.source = source;
        this.transform = transform;
    }
    
    @Override
    public CompletionStage<R> run() {
        return transform.apply(source);
    }

    @Override
    public CompletionStage<R> run(ReactiveStreamsEngine engine) {
     // FIXME should we unroll the chain?
        return run();
    }

}
