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
package iterator.model.functions;

import java.awt.geom.Point2D;

/**
 * Identity Co-ordinate Transform.
 */
public class Identity extends CoordinateTransform {

    private Identity() {
        this.id = 0;
    }

    public static Identity create() {
        return new Identity();
    }

    @Override
    public Point2D apply(Point2D src) {
        return src;
    }
}
