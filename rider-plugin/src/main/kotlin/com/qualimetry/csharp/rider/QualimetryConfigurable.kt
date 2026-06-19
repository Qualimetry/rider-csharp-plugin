package com.qualimetry.csharp.rider

import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

class QualimetryConfigurable : Configurable {

    private val urlField = JBTextField()
    private val tokenField = JBPasswordField()
    private val profileField = JBTextField()
    private val analyzerVersionField = JBTextField()

    private var panel: JPanel? = null

    override fun getDisplayName(): String = "Qualimetry C#"

    override fun createComponent(): JComponent {
        val built = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("SonarQube URL:"), urlField, 1, false)
            .addLabeledComponent(JBLabel("Token:"), tokenField, 1, false)
            .addLabeledComponent(JBLabel("Profile name:"), profileField, 1, false)
            .addLabeledComponent(JBLabel("Analyzer version:"), analyzerVersionField, 1, false)
            .addComponentFillVertically(JPanel(), 0)
            .panel
        panel = built
        reset()
        return built
    }

    override fun isModified(): Boolean {
        val s = QualimetrySettings.getInstance()
        return urlField.text != s.sonarQubeUrl ||
            String(tokenField.password) != s.token ||
            profileField.text != s.profileName ||
            analyzerVersionField.text != s.analyzerVersion
    }

    override fun apply() {
        val s = QualimetrySettings.getInstance()
        s.sonarQubeUrl = urlField.text.trim()
        s.token = String(tokenField.password)
        s.profileName = profileField.text.trim()
        s.analyzerVersion = analyzerVersionField.text.trim()
    }

    override fun reset() {
        val s = QualimetrySettings.getInstance()
        urlField.text = s.sonarQubeUrl
        tokenField.text = s.token
        profileField.text = s.profileName
        analyzerVersionField.text = s.analyzerVersion
    }

    override fun disposeUIResources() {
        panel = null
    }
}
