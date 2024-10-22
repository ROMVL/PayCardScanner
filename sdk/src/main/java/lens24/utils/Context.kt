package lens24.utils

import android.content.Context
import android.content.res.Resources
import androidx.annotation.Dimension

fun Context.dpToPx(@Dimension(unit = Dimension.DP) dp: Int) = resources.dpToPx(dp)
fun Context.dpToPx(@Dimension(unit = Dimension.DP) dp: Float) = resources.dpToPx(dp)

fun Resources.dpToPx(@Dimension(unit = Dimension.DP) dp: Int) = dp * displayMetrics.density
fun Resources.dpToPx(@Dimension(unit = Dimension.DP) dp: Float) = dp * displayMetrics.density
