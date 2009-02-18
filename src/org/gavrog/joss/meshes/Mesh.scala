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


package org.gavrog.joss.meshes

import java.io._

import scala.collection.mutable._
import scala.io.Source
import Sums._
import Vectors._

object Mesh {
  class Chamber {
    private var _vertex  : Vertex        = null
    private var _cell    : Cell          = null
    private var _tVertex : TextureVertex = null
    private var _normal  : Normal        = null
    private var _s0      : Chamber       = null
    private var _s1      : Chamber       = null
    private var _s2      : Chamber       = null
    
    def vertex = _vertex
    def vertexNr = if (vertex != null) vertex.nr else 0
    def vertex_=(v : Vertex) {
      _vertex = v
      if (v != null && v.chamber == null) v.chamber = this
    }
    
    def tVertex = _tVertex
    def tVertexNr = if (tVertex != null) tVertex.nr else 0
    def tVertex_=(t : TextureVertex) {
      _tVertex = t
      if (t != null && t.chamber == null) t.chamber = this
    }
    
    def normal = _normal
    def normalNr = if (normal != null) normal.nr else 0
    def normal_=(n : Normal) {
      _normal = n
      if (n != null && n.chamber == null) n.chamber = this
    }
    
    def cell = _cell
    def cell_=(c : Cell) {
      _cell = c
      if (c != null && c.chamber == null) c.chamber = this
    }
    
    def this(v: Vertex, c: Cell) = {
      this()
      vertex = v
      cell = c
    }
    
    def s0 = _s0
    def s0_=(ch : Chamber) {
      if (_s0 != null) _s0._s0 = null
      if (ch._s0 != null) ch._s0._s0 = null
      _s0 = ch
      ch._s0 = this
    }
    def s1 = _s1
    def s1_=(ch : Chamber) {
      if (_s1 != null) _s1._s1 = null
      if (ch._s1 != null) ch._s1._s1 = null
      _s1 = ch
      ch._s1 = this
    }
    def s2 = _s2
    def s2_=(ch : Chamber) {
      if (_s2 != null) _s2._s2 = null
      if (ch._s2 != null) ch._s2._s2 = null
      _s2 = ch
      ch._s2 = this
    }
    
    def nextAtFace   = s0.s1
    def prevAtFace   = s1.s0
    def nextAtVertex = s1.s2
    def prevAtVertex = s2.s1
    def opposite     = s0.s2
    
    def start        = vertex
    def end          = s0.vertex
    
    def onTextureBorder = tVertex != s2.tVertex || s0.tVertex != s0.s2.tVertex
    
    override def toString = "Chamber(%s -> %s, %s)" format (start, end, cell)
  }

  class Vertex(number : Int, xpos : Double, ypos : Double, zpos : Double) {
    var nr : Int     = number
    var x  : Double  = xpos
    var y  : Double  = ypos
    var z  : Double  = zpos

    private var _ch : Chamber = null	
    def chamber = _ch
    def chamber_=(c: Chamber) {
      _ch = c
      if (c != null && c.vertex != this) c.vertex = this
    }
    
    def cellChambers = {
      def from(c: Chamber) : Stream[Chamber] =
        if (c == _ch) Stream.empty else Stream.cons(c, from(c.nextAtVertex))
      if (_ch == null) Stream.empty else Stream.cons(_ch, from(_ch.nextAtVertex))
    }
    
    def faces = cellChambers.map(_.cell)
    def degree = cellChambers.size
    def neighbors = cellChambers.map(_.s0.vertex)
    
    override def toString = "Vertex(%d: %f, %f, %f)" format (nr, x, y, z)

    def pos = Vec3(x, y, z)
    def moveTo(p: Vec3) { x = p.x; y = p.y; z = p.z }
  }
  implicit def vertex2vec3(v: Vertex) = v.pos

  class TextureVertex(number : Int, xpos : Double, ypos : Double) {
    var nr      : Int     = number
    var x       : Double  = xpos
    var y       : Double  = ypos
   
    private var _ch : Chamber = null	
    def chamber = _ch
    def chamber_=(c: Chamber) {
      _ch = c
      if (c != null && c.tVertex != this) c.tVertex = this
    }
    
    def cellChambers = chamber.vertex.cellChambers
    def faces = cellChambers.filter(_.tVertex == this).map(_.cell)
    def onBorder = cellChambers.exists(_.onTextureBorder)

    def degree = cellChambers.sum(c =>
      if (c.tVertex == this || c.s2.tVertex == this)
        if (c.tVertex == c.s2.tVertex && c.s0.tVertex != c.s2.s0.tVertex) 2
        else 1
      else 0
    )
    
    override def toString = "TextureVertex(%d: %f, %f)" format (nr, x, y)

    def pos = Vec2(x, y)
    def moveTo(p: Vec2) { x = p.x; y = p.y }
  }
  implicit def tVertex2vec2(t: TextureVertex) = t.pos

  class Normal(number : Int, xpos : Double, ypos : Double, zpos : Double) {
    var nr   : Int    = number
    var x    : Double = xpos
    var y    : Double = ypos
    var z    : Double = zpos
   
