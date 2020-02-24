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

import static org.junit.Assert.fail;

import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreamsFactory;
import org.jboss.arquillian.container.test.spi.util.ServiceLoader;
import org.junit.Test;

public class RxJavaPublisherFactoryTest {

    @Test
    public void serviceLoader() {
        for (ReactiveStreamsFactory rsf : ServiceLoader.load(ReactiveStreamsFactory.class)) {
            if (rsf instanceof RxJavaPublisherFactory) {
                return;
            }
        }
        fail("Service loader didn't return RxJavaPublisherFactory");
    }
}
