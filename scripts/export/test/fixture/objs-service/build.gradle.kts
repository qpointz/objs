plugins {
    `java-library`
}

dependencies {
    implementation(project.parent!!.project("objs-core"))
    // CUSTOM_EXPORT_MARKER
}
