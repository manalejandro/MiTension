package com.manalejandro.mitension.ui.components

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.manalejandro.mitension.R
import com.manalejandro.mitension.ui.theme.BpElevated
import com.manalejandro.mitension.ui.theme.BpHyp1
import com.manalejandro.mitension.ui.theme.BpHyp2
import com.manalejandro.mitension.ui.theme.BpLow
import com.manalejandro.mitension.ui.theme.BpNormal
import com.manalejandro.mitension.util.BloodPressure
import com.manalejandro.mitension.util.BloodPressure.Category

fun categoryColor(category: Category): Color = when (category) {
    Category.LOW -> BpLow
    Category.NORMAL -> BpNormal
    Category.ELEVATED -> BpElevated
    Category.HYPERTENSION_1 -> BpHyp1
    Category.HYPERTENSION_2 -> BpHyp2
}

fun categoryColor(systolic: Double, diastolic: Double): Color =
    categoryColor(BloodPressure.category(systolic, diastolic))

@StringRes
fun categoryLabelRes(category: Category): Int = when (category) {
    Category.LOW -> R.string.category_low
    Category.NORMAL -> R.string.category_normal
    Category.ELEVATED -> R.string.category_elevated
    Category.HYPERTENSION_1 -> R.string.category_hyp1
    Category.HYPERTENSION_2 -> R.string.category_hyp2
}