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
 * Bent Co-ordinate Transform.
 *
 * Variation 14.
 */
public class Bent extends CoordinateTransform {

    private Bent() {
        this.id = 14;
    }

    public static Bent create() {
        return new Bent();
    }

    @Override
    public Point2D apply(Point2D src) {
        double ox = sw / 2d;
        double oy = sh / 2d;
        double x = (src.getX() - ox) / ox;
        double y = (src.getY() - oy) / oy;

        double fx = ox + (ox * x);
        double fy = oy + (ox * y);
        if (x < 0) {
            fx = ox + (ox * 2d * x);
        }
        if (y < 0) {
            fy = oy + (oy * y / 2d);
        }

        return new Point2D.Double(fx, fy);
    }
}
