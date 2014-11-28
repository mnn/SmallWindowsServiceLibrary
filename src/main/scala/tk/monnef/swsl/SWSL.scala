package tk.monnef.swsl

import scala.sys.process._
import scala.collection.mutable.ListBuffer
import Utils._

object SWSL {

  import ParsingHelper._
  import ExecutionHelper._

  def generateServiceList(): ServiceList = {
    val lines = executeGetService() |> joinLinesStartingWithSpace
    splitByDelimiter(lines, "").map(createServiceDescriptor) |> ServiceList.apply
  }
}

class ServiceList(val services: Seq[ServiceDescriptor]) {
  lazy val running = services.filter(_.status == StatusRunning)
  lazy val stopped = services.filter(_.status == StatusStopped)
  lazy val paused = services.filter(_.status == StatusPaused)

  def findByName(name: String): Option[ServiceDescriptor] = services.find(_.name == name)
}

object ServiceList {
  def apply(services: Seq[ServiceDescriptor]): ServiceList = new ServiceList(services)
}

object ExecutionHelper {
  private def wrapAnyException[T](errorMessage: String)(code: => T): T = {
    try {
      code
    } catch {
      case e: Throwable => throw new ExecutionException(errorMessage, e)
    }
  }

  def executeGetService(): Seq[String] = wrapAnyException("Unable to execute Get-Service powershell command") {
    Seq("powershell", "-command ", "Get-Service | Format-List").lineStream.toSeq
  }
}

object ParsingHelper {
  def joinLinesStartingWithSpace(lines: Seq[String]): Seq[String] = {
    if (lines.size <= 1) lines
    var buff = ListBuffer[String]()
    var current: String = lines(0)
    for {line <- lines.drop(1)} {
      if (line.startsWith(" ")) current += line.dropWhile(_ == ' ')
      else {
        buff += current
        current = line
      }
    }
    buff :+ current
  }

  def splitByDelimiter[T](data: Seq[T], delimiter: T): Seq[Seq[T]] =
    data.foldLeft(List(List[T]())) { (buff: List[List[T]], item) =>
      if (item == delimiter && buff.last.nonEmpty) buff :+ List[T]()
      else if (item != delimiter) buff.dropRight(1) :+ (buff.last :+ item)
      else buff
    }.filterNot(_.isEmpty)

  def createServiceDescriptor(lines: Seq[String]): ServiceDescriptor = {
    val fieldMap = lines.map(convertLineToField).toMap
    ServiceDescriptor(
      fieldMap("Name"),
      fieldMap("DisplayName"),
      ServiceStatus.fromString(fieldMap("Status"))
    )
  }

  def convertLineToField(line: String): (String, String) = {
    val delimPos = line.indexOf(':')
    if (delimPos == -1) throw new ParsingException(s"Colon in field not found: $line")
    val name = line.take(delimPos).reverse.dropWhile(_ == ' ').reverse
    val value = line.drop(delimPos + 2)
    (name, value)
  }
}

case class ServiceDescriptor(name: String, title: String, status: ServiceStatus)

sealed class ServiceStatus

case object StatusStopped extends ServiceStatus

case object StatusRunning extends ServiceStatus

case object StatusPaused extends ServiceStatus

object ServiceStatus {
  def fromString(text: String): ServiceStatus = text match {
    case "Running" => StatusRunning
    case "Stopped" => StatusStopped
    case "Paused" => StatusPaused
    case s: String => throw new ParsingException(s"Unknown status $s")
  }
}

class SWSLException(message: String, cause: Throwable) extends RuntimeException(message, cause) {
  def this(message: String) = this(message, null)
}

class ParsingException(message: String) extends SWSLException(message)

class ExecutionException(message: String, cause: Throwable) extends SWSLException(message, cause) {
  def this(message: String) = this(message, null)
}