    private var _ch : Chamber = null	
    def chamber = _ch
    def chamber_=(c: Chamber) {
      _ch = c
      if (c != null && c.normal != this) c.normal = this
    }
    
    override def toString = "Normal(%d: %f, %f, %f)" format (nr, x, y, z)

    def value = Vec3(x, y, z)
    def changeTo(p: Vec3) { x = p.x; y = p.y; z = p.z }
  }
  implicit def normal2vec3(n: Normal) = n.value

  class Cell {
    var group    : Group    = null
    var material : Material = null
    var smoothingGroup : Int = 0
    
    private var _ch : Chamber = null	
    def chamber = _ch
    def chamber_=(c: Chamber) {
      _ch = c
      if (c != null && c.cell != this) c.cell = this
    }
    
    def vertexChambers = {
      def from(c: Chamber) : Stream[Chamber] =
        if (c == _ch) Stream.empty else Stream.cons(c, from(c.nextAtFace))
      Stream.cons(_ch, from(_ch.nextAtFace))
    }
    
    def vertices = vertexChambers.map(_.vertex)
    def textureVertices = vertexChambers.map(_.tVertex)
    def normals = vertexChambers.map(_.normal)
    def degree = vertexChambers.size
    
    def formatVertices(v0 : Int, vt0 : Int, vn0 : Int) = {
      val buf = new StringBuilder(50)
      for (ch <- vertexChambers) {
        val t = if (ch.tVertex != null) ch.tVertex.nr + vt0 else ""
        val n = if (ch.normal  != null) ch.normal.nr  + vn0 else ""
        buf.append(" %d/%s/%s" format (ch.start.nr + v0, t, n))
      }
      buf.toString
    }
    
    def formatVertices : String = formatVertices(0, 0, 0)
    
    override def toString = "Face(%s; group = '%s', material = '%s')" format
      (formatVertices, group.name, material.name)
  }
  
  case class Face() extends Cell {
    override def hashCode = chamber.hashCode
    override def equals(that: Any) =
      that.isInstanceOf[Face] && that.asInstanceOf[Face].chamber == this.chamber
  }
  case class Hole() extends Cell {
    override def hashCode = chamber.hashCode
    override def equals(that: Any) =
      that.isInstanceOf[Hole] && that.asInstanceOf[Hole].chamber == this.chamber
  }

  case class Group(name : String) {
    override def toString = "Group(" + name + ")"
  }
  
  case class Material(name : String) {
    override def toString = "Material(" + name + ")"
  }

  case class Component(mesh : Mesh, faces : Set[Face], vertices : Set[Vertex]) {
    def coarseningClassifications = faces.elements.next.vertices
      .map(mesh.classifyForCoarsening(_)).filter(null !=)
  }
  
  case class Chart(mesh : Mesh,
                   faces : Set[Face], vertices : Set[TextureVertex])
  
  case class VertexClassification(wasVertex:     Set[Vertex],
                                  wasEdgeCenter: Set[Vertex],
                                  wasFaceCenter: Set[Vertex])
  {
    def cost(v: Vertex) = {
      var n = 0
      if (wasFaceCenter(v)) {
        for (c <- v.cellChambers) {
          val d = c.s2
          if (c.tVertex != d.tVertex) n += 3
          if (c.cell.material != d.cell.material) n += 1
          if (c.cell.group != d.cell.group) n += 1
        }
      }
      n
    }

    def cost : Int = wasFaceCenter.sum(cost(_))
    def isStrict = cost == 0
  }

  def read(filename : String, joinObjects: Boolean) : Seq[Mesh] =
    read(Source.fromFile(filename), joinObjects)
  def read(filename : String) : Seq[Mesh] =
    read(Source.fromFile(filename))
  def read(in : java.io.InputStream, joinObjects: Boolean) : Seq[Mesh] =
    read(Source.fromInputStream(in), joinObjects)
  def read(in : java.io.InputStream) : Seq[Mesh] =
    read(Source.fromInputStream(in))
  
  def read(source: Source) : Seq[Mesh] = read(source, false)
  
