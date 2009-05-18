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
  class Chamber(v: Vertex, f: Cell) {
    private var _vertex = v
    private var _cell   = f

    private var _tVertex: TextureVertex = null
    private var _normal : Normal        = null
    private var _s0     : Chamber       = null
    private var _s1     : Chamber       = null
    private var _s2     : Chamber       = null
    
    def vertex = _vertex
    def vertexNr = if (vertex != null) vertex.nr else 0
    
    def mesh = vertex.mesh
    
    def tVertex = _tVertex
    def tVertexNr = if (tVertex != null) tVertex.nr else 0
    def tVertex_=(t: TextureVertex) {
      _tVertex = t
      if (t != null && t.chamber == null) t.chamber = this
    }
    
    def normal = _normal
    def normalNr = if (normal != null) normal.nr else 0
    def normal_=(n: Normal) {
      _normal = n
      if (n != null && n.chamber == null) n.chamber = this
    }
    
    def cell = _cell
    
    def face = if (cell.isInstanceOf[Face]) cell.asInstanceOf[Face] else null
    
    def s0 = _s0
    def s0_=(ch: Chamber) {
      if (_s0 != null) _s0._s0 = null
      if (ch._s0 != null) ch._s0._s0 = null
      _s0 = ch
      ch._s0 = this
    }
    def s1 = _s1
    def s1_=(ch: Chamber) {
      if (_s1 != null) _s1._s1 = null
      if (ch._s1 != null) ch._s1._s1 = null
      _s1 = ch
      ch._s1 = this
    }
    def s2 = _s2
    def s2_=(ch: Chamber) {
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
    
    def orbit(next: Chamber => Chamber) = {
      def from(c: Chamber): Stream[Chamber] =
        if (c == this) Stream.empty else Stream.cons(c, from(next(c)))
      Stream.cons(this, from(next(this)))
    }
    
    def cellChambers = orbit(_.nextAtVertex)
    def vertexChambers = orbit(_.nextAtFace)
    
    override def toString = "Chamber(%s -> %s)" format (start, end)
  }

  trait Vertex {
    def mesh   : Mesh
    def nr     : Int
    def chamber: Chamber
    def pos    : Vec3

    def pos_=(p: Vec3)

    def cellChambers = chamber.cellChambers
    def degree       = cellChambers.size
    def faces        = cellChambers.map(_.cell)
    def neighbors    = cellChambers.map(_.s0.vertex)
    def x = pos.x
    def y = pos.y
    def z = pos.z

    def pos_=(p: (Double, Double, Double)) { pos = Vec3(p._1, p._2, p._3) }

    override def toString = "Vertex(%d)" format (nr)
  }
  
  trait TextureVertex {
    def mesh   : Mesh
    def nr     : Int
    def chamber: Chamber
    def pos    : Vec2

    def pos_=(p: Vec2)
    def chamber_=(c: Chamber)

    def cellChambers = chamber.cellChambers
    def faces        = cellChambers.filter(_.tVertex == this).map(_.cell)
    def onBorder     = cellChambers.exists(_.onTextureBorder)

    def degree = cellChambers.sum(c =>
      if (c.tVertex == this || c.s2.tVertex == this)
        if (c.tVertex == c.s2.tVertex && c.s0.tVertex != c.s2.s0.tVertex) 2
        else 1
      else 0
    )

    def x = pos.x
    def y = pos.y
    
    def pos_=(p: (Double, Double)) { pos = Vec2(p._1, p._2) }
    
    override def toString = "TextureVertex(%d)" format (nr)
  }

  trait Normal {
    def mesh   : Mesh
    def nr     : Int
    def chamber: Chamber
    def value  : Vec3

    def value_=(p: Vec3)
    def chamber_=(c: Chamber)

    def x = value.x
    def y = value.y
    def z = value.z

    def value_=(p: (Double, Double, Double)) { value = Vec3(p._1, p._2, p._3) }

    override def toString = "Normal(%d)" format (nr)
  }

  trait Cell {
    def mesh   : Mesh
    def chamber: Chamber
    
    def vertexChambers = chamber.vertexChambers
    def vertices = vertexChambers.map(_.vertex)
    def degree   = vertexChambers.size
    
    def formatVertices(v0: Int, vt0: Int, vn0: Int) =
      vertexChambers.map(ch =>
        "%d/%s/%s" format (ch.start.nr + v0,
                           if (ch.tVertex != null) ch.tVertex.nr + vt0 else "",
                           if (ch.normal  != null) ch.normal.nr  + vn0 else "")
      ).mkString(" ")

    def formatVertices: String = formatVertices(0, 0, 0)
  }
  
  trait Face extends Cell {
    def obj           : Object
    def group         : Group
    def material      : Material
    def smoothingGroup: Int
    
    def obj_=           (o: Object)
    def group_=         (g: Group)
    def material_=      (m: Material)
    def smoothingGroup_=(s: Int)
    
    def textureVertices = vertexChambers.map(_.tVertex)
    def normals = vertexChambers.map(_.normal)

    override def toString = "Face(%s; group = '%s', material = '%s')" format
      (formatVertices, group.name, material.name)
  }
  trait Hole extends Cell {
    override def toString = "Hole(%s)" format formatVertices
  }

  class Edge(v: Vertex, w: Vertex) {
    val (from, to) = if (v.nr <= w.nr) (v, w) else (w, v)
    
    override def equals (other: Any) = other.isInstanceOf[Edge] && {
      val e = other.asInstanceOf[Edge]
      e.from.nr == from.nr && e.to.nr == to.nr
    }
    
    override def hashCode = from.nr * 37 + to.nr
  }
  
  case class Object(name: String) {
    override def toString = "Object(" + name + ")"
  }
  
  case class Group(name: String) {
    override def toString = "Group(" + name + ")"
  }
  
  case class Material(name: String) {
    override def toString = "Material(" + name + ")"
  }

  case class Component(mesh: Mesh, faces: Set[Face], vertices: Set[Vertex]) {
    def coarseningClassifications = faces.elements.next.vertices
      .map(mesh.classifyForCoarsening(_)).filter(null !=)
  }
  
  case class Chart(mesh: Mesh,
                   faces: Set[Face], vertices: Set[TextureVertex])
  
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
          if (c.face != null) {
        	if (c.face.material != d.face.material) n += 1
        	if (c.face.group != d.face.group) n += 1
          }
        }
      }
      n
    }

    def cost: Int = wasFaceCenter.sum(cost(_))
    def isStrict = cost == 0
  }

  def read(filename: String, joinObjects: Boolean): Seq[Mesh] =
    read(Source.fromFile(filename), joinObjects)
  def read(filename: String): Seq[Mesh] =
    read(Source.fromFile(filename))
  def read(in: java.io.InputStream, joinObjects: Boolean): Seq[Mesh] =
    read(Source.fromInputStream(in), joinObjects)
  def read(in: java.io.InputStream): Seq[Mesh] =
    read(Source.fromInputStream(in))
  
  def read(source: Source): Seq[Mesh] = read(source, false)
  
  def read(source: Source, joinObjects: Boolean): Seq[Mesh] = {
    var mesh: Mesh = new Mesh("unnamed")
    val faces = new ArrayBuffer[Tuple6[Seq[Int], Seq[Int], Seq[Int],
                                       Mesh.Group, Mesh.Material, Int]]
    var v_base  = 0
    var vt_base = 0
    var vn_base = 0
    var group   : Group    = null
    var material: Material = null
    var smoothingGroup = 0
    val result = new ArrayBuffer[Mesh]
    val mtllib = new HashMap[String, String]
    
    for(raw <- source.getLines;
        line = raw.trim
        if line.length > 0 && !line.startsWith("#")) {
      val fields: Seq[String] = line.split("\\s+")
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
            val fields: Seq[String] = line.split("\\s+")
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
          val parts = (s + "/0/0").split("/").map(
            (s: String) => if (s.length == 0) 0 else s.toInt)
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

  def write(target: OutputStream, basename: String, meshes: Mesh*): Unit =
    write(target, meshes, basename)
  
  def write(target: Writer, basename: String, meshes: Mesh*): Unit =
    write(target, meshes, basename)
  
  def write(target: OutputStream, meshes: Seq[Mesh], basename: String): Unit =
    write(new OutputStreamWriter(target), meshes, basename)

  def write(target: Writer, meshes: Seq[Mesh], basename: String) {
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
                      uvs: Boolean): Map[Chamber, Chamber] = {
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
  def allMatches(c1: Component,
                 c2: Component): Seq[Map[Chamber, Chamber]] = {
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
  
  def distance(map: Map[Chamber, Chamber]): Double = {
    val verts = new HashSet[Vertex]
    for ((c, d) <- map) verts += c.vertex
    
    var dist: Double = 0
    for (v <- verts) {
      val w = map(v.chamber).start
      val (dx, dy, dz) = (w.x - v.x, w.y - v.y, w.z - v.z)
      dist += dx * dx + dy * dy + dz * dz
    }
    dist
  }
  
  def bestMatch(c1: Component, c2: Component): Map[Chamber, Chamber] = {
    var best: Map[Chamber, Chamber] = null
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
  
  def allMatches(c1: Chart, c2: Chart): Seq[Map[Chamber, Chamber]] = {
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
  
  def textureDistance(map: Map[Chamber, Chamber]): Double = {
    val verts = new HashSet[TextureVertex]
    for ((c, d) <- map) verts += c.tVertex
    
    var dist: Double = 0
    for (v <- verts) {
      val w = map(v.chamber).tVertex
      val (dx, dy) = (w.x - v.x, w.y - v.y)
      dist += dx * dx + dy * dy
    }
    dist
  }
  
  def bestMatch(c1: Chart, c2: Chart): Map[Chamber, Chamber] = {
    var best: Map[Chamber, Chamber] = null
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

class Mesh(s: String) extends MessageSource {
  import Mesh._

  val name = s
  
  private val _vertices = new ArrayBuffer[Vertex]
  private val _normals  = new ArrayBuffer[Normal]
  private val _texverts = new ArrayBuffer[TextureVertex]
  private val _faces    = new ArrayBuffer[Face]
  private val _holes    = new ArrayBuffer[Hole]
  private val _chambers = new ArrayBuffer[Chamber]
  private val _objects  = new LinkedHashMap[String, Object]
  private val _groups   = new LinkedHashMap[String, Group]
  private val _mats     = new LinkedHashMap[String, Material]
  private val _edges    = new HashMap[Edge, Chamber]
  
  private val _chamber_at_vertex  = new HashMap[Vertex, Chamber]
  private val _chamber_at_tvertex = new HashMap[TextureVertex, Chamber]
  private val _chamber_at_cell    = new HashMap[Cell, Chamber]
  
  private val _vertex_pos  = new HashMap[Vertex, Vec3]
  private val _texture_pos = new HashMap[TextureVertex, Vec2]
  private val _normal_val  = new HashMap[Normal, Vec3]
  
  private val _object_for_face          = new HashMap[Face, Object]
  private val _group_for_face           = new HashMap[Face, Group]
  private val _material_for_face        = new HashMap[Face, Material]
  private val _smoothing_group_for_face = new HashMap[Face, Int]

  object vertex_position {
    def apply(v: Vertex) = _vertex_pos(v)
    def update(v: Vertex, p: Vec3) { _vertex_pos(v) = p }
  }
  
  object texture_position {
    def apply(t: TextureVertex) = _texture_pos(t)
    def update(t: TextureVertex, p: Vec2) { _texture_pos(t) = p }
  }
  
  object normal_value {
    def apply(n: Normal) = _normal_val(n)
    def update(n: Normal, p: Vec3) { _normal_val(n) = p }
  }
  
  val mtllib = new HashMap[String, String]

  private def addChamber(v: Vertex, f: Cell) = new Chamber(v, f) {
    _chambers += this
    if (_chamber_at_vertex.get(v) == None) _chamber_at_vertex(v) = this
    if (_chamber_at_cell.get(f)   == None) _chamber_at_cell(f)   = this
  }
  
  def numberOfChambers = _chambers.size
  def chambers         = _chambers.elements
  def hardChambers     = {
    val smoothing =
      !chambers.forall(c => c.face == null || c.face.smoothingGroup == 0)
    chambers.filter(c => {
      val (f, g) = (c.face, c.s2.face)
      if (f == null || g == null)
        true
      else if (smoothing)
        f.smoothingGroup == 0 || g.smoothingGroup == 0 ||
        f.smoothingGroup != g.smoothingGroup
      else
        false
    })
  }
  
  def addVertex(p: Vec3): Vertex = addVertex(p.x, p.y, p.z)
  
  def addVertex(_x: Double, _y: Double, _z: Double) = new Vertex {
    val mesh = Mesh.this
    val nr = numberOfVertices + 1
    
    def chamber = _chamber_at_vertex(this)
    
    def pos = vertex_position(this)
    def pos_=(p: Vec3) { vertex_position(this) = p }
    
    _vertices += this
    pos = (_x, _y, _z)
  }
  
  def numberOfVertices = _vertices.size
  def vertices         = _vertices.elements
  def vertex(n: Int)  =
    if (n > 0 && n <= numberOfVertices) _vertices(n - 1) else null
  
  def clearTextureVertices = _texverts.clear

  def addTextureVertex(p: Vec2): TextureVertex = addTextureVertex(p.x, p.y)
  
  def addTextureVertex(_x: Double, _y: Double) = new TextureVertex {
    val mesh = Mesh.this
    val nr = numberOfTextureVertices + 1
    
    private var _ch: Chamber = null    
    def chamber = _ch
    def chamber_=(c: Chamber) {
      _ch = c
      if (c != null && c.tVertex != this) c.tVertex = this
    }
    
    def pos = texture_position(this)
    def pos_=(p: Vec2) { texture_position(this) = p }

    _texverts += this
    pos = (_x, _y)
  }

  def numberOfTextureVertices = _texverts.size
  def textureVertices         = _texverts.elements
  def textureVertex(n: Int)  =
    if (n > 0 && n <= numberOfTextureVertices) _texverts(n - 1) else null
  
  def clearNormals = _normals.clear
  
  def addNormal(p: Vec3): Normal = addNormal(p.x, p.y, p.z)
  
  def addNormal(_x: Double, _y: Double, _z: Double) = new Normal {
    val mesh = Mesh.this
    val nr = numberOfNormals + 1
    
    private var _ch: Chamber = null    
    def chamber = _ch
    def chamber_=(c: Chamber) {
      _ch = c
      if (c != null && c.normal != this) c.normal = this
    }
    
    def value = normal_value(this)
    def value_=(p: Vec3) { normal_value(this) = p }

    _normals += this
    value = (_x, _y, _z)
  }
  
  def numberOfNormals  = _normals.size
  def normals          = _normals.elements
  def normal(n: Int)  =
    if (n > 0 && n <= numberOfNormals) _normals(n - 1) else null

  def numberOfEdges    = _edges.size
  def edges            = _edges.keys
  
  def numberOfFaces    = _faces.size
  def faces            = _faces.elements

  def numberOfHoles    = _holes.size
  def holes            = _holes.elements

  def obj(name: String) = _objects.get(name) match {
    case Some(o) => o
    case None    => val o = new Object(name); _objects.put(name, o); o
  }
  def numberOfObjects = _objects.size
  def objects         = _objects.values
  def clearObjects    = _objects.clear
  
  def group(name: String) = _groups.get(name) match {
    case Some(g) => g
    case None    => val g = new Group(name); _groups.put(name, g); g
  }
  def numberOfGroups = _groups.size
  def groups         = _groups.values
  def clearGroups    = _groups.clear
  
  def material(name: String) = _mats.get(name) match {
    case Some(m) => m
    case None    => val m = new Material(name); _mats.put(name, m); m
  }
  def numberOfMaterials = _mats.size
  def materials         = _mats.values
  def clearMaterials    = _mats.clear

  override def clone = {
    val w = new StringWriter
    Mesh.write(w, List(this), null)
    Mesh.read(Source.fromString(w.toString))(0)
  }

  def addFace(verts   : Seq[Int], tverts  : Seq[Int], normvecs: Seq[Int]) = {
    val f = new Face {
      val mesh = Mesh.this
      def chamber = _chamber_at_cell(this)
      
      def obj            = _object_for_face(this)
      def group          = _group_for_face(this)
      def material       = _material_for_face(this)
      def smoothingGroup = _smoothing_group_for_face(this)
      
      def obj_=(o: Object)         { _object_for_face(this) = o }
      def group_=(g: Group)        { _group_for_face(this) = g }
      def material_=(m: Material)  { _material_for_face(this) = m }
      def smoothingGroup_=(s: Int) { _smoothing_group_for_face(this) = s }
    }
    
    val n = verts.length
    val chambers = new ArrayBuffer[Chamber]
    for (i <- 0 until n; j <- List(i, (i + 1) % n)) {
      val c = addChamber(vertex(verts(j)), f)
      c.tVertex = textureVertex(tverts(j))
      c.normal  = normal(normvecs(j))
      chambers  += c
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
  
  def addFaces(faces: Seq[Tuple6[Seq[Int], Seq[Int], Seq[Int],
                                  Mesh.Group, Mesh.Material, Int]]) =
    for ((fc, ft, fn, g, mtl, s) <- faces) {
      val f = addFace(fc, ft, fn)
      f.group          = g
      f.material       = mtl
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
      
      val f = new Hole {
        val mesh = Mesh.this
        def chamber = _chamber_at_cell(this)
      }

      val hole = boundary.map(d => addChamber(d.vertex, f)).toSeq
      for (i <- 0 until n) {
        val d = hole(i)
        d.s2 = boundary(i)
        if (i % 2 == 0) d.s0 = hole(i + 1) else d.s1 = hole((i + 1) % n)
      }
      for (d <- hole) d.tVertex = null
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
          if (e != null && !seen(e) && e.tVertex == t) {
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
    for (g <- _groups.values) parts(g.name) = new ArrayBuffer[Face]
    for (f <- _faces) parts(f.group.name) += f
    split(parts)
  }
  
  def splitByMaterial = {
    val parts = new HashMap[String, Buffer[Face]]
    for (m <- _mats.values) parts(m.name) = new ArrayBuffer[Face]
    for (f <- _faces) parts(f.material.name) += f
    split(parts)
  }
  
  def split(parts: Iterable[(String, Seq[Face])]) = {
    val output = new ArrayBuffer[Mesh]
    for ((part_name, faces) <- parts) {
      val m = new Mesh("%s_%s" format (name, part_name))
      val vMap = new LazyMap((v: Int) => m.addVertex(vertex(v).pos).nr)
      val nMap = new LazyMap((n: Int) =>
        if (n == 0) 0 else m.addNormal(normal(n).value).nr)
      val tMap = new LazyMap((t: Int) =>
        if (t == 0) 0 else m.addTextureVertex(textureVertex(t).pos).nr)
      for (f <- faces) {
        val cs = f.vertexChambers.toSeq
        val vs = cs.map(c => vMap(c.vertexNr))
        val vt = cs.map(c => tMap(c.tVertexNr))
        val vn = cs.map(c => nMap(c.normalNr))
        val g = m.addFace(vs, vt, vn)
        g.material = m.material(f.material.name)
        g.group = m.group(f.group.name)
        g.smoothingGroup = f.smoothingGroup
      }
      output += m
    }
    output
  }
  
  def withMorphApplied(donor: Mesh) = {
    val result = clone
    val originals = result.components
    
    for (comp <- donor.components) {
      send("Matching donor component with %d vertices and %d faces..."
  		   format (comp.vertices.size, comp.faces.size))
      var dist = Double.MaxValue
      var map: Map[Mesh.Chamber, Mesh.Chamber] = null
      var image: Mesh.Component = null
      try {
        for (c <- originals) {
          val candidate = Mesh.bestMatch(comp, c)
          if (candidate != null) {
            val d = Mesh.distance(candidate)
            if (d < dist) {
              dist = d
              map = candidate
              image = c
            }
          }
        }
        if (map == null) send("No match found.")
      } catch {
        case _ => send("Error while matching! Skipping this component.")
      }
      if (map != null) {
        send("Match found. Applying morph...")
        for ((c, d) <- map) d.vertex.pos = c.vertex.pos
      }
    }
    result
  }
  
  def subdivision: Mesh = subdivision(name)
  
  def subdivision(name: String) = {
    // -- create a new empty mesh
    val subD = new Mesh(name)
    
    // -- copy the original vertices, texture vertices and normals
    for (v <- vertices) subD.addVertex(v.pos)
    for (t <- textureVertices) subD.addTextureVertex(t.pos)
    for (n <- normals) subD.addNormal(n.value)
    
    // -- create edge centers
    val ch2ev = new HashMap[Chamber, Vertex]
    for (c <- _edges.values) {
      val z = subD.addVertex((c.start.pos + c.end.pos) / 2)
      for (d <- List(c, c.s0, c.s2, c.s0.s2)) ch2ev(d) = z
    }
    
    // -- interpolate texture coordinates along vertices
    val ch2etnr = new HashMap[Chamber, Int]
    for (c <- chambers if !ch2etnr.contains(c)) {
      val t1 = c.tVertex
      val t2 = c.s0.tVertex
      if (t1 != null && t2 != null) {
        val z = subD.addTextureVertex((t1.pos + t2.pos) / 2)
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
      z.pos = z.cellChambers.sum(_.s0.vertex.pos) / 4
    }
    
    // -- adjust positions of (copied) original non-border vertices
    for (n <- 1 to numberOfVertices; val v = subD.vertex(n) if !onBorder(v)) {
      val k = v.degree
      val cs = v.cellChambers.toSeq
      v.pos = (((k - 3) * v.pos
                + 4 * cs.sum(_.s0.vertex.pos) / k
                - cs.sum(_.s0.s1.s0.vertex.pos) / k) / k)
    }
    
    // -- do the same for border vertices
    val hard = new HashSet[Chamber]; hard ++ subD.hardChambers
    for (v <- onBorder if v.nr <= numberOfVertices) {
      val breaks = v.cellChambers.filter(hard).toSeq
      if (breaks.size == 2)
        v.pos =
          (breaks(0).s0.vertex.pos + breaks(1).s0.vertex.pos + 2 * v.pos) / 4
    }
    
    // -- return the result
    subD.mtllib ++ mtllib
    subD
  }

  def classifyForCoarsening(seed: Vertex): VertexClassification = {
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
  
  def coarsening: Mesh = coarsening(name)
  
  def coarsening(name: String): Mesh = {
    send("  classifying vertices...")
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
    send("  finding borders...")
    val hard     = new HashSet[Chamber]; hard     ++ hardChambers
    val onBorder = new HashSet[Vertex] ; onBorder ++ hard.map(_.vertex)

    // -- vertices for which the final position has been computed
    val done = new HashSet[Vertex]

    // -- initialize the new mesh
    send("  initializing new mesh...")
    val m = new Mesh(name)
    
    // -- define how old vertices map to new ones
    send("  defining maps...")
    val mapV = new LazyMap((i: Int) => {
      val v = vertex(i)
      val k  = v.degree
      val cs = v.cellChambers.toSeq
      val w = if (onBorder(v)) {
        done += v
        val breaks = cs.filter(hard).toSeq
        if (breaks.size == 2) m.addVertex(
            v.pos * 2 - (breaks(0).s0.vertex.pos + breaks(1).s0.vertex.pos) / 2)
        else
          m.addVertex(v.pos)
      } else if (k != 3) {
        done += v
        m.addVertex((v.pos * k + (cs.sum(_.s0.s1.s0.vertex.pos)
                                  - 4 * cs.sum(_.s0.vertex.pos)) / k) / (k - 3))
      } else {
        m.addVertex(v.pos)
      }
      w.nr
    })
    
    // -- do the same for texture vertices and normals
    val mapT = new LazyMap((t: Int) =>
      if (t != 0) m.addTextureVertex(textureVertex(t).pos).nr else 0)
    val mapN = new LazyMap((n: Int) =>
      if (n != 0) m.addNormal(normal(n).value).nr else 0)
    
    // -- create the faces of the new mesh along with necessary vertices etc.
    send("  making faces...")
    for (p <- components; f <- vc(p).wasFaceCenter) {
      val cs = f.cellChambers.toSeq
      val vs = cs.map(c => mapV(c.s0.s1.s0.vertexNr))
      val vt = cs.map(c => mapT(c.s0.s1.s0.tVertexNr))
      val vn = cs.map(c => mapN(c.s0.s1.s0.normalNr))
      val face = m.addFace(vs.reverse, vt.reverse, vn.reverse)
      
      val groups    = new HashMap[Group, Int]
      val materials = new HashMap[Material, Int]
      val sgroups   = new HashMap[Int, Int]
      var badUVs    = false
      for (c <- cs) {
        val face = c.face
        groups(face.group) = groups.getOrElse(face.group, 0) + 1
        materials(face.material) = materials.getOrElse(face.material, 0) + 1
        sgroups(face.smoothingGroup) =
          sgroups.getOrElse(face.smoothingGroup, 0) + 1
        if (c.tVertex != c.s2.tVertex || c.s0.tVertex != c.s0.s2.tVertex)
          badUVs = true
      }
      if (badUVs)
          messages += ("Inconsistent texture vertices: %s" format groups)
      if (groups.size > 1)
        messages += ("Inconsistent grouping: %s" format groups)
      if (materials.size > 1)
        messages += ("Inconsistent materials: %s" format materials)
      if (sgroups.size > 1) messages += "Inconsistent smoothing groups."
      face.group = groups.keys.next
      face.material = materials.keys.next
      face.smoothingGroup = sgroups.keys.next
    }
    
    // -- print out the accumulated warning messages
    for (msg <- messages) send("Warning: " + msg)
    
    // -- compute final positions for the 3-pole vertices in waves
    send("  handling 3-poles...")
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
            p += (4 * c.s0.vertex.pos - m.vertex(mapV(w.nr)).pos
                  - c.s0.s1.s0.vertex.pos - c.s0.s2.s1.s0.vertex.pos)
            n += 1
          } else {
            nextWave += w
          }
        }
        if (!done(v)) vnew.pos = p / n
      }
      done ++ thisWave
    }
    
    // -- return the new mesh
    m.mtllib ++ mtllib
    m
  }
}
