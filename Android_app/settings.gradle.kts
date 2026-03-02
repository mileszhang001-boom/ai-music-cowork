pluginManagement {
    repositories {
        google()
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

rootProject.name = "MusicRecommendation"

// Core
include(":core:core-api")

// Features - Perception
include(":features:perception:module-perception-api")
include(":features:perception:module-perception")

// Features - Semantic
include(":features:semantic:module-semantic")
include(":features:semantic:app-demo-semantic")

// Features - Layer3 (替代 Generation)
include(":features:localmusic:module-localmusic")
include(":features:localmusic:app-demo-localmusic")

// Features - Layer3
include(":features:layer3:layer3-api")
include(":features:layer3:layer3-sdk")
include(":features:layer3:app-demo-layer3")

// App
include(":app:app-main")
