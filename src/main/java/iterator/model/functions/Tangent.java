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
 * Tangent Co-ordinate Transform.
 * <p>
 * Variation 42.
 */
public class Tangent extends CoordinateTransform {

    private Tangent() {
        this.id = 42;
    }

    public static Tangent create() {
        return new Tangent();
    }

    @Override
    public Point2D apply(Point2D src) {
        double ox = sw / 2d;
        double oy = sh / 2d;
        double ux = ox / 4d;
        double uy = oy / 4d;
        double x = (src.getX() - ox) / ux;
        double y = (src.getY() - oy) / uy;

        double fx = ox + (ux * Math.sin(x * 2d * Math.PI) / Math.cos(y * 2d * Math.PI));
        double fy = oy + (uy * Math.tan(y * 2d * Math.PI));

        return new Point2D.Double(fx, fy);
    }
}
