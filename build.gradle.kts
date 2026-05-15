plugins {
    id("com.android.application") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}

val localBuildRoot = file(
    "${System.getenv("LOCALAPPDATA") ?: System.getProperty("java.io.tmpdir")}\\CoordSnapBuild"
)

allprojects {
    val buildFolderName = path
        .removePrefix(":")
        .replace(':', '_')
        .ifBlank { "root" }

    layout.buildDirectory.set(localBuildRoot.resolve(buildFolderName))
}
