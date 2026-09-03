plugins {
    `java-library`
}

dependencies {
    implementation(project.parent!!.project("objs-persistence"))
    // CUSTOM_EXPORT_MARKER
}
