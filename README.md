Small Windows Service Library
=============================

Scala library for accessing services in Windows (via powershell and net command).
It is able to obtain basic information about services (id, name, description, status)
and execute basic actions over services (start, stop, pause, unpause).


Example
-------
```scala
import tk.monnef.swsl.SWSL._

val list = generateServiceList() // getting a list of all services
val descs = list.running.map(_.title) // descriptions of running services

println(descs.mkString(", "))
```

This code gives following output (tuncated):
```
Application Experience, Application Layer Gateway Service, Application Information, Application Management, Windows Audio Endpoint Builder,
 Windows Audio, Base Filtering Engine, Background Intelligent Transfer Service, Computer Browser, ...
```

More examples can be found at `demo` directory (Scala scripts).


Requirements
------------
- Scala 2.11


License
-------
GNU General Public License 3 (GPL3)
For more details read `LICENSE.txt`.
