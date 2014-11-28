package tk.monnef.swsl

import org.scalatest.FlatSpec

class TestServiceOperations extends FlatSpec {

  import SWSL._
  import ParsingHelper._

  val SUPER_FETCH_NAME = "SysMain"

  "ExecutionHelper" should "be able to toggle (stop/start) twice super fetch service" in {
    var desc = generateServiceList().findByName(SUPER_FETCH_NAME).get
    val originallyRunning = desc.status == StatusRunning

    if (originallyRunning) desc.stop()
    else desc.start()

    desc = desc.refreshedState()
    var nowRunning = desc.status == StatusRunning
    assert(originallyRunning != nowRunning)

    if (originallyRunning) desc.start()
    else desc.stop()

    desc = desc.refreshedState()
    nowRunning = desc.status == StatusRunning
    assert(originallyRunning == nowRunning, s"Status is ${desc.status}.")
  }

}
