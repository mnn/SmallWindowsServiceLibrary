import tk.monnef.swsl.SWSL._

val list = generateServiceList() // getting a list of all services
val descs = list.running.map(_.title) // descriptions of running services

println(descs.mkString(", "))
