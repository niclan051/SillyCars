plugins {
	alias(libs.plugins.kotlin.jvm)
	alias(libs.plugins.loom)
	alias(libs.plugins.blossom)
	alias(libs.plugins.ksp)
	alias(libs.plugins.spotless)
	id("maven-publish")
}

class ModData {
	val id = property("mod.id") as String
	val name = property("mod.name") as String
	val version = property("mod.version") as String
	val group = property("mod.group") as String
	val description = property("mod.description") as String
	val source = property("mod.source") as String
	val issues = property("mod.issues") as String
	val license = property("mod.license") as String
	val modrinth = property("mod.modrinth") as String
	val curseforge = property("mod.curseforge") as String
	val discord = property("mod.discord") as String
	val minecraftVersion = property("mod.minecraft_version") as String
	val minecraftVersionRange = property("mod.minecraft_version_range") as String
}

class Dependencies {
	val fabricLoaderVersion = property("deps.fabric_loader_version") as String?

	val devAuthVersion = property("deps.devauth_version") as String?
	val lombokVersion = property("deps.lombok_version") as String?
	val mixinConstraintsVersion = property("deps.mixinconstraints_version") as String?
	val mixinSquaredVersion = property("deps.mixinsquared_version") as String?

	// Versioned
	val fabricApiVersion = property("deps.fabric_api_version") as String?
}

val mod = ModData()
val deps = Dependencies()

class LoaderData {
	val name = "fabric"
}

val loader = LoaderData()

val versionString = "${mod.version}-${mod.minecraftVersion}_${loader.name}"
group = mod.group
base {
	archivesName.set("${mod.id}-${versionString}")
}

java {
	sourceCompatibility = JavaVersion.VERSION_25
	targetCompatibility = JavaVersion.VERSION_25
}

loom {
	splitEnvironmentSourceSets()

	mods {
		register(mod.id) {
			sourceSet(sourceSets.main.get())
			sourceSet(sourceSets.getByName("client"))
		}
	}

	runs {
		afterEvaluate {
			configureEach {
				property("mixin.hotSwap", "true")
				property("mixin.debug.export", "true") // Puts mixin outputs in /run/.mixin.out
				property("devauth.enabled", "true")
				property("devauth.account", "main")
			}
		}
	}
}

fabricApi {
	configureDataGeneration {
		client = true
	}
}

repositories {
	mavenCentral()
	mavenLocal()
	maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1") // DevAuth
	maven("https://maven.parchmentmc.org") // Parchment
	maven("https://maven.neoforged.net/releases") // NeoForge
	maven("https://maven.bawnorton.com/releases") // MixinSquared
	maven("https://maven.terraformersmc.com/") // Mod Menu
	maven("https://maven.isxander.dev/releases") // YACL
}

dependencies {
	minecraft("com.mojang:minecraft:${mod.minecraftVersion}")
	implementation("net.fabricmc:fabric-loader:${deps.fabricLoaderVersion}")!!

	compileOnly("org.projectlombok:lombok:${deps.lombokVersion}")
	annotationProcessor("org.projectlombok:lombok:${deps.lombokVersion}")
    if (!providers.environmentVariable("CI").isPresent)
	runtimeOnly("me.djtheredstoner:DevAuth-${loader.name}:${deps.devAuthVersion}")
	include(implementation("com.moulberry:mixinconstraints:${deps.mixinConstraintsVersion}")!!)!!
	include(implementation(annotationProcessor("com.github.bawnorton.mixinsquared:mixinsquared-${loader.name}:${deps.mixinSquaredVersion}")!!)!!)

	implementation("net.fabricmc.fabric-api:fabric-api:${deps.fabricApiVersion}")

	optionalProp("deps.modmenu_version") { prop ->
		implementation("com.terraformersmc:modmenu:$prop") {
			exclude(group = "net.fabricmc.fabric-api")
		}
	}

	optionalProp("deps.yacl_version") { prop ->
		implementation("dev.isxander:yet-another-config-lib:$prop-fabric") {
			exclude(group = "net.fabricmc.fabric-api")
		}
	}
}

tasks {
	processResources {
		val props = buildMap {
			put("id", mod.id)
			put("name", mod.name)
			put("version", mod.version)
			put("description", mod.description)
			put("source", mod.source)
			put("issues", mod.issues)
			put("license", mod.license)
			put("modrinth", mod.modrinth)
			put("curseforge", mod.curseforge)
			put("discord", mod.discord)

			val minecraftVersionRange = if (mod.minecraftVersionRange.contains(' ')) {
				val parts = mod.minecraftVersionRange.trim().split(' ')
				">=" + parts.first() + ' ' + "<=" + parts.last()
			} else {
				mod.minecraftVersionRange
			}

			put("minecraft_version_range", minecraftVersionRange)
			put("fabric_api_version", deps.fabricApiVersion?.trim())
			put("fabric_loader_version", deps.fabricLoaderVersion?.trim())
		}

		props.forEach(inputs::property)
		filesMatching("**/lang/en_us.json") { // Defaults description to English translation
			expand(props)
			filteringCharset = "UTF-8"
		}

		filesMatching("fabric.mod.json") { expand(props) }
	}

	register<Copy>("buildAndCollect") {
		group = "build"

		into(rootProject.layout.buildDirectory.file("libs/${mod.version}"))
		dependsOn("build")
	}
}

val currentCommitHash: String by lazy {
	Runtime.getRuntime()
		.exec(arrayOf("git", "rev-parse", "--verify", "--short", "HEAD"), null, rootDir)
		.inputStream.bufferedReader().readText().trim()
}

blossom {
	replaceToken("@MODID@", mod.id)
	replaceToken("@VERSION@", mod.version)
	replaceToken("@COMMIT_HASH@", currentCommitHash)
}

publishing {
	publications {
		create<MavenPublication>("mavenJava") {
			artifactId = mod.id
			group = project.group
			version = versionString
			from(components["java"])
		}
	}

	repositories {}
}

fun <T> optionalProp(property: String, block: (String) -> T?): T? =
	findProperty(property)?.toString()?.takeUnless { it.isBlank() }?.let(block)

// Header
spotless {
	val licenseHeader = rootProject.file("HEADER")
	lineEndings = com.diffplug.spotless.LineEnding.UNIX

	java {
		licenseHeaderFile(licenseHeader)
		target("src/**/*.java")
	}

	kotlin {
		licenseHeaderFile(licenseHeader)
		target("src/**/*.kt")
	}
}
