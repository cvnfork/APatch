package me.bmax.apatch.ui.viewmodel

import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.bmax.apatch.Natives
import java.text.Collator
import java.util.Locale

class KPModuleViewModel : ViewModel() {
    companion object {
        private const val TAG = "KPModuleViewModel"
        private var modules by mutableStateOf<List<KPModel.KPMInfo>>(emptyList())
    }

    var isRefreshing by mutableStateOf(false)
        private set

    val moduleList by derivedStateOf {
        val comparator = compareBy(Collator.getInstance(Locale.getDefault()), KPModel.KPMInfo::name)
        modules.sortedWith(comparator)
    }

    var isNeedRefresh by mutableStateOf(false)
        private set

    fun markNeedRefresh() {
        isNeedRefresh = true
    }

    fun fetchModuleList() {
        viewModelScope.launch(Dispatchers.IO) {
            isRefreshing = true

            // Artificial delay to prevent state coalescing in Compose
            delay(50)

            val start = SystemClock.elapsedRealtime()

            try {
                var names = Natives.kernelPatchModuleList()
                if (Natives.kernelPatchModuleNum() <= 0) names = ""
                val nameList = names.split('\n').filter { it.isNotEmpty() }

                modules = nameList.map { id ->
                    val infoline = Natives.kernelPatchModuleInfo(id)
                    val spi = infoline.split('\n')

                    fun getVal(key: String) = spi.find { it.startsWith("$key=") }?.substringAfter('=') ?: ""

                    KPModel.KPMInfo(
                        KPModel.ExtraType.KPM,
                        getVal("name"), "", getVal("args"), getVal("version"),
                        getVal("license"), getVal("author"), getVal("description")
                    )
                }
                isNeedRefresh = false
            } catch (e: Exception) {
                Log.e(TAG, "fetch failed", e)
            } finally {
                // Always reset refreshing state here
                isRefreshing = false
                Log.i(TAG, "load cost: ${SystemClock.elapsedRealtime() - start}")
            }
        }
    }
}
