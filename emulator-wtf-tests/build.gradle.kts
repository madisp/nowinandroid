import dagger.hilt.android.plugin.util.capitalize
import wtf.emulator.OutputType
import wtf.emulator.async.EwAsyncExecService
import wtf.emulator.async.EwAsyncExecTask

plugins {
    alias(libs.plugins.emulatorwtf)
}

data class Test(
    val appApk: String? = null,
    val testApk: String? = null,
    val libraryTestApk: String? = null,
    val name: String,
)

val tests = listOf(
    Test(
        appApk = "app/build/outputs/apk/demo/debug/app-demo-debug.apk",
        testApk = "app/build/outputs/apk/androidTest/demo/debug/app-demo-debug-androidTest.apk",
        name = "app",
    ),
    Test(
        libraryTestApk = "core/database/build/outputs/apk/androidTest/demo/debug/database-demo-debug-androidTest.apk",
        name = "database",
    ),
    Test(
        libraryTestApk = "core/designsystem/build/outputs/apk/androidTest/demo/debug/designsystem-demo-debug-androidTest.apk",
        name = "designsystem",
    ),
    Test(
        libraryTestApk = "core/ui/build/outputs/apk/androidTest/demo/debug/ui-demo-debug-androidTest.apk",
        name = "ui",
    ),
    Test(
        libraryTestApk = "feature/bookmarks/build/outputs/apk/androidTest/demo/debug/bookmarks-demo-debug-androidTest.apk",
        name = "bookmarks",
    ),
    Test(
        libraryTestApk = "feature/foryou/build/outputs/apk/androidTest/demo/debug/foryou-demo-debug-androidTest.apk",
        name = "foryou",
    ),
    Test(
        libraryTestApk = "feature/interests/build/outputs/apk/androidTest/prod/debug/interests-prod-debug-androidTest.apk",
        name = "interests",
    ),
    Test(
        libraryTestApk = "feature/settings/build/outputs/apk/androidTest/demo/debug/settings-demo-debug-androidTest.apk",
        name = "settings",
    ),
    Test(
        libraryTestApk = "feature/topic/build/outputs/apk/androidTest/prod/debug/topic-prod-debug-androidTest.apk",
        name = "topic",
    ),
    Test(
        libraryTestApk = "sync/work/build/outputs/apk/androidTest/demo/debug/work-demo-debug-androidTest.apk",
        name = "sync",
    ),
)

val toolClasspath = configurations.maybeCreate("emulatorWtfCli")

// register async service
var service: Provider<EwAsyncExecService> =
    gradle.sharedServices.registerIfAbsent(
        EwAsyncExecService.NAME,
        EwAsyncExecService::class.java,
    ) {}

val baseConfiguration = Action<EwAsyncExecTask> {
    token.set(providers.environmentVariable("EW_API_TOKEN"))
    classpath.set(toolClasspath)
    execService.set(service)
    recordVideo.set(true)
    shardTargetRuntime.set(2)
    numFlakyTestAttempts.set(3)
    async.set(true)

    devices.set(listOf(
        mapOf("model" to "Pixel2", "version" to "29", "gpu" to "auto")
    ))

    // just during testing to ensure a full re-run of all the tests
    sideEffects.set(true)

    outputs.upToDateWhen { false }
}

val testTasks = tests.map { test ->
    tasks.register("test${test.name.capitalize(java.util.Locale.US)}", EwAsyncExecTask::class.java) {
        baseConfiguration(this)
        displayName.set(test.name)

        if (test.libraryTestApk != null) {
            libraryTestApk.set(rootProject.file(test.libraryTestApk))
        } else if (test.appApk != null && test.testApk != null) {
            appApk.set(rootProject.file(test.appApk))
            testApk.set(rootProject.file(test.testApk))
        } else {
            throw IllegalArgumentException("Either appApk+testApk or libraryTestApk must be set")
        }
    }
}

tasks.register("runTests") {
    testTasks.forEach { this.dependsOn(it) }
}

emulatorwtf {
    collectResultsTaskEnabled.set(true)
    outputs.set(listOf(OutputType.MERGED_RESULTS_XML))
}
