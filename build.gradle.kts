plugins {
    java
}

group = "ru.kotmiks"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.dmulloy2.net/repository/public/")
    maven("https://maven.enginehub.org/repo/")          // WorldGuard / WorldEdit
    maven("https://repo.helpch.at/releases/")           // PlaceholderAPI (актуальный репозиторий)
    maven("https://repo.extendedclip.com/releases/")    // PlaceholderAPI (прежний репозиторий, запасной)
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
    // Опциональные интеграции (на сервере могут отсутствовать — классы грузятся только при наличии плагинов)
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.13")
    compileOnly("me.clip:placeholderapi:2.11.6") {
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
