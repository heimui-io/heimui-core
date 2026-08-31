package io.heimui.core

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform