package io.heimui.core.presentation.state

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import kotlin.jvm.JvmInline

/**
 * The namespace a form field belongs to.
 *
 * A screen used to have exactly one: `state_key` was both the name of a field and its identity, so
 * two fields with the same key were the same field. That was true and harmless while a screen was
 * a fixed set of inputs. It stopped being harmless when a list could repeat a template: three
 * passengers on screen share one `full_name`, so typing in the third row fills the first, one
 * validation rule covers all three, and the submitted payload carries a single name.
 *
 * The scope is the missing half of the identity. It is written by whatever expanded the list --
 * the server, today -- and the author keeps writing `full_name`. Empty means the screen itself,
 * which is every screen that existed before this and every screen without a repeated form.
 */
@JvmInline
public value class HeimStateScope(public val path: String) {

    public val isRoot: Boolean get() = path.isEmpty()

    /**
     * The key this scope stores [stateKey] under.
     *
     * The separator has to be something an author would not write. `::` is the compromise: it
     * reads in a draft file (`passengers/1::full_name`) and the Studio refuses a `state_key`
     * containing it, which is cheaper than encoding every key.
     */
    public fun resolve(stateKey: String): String =
        if (path.isEmpty() || stateKey.isEmpty()) stateKey else "$path$SEPARATOR$stateKey"

    /** The scope one level in, for a list inside a list. */
    public fun child(source: String, item: String): HeimStateScope =
        HeimStateScope(if (path.isEmpty()) "$source/$item" else "$path/$source/$item")

    public companion object {
        public const val SEPARATOR: String = "::"

        public val Root: HeimStateScope = HeimStateScope("")

        /** Splits a stored key back into the scope that owns it and the key the author wrote. */
        public fun split(storedKey: String): Pair<HeimStateScope, String> {
            val index = storedKey.lastIndexOf(SEPARATOR)
            return if (index < 0) {
                Root to storedKey
            } else {
                HeimStateScope(storedKey.substring(0, index)) to
                    storedKey.substring(index + SEPARATOR.length)
            }
        }
    }
}

/**
 * The scope in force for the subtree being composed.
 *
 * Static because it changes only where a repeated item begins: making it dynamic would invalidate
 * every reader on any change, which is the opposite of what a form inside a long list needs.
 */
public val LocalHeimStateScope: ProvidableCompositionLocal<HeimStateScope> =
    staticCompositionLocalOf { HeimStateScope.Root }
