package io.heimui.core.domain.model.component

/**
 * Intrinsic dimensions a component asks for, in dp.
 *
 * Separate from `weight`, which is *relational*: weight is a share of the parent's axis and only
 * the parent can apply it, while these are what the component asks for regardless of who holds
 * it. Every major layout system draws the same line — Compose has `Modifier.size` and
 * `RowScope.weight`, Flutter has `SizedBox` and `Expanded`, SwiftUI has `.frame` and layout
 * priority.
 *
 * ### Which one to reach for
 *
 * [minHeight] and [minWidth] are the safe pair, and should be the default choice. They guarantee a
 * floor while letting the component grow, so a card that must look consistent in a list stays
 * consistent without breaking when the content is longer than the author expected.
 *
 * [aspectRatio] keeps a proportion instead of a measurement, so a 16:9 banner is correct on a
 * folding phone and on a tablet without the server knowing either exists.
 *
 * [width] and [height] are fixed and **do not grow**. That is right for an avatar, an icon frame,
 * or carousel items that must all measure the same — and wrong for anything containing text. A
 * user running their font at 200% for accessibility will see text clipped inside a fixed box, and
 * there is nothing they can do about it. In server-driven UI that mistake is worse than in native
 * code: it ships to every device at once, with no one having seen it rendered.
 *
 * ### How they combine
 *
 * A fixed dimension wins over the minimum for the same axis — asking for both is contradictory,
 * and the explicit measurement is the more specific instruction. [aspectRatio] applies only when
 * exactly one axis is otherwise unconstrained, since it exists to derive the other one; giving
 * both a width and a height leaves it nothing to compute.
 */
public data class HeimSize(
    public val width: Int? = null,
    public val height: Int? = null,
    public val minWidth: Int? = null,
    public val minHeight: Int? = null,
    public val aspectRatio: Float? = null,
) {
    /** True when nothing is constrained, letting a renderer skip the modifiers entirely. */
    public val isEmpty: Boolean
        get() = width == null && height == null &&
            minWidth == null && minHeight == null && aspectRatio == null

    /**
     * Drops values Compose would reject.
     *
     * A zero or negative dimension is not a layout instruction, it is a bug upstream — and one
     * bad number should cost its own component's sizing, not the whole screen.
     */
    public fun sanitized(): HeimSize = HeimSize(
        width = width?.takeIf { it > 0 },
        height = height?.takeIf { it > 0 },
        minWidth = minWidth?.takeIf { it > 0 },
        minHeight = minHeight?.takeIf { it > 0 },
        aspectRatio = aspectRatio?.takeIf { it > 0f && it.isFinite() },
    )

    public companion object {
        public val None: HeimSize = HeimSize()
    }
}
