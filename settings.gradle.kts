pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "YAWMI"

// المحركات النقية (بدون Android) — قابلة للاختبار على أي JVM
include(":sequence-engine")
include(":domain")
// تطبيق Android (واجهات + قاعدة بيانات Room)
include(":app")
