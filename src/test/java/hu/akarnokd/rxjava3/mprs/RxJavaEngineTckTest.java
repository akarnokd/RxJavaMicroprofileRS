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

import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreamsFactory;
import org.eclipse.microprofile.reactive.streams.operators.spi.*;
import org.eclipse.microprofile.reactive.streams.operators.tck.ReactiveStreamsTck;
import org.reactivestreams.*;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.*;

/**
 * This test tries to have the engine used instead of the shortcuts.
 */
public class RxJavaEngineTckTest
extends ReactiveStreamsTck<ReactiveStreamsEngine> {

    public RxJavaEngineTckTest() {
        super(new TestEnvironment(50));
    }

    @Override
    protected ReactiveStreamsEngine createEngine() {
        return HideEngine.INSTANCE;
    }

    @Override
    protected ReactiveStreamsFactory createFactory() {
        return RxJavaPublisherFactory.INSTANCE;
    }

    @Override
    protected boolean isEnabled(Object test) {
        return test.toString().contains("CoupledStageVerification");
//        return true;
    }

    @BeforeSuite(alwaysRun = true)
    public void before() {
        RxJavaMicroprofilePlugins.enableBuildGraph();
        RxJavaMicroprofilePlugins.enableImmutableBuilders();
    }

    @AfterSuite(alwaysRun = true)
    public void after() {
        RxJavaMicroprofilePlugins.disableBuildGraph();
        RxJavaMicroprofilePlugins.disableImmutableBuilders();
    }
    
    enum HideEngine implements ReactiveStreamsEngine {
        INSTANCE;

        @Override
        public <T> Publisher<T> buildPublisher(Graph graph) throws UnsupportedStageException {
            return RxJavaEngine.INSTANCE.buildPublisher(graph);
        }

        @Override
        public <T, R> SubscriberWithCompletionStage<T, R> buildSubscriber(Graph graph)
                throws UnsupportedStageException {
            return RxJavaEngine.INSTANCE.buildSubscriber(graph);
        }

        @Override
        public <T, R> Processor<T, R> buildProcessor(Graph graph) throws UnsupportedStageException {
            return RxJavaEngine.INSTANCE.buildProcessor(graph);
        }

        @Override
        public <T> CompletionStage<T> buildCompletion(Graph graph) throws UnsupportedStageException {
            return RxJavaEngine.INSTANCE.buildCompletion(graph);
        }
    }
}
