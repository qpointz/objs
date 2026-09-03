rootProject.name = "objs"

include(":objs-api")
include(":objs-core")
include(":objs-autoconfigure")
include(":objs-service")
project(":objs-api").projectDir = file("objs-api")
project(":objs-core").projectDir = file("objs-core")
project(":objs-autoconfigure").projectDir = file("objs-autoconfigure")
project(":objs-service").projectDir = file("objs-service")
