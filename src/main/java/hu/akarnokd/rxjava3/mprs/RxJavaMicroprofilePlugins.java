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

/**
 * Hooks and global settings for the RxJava Microprofile Reactive Streams Operators
 * services.
 */
public final class RxJavaMicroprofilePlugins {

    /** Static-only class. */
    private RxJavaMicroprofilePlugins() {
        throw new IllegalStateException("No instances!");
    }
    
    static volatile boolean BUILD_GRAPH;

    /**
     * Globally enable building the Stage graph along with the
     * Flowable graph.
     */
    public static void enableBuildGraph() {
        BUILD_GRAPH = true;
    }

    /**
     * Globally disable building the Stage graph along with the
     * Flowable graph.
     */
    public static void disableBuildGraph() {
        BUILD_GRAPH = false;
    }

    public static boolean buildGraph() {
        return BUILD_GRAPH;
    }
}
