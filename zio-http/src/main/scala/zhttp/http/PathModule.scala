package zhttp.http

import scala.annotation.tailrec

private[zhttp] trait PathModule { module =>
  val !! : Path = Path.End

  sealed trait Path { self =>
    final override def toString: String = this.encode

    final def /(name: String): Path = Path(self.toList :+ name)

    final def /:(name: String): Path = append(name)

    final def append(name: String): Path = if (name.isEmpty) self
    else if (name == "$") Path.Cons("", self)
    else Path.Cons(name, self)

    final def drop(n: Int): Path = Path(self.toList.drop(n))

    final def dropLast(n: Int): Path = Path(self.toList.reverse.drop(n).reverse)

    final def encode: String = {
      @tailrec
      def loop(self: Path, str: String): String = {
        self match {
          case Path.End              => str
          case Path.Cons(name, path) => loop(path, s"$str/$name")
        }
      }
      val res                                   = loop(self, "")
      if (res.isEmpty) "/" else res
    }

    final def initial: Path = self match {
      case Path.End           => self
      case Path.Cons(_, path) => path
    }

    final def isEnd: Boolean = self match {
      case Path.End        => true
      case Path.Cons(_, _) => false
    }

    final def last: Option[String] = self match {
      case Path.End           => None
      case Path.Cons(name, _) => Option(name)
    }

    final def reverse: Path = Path(toList.reverse)

    @tailrec
    final def startsWith(other: Path): Boolean = {
      if (self == other) true
      else
        (self, other) match {
          case (/(p1, _), p2) => p1.startsWith(p2)
          case _              => false

        }
    }

    final def take(n: Int): Path = Path(self.toList.take(n))

    def toList: List[String]
  }

  object Path {
    def apply(): Path               = End
    def apply(string: String): Path = if (string.trim.isEmpty) End
    else if (string.endsWith("/") && string.length > 1) getPath(string + "$")
    else getPath(string)

    def apply(seqString: String*): Path = Path(seqString.toList)
    def apply(list: List[String]): Path = list.foldRight[Path](End)((s, a) => a.append(s))

    private def getPath(string: String): Path = Path(string.split("/").toList)

    def empty: Path = End

    def unapplySeq(arg: Path): Option[List[String]] = Option(arg.toList)

    case class Cons(name: String, path: Path) extends Path {
      override def toList: List[String] = name :: path.toList
    }

    case object End extends Path {
      override def toList: List[String] = Nil
    }
  }

  object /: {
    def unapply(path: Path): Option[(String, Path)] =
      path match {
        case Path.End              => None
        case Path.Cons(name, path) => Option(name -> path)
      }
  }

  object / {
    def unapply(path: Path): Option[(Path, String)] = {
      path.toList.reverse match {
        case Nil          => None
        case head :: next => Option((Path(next.reverse), head))
      }
    }
  }
}