  def read(source: Source, joinObjects: Boolean) : Seq[Mesh] = {
    var mesh : Mesh = new Mesh("unnamed")
    val faces = new ArrayBuffer[Tuple6[Seq[Int], Seq[Int], Seq[Int],
                                       Mesh.Group, Mesh.Material, Int]]
    var v_base  = 0
    var vt_base = 0
    var vn_base = 0
    var group    : Group    = null
    var material : Material = null
    var smoothingGroup = 0
    val result = new ArrayBuffer[Mesh]
    val mtllib = new HashMap[String, String]
    
    for(raw <- source.getLines;
        line = raw.trim
        if line.length > 0 && !line.startsWith("#")) {
      val fields : Seq[String] = line.split("\\s+")
      val cmd = fields(0)
      val pars = fields.slice(1, fields.length)

      cmd match {
      case "o" => {
        if (!joinObjects) {
          if (mesh.numberOfVertices > 0) {
            mesh.addFaces(faces)
            mesh.fixHoles
            result += mesh
            v_base += mesh.numberOfVertices
            vt_base += mesh.numberOfTextureVertices
            vn_base += mesh.numberOfNormals
          }
          faces.clear
          mesh = new Mesh(pars(0))
          mesh.mtllib ++ mtllib
        }
      }
      case "mtllib" => {
        try {
          var curmtl: String = null
          for(raw <- Source.fromFile(pars(0)).getLines;
              line = raw.trim
              if line.length > 0 && !line.startsWith("#")) {
            val fields : Seq[String] = line.split("\\s+")
            if (fields(0) == "newmtl") {
              curmtl = fields(1)
              mtllib(curmtl) = ""
            }
            else mtllib(curmtl) += line + "\n"
          }
        } catch {
          case e: FileNotFoundException => System.err.println(
            "WARNING: material file %s not found." format pars(0))
        }
      }
      case "v"  => {
        mesh.addVertex(pars(0).toDouble, pars(1).toDouble, pars(2).toDouble)
      }
      case "vn" => {
        mesh.addNormal(pars(0).toDouble, pars(1).toDouble, pars(2).toDouble)
      }
      case "vt" => {
        mesh.addTextureVertex(pars(0).toDouble, pars(1).toDouble)
      }
      case "g"  => {
        if (pars.size > 0) group = mesh group pars(0)
      }
      case "usemtl"  => {
        if (pars.size > 0) material = mesh material pars(0)
      }
      case "s" => {
        if (pars.size > 0) smoothingGroup = pars(0).toInt
      }
      case "f"  => {
        val n  = pars.length
        val fc = new ArrayBuffer[Int]
        val ft = new ArrayBuffer[Int]
        val fn = new ArrayBuffer[Int]
        pars.foreach { s =>
          val parts = (s + "/0").split("/").map(
            (s : String) => if (s.length == 0) 0 else s.toInt)
          fc += parts(0)
          ft += parts(1)
          fn += parts(2)
        }
        if (group == null) group = mesh.group("_default")
        if (material == null) material = mesh.material("_default")
        faces += (fc.map(-v_base +), ft.map(-vt_base +), fn.map(-vn_base +),
                  group, material, smoothingGroup)
      }
      case _ => println("?? " + cmd + "(" + pars.mkString(", ") + ")")
      }
    }
    if (mesh.numberOfVertices > 0) {
      mesh.addFaces(faces)
      mesh.fixHoles
      result += mesh
    }
    result
  }

  def write(target: OutputStream, meshes: Seq[Mesh], basename: String) : Unit =
    write(new OutputStreamWriter(target), meshes, basename)

  def write(target : Writer, meshes : Seq[Mesh], basename: String) {
    val writer = new BufferedWriter(target)
    var v_base  = 0
    var vt_base = 0
    var vn_base = 0
    
    if (basename != null) {
      val mtllib = new HashMap[String, String]
      for (mesh <- meshes) mtllib ++ mesh.mtllib
      val mtl = new BufferedWriter(new FileWriter("%s.mtl" format basename))
      for ((name, definition) <- mtllib)
        mtl.write("newmtl %s\n%s\n" format (name, definition))
      mtl.flush
      mtl.close
      writer.write("mtllib %s.mtl\n" format basename)
    }
    
    for (mesh <- meshes) {
      writer.write("o %s\n" format mesh.name)
      for (v <- mesh.vertices)
        writer.write("v %f %f %f\n" format (v.x, v.y, v.z))
      for (v <- mesh.normals)
        writer.write("vn %f %f %f\n" format (v.x, v.y, v.z))
      for (v <- mesh.textureVertices)
        writer.write("vt %f %f\n" format (v.x, v.y))
      
      val parts = new LinkedHashMap[(Group, Material, Int), Buffer[Face]]
      var useSmoothing = false
      for (f <- mesh.faces) {
        parts.getOrElseUpdate((f.group, f.material, f.smoothingGroup),
                              new ArrayBuffer[Face]) += f
        useSmoothing ||= (f.smoothingGroup != 0)
      }
      for (((group, material, smoothingGroup), faces) <- parts) {
        writer.write("g %s\n" format group.name)
        writer.write("usemtl %s\n" format material.name)
        if (useSmoothing) writer.write("s %d\n" format smoothingGroup)
        for (f <- faces) writer.write(
            "f %s\n" format f.formatVertices(v_base, vt_base, vn_base))
      }
      v_base  += mesh.numberOfVertices
      vt_base += mesh.numberOfTextureVertices
      vn_base += mesh.numberOfNormals
    }
    writer.flush()
  }
  
