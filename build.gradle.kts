plugins {
    // This plugin applies the correct loom variant based on the Minecraft version
    id("dev.kikugie.loom-back-compat")
    // https://modmuss50.github.io/mod-publish-plugin/
    id("me.modmuss50.mod-publish-plugin") version "2.2.0"
}

// DO NOT set group = ...!
version = "${property("mod.version")}+${sc.current.version}"
base.archivesName = property("mod.id") as String

val requiredJava: JavaVersion = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    sc.current.parsed >= "1.18" -> JavaVersion.VERSION_17
    sc.current.parsed >= "1.17" -> JavaVersion.VERSION_16
    else -> JavaVersion.VERSION_1_8
}

// This can be used for publishing on Modrinth and Curseforge
val compatibleVersions: List<String> = sc.properties.rawOrNull("mod", "mc_releases")
    ?.asList().orEmpty().map { it.toString() }

repositories {
    /**
     * Restricts dependency search of the given [groups] to the [maven URL][url],
     * improving the setup speed.
     */
    fun strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
        forRepository { maven(url) { name = alias } }
        filter { groups.forEach(::includeGroup) }
    }
    strictMaven("https://www.cursemaven.com", "CurseForge", "curse.maven")
    strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")
    strictMaven("https://maven.nucleoid.xyz", "Nucleoid", "xyz.nucleoid", "fr.catcore")
}

dependencies {
    /**
     * Fetches only the required Fabric API modules to not waste time downloading all of them for each version.
     * @see <a href="https://github.com/FabricMC/fabric">List of Fabric API modules</a>
     */
    fun fapi(vararg modules: String) {
        for (it in modules) modImplementation(fabricApi.module(it, sc.properties["deps.fabric_api"]))
    }

    minecraft("com.mojang:minecraft:${sc.current.version}")
    // Applies Mojang Mappings on obfuscated versions
    loomx.applyMojangMappings()

    testImplementation("net.fabricmc:fabric-loader-junit:${property("deps.fabric_loader")}")

    // Use `mod{dependency type}` even on 26.1+ - loom-back-compat converts them
    // Remember to update dependencies in fabric.mod.json and the publishing task
    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    val translationsApiNamespace = if (sc.current.parsed < "1.19.4") "fr.catcore" else "xyz.nucleoid"
    "$translationsApiNamespace:server-translations-api:${property("deps.server_translations")}".let {
        include(it) {
            exclude("net.fabricmc.fabric-api", "fabric-api")
        }
        modImplementation(it) {
            exclude("net.fabricmc.fabric-api", "fabric-api")
        }
    }
    // Added so server-translations-api will work. Depended on in fmj so loader will load them.
    fapi("fabric-resource-loader-v${property("deps.resource_loader")}", "fabric-lifecycle-events-v1", "fabric-networking-api-v1", "fabric-api-base")
}

loom {
    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json") // Useful for interface injection
    accessWidenerPath = sc.process(
        rootProject.file("src/main/resources/no-hat-for-da-ground.ct"),
        "build/processed.ct"
    )

    decompilerOptions.named("vineflower") {
        options.put("mark-corresponding-synthetics", "1") // Adds names to lambdas - useful for mixins
    }

    runConfigs.all {
        preferGradleTask = true
        generateRunConfig = true
        runDirectory = rootProject.file("run") // Shares the run directory between versions
        jvmArguments.add("-Dmixin.debug.export=true") // Exports transformed classes for debugging
    }
}

java {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava

    toolchain {
        vendor = JvmVendorSpec.ADOPTIUM
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}

tasks {
    processResources {
        fun MutableMap<String, String>.register(key: String, property: String) {
            val value: String = sc.properties[property]
            inputs.property(key, value)
            set(key, value)
        }
        inputs.dir("../../src/main/resources/assets/no-hat-for-da-ground/lang")

        // fmj
        val props = buildMap {
            register("id", "mod.id")
            register("name", "mod.name")
            register("version", "mod.version")
            register("minecraft", "mod.mc_compat")
            register("fabric_loader", "deps.fabric_loader")
            register("fabric_api", "deps.fabric_api")
            register("resource_loader", "deps.resource_loader")
        }

        filesMatching("fabric.mod.json") { expand(props) }

        // mixins.json
        val mixinJava = "JAVA_${requiredJava.majorVersion}"
        filesMatching("*.mixins.json") { expand("java" to mixinJava) }


        // lang files
        from("../../src/main/resources/assets/no-hat-for-da-ground/lang") {
            into("data/no-hat-for-da-ground/lang")
        }
    }

    // Includes the license file in the built mod
    withType<Jar> {
        val name = project.property("mod.id")
        inputs.property("mod_id", name)
        from("../../LICENSE") { rename { "$it-$name" } }
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds mod jars and copies results to `build/libs/{mod version}/`"

        inputs.property("version", project.property("mod.version"))
        // loomx.mod(Sources)Jar returns the jar task for the applied loom variant
        from(loomx.modJar.flatMap { it.archiveFile }, loomx.modSourcesJar.flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
    }

    publishMods {
        file = loomx.modJar.map { it.archiveFile.get() }
        dryRun = property("publish.modrinth.token") == null
                || property("publish.curseforge.token") == null

        // Metadata
        type.set(STABLE)
        version = property("mod.version") as String
        modLoaders.add("fabric")

        // User-facing
        displayName.set("${property("mod.name")} ${property("mod.version")} for ${property("mod.mc_title")}")
        changelog = provider { rootProject.file("CHANGELOG-LATEST.md").readText() }

        modrinth {
            projectId = property("publish.modrinth") as String
            accessToken = property("publish.modrinth.token") as String

            projectDescription.set(providers.fileContents(layout.projectDirectory.file("README.md")).asText)

            minecraftVersions.addAll(compatibleVersions)
            additionalFile(loomx.modSourcesJar.map { it.archiveFile.get() }) {
                type.set(SOURCES_JAR)
            }
            environment.set(CLIENT_OR_SERVER)

            requires {
                slug = "fabric-api"
            }
        }

        curseforge {
            projectId = property("publish.curseforge") as String
            accessToken = property("publish.curseforge.token") as String
            client.set(true)
            server.set(true)
            
            minecraftVersions.addAll(compatibleVersions)
            additionalFile(loomx.modSourcesJar.map { it.archiveFile.get() }) {
                this.name = "Sources Jar"
            }

            requires {
                slug = "fabric-api"
            }
        }
    }

    test {
        // Workaround for https://github.com/FabricMC/fabric-loader/issues/817
        if (sc.current.parsed > "1.18") {
            useJUnitPlatform()
        } else {
            enabled = false
        }
    }
}