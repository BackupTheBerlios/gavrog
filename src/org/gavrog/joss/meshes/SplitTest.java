/*
   Copyright 2009 Olaf Delgado-Friedrichs

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


package org.gavrog.joss.meshes;

import java.io.*;

import scala.collection.mutable.*;

/**
 * @author Olaf Delgado
 * @version $Id:$
 */
public class SplitTest {
  public static void main(final String argv[]) throws IOException {
    final Mesh mesh = new Mesh(new File(argv[0]));
    final ArrayBuffer<Mesh> parts = mesh.splitByGroup();
    for (int i = 0; i < mesh.splitByGroup().size(); ++i) {
      final FileWriter writer =
        new FileWriter(String.format("zSplit_%d.obj", i));
      parts.apply(i).write((Writer) writer, "zSplit");
      writer.flush();
      writer.close();
    }
  }
}