  def matchTopologies(ch1: Chamber, ch2: Chamber,
                      uvs: Boolean) : Map[Chamber, Chamber] = {
    val seen1 = new HashSet[Chamber]
    val seen2 = new HashSet[Chamber]
    val queue = new Queue[(Chamber, Chamber)]
    val map   = new HashMap[Chamber, Chamber]
    seen1 += ch1
    seen2 += ch2
    queue += (ch1, ch2)
    map(ch1) = ch2
    
    def neighbors(c: Chamber) =
      List(c.s0, c.s1, if (!(uvs && c.onTextureBorder)) c.s2 else null)
    
    while (queue.length > 0) {
      val (d1, d2) = queue.dequeue
      //if (d1.start.degree != d2.start.degree) return null
      for ((e1, e2) <- neighbors(d1).zip(neighbors(d2))) {
        if ((e1 == null) != (e2 == null)) return null
        if (e1 != null) {
	        if (seen1(e1) != seen2(e2)) return null
	        if (e1.cell.getClass != e2.cell.getClass) return null
	        if (seen1(e1)) {
	          if (map(e1) != e2) return null
	        } else {
	          queue += (e1, e2)
	          seen1 += e1
	          seen2 += e2
	          map(e1) = e2
	        }
         }
      }
    }
    map
  }
  
  //TODO avoid code duplication in the following
  def allMatches(c1 : Component,
                 c2 : Component) : Seq[Map[Chamber, Chamber]] = {
    val result = new ArrayBuffer[Map[Chamber, Chamber]]
    if (c1.vertices.size != c2.vertices.size)
      return result
    
    val counts = new HashMap[Int, Int]
    for (v <- c1.vertices) {
      val d = v.degree
      counts(d) = counts.getOrElse(d, 0) + 1
    }
    var (bestD, minN) = (0, c1.vertices.size + 1)
    for ((d, n) <- counts)
      if (n > 0 && n < minN) {
        bestD = d
        minN = n
      }

    val v1 = c1.vertices.filter(_.degree == bestD).elements.next
    val ch1 = v1.chamber
    for (v2 <- c2.vertices.filter(_.degree == bestD)) {
      var ch2 = v2.chamber
      do {
        for (d <- List(ch2, ch2.s1)) {
          val map = matchTopologies(ch1, d, false)
          if (map != null) result += map
        }
        ch2 = ch2.nextAtVertex
      } while (ch2 != v2.chamber)
    }
    
    result
  }
  
  def distance(map : Map[Chamber, Chamber]) : Double = {
    val verts = new HashSet[Vertex]
    for ((c, d) <- map) verts += c.vertex
    
    var dist : Double = 0
    for (v <- verts) {
      val w = map(v.chamber).start
      val (dx, dy, dz) = (w.x - v.x, w.y - v.y, w.z - v.z)
      dist += dx * dx + dy * dy + dz * dz
    }
    dist
  }
  
  def bestMatch(c1 : Component, c2 : Component) : Map[Chamber, Chamber] = {
    var best : Map[Chamber, Chamber] = null
    var dist = Double.MaxValue
    
    for (map <- allMatches(c1, c2)) {
      val d = distance(map)
      if (d < dist) {
        dist = d
        best = map
      }
    }
    best
  }
  
  def allMatches(c1 : Chart, c2 : Chart) : Seq[Map[Chamber, Chamber]] = {
    val result = new ArrayBuffer[Map[Chamber, Chamber]]
    if (c1.vertices.size != c2.vertices.size)
      return result
    
    val counts = new HashMap[Int, Int]
    for (v <- c1.vertices if v.onBorder) {
      val d = v.degree
      counts(d) = counts.getOrElse(d, 0) + 1
    }
    var (bestD, minN) = (0, c1.vertices.size + 1)
    for ((d, n) <- counts)
      if (n > 0 && n < minN) {
        bestD = d
        minN = n
      }

    val v1 =
      c1.vertices.filter(v => v.onBorder && v.degree == bestD).elements.next
    val ch1 = v1.chamber
    for (v2 <- c2.vertices.filter(v => v.degree == bestD && v.onBorder)) {
      for (ch2 <- v2.cellChambers;
           d <- List(ch2, ch2.s1)
           if (d.cell.isInstanceOf[Face]
               && c2.faces.contains(d.cell.asInstanceOf[Face]))) {
        val map = matchTopologies(ch1, d, true)
        if (map != null) result += map
      }
    }
    
    result
  }
  
  def textureDistance(map : Map[Chamber, Chamber]) : Double = {
    val verts = new HashSet[TextureVertex]
    for ((c, d) <- map) verts += c.tVertex
    
    var dist : Double = 0
    for (v <- verts) {
      val w = map(v.chamber).tVertex
      val (dx, dy) = (w.x - v.x, w.y - v.y)
      dist += dx * dx + dy * dy
    }
    dist
  }
  
  def bestMatch(c1 : Chart, c2 : Chart) : Map[Chamber, Chamber] = {
    var best : Map[Chamber, Chamber] = null
    var dist = Double.MaxValue
    
    for (map <- allMatches(c1, c2)) {
      val d = textureDistance(map)
      if (d < dist) {
        dist = d
        best = map
      }
    }
    best
  }
}

class Mesh(s : String) {
  import Mesh._

  val name = s
  
