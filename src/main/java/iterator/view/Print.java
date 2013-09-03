/*
 * Copyright 2012-2013 by Andrew Kennedy.
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
package iterator.view;

import iterator.Explorer;
import iterator.model.IFS;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

/**
 * Printing.
 *
 * TODO Add native printing support
 */
public class Print {
    @SuppressWarnings("unused")
    private final EventBus bus;
    @SuppressWarnings("unused")
    private final Explorer controller;

    @SuppressWarnings("unused")
    private IFS ifs;

    public Print(EventBus bus, Explorer controller) {
        super();
        this.bus = bus;
        this.controller = controller;

        bus.register(this);
    }

    @Subscribe
    public void update(IFS ifs) {
        this.ifs = ifs;
    }
}
