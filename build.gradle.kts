plugins {
    java
}

group = "ru.kotmiks"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.dmulloy2.net/repository/public/")
}

dependencies {
    // Paper API (Adventure, MiniMessage, Gson/Legacy serializers приходят транзитивно)
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    // JOML (Vector3f для метаданных Display-сущностей) — на сервере уже есть
    compileOnly("org.joml:joml:1.10.8")
    // ProtocolLib — обязательная зависимость, на сервере ставится отдельным плагином
    compileOnly("com.comphenix.protocol:ProtocolLib:5.3.0") {
        isTransitive = false
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.processResources {
    filteringCharset = "UTF-8"
}

tasks.jar {
    archiveBaseName.set("KotMiksHolo")
    archiveClassifier.set("")
}

tasks.build {
    dependsOn(tasks.jar)
}
