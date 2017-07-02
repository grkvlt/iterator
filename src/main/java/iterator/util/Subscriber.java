/*
 * Copyright 2012-2017 by Andrew Kennedy.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package iterator.util;

import java.awt.Dimension;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import iterator.model.IFS;

/**
 * {@link EventBus} subscription callbacks.
 *
 * @see Subscribe
 */
public interface Subscriber {

    /** Callback for the IFS changes. */
    void updated(IFS ifs);

    /** Callback for display size changes. */
    void resized(Dimension size);

}
    