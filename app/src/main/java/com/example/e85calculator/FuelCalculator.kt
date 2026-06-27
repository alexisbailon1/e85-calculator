package com.example.e85calculator

data class BlendResult(
    val gallonsE85Needed: Double,
    val gallonsPumpGasNeeded: Double,
    val totalFillVolume: Double
)

object FuelCalculator {
    fun calculateBlend(
        tankCapacity: Double,
        currentFuelLevelPercentage: Double,
        currentEthanolPercentage: Double,
        targetEthanolPercentage: Double,
        pumpE85Percentage: Double,
        pumpGasPercentage: Double
    ): BlendResult {
        if (tankCapacity <= 0 || currentFuelLevelPercentage >= 100.0) return BlendResult(0.0, 0.0, 0.0)

        val currentFuelVolume = tankCapacity * (currentFuelLevelPercentage / 100.0)
        val totalFillVolume = tankCapacity - currentFuelVolume
        if (totalFillVolume <= 0) return BlendResult(0.0, 0.0, 0.0)

        val eTarget = targetEthanolPercentage / 100.0
        val eCurrent = currentEthanolPercentage / 100.0
        val eE85 = pumpE85Percentage / 100.0
        val eGas = pumpGasPercentage / 100.0

        val numerator = (eTarget * tankCapacity) - (eCurrent * currentFuelVolume) - (eGas * totalFillVolume)
        val denominator = eE85 - eGas
        if (denominator == 0.0) return BlendResult(0.0, 0.0, 0.0)

        val gallonsE85 = numerator / denominator
        val gallonsGas = totalFillVolume - gallonsE85

        return if (gallonsE85 < 0 || gallonsGas < 0 || gallonsE85 > totalFillVolume) {
            BlendResult(0.0, 0.0, 0.0)
        } else {
            BlendResult(gallonsE85, gallonsGas, totalFillVolume)
        }
    }
}