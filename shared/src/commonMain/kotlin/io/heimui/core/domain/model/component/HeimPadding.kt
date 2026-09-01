package io.heimui.core.domain.model.component

/**
 * Padding around a component, in dp.
 *
 * A payload may write it two ways, and both mean the same thing when all sides are equal:
 *
 * ```json
 * "padding": 16
 * "padding": { "horizontal": 16, "top": 24 }
 * ```
 *
 * The shorthand keys fill the sides they cover, and an explicit side always wins over the
 * shorthand that would otherwise set it — so `{ "horizontal": 16, "start": 0 }` reads exactly as
 * written rather than depending on key order, which JSON does not guarantee.
 *
 * Sides are [start] and [end] rather than left and right, so a payload authored once lays out
 * correctly in Arabic and Hebrew without the server knowing the reader's locale.
 */
public data class HeimPadding(
    public val start: Int = 0,
    public val top: Int = 0,
    public val end: Int = 0,
    public val bottom: Int = 0,
) {
    /** True when there is nothing to apply, letting a renderer skip the modifier entirely. */
    public val isEmpty: Boolean get() = start == 0 && top == 0 && end == 0 && bottom == 0

    /**
     * Clamps every side to zero or more.
     *
     * A negative padding is not a layout instruction, it is a bug upstream — Compose would treat
     * it as an error rather than shrinking the box, so the whole screen would fail over one bad
     * number in one component.
     */
    public fun clampedToZero(): HeimPadding =
        if (start >= 0 && top >= 0 && end >= 0 && bottom >= 0) {
            this
        } else {
            HeimPadding(
                start = maxOf(0, start),
                top = maxOf(0, top),
                end = maxOf(0, end),
                bottom = maxOf(0, bottom),
            )
        }

    public companion object {
        public val None: HeimPadding = HeimPadding()

        /** The same value on all four sides — what `"padding": 16` means. */
        public fun all(value: Int): HeimPadding =
            HeimPadding(start = value, top = value, end = value, bottom = value)
    }
}
