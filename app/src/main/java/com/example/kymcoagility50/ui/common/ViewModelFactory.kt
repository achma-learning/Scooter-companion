package com.example.kymcoagility50.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kymcoagility50.AppContainer

/**
 * Constructs a ViewModel whose constructor takes an [AppContainer], resolved from the
 * singleton instance for this process. Avoids pulling in a DI framework for a project this
 * size while keeping every screen's ViewModel trivially testable (pass a fake AppContainer).
 */
@Composable
inline fun <reified VM : ViewModel> appViewModel(crossinline create: (AppContainer) -> VM): VM {
    val context = LocalContext.current
    val container = AppContainer.getInstance(context)
    val factory = remember(container) {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return create(container) as T
            }
        }
    }
    return viewModel(factory = factory)
}
