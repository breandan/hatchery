package edu.umontreal.hatchery.modules

import com.intellij.ide.util.projectWizard.ModuleWizardStep
import javax.swing.JLabel


class DemoModuleWizardStep : ModuleWizardStep() {
  override fun getComponent() = JLabel("Provide some setting here")

  override fun updateDataModel() {
    //todo update model according to UI
  }
}