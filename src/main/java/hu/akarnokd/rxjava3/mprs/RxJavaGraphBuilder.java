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

import org.eclipse.microprofile.reactive.streams.operators.spi.*;

/**
 * Base interface for collecting up graph {@link Stage}s. 
 */
interface RxJavaGraphBuilder extends Graph {

    /**
     * @return if true, this builder will accept {@link Stage}s;
     *         if false, this builder will ignore those stages
     */
    boolean isEnabled();

    /**
     * Add a new {@link Stage} to this graph.
     * @param stage the stage to add, not null (not verified)
     */
    void add(Stage stage);
}
