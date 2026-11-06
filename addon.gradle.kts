// Extra build logic, applied on top of build.gradle.kts (see dependencies.gradle for the main dependency list).
// Accessors such as testImplementation(...) are not generated for addon scripts, so configurations are
// addressed by name when needed.

// ComputerCraft 3.2.0 declares `required-after:lwjgl3ify` in its mcmod.info, and lwjgl3ify needs a modern
// JVM. GTNHGradle's plain runServer/runClient use the Java 8 toolchain and carry no lwjgl3ify on the
// classpath, so they die at MissingModsException before the game starts. Forward them to the modern variants
// (which GTNHGradle wires to lwjgl3ify and a Java 17+/21/25 JVM), so both `./gradlew runClient`/`runServer`
// and CI's server smoke test keep working. Mirrors the same workaround in OpenPeripheral.
mapOf("runServer" to "runServer25", "runClient" to "runClient25").forEach { (plain, modern) ->
    tasks.matching { it.name == plain }.configureEach {
        if (project.tasks.names.contains(modern)) {
            description = "Alias for $modern - ComputerCraft 3.2.0 requires lwjgl3ify, which needs Java 17+"
            dependsOn(modern)
            onlyIf("the plain task forwards to $modern") { false }
        }
    }
}
