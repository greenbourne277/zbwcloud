plugins {
    id("zbw.kotlin-application")
    id("zbw.kotlin-conventions")
    id("zbw.kotlin-microservice-scaffold")
    id("zbw.kotlin-tests")
    id("zbw.tracing")
    id("zbw.kotlin-json")
}

repositories {
    mavenLocal()
    mavenCentral()
    google()
    maven { setUrl("https://jitpack.io") }
}

dependencies {
    val ktorVersion by System.getProperties()
    implementation("io.ktor:ktor-gson:$ktorVersion")
    implementation(project(":app:lori:api"))
    implementation("org.postgresql:postgresql:42.3.1")
    implementation("io.zonky.test:embedded-postgres:1.3.1")
    implementation("org.flywaydb:flyway-core:7.15.0")
    implementation("com.mchange:c3p0:0.9.5.5")
    implementation("com.github.lamba92.ktor-spa:ktor-spa:1.2.1")
    runtimeOnly(project(path = ":app:lori:server:ui", configuration = "npmResources"))
}

application {
    mainClass.set("de.zbw.api.lori.server.LoriServer")
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.886".toBigDecimal()
            }
        }
    }
}