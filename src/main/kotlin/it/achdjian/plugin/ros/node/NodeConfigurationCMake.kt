package it.achdjian.plugin.ros.node

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.InvalidDataException
import com.intellij.openapi.util.WriteExternalException
import com.jetbrains.cidr.cpp.execution.CMakeAppRunConfiguration
import com.jetbrains.cidr.cpp.execution.CMakeRunConfigurationType
import com.jetbrains.cidr.execution.BuildTargetAndConfigurationData
import com.jetbrains.cidr.execution.CidrCommandLineState
import com.jetbrains.cidr.execution.CidrExecutableDataHolder
import com.jetbrains.cidr.execution.ExecutableData
import it.achdjian.plugin.ros.utils.getPackages
import org.jdom.Element

class NodeConfigurationCMake(project: Project, configurationFactory: ConfigurationFactory, targetName: String) :
  CMakeAppRunConfiguration(project, configurationFactory, targetName), CidrExecutableDataHolder {

  companion object {
    const val PACKAGE_TAG = "rosPackageName"
    const val NODE_TAG = "rosNodeName"
    const val ROS_MASTER_ADDR_TAG = "ros_master_addr"
    const val ROS_MASTER_PORT_TAG = "ros_master_port"
    const val WORKING_DIR = "working_dir"
    const val PARAMS = "params"
    const val PROJECT_TAG = "project"
    const val TARGET_TAG = "target"
    const val CONFIG_TAG = "config"
  }

  var rosPackageName: String? = null
  var rosNodeName: String? = null
  var rosMasterAddr = "localhost"
  var rosMasterPort = 11311

  override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> =
    NodeRunEditorCMake(project, CMakeRunConfigurationType.getHelper(project))

  override fun getState(executor: Executor, environment: ExecutionEnvironment) =
    CidrCommandLineState(environment, NodeLauncherCMake(this, project, environment))

  @Throws(InvalidDataException::class)
  override fun readExternal(parentElement: Element) = parentElement.run {
    val config = getAttributeValue(CONFIG_TAG)?.let { it } ?: "Release"
    val target = getAttributeValue(TARGET_TAG)?.let { it } ?: "all"
    val prj = getAttributeValue(PROJECT_TAG)?.let { it } ?: "Project"
    getAttributeValue(PACKAGE_TAG)?.let { rosPackageName = it }
    getAttributeValue(NODE_TAG)?.let { rosNodeName = it }
    getAttributeValue(ROS_MASTER_ADDR_TAG)?.let { rosMasterAddr = it }
    getAttributeValue(ROS_MASTER_PORT_TAG)?.let { rosMasterPort = it.toInt() }
    getAttributeValue(WORKING_DIR)?.let { workingDirectory = it }
    getAttributeValue(PARAMS)?.let { programParameters = it }
    getPackages(project).firstOrNull { it.name == rosPackageName }?.let { pkg ->
      envs.putAll(pkg.env)
      pkg.getNodes().firstOrNull { node -> node.name == rosNodeName }?.let {
        executableData = ExecutableData(pkg.path.toString())
      }
    }
    if (rosMasterAddr.isNotEmpty()) envs["ROS_MASTER_URI"] = "http://$rosMasterAddr:$rosMasterPort"
    targetAndConfigurationData = BuildTargetAndConfigurationData(prj, target, config)
    setExplicitBuildTargetName("all")
  }

  @Throws(WriteExternalException::class)
  override fun writeExternal(parentElement: Element) {
    targetAndConfigurationData?.let {
      it.target?.let { buildTargetData ->
        parentElement.setAttribute(TARGET_TAG, buildTargetData.targetName)
        parentElement.setAttribute(PACKAGE_TAG, buildTargetData.projectName)
      }
      parentElement.setAttribute(CONFIG_TAG, it.configurationName)
    }
    rosPackageName?.let { parentElement.setAttribute(PACKAGE_TAG, it) }
    rosNodeName?.let { parentElement.setAttribute(NODE_TAG, it) }
    parentElement.setAttribute(ROS_MASTER_ADDR_TAG, rosMasterAddr)
    parentElement.setAttribute(ROS_MASTER_PORT_TAG, rosMasterPort.toString())
    workingDirectory?.let { parentElement.setAttribute(WORKING_DIR, it) }
    programParameters?.let { parentElement.setAttribute(PARAMS, it) }
  }
}