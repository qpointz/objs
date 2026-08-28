rootProject.name = "objs"

include(":objs-core")
include(":objs-service")
project(":objs-core").projectDir = file("objs-core")
project(":objs-service").projectDir = file("objs-service")
