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
 * Polar Co-ordinate Transform.
 *
 * Variation 5.
 */
public class Polar extends CoordinateTransform {

    private Polar() {
        this.id = 5;
    }

    public static Polar create() {
        return new Polar();
    }

    @Override
    public Point2D apply(Point2D src) {
        double ox = sw / 2d;
        double oy = sh / 2d;
        double u = Point2D.distance(0d, 0d, ox / 2d, oy / 2d);
        double r = Point2D.distance(ox, oy, src.getX(), src.getY()) / u;
        double scale = 1d / (r * r);
        double x = (src.getX() - ox) / u;
        double y = (src.getY() - oy) / u;
        double theta = Math.atan2(y,  x);

        double fx = ox + (u * scale * (theta / Math.PI));
        double fy = oy + (u * scale * (r - 1d));

        return new Point2D.Double(fx, fy);
    }
}