  private val _vertices = new ArrayBuffer[Vertex]
  private val _normals  = new ArrayBuffer[Normal]
  private val _texverts = new ArrayBuffer[TextureVertex]
  private val _faces    = new ArrayBuffer[Face]
  private val _holes    = new ArrayBuffer[Hole]
  private val _chambers = new ArrayBuffer[Chamber]
  private val _groups   = new LinkedHashMap[String, Group]
  private val _mats     = new LinkedHashMap[String, Material]
  private val _edges    = new HashMap[Edge, Chamber]

  val mtllib = new HashMap[String, String]

  class Edge(v : Vertex, w : Vertex) {
    val (from, to) = if (v.nr <= w.nr) (v, w) else (w, v)
    
    override def equals (other : Any) = other.isInstanceOf[Edge] && {
      val e = other.asInstanceOf[Edge]
      e.from.nr == from.nr && e.to.nr == to.nr
    }
    
    override def hashCode = from.nr * 37 + to.nr
  }
  
  def addVertex(p : Vec3) : Vertex = addVertex(p.x, p.y, p.z)
  
  def addVertex(x : Double, y : Double, z : Double) : Vertex = {
    val v = new Vertex(_vertices.size + 1, x, y, z)
    _vertices += v
    v
  }
  def numberOfVertices = _vertices.size
  def vertices         = _vertices.elements
  def vertex(n : Int)  =
    if (n > 0 && n <= numberOfVertices) _vertices(n - 1) else null
  
  def clearNormals = _normals.clear
  
  def addNormal(p: Vec3) : Normal = addNormal(p.x, p.y, p.z)
  
  def addNormal(x : Double, y : Double, z : Double) : Normal = {
    val n = new Normal(_normals.size + 1, x, y, z)
    _normals += n
    n
  }
  def numberOfNormals  = _normals.size
  def normals          = _normals.elements
  def normal(n : Int)  =
    if (n > 0 && n <= numberOfNormals) _normals(n - 1) else null

  def clearTextureVertices = _texverts.clear

  def addTextureVertex(p: Vec2) : TextureVertex = addTextureVertex(p.x, p.y)
  
  def addTextureVertex(x : Double, y : Double) : TextureVertex = {
    val t = new TextureVertex(_texverts.size + 1, x, y)
    _texverts += t
    t
  }
  def numberOfTextureVertices = _texverts.size
  def textureVertices         = _texverts.elements
  def textureVertex(n : Int)  =
    if (n > 0 && n <= numberOfTextureVertices) _texverts(n - 1) else null
  
  def numberOfChambers = _chambers.size
  def chambers         = _chambers.elements
  def hardChambers     = {
    val smoothing = !chambers.forall(_.cell.smoothingGroup == 0)
    chambers.filter(c => {
    val (f, g) = (c.cell, c.s2.cell)
    f.isInstanceOf[Hole] || g.isInstanceOf[Hole] || smoothing &&
      (f.smoothingGroup == 0 || g.smoothingGroup == 0 ||
         f.smoothingGroup != g.smoothingGroup)
    })
  }
  
  def numberOfEdges    = _edges.size
  def edges            = _edges.keys
  
  def numberOfFaces    = _faces.size
  def faces            = _faces.elements

  def numberOfHoles    = _holes.size
  def holes            = _holes.elements

  def group(name : String) = _groups.get(name) match {
    case Some(g) => g
    case None    => val g = new Group(name); _groups.put(name, g); g
  }
  def numberOfGroups = _groups.size
  def groups         = _groups.values
  def clearGroups    = _groups.clear
  implicit def group2string(g: Group) = g.name
  implicit def string2group(s: String) = group(s)
  
  def material(name : String) = _mats.get(name) match {
    case Some(m) => m
    case None    => val m = new Material(name); _mats.put(name, m); m
  }
  def numberOfMaterials = _mats.size
  def materials         = _mats.values
  def clearMaterials    = _mats.clear
  implicit def mat2string(m: Material) = m.name
  implicit def string2mat(s: String) = material(s)

  override def clone = {
    val w = new StringWriter
    Mesh.write(w, List(this), null)
    Mesh.read(Source.fromString(w.toString))(0)
  }

  def addFace(verts   : Seq[Int],
              tverts  : Seq[Int],
              normals : Seq[Int]) : Face = {
    val n = verts.length
    val f = new Face
    val chambers = new ArrayBuffer[Chamber]
    for (i <- 0 until n; j <- List(i, (i + 1) % n)) {
      val c = new Chamber(vertex(verts(j)), f)
      c.tVertex = textureVertex(tverts(j))
      c.normal  = normal(normals(j))
      chambers  += c
      _chambers += c
    }
    
    for (i <- 0 until n) {
      val c = chambers(2*i)
      c.s0  = chambers(2*i + 1)
      c.s1  = chambers((i + n - 1) % n * 2 + 1)
      val e = new Edge(c.start, c.end)
      var d = _edges.getOrElse(e, null)
      if (d != null) {
        if (d.s2 != null) error("More than two faces at " + c)
        if (c.start != d.start) d = d.s0
        c.s2    = d
        c.s0.s2 = d.s0
      }
      _edges(e) = c
    }
    
    _faces += f
    f
  }
  
