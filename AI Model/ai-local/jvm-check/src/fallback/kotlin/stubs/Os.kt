package android.os

class Looper { companion object { @JvmStatic fun getMainLooper() = Looper() } }

class Handler(@Suppress("UNUSED_PARAMETER") l: Looper) { fun post(r: Runnable): Boolean { r.run(); return true } }
