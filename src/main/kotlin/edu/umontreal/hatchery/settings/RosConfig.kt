package edu.umontreal.hatchery.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.options.Configurable
import javax.swing.JComponent

/* Persists the state of the AceJump IDE settings across IDE restarts.
 * https://www.jetbrains.org/intellij/sdk/docs/basics/persisting_state_of_components.html
 */

@State(name = "RosConfig", storages = [(Storage("hatchery.xml"))])
object RosConfig : Configurable, PersistentStateComponent<RosSettings> {
  private val logger = Logger.getInstance(RosConfig::class.java)
  var settings: RosSettings = RosSettings()

  override fun getState() = settings

  override fun loadState(state: RosSettings) {
    logger.info("Loaded RosConfig settings: $settings")
    settings = state
  }

  private val panel = RosSettingsPanel()

  override fun getDisplayName() = "Hatchery"

  override fun createComponent(): JComponent = panel.rootPanel

  override fun isModified() = panel.rosPath != settings.rosPath

  override fun apply() {
    settings.rosPath = panel.rosPath
    logger.info("User applied new settings: $settings")
  }

  override fun reset() = panel.reset(settings)
}