  def addFaces(faces : Seq[Tuple6[Seq[Int], Seq[Int], Seq[Int],
                                  Mesh.Group, Mesh.Material, Int]]) =
    for ((fc, ft, fn, g, mtl, s) <- faces) {
      val f = addFace(fc, ft, fn)
      f.group    = g
      f.material = mtl
      f.smoothingGroup = s
    }
  
  def fixHoles {
    val seen = new HashSet[Chamber]
    for (c <- chambers if c.s2 == null && !seen(c)) {
      val boundary = new ArrayBuffer[Chamber]
      var d = c
      do {
        boundary += d
        boundary += d.s0
        d = d.s0.s1
        while (d.s2 != null) d = d.s2.s1
      } while (d != c)
      seen ++ boundary
      val n = boundary.length
      val f = new Hole
      val hole = boundary.map(d => new Chamber(d.vertex, f)).toSeq
      for (d <- hole) _chambers += d
      for (i <- 0 until n) {
        val d = hole(i)
        d.s2 = boundary(i)
        if (i % 2 == 0) d.s0 = hole(i + 1) else d.s1 = hole((i + 1) % n)
      }
      for (d <- hole) d.tVertex = null
      _holes += f
    }
  }
  
  def computeNormals = {
    clearNormals
    val normal4face = new LazyMap((f: Cell) =>
      f.vertexChambers.sum(c => c.vertex.pos x c.nextAtFace.vertex.pos).unit)
    for (v <- vertices) {
      val n = addNormal(v.cellChambers.sum(c =>
        if (c.cell.isInstanceOf[Face]) normal4face(c.cell) else zero3).unit)
      for (c <- v.cellChambers; d <- List(c, c.s1)) d.normal = n
    }
  }
  
  def components = {
    val result = new ArrayBuffer[Component]
    val seen   = new HashSet[Chamber]
    
    for (c <- chambers if !seen(c)) {
      val faces    = new HashSet[Face]
      val vertices = new HashSet[Vertex]
      val queue    = new Queue[Chamber]
      queue += c
      seen  += c
      
      while (queue.length > 0) {
        val d = queue.dequeue
        vertices += d.vertex
        val f = d.cell
        if (f.isInstanceOf[Face]) faces += f.asInstanceOf[Face]
        for (e <- List(d.s0, d.s1, d.s2) if !seen(e)) {
          queue += e
          seen  += e
        }
      }
      
      result += Component(this, faces, vertices)
    }
    
    result
  }
  
  def charts = {
    val result = new ArrayBuffer[Chart]
    val seen   = new HashSet[Chamber]
    
    for (c <- chambers if !seen(c) && c.tVertex != null) {
      val faces  = new HashSet[Face]
      val tVerts = new HashSet[TextureVertex]
      val queue  = new Queue[Chamber]
      queue += c
      seen  += c
      
      while (queue.length > 0) {
        val d = queue.dequeue
        val t = d.tVertex
        if (t != null) {
          tVerts += t
          val f = d.cell
          if (f.isInstanceOf[Face]) faces += f.asInstanceOf[Face]
          for (e <- List(d.s0, d.s1) if !seen(e)) {
            queue += e
            seen  += e
          }
          val e = d.s2
          if (!seen(e) && e.tVertex == t) {
            queue += e
            seen  += e
          }
        }
      }
      
      result += Chart(this, faces, tVerts)
    }
    
    result
  }
  
  def splitByGroup = {
    val parts = new HashMap[String, Buffer[Face]]
    for (g <- _groups.values) parts(g) = new ArrayBuffer[Face]
    for (f <- _faces) parts(f.group) += f
    split(parts)
  }
  
  def splitByMaterial = {
    val parts = new HashMap[String, Buffer[Face]]
    for (m <- _mats.values) parts(m) = new ArrayBuffer[Face]
    for (f <- _faces) parts(f.material) += f
    split(parts)
  }
  
  def split(parts: Iterable[(String, Seq[Face])]) = {
    val output = new ArrayBuffer[Mesh]
    for ((part_name, faces) <- parts) {
      val m = new Mesh("%s_%s" format (name, part_name))
      val vMap = new LazyMap((v: Int) => m.addVertex(vertex(v)).nr)
      val nMap = new LazyMap((n: Int) =>
        if (n == 0) 0 else m.addNormal(normal(n)).nr)
      val tMap = new LazyMap((t: Int) =>
        if (t == 0) 0 else m.addTextureVertex(textureVertex(t)).nr)
      for (f <- faces) {
        val cs = f.vertexChambers.toSeq
        val vs = cs.map(c => vMap(c.vertexNr))
        val vt = cs.map(c => tMap(c.tVertexNr))
        val vn = cs.map(c => nMap(c.normalNr))
        val g = m.addFace(vs, vt, vn)
        g.material = m.material(f.material)
        g.group = m.group(f.group)
        g.smoothingGroup = f.smoothingGroup
      }
      output += m
    }
    output
  }
  
