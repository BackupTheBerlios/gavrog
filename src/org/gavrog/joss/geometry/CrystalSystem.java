/*
Copyright 2006 Olaf Delgado-Friedrichs

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


/**
 * Represents a 3-dimensional crystal system. Currently, this is just a wrapper for the
 * strings representing the names of the systems.
 * 
 * @author Olaf Delgado
 * @version $Id: CrystalSystem.java,v 1.1 2006/04/10 23:08:17 odf Exp $
 */
public class CrystalSystem {
    final public static CrystalSystem CUBIC = new CrystalSystem("Cubic");
    final public static CrystalSystem ORTHORHOMBIC = new CrystalSystem("Orthorhombic");
    final public static CrystalSystem HEXAGONAL = new CrystalSystem("Hexagonal");
    final public static CrystalSystem TETRAGONAL = new CrystalSystem("Tetragonal");
    final public static CrystalSystem TRIGONAL = new CrystalSystem("Trigonal");
    final public static CrystalSystem MONOCLINIC = new CrystalSystem("Monoclinic");
    final public static CrystalSystem TRICLINIC = new CrystalSystem("Triclinic");
    
    private final String name;

    /**
     * Making the constructor private makes sure that no other instances than the above
     * are created.
     * 
     * @param name the name of the crystal system.
     */
    private CrystalSystem(final String name) {
        this.name = name;
    }
    
    public String toString() {
        return this.name;
    }
}