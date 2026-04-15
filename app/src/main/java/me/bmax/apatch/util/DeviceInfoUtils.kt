package me.bmax.apatch.util

import android.util.Log
import com.topjohnwu.superuser.Shell

fun getSELinuxStatus(): String {
    val shell = Shell.Builder.create().build("sh")
    val list = ArrayList<String>()
    val result = shell.newJob().add("getenforce").to(list, list).exec()
    val output = result.out.joinToString("\n").trim()

    return if (result.isSuccess) output
    else if (output.endsWith("Permission denied")) "Enforcing"
    else "Unknown"
}

private fun getSystemProperty(key: String): Boolean {
    try {
        val c = Class.forName("android.os.SystemProperties")
        val get = c.getMethod(
            "getBoolean",
            String::class.java,
            Boolean::class.javaPrimitiveType
        )
        return get.invoke(c, key, false) as Boolean
    } catch (e: Exception) {
        Log.e("APatch", "[DeviceUtils] Failed to get system property: ", e)
    }
    return false
}

// Check to see if device supports A/B (seamless) system updates
fun isABDevice(): Boolean {
    return getSystemProperty("ro.build.ab_update")
}