  def subdivision : Mesh = subdivision(name)
  
  def subdivision(name: String) = {
    // -- create a new empty mesh
    val subD = new Mesh(name)
    
    // -- copy the original vertices, texture vertices and normals
    for (v <- vertices) subD.addVertex(v)
    for (t <- textureVertices) subD.addTextureVertex(t)
    for (n <- normals) subD.addNormal(n)
    
    // -- create edge centers
    val ch2ev = new HashMap[Chamber, Vertex]
    for (c <- _edges.values) {
      val z = subD.addVertex((c.start + c.end) / 2)
      for (d <- List(c, c.s0, c.s2, c.s0.s2)) ch2ev(d) = z
    }
    
    // -- interpolate texture coordinates along vertices
    val ch2etnr = new HashMap[Chamber, Int]
    for (c <- chambers if !ch2etnr.contains(c)) {
      val t1 = c.tVertex
      val t2 = c.s0.tVertex
      if (t1 != null && t2 != null) {
        val z = subD.addTextureVertex((t1 + t2) / 2)
        for (d <- List(c, c.s0)) ch2etnr(d) = z.nr
      }
    }
    
    // -- create face centers and subdivision faces
    for (f <- faces) {
      val n = f.degree
      val z = subD.addVertex(f.vertices.sum(_.pos) / n).nr
      val tz =
        if (f.textureVertices.forall(null !=))
          subD.addTextureVertex(f.textureVertices.sum(_.pos) / n).nr
        else 0
      for (c <- f.vertexChambers) {
        val t = c.tVertexNr
        val n = c.normalNr
        val g = subD.addFace(
          List(c.vertexNr,  ch2ev(c).nr, z, ch2ev(c.s1).nr),
          List(t, ch2etnr.getOrElse(c, 0), tz, ch2etnr.getOrElse(c.s1, 0)),
          List(n, n, n, n))
        g.material = subD.material(f.material.name)
        g.group    = subD.group(f.group.name)
        g.smoothingGroup = f.smoothingGroup
      }
    }
    
    // -- fill holes and flag border vertices
    subD.fixHoles
    val onBorder = new HashSet[Vertex]
    for (c <- subD.hardChambers) onBorder += c.vertex
    
    // -- adjust positions of edge centers
    for (c <- _edges.values; val z = ch2ev(c) if !onBorder(z)) {
      val z = ch2ev(c)
      if (z.degree != 4) error("bad new vertex degree in subdivision()")
      z.moveTo(z.cellChambers.sum(_.s0.vertex.pos) / 4)
    }
    
    // -- adjust positions of (copied) original non-border vertices
    for (n <- 1 to numberOfVertices; val v = subD.vertex(n) if !onBorder(v)) {
      val k = v.degree
      val cs = v.cellChambers.toSeq
      v.moveTo((v * (k - 3)
                + 4 * cs.sum(_.s0.vertex.pos) / k
                - cs.sum(_.s0.s1.s0.vertex.pos) / k) / k)
    }
    
    // -- do the same for border vertices
    val hard = new HashSet[Chamber]; hard ++ subD.hardChambers
    for (v <- onBorder if v.nr <= numberOfVertices) {
      val breaks = v.cellChambers.filter(hard).toSeq
      if (breaks.size == 2)
        v.moveTo(breaks(0).s0.vertex / 4 + v / 2 + breaks(1).s0.vertex / 4)
    }
    
    // -- return the result
    subD.mtllib ++ mtllib
    subD
  }

  def classifyForCoarsening(seed: Vertex) : VertexClassification = {
    val wasVertex     = new HashSet[Vertex]
    val wasEdgeCenter = new HashSet[Vertex]
    val wasFaceCenter = new HashSet[Vertex]

    val onBorder = new HashSet[Vertex]
    for (h <- holes; v <- h.vertices) onBorder += v

    val queue = new Queue[Vertex]
    queue     += seed
    wasVertex += seed
    while (queue.length > 0) {
      val v = queue.dequeue
      for (c <- v.cellChambers) {
        val w = c.s0.vertex
        if (wasVertex(w) || wasFaceCenter(w)) return null
        if (onBorder(w)) {
          if (!onBorder(v) || w.degree != 3) return null
        } else {
          if (w.degree != 4) return null
        }
        wasEdgeCenter += w
        for (d <- List(c.s0.s1.s0, c.s2.s0.s1.s0)) {
          val u = d.vertex
          if (wasEdgeCenter(u)) return null
          if (onBorder(u)) {
            if (!onBorder(w)) return null
            if (!wasVertex(u)) {
              queue     += u
              wasVertex += u
            }
          } else {
            if (wasVertex(u)) return null
            wasFaceCenter += u
          }
        }
        if (!onBorder(w)) {
          val u = c.s0.s1.s2.s1.s0.vertex
          if (wasEdgeCenter(u) || wasFaceCenter(u)) return null
          if (!wasVertex(u)) {
            queue     += u
            wasVertex += u
          }
        }
      }
    }
    VertexClassification(wasVertex, wasEdgeCenter, wasFaceCenter)
  }
  
