package com.lifestyle.nestly.dream

import android.service.dreams.DreamService
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.lifestyle.nestly.appContainer
import com.lifestyle.nestly.ui.theme.NestlyTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Nightstand Mode: an interactive DreamService shown by the system while the
 * device is idle and charging/docked, after the user selects Nestly as their
 * screen saver in system settings. The Log Feed button saves the configured
 * default bottle feed in place; Done (or the power button) ends the dream.
 */
class NestlyDreamService : DreamService() {

    private var dreamScope: CoroutineScope? = null
    private var owner: DreamLifecycleOwner? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setFullscreen(true)
        // Let the system keep the screen dim rather than forcing brightness.
        setScreenBright(false)
        // Interactive so the Log Feed button can be tapped without waking the
        // device out of the dream; exiting is via Done or the power button.
        setInteractive(true)

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        dreamScope = scope
        val container = applicationContext.appContainer
        val viewModel = NightstandViewModel(container, scope)
        val quickLog = com.lifestyle.nestly.widget.QuickBottleLog(container)

        val lifecycleOwner = DreamLifecycleOwner().also { it.create() }
        owner = lifecycleOwner

        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                NestlyTheme {
                    NightstandScreen(
                        viewModel = viewModel,
                        onLogFeed = { quickLog.save() },
                        onExit = { finish() },
                    )
                }
            }
        }
        // A Service has no Activity to provide these owners; Compose and
        // lifecycle-aware collection look them up through the view tree.
        listOfNotNull(window?.decorView, composeView).forEach { view ->
            view.setViewTreeLifecycleOwner(lifecycleOwner)
            view.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            view.setViewTreeViewModelStoreOwner(lifecycleOwner)
        }
        setContentView(composeView)
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        owner?.start()
    }

    override fun onDreamingStopped() {
        owner?.stop()
        super.onDreamingStopped()
    }

    override fun onDetachedFromWindow() {
        owner?.destroy()
        owner = null
        dreamScope?.cancel()
        dreamScope = null
        super.onDetachedFromWindow()
    }
}

/** Minimal owners so a [ComposeView] hosted in a Service can run Compose. */
private class DreamLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
    private val registry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry
    override val viewModelStore = ViewModelStore()

    fun create() {
        savedStateController.performRestore(null)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    fun start() {
        registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun stop() {
        registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    fun destroy() {
        registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        viewModelStore.clear()
    }
}
