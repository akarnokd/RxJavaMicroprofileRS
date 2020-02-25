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

import java.util.Collection;

import org.eclipse.microprofile.reactive.streams.operators.spi.Stage;

/**
 * Ignores all {@link Stage}s added to it and fails with an
 * IllegalStateException when asked for the collection of stages.
 */
enum RxJavaNoopGraphBuilder implements RxJavaGraphBuilder {

    INSTANCE;

    @Override
    public Collection<Stage> getStages() {
        throw new IllegalStateException("Graph was not built for this sequence.");
    }

    @Override
    public void add(Stage stage) {
        // deliberately no-op
    }

    @Override
    public boolean isEnabled() {
        return false;
    }
}
