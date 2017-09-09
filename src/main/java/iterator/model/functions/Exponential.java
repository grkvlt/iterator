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
 * Exponential Co-ordinate Transform.
 * <p>
 * Variation 18.
 */
public class Exponential extends CoordinateTransform {

    private Exponential() {
        this.id = 18;
    }

    public static Exponential create() {
        return new Exponential();
    }

    @Override
    public Point2D apply(Point2D src) {
        double ox = sw / 2d;
        double oy = sh / 2d;
        double x = (src.getX() - ox) / ox;
        double y = (src.getY() - oy) / oy;
        double e = Math.exp(x - 1d);

        double fx = ox + (ox * e * Math.cos(y * 2d * Math.PI));
        double fy = oy + (oy * e * Math.sin(y * 2d * Math.PI));

        return new Point2D.Double(fx, fy);
    }
}
