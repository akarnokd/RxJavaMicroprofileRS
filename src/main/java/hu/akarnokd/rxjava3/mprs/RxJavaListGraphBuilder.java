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

import org.eclipse.microprofile.reactive.streams.operators.spi.Stage;

/**
 * Collects the various {@link Stage} instances into an internal {@link List}.
 */
public class RxJavaListGraphBuilder implements RxJavaGraphBuilder {

    final List<Stage> stages = new ArrayList<>();
    
    @Override
    public Collection<Stage> getStages() {
        return stages;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void add(Stage stage) {
        stages.add(stage);
    }

}
