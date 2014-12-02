import tk.monnef.swsl._
import tk.monnef.swsl.SWSL._

val SUPER_FETCH_NAME = "SysMain"

def getFreshStatus() = getFreshServiceInfo().status

def getFreshServiceInfo(): ServiceDescriptor = generateServiceList().findByName(SUPER_FETCH_NAME) match {
  case Some(service) => service
  case None =>
    println("Super Fetch service not found.")
    System.exit(1)
    null
}

def toggleRunningOfSuperFecth() {
  val service = getFreshServiceInfo()

  if (service.status == StatusRunning) service.stop()
  else service.start()
}

println(s"Before any action Super Fetch's status is ${getFreshStatus()}.")

try {
  toggleRunningOfSuperFecth()
} catch {
  case _: AccessDeniedException =>
    println("It appears you don't have proper rights to control services.")
    System.exit(2)
}
println(s"After first toggle status is ${getFreshStatus()}.")

toggleRunningOfSuperFecth()
println(s"After second toggle status is ${getFreshStatus()}.")
