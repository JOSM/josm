/**
 * Copyright 2012-2017 Michael Tamm and other junit-toolbox contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openstreetmap.josm.testutils;

import org.junit.runners.Parameterized;

/**
 * An extension of the JUnit {@link Parameterized} runner,
 * which executes the tests for each parameter set concurrently.
 * <p>The maximum number of test threads will be the number of
 * {@link Runtime#availableProcessors() available processors}.
 *
 * @author Michael Tamm (junit-toolbox)
 */
public class ParallelParameterized extends Parameterized {

    /**
     * Constructs a new {@code ParallelParameterized}.
     * @param klass the root of the suite
     * @throws Throwable in case of error
     */
    public ParallelParameterized(Class<?> klass) throws Throwable {
        super(klass);
        setScheduler(new ParallelScheduler());
    }
}
