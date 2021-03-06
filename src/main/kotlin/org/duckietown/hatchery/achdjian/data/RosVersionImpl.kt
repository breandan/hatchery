package org.duckietown.hatchery.achdjian.data

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.*
import com.intellij.util.xmlb.annotations.Transient
import org.duckietown.hatchery.achdjian.settings.*
import java.nio.file.*

interface RosVersion {
    val env: Map<String, String>

    fun initWorkspace(projectPath: VirtualFile): VirtualFile?
    fun createPackage(path: VirtualFile, name: String, dependencies: List<String>)
    fun searchPackages(): List<RosPackage>
}

data class RosVersionImpl(var path: String, var name: String) : RosVersion {
    companion object {
        private val LOG = Logger.getInstance(RosVersionImpl::class.java)

        private fun splitPath(env: Map<String, String>): List<String> {
            var path = env["PATH"]

            if (path == null) {
                val actualEnv = System.getenv()
                path = actualEnv["PATH"]
            }
            val splittedPath = ArrayList<String>()
            path?.let {
                splittedPath.addAll(it.split(":"))
            }

            return splittedPath
        }
    }

    override val env = diffEnvironment(Paths.get(path))
    private val initWorkspaceCmd = findInitCmd(env["PATH"] ?: "")
    val envPath: List<String>
    val rosLaunch: String
        get() = "$path/bin/roslaunch"
    private val createPackage: CreatePackage

    init {
        LOG.trace("scan version $name in path $path")
        envPath = splitPath(env)
        createPackage = createPackageFactory(this)
    }

    @Transient
    val packages: MutableList<RosPackage> = ArrayList()

    override fun initWorkspace(baseDir: VirtualFile): VirtualFile? {
        LOG.trace("init workspace at ${baseDir.path}")
        initWorkspaceCmd?.let {
            val srcDir = VfsUtil.createDirectoryIfMissing(baseDir, "src")
            LOG.trace("cmd: $it")
            val processBuilder = ProcessBuilder()
                    .directory(VfsUtil.virtualToIoFile(srcDir))
                    .command(it.toString())

            processBuilder.environment().putAll(env)
            val process = processBuilder.start()
            val processEnd = process.waitFor()
            LOG.trace("Init workspace finished with code $processEnd")
            val cmakeFile = baseDir.findChild("src")?.findChild("CMakeLists.txt")
            LOG.trace("Created file ${cmakeFile?.path}")
            return cmakeFile
        }
        return null
    }

    override fun createPackage(path: VirtualFile, name: String, dependencies: List<String>) = createPackage.createPackage(path, name, dependencies)

    override fun searchPackages(): List<RosPackage> {
        packages.clear()
        val packagesPath = env["ROS_PACKAGE_PATH"]
        LOG.trace("packagePath: $packagesPath")
        packagesPath?.let { path ->
            Files.list(Paths.get(path))
                    .filter { toFilter -> toFilter.resolve("package.xml").toFile().exists() }
                    .map { path -> RosPackage(path, env) }
                    .forEach { rosPackage -> packages.add(rosPackage) }
        }
        packages.sortWith(PackagesComparator())
        return packages
    }

}

object RosVersionNull : RosVersion {
    override val env = HashMap<String, String>()

    override fun initWorkspace(projectPath: VirtualFile): VirtualFile? = null

    override fun createPackage(path: VirtualFile, name: String, dependencies: List<String>) = Unit

    override fun searchPackages(): List<RosPackage> = emptyList()

}

class PackagesComparator : Comparator<RosPackage> {
    override fun compare(a: RosPackage, b: RosPackage): Int {
        val isTopA = isTop(a.name)
        val isTopB = isTop(b.name)

        return when {
            isTopA && isTopB -> a.name.compareTo(b.name)
            isTopA && !isTopB -> -1
            !isTopA && isTopB -> 1
            else -> a.name.compareTo(b.name)
        }
    }

    private fun isTop(name: String) = name == "roscpp" || name == "rosmsg" || name == "rospy"
}