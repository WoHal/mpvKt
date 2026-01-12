import io.gitlab.arturbosch.detekt.Detekt
import org.apache.commons.io.output.ByteArrayOutputStream
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.jetbrains.kotlin.android)
  alias(libs.plugins.kotlin.compose.compiler)
  alias(libs.plugins.room)
  alias(libs.plugins.detekt)
  alias(libs.plugins.about.libraries)
  alias(libs.plugins.kotlinx.serialization)
  alias(libs.plugins.ksp)
  alias(libs.plugins.maven.publish)
}

val libVersion = "0.0.2"

android {
  namespace = "live.mehiz.mpvkt"
  compileSdk = 36

  defaultConfig {
    minSdk = 21

    vectorDrawables {
      useSupportLibrary = true
    }

    buildConfigField("String", "GIT_SHA", "\"${getCommitSha()}\"")
    buildConfigField("int", "GIT_COUNT", getCommitCount())
  }
  splits {
    abi {
      isEnable = true
      reset()
      include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
      isUniversalApk = true
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      isShrinkResources = false
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro",
      )
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  composeCompiler {
    includeSourceInformation = true
  }
  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }

  lint {
    checkOnly += "NewApi" + "HandlerLeak"
    baseline = file("lint.xml")
  }
}

kotlin {
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_17)
    freeCompilerArgs.addAll("-Xwhen-guards", "-Xcontext-parameters", "-XXLanguage:+PropertyParamAnnotationDefaultTargetMode")
  }
}

room {
  schemaDirectory("$projectDir/schemas")
}

dependencies {
  implementation(libs.androidx.activity.compose)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.ui)
  implementation(libs.androidx.ui.graphics)
  implementation(libs.androidx.material3.android)
  implementation(libs.androidx.ui.tooling.preview)
  debugImplementation(libs.androidx.ui.tooling)
  implementation(libs.bundles.compose.navigation3)
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.compose.constraintlayout)
  implementation(libs.androidx.material3.icons.extended)
  implementation(libs.androidx.compose.animation.graphics)
  implementation(libs.material)
  implementation(libs.androidx.preferences.ktx)
  implementation(libs.androidx.documentfile)
  implementation(libs.mediasession)
  implementation(libs.saveable)

  implementation(libs.mpv.lib)
  implementation(libs.timber)

  implementation(platform(libs.koin.bom))
  implementation(libs.bundles.koin)

  implementation(libs.seeker)
  implementation(libs.compose.prefs)
  implementation(libs.bundles.about.libs)
  implementation(libs.simple.icons)

  implementation(libs.room.runtime)
  ksp(libs.room.compiler)
  implementation(libs.room.ktx)

  implementation(libs.preferences)

  implementation(libs.detekt.gradle.plugin)
  detektPlugins(libs.detekt.rules.compose)
  detektPlugins(libs.detekt.formatter)

  implementation(libs.kotlinx.immutable.collections)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.truetype.parser)
  implementation(libs.fsaf)
}

detekt {
  parallel = true
  allRules = false
  buildUponDefaultConfig = true
  config.setFrom("$rootDir/config/detekt/detekt.yml")
}

tasks.withType<Detekt>().configureEach {
  setSource(files(project.projectDir))
  exclude("**/build/**")
  reports {
    html.required.set(true)
    md.required.set(true)
  }
}

fun getCommitCount(): String = runCommand("git rev-list --count HEAD")
fun getCommitSha(): String = runCommand("git rev-parse --short HEAD")
fun runCommand(command: String): String {
  val stdOut = ByteArrayOutputStream()
  exec {
    commandLine = command.split(' ')
    standardOutput = stdOut
  }
  return String(stdOut.toByteArray()).trim()
}

aboutLibraries {
  excludeFields = arrayOf("generated")
}

afterEvaluate {
  mavenPublishing {
    publishToMavenCentral(true)
    signAllPublications()
    coordinates(
      "io.github.wohal",
      "mpvplayer-core",
      libVersion
    )

    pom {
      name = "Android mpvplayer core"
      description = "The android mpvplayer core library."
      inceptionYear = "2025"
      url = "https://github.com/WoHal/mpvkt/"
      licenses {
        license {
          name = "MIT License"
          url = "https://opensource.org/license/mit/"
          distribution = "repo"
        }
      }
      developers {
        developer {
          id = "WoHal"
          name = "WoHal"
          url = "https://github.com/WoHal/"
        }
      }
      scm {
        url = "https://github.com/WoHal/mpvkt/"
        connection = "scm:git:git://github.com/WoHal/mpvkt.git"
        developerConnection = "scm:git:ssh://git@github.com/WoHal/mpvkt.git"
      }
    }
  }
}
