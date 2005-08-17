/*
Copyright 2005 Olaf Delgado-Friedrichs

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package org.gavrog.joss.geometry;

import org.gavrog.jane.numbers.IArithmetic;

/**
 * @author Olaf Delgado
 * @version $Id: IOperator.java,v 1.2 2005/08/17 02:01:17 odf Exp $
 */
public interface IOperator extends IArithmetic {
    public int getDimension();
    public IOperator getLinearPart();
    public Point getImageOfOrigin();
    public Point applyTo(Point p);
}
