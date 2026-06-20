package com.qualimetry.csharp.rider

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(
    name = "QualimetryCSharpSettings",
    storages = [Storage("qualimetry-csharp.xml")]
)
class QualimetrySettings : PersistentStateComponent<QualimetrySettings.State> {

    data class State(
        var sonarQubeUrl: String = "",
        var token: String = "",
        var profileName: String = DEFAULT_PROFILE_NAME,
        var analyzerVersion: String = DEFAULT_ANALYZER_VERSION,
        var autoProvision: Boolean = true
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    var sonarQubeUrl: String
        get() = state.sonarQubeUrl
        set(value) { state.sonarQubeUrl = value }

    var token: String
        get() = state.token
        set(value) { state.token = value }

    var profileName: String
        get() = state.profileName.ifBlank { DEFAULT_PROFILE_NAME }
        set(value) { state.profileName = value }

    var analyzerVersion: String
        get() = state.analyzerVersion.ifBlank { DEFAULT_ANALYZER_VERSION }
        set(value) { state.analyzerVersion = value }

    var autoProvision: Boolean
        get() = state.autoProvision
        set(value) { state.autoProvision = value }

    companion object {
        const val DEFAULT_PROFILE_NAME = "Qualimetry C#"
        const val DEFAULT_ANALYZER_VERSION = "1.0.9"

        fun getInstance(): QualimetrySettings =
            ApplicationManager.getApplication().getService(QualimetrySettings::class.java)
    }
}
