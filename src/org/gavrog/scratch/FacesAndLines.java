package org.gavrog.scratch;

import java.awt.Color;

import de.jreality.geometry.IndexedFaceSetFactory;
import de.jreality.scene.Appearance;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.shader.CommonAttributes;
import de.jreality.ui.viewerapp.ViewerApp;
import de.jreality.util.CameraUtility;

public class FacesAndLines {
  
  public static void main(String[] args) {
    
    IndexedFaceSetFactory ifsf = new IndexedFaceSetFactory();
    
    double [][] vertices  = new double[][] {
        {-1, -1, 0}, {0, -1, 0}, {1, -1, 0},
        {-1,  0, 0}, {0,  0, 0}, {1,  0, 0},
        {-1,  1, 0}, {0,  1, 0}, {1,  1, 0}
    };
    
    int [][] faceIndices = new int [][] {
        {0, 1, 4, 3}, {1, 2, 5, 4}, {3, 4, 7, 6}, {4, 5, 8, 7}
    };
    
    ifsf.setVertexCount( vertices.length );
    ifsf.setVertexCoordinates( vertices );
    ifsf.setFaceCount( faceIndices.length);
    ifsf.setFaceIndices( faceIndices );
    
    ifsf.setGenerateEdgesFromFaces( true );
    ifsf.setGenerateFaceNormals( true );

    ifsf.update();
    
    SceneGraphComponent sc = new SceneGraphComponent();
    sc.setGeometry(ifsf.getIndexedFaceSet());
    Appearance a = new Appearance();
    a.setAttribute(CommonAttributes.TUBES_DRAW, false);
    a.setAttribute(CommonAttributes.VERTEX_DRAW, false);
    a.setAttribute(CommonAttributes.POLYGON_SHADER +
    		'.' + CommonAttributes.DIFFUSE_COLOR, Color.WHITE);
    sc.setAppearance(a);
    
    ViewerApp va = ViewerApp.display(sc);
    CameraUtility.encompass(va.getCurrentViewer());
 }
}