  def coarsening : Mesh = coarsening(name)
  
  def coarsening(name: String) : Mesh = {
    System.err.println("  classifying vertices...")
    val vc = new HashMap[Component, VertexClassification]
    for (p <- components) {
      var cost = Int.MaxValue
      var best: VertexClassification = null
      for (c <- p.coarseningClassifications if c.cost < cost) {
        cost = c.cost
        best = c
      }
      if (best == null) error("mesh cannot be coarsened")
      vc(p) = best
    }
    coarsening(vc, name)
  }
  
  def coarsening(vc: Component => VertexClassification, name: String) = {
    //TODO resolve conflicts (texture vertices, materials, groups)

    // -- warning messages
    val messages = new HashSet[String]
    
    // -- chambers and vertices on mesh or smoothing group borders
    System.err.println("  finding borders...")
    val hard     = new HashSet[Chamber]; hard     ++ hardChambers
    val onBorder = new HashSet[Vertex] ; onBorder ++ hard.map(_.vertex)

    // -- vertices for which the final position has been computed
    val done = new HashSet[Vertex]

    // -- initialize the new mesh
    System.err.println("  initializing new mesh...")
    val m = new Mesh(name)
    
    // -- define how old vertices map to new ones
    System.err.println("  defining maps...")
    val mapV = new LazyMap((i: Int) => {
      val v = vertex(i)
      val k  = v.degree
      val cs = v.cellChambers.toSeq
      val w = if (onBorder(v)) {
        done += v
        val breaks = cs.filter(hard).toSeq
        if (breaks.size == 2)
          m.addVertex(v * 2 - (breaks(0).s0.vertex + breaks(1).s0.vertex) / 2)
        else
          m.addVertex(v)
      } else if (k != 3) {
        done += v
        m.addVertex((v * k + (cs.sum(_.s0.s1.s0.vertex.pos)
                              - 4 * cs.sum(_.s0.vertex.pos)) / k) / (k - 3))
      } else {
        m.addVertex(v)
      }
      w.nr
    })
    
    // -- do the same for texture vertices and normals
    val mapT = new LazyMap((t: Int) =>
      if (t != 0) m.addTextureVertex(textureVertex(t)).nr else 0)
    val mapN = new LazyMap((n: Int) =>
      if (n != 0) m.addNormal(normal(n)).nr else 0)
    
    // -- create the faces of the new mesh along with necessary vertices etc.
    System.err.println("  making faces...")
    for (p <- components; f <- vc(p).wasFaceCenter) {
      val cs = f.cellChambers.toSeq
      val vs = cs.map(c => mapV(c.s0.s1.s0.vertexNr))
      val vt = cs.map(c => mapT(c.s0.s1.s0.tVertexNr))
      val vn = cs.map(c => mapN(c.s0.s1.s0.normalNr))
      val face = m.addFace(vs.reverse, vt.reverse, vn.reverse)
      
      val groups    = new HashMap[Group, Int]
      val materials = new HashMap[Material, Int]
      val sgroups   = new HashMap[Int, Int]
      for (c <- cs) {
        val face = c.cell
        groups(face.group) = groups.getOrElse(face.group, 0) + 1
        materials(face.material) = materials.getOrElse(face.material, 0) + 1
        sgroups(face.smoothingGroup) =
          sgroups.getOrElse(face.smoothingGroup, 0) + 1
        if (c.tVertex != c.s2.tVertex || c.s0.tVertex != c.s0.s2.tVertex)
          messages += "Inconsistent texture vertices."
      }
      if (groups.size > 1) messages += "Inconsistent grouping."
      if (materials.size > 1) messages += "Inconsistent materials."
      if (sgroups.size > 1) messages += "Inconsistent smoothing groups."
      face.group = groups.keys.next
      face.material = materials.keys.next
      face.smoothingGroup = sgroups.keys.next
    }
    
    // -- print out the accumulated warning messages
    for (msg <- messages) System.err.println("Warning: " + msg)
    
    // -- compute final positions for the 3-pole vertices in waves
    System.err.println("  handling 3-poles...")
    val nextWave = new HashSet[Vertex]
    nextWave ++ done
    while (nextWave.size > 0) {
      val thisWave = new HashSet[Vertex]
      thisWave ++ nextWave
      nextWave.clear
      for (v <- thisWave) {
        val vnew = m.vertex(mapV(v.nr))
        var p = Vec3(0, 0, 0)
        var n = 0
        for (c <- v.cellChambers if !hard(c)) {
          val w = c.s0.s1.s2.s1.s0.vertex
          if (done(w)) {
            p += (4 * c.s0.vertex.pos - m.vertex(mapV(w.nr))
                  - c.s0.s1.s0.vertex - c.s0.s2.s1.s0.vertex)
            n += 1
          } else {
            nextWave += w
          }
        }
        if (!done(v)) vnew.moveTo(p / n)
      }
      done ++ thisWave
    }
    
    // -- return the new mesh
    m.mtllib ++ mtllib
    m
  }
}
