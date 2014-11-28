package tk.monnef.swsl

import scala.sys.process._
import scala.collection.mutable.ListBuffer
import Utils._
import scala.util.control.NonFatal
import scala.annotation.tailrec
import java.io.{ByteArrayOutputStream, PrintWriter}

object SWSL {

  import ParsingHelper._
  import ExecutionHelper._

  def generateServiceList(): ServiceList = {
    val lines = executeGetService() |> joinLinesStartingWithSpace
    splitByDelimiter(lines, "").map(createServiceDescriptor) |> ServiceList.apply
  }
}

class ServiceList(val services: Seq[ServiceDescriptor]) {
  lazy val running = filterByStatus(StatusRunning)
  lazy val stopped = filterByStatus(StatusStopped)
  lazy val paused = filterByStatus(StatusPaused)
  lazy val stopPending = filterByStatus(StatusStopPending)

  def findByName(name: String): Option[ServiceDescriptor] = services.find(_.name == name)

  def filterByStatus(status: ServiceStatus): Seq[ServiceDescriptor] = services.filter(_.status == status)
}

object ServiceList {
  def apply(services: Seq[ServiceDescriptor]): ServiceList = new ServiceList(services)

}

object ExecutionHelper {
  private def wrapAnyException[T](errorMessage: String)(code: => T): T = {
    try {
      code
    } catch {
      case NonFatal(e) if !e.isInstanceOf[ExecutionException] => throw new ExecutionException(errorMessage, e)
    }
  }

  /**
   * Executes given command with arguments.
   * @param cmd Comannand followed by arguments.
   * @return (exit value, std our, std err)
   * @author Rogach
   */
  def runCommand(cmd: Seq[String]): (Int, String, String) = {
    val stdout = new ByteArrayOutputStream
    val stderr = new ByteArrayOutputStream
    val stdoutWriter = new PrintWriter(stdout)
    val stderrWriter = new PrintWriter(stderr)
    val exitValue = cmd ! ProcessLogger(stdoutWriter.println, stderrWriter.println)
    stdoutWriter.close()
    stderrWriter.close()
    (exitValue, stdout.toString, stderr.toString)
  }

  def executeGetService(): Seq[String] = wrapAnyException("Unable to execute Get-Service powershell command") {
    Seq("powershell", "-command ", "Get-Service | Format-List").lineStream.toList
  }

  final val NET_COMMAND_SAFE_TO_IGNORE_EXIT_CODES = Seq(
    10, // service already running
    24 // service already paused
  )

  final val NET_COMMAND_EXIT_VALUE_ACCESS_DENIED = 2

  def executeNetCommand(action: String, name: String) {
    wrapAnyException(s"Unable to execute net command with action $action and service name $name.") {
      val (exitValue, stdout, stderr) = runCommand(Seq("net", action, name))
      if (exitValue != 0 && !NET_COMMAND_SAFE_TO_IGNORE_EXIT_CODES.contains(exitValue)) {
        if (exitValue == NET_COMMAND_EXIT_VALUE_ACCESS_DENIED) throw new AccessDeniedException
        else throw new ExecutionException(s"Execution of net command ended with error return value.\nOutput: $stdout\nError output: $stderr")
      }
    }
  }

  def executeStopService(name: String) { executeNetCommand("stop", name) }

  def executeStartService(name: String) { executeNetCommand("start", name) }

  def executePauseService(name: String) { executeNetCommand("pause", name) }

  def executeContinueService(name: String) { executeNetCommand("continue", name) }
}

object ParsingHelper {
  def joinLinesStartingWithSpace(lines: Seq[String]): Seq[String] = {
    @tailrec
    def loop(current: String, rest: Seq[String], result: Seq[String]): Seq[String] = {
      rest match {
        case line :: restTail =>
          if (line.startsWith(" ")) loop(current + line.dropWhile(_ == ' '), restTail, result)
          else loop(line, restTail, result :+ current)
        case _ => result :+ current
      }
    }

    if (lines.size <= 1) lines
    else loop(lines(0), lines.drop(1), Seq())
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

case class ServiceDescriptor(name: String, title: String, status: ServiceStatus) {
  def stop() { ExecutionHelper.executeStopService(name) }

  def start() { ExecutionHelper.executeStartService(name) }

  def pause() { ExecutionHelper.executePauseService(name) }

  def continue() { ExecutionHelper.executeContinueService(name) }

  def refreshedState(): ServiceDescriptor = SWSL.generateServiceList().findByName(name) match {
    case Some(sd) => sd
    case None => throw new ServiceNotFoundException(s"Service '$name' not found.")
  }
}

sealed class ServiceStatus

case object StatusStopped extends ServiceStatus

case object StatusRunning extends ServiceStatus

case object StatusPaused extends ServiceStatus

case object StatusStopPending extends ServiceStatus

object ServiceStatus {
  def fromString(text: String): ServiceStatus = text match {
    case "Running" => StatusRunning
    case "Stopped" => StatusStopped
    case "Paused" => StatusPaused
    case "StopPending" => StatusStopPending
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

class ServiceNotFoundException(message: String) extends SWSLException(message) {
  def this() = this("")
}

class AccessDeniedException extends ExecutionException("Insufficient rights.")
