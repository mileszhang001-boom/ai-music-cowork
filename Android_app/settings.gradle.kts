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
include(":features:perception:module-perception")
include(":features:perception:app-demo-perception")

// Features - Semantic
include(":features:semantic:module-semantic")
include(":features:semantic:app-demo-semantic")

// Features - Generation
include(":features:generation:module-generation")
include(":features:generation:app-demo-generation")

// App
include(":app:app-main")
