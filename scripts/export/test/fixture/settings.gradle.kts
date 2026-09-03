rootProject.name = "objs"

include(":objs-api")
include(":objs-persistence")
include(":objs-autoconfigure")
include(":objs-service")
project(":objs-api").projectDir = file("objs-api")
project(":objs-persistence").projectDir = file("objs-persistence")
project(":objs-autoconfigure").projectDir = file("objs-autoconfigure")
project(":objs-service").projectDir = file("objs-service")
