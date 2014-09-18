package ladybugs.calculation

case class Vec2d(x: Double, y: Double) {

  def +(v: Vec2d) = Vec2d(v.x + x, v.y + y)

  def -(v: Vec2d) = Vec2d(v.x - x, v.y - y)

  def unary_- : Vec2d = Vec2d(-x, -y)

  def /(factor: Double) = Vec2d(x / factor, y / factor)

  def *(factor: Double) = Vec2d(x * factor, y * factor)

  def magnitude = math.sqrt(x * x + y * y)

  def normalised = this / magnitude

  def dot(v: Vec2d) = x * v.x + y * v.y

  def rotate(angleRadian: Double) = {
    val cos = math.cos(angleRadian)
    val sin = math.sin(angleRadian)
    Vec2d(x * cos - y * sin, x * sin + y * cos)
  }

  def angle = Math.atan2(y, x)

}

object Vec2d {

  val right = Vec2d(1, 0)
  val up    = Vec2d(0, -1)
  val left  = Vec2d(-1, 0)
  val down  = Vec2d(0, 1)
}