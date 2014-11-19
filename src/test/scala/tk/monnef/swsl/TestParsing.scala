package tk.monnef.swsl

import org.scalatest.FlatSpec

class TestParsing extends FlatSpec {

  import SWSL._
  import ParsingHelper._

  "convertLineToField method" should "parse a standard field" in {
    assert(convertLineToField("DependentServices   : {WwanSvc, WbioSrvc, wudfsvc, vds...}") ==("DependentServices", "{WwanSvc, WbioSrvc, wudfsvc, vds...}"))
  }

  it should "parse a field with empty value" in {
    assert(convertLineToField("name   :") ==("name", ""))
  }

  it should "produce ParsingException when delimiter is missing" in {
    intercept[ParsingException] {
      convertLineToField("name   value")
    }
  }

  // ---

  "ServiceStatus.fromString" should "return its case class counterpart" in {
    assert(ServiceStatus.fromString("Running") == StatusRunning)
    assert(ServiceStatus.fromString("Stopped") == StatusStopped)
  }

  it should "produce ParsingException on unknown service status" in {
    intercept[ParsingException] {
      ServiceStatus.fromString("UnknownStatus")
    }
  }

  // ---

  "splitByDelimiter method" should "split by delimiter given non-edge inputs" in {
    assert(
      splitByDelimiter(Seq(1, 2, 3, 33, 333, 2, 4, 44), 2) == Seq(Seq(1), Seq(3, 33, 333), Seq(4, 44))
    )
  }

  it should "split by delimiter given edge inputs" in {
    assert(splitByDelimiter(Seq[Int](), 2) == Seq[Seq[Int]]())
    assert(splitByDelimiter(Seq(2), 2) == Seq[Seq[Int]]())
    assert(splitByDelimiter(Seq(1, 2), 2) == Seq(Seq(1)))
    assert(splitByDelimiter(Seq(2, 1), 2) == Seq(Seq(1)))
  }

  // ---

  val (fn_name, fn_title, fn_status) = ("Name", "DisplayName", "Status")

  "createServiceDescriptor" should "parse and create matching service descriptors" in {
    val d = createServiceDescriptor(
      Seq(
        s"$fn_name      : service_a",
        "random field   : xxx xxx y : z",
        s"$fn_title     : Service A",
        s"$fn_status    : Running"
      )
    )
    assert(d == ServiceDescriptor("service_a", "Service A", StatusRunning))
  }

  // ---

  import ExecutionHelper._

  val TASK_SCHEDULER = "Task Scheduler"

  "executeGetService" should "return output of Get-Service" in {
    val data = executeGetService()
    assert(data.nonEmpty)
    assert(data.filter { l => l.startsWith("DisplayName") && l.contains(TASK_SCHEDULER)}.nonEmpty, data)
  }

  // ---

  "generateServiceList" should "generate a service list" in {
    val list = generateServiceList()
    assert(list.services.nonEmpty)
    assert(list.services.exists(_.title == TASK_SCHEDULER))
    assert((list.paused ++ list.stopped ++ list.running).toSet == list.services.toSet)
  }

  // ---

  "joinLinesStartingWithSpace" should "join lines starting with space(s)" in {
    assert(joinLinesStartingWithSpace(Seq("a", "b", "c")) == Seq("a", "b", "c"))
    assert(joinLinesStartingWithSpace(Seq("a", " b", "c")) == Seq("ab", "c"))
    assert(joinLinesStartingWithSpace(Seq("a", "          b", "c")) == Seq("ab", "c"))
    assert(joinLinesStartingWithSpace(Seq("     a", "b", "c")) == Seq("     a", "b", "c"))
    assert(joinLinesStartingWithSpace(Seq("a", " b", " c")) == Seq("abc"))
    assert(joinLinesStartingWithSpace(Seq("a", "  b", "  c")) == Seq("abc"))
  }
}
