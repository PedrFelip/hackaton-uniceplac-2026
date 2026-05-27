package com.nutriscan.app.data.diabetic

import com.nutriscan.app.data.model.Product

/**
 * Níveis de risco glicêmico para pessoas com diabetes.
 */
enum class DiabetesRiskLevel {
    /** Risco baixo — adequado para consumo moderado. */
    SAFE,
    /** Risco moderado — atenção recomendada, consumir com moderação. */
    MODERATE,
    /** Risco alto — não recomendado, alto teor de açúcar/carboidratos. */
    HIGH
}

/**
 * Resultado da análise de risco para diabéticos.
 *
 * @property level Nível de risco calculado
 * @property message Mensagem amigável ao usuário
 * @property sugarPer100g Quantidade de açúcar por 100g (ou null se indisponível)
 * @property carbsPer100g Quantidade de carboidratos por 100g (ou null se indisponível)
 */
data class DiabetesRiskResult(
    val level: DiabetesRiskLevel,
    val message: String,
    val sugarPer100g: Double? = null,
    val carbsPer100g: Double? = null
)

/**
 * Calcula o nível de risco glicêmico de um produto para pessoas com diabetes.
 *
 * Regras de avaliação:
 * - **HIGH**: açúcar > 15g/100g OU carboidratos > 50g/100g
 * - **MODERATE**: açúcar > 5g/100g OU carboidratos > 30g/100g
 * - **SAFE**: abaixo dos limiares moderados
 *
 * Quando o produto é ultra-processado (NOVA 4) e tem açúcar elevado,
 * o risco é elevado automaticamente.
 */
object DiabetesRiskCalculator {

    private const val SUGAR_HIGH_THRESHOLD = 15.0   // g/100g
    private const val SUGAR_MODERATE_THRESHOLD = 5.0 // g/100g
    private const val CARBS_HIGH_THRESHOLD = 50.0    // g/100g
    private const val CARBS_MODERATE_THRESHOLD = 30.0 // g/100g

    fun calculate(product: Product): DiabetesRiskResult {
        val sugars = product.nutriments?.sugars100g
        val carbs = product.nutriments?.carbohydrates100g
        val novaGroup = product.novaGroup

        val isUltraProcessed = novaGroup == 4

        return when {
            // Alto risco: açúcar muito elevado OU carboidratos muito elevados
            sugars != null && sugars > SUGAR_HIGH_THRESHOLD ->
                DiabetesRiskResult(
                    level = DiabetesRiskLevel.HIGH,
                    message = "Alto teor de açúcar (${String.format("%.1f", sugars)}g/100g). " +
                            "Não recomendado para diabéticos.",
                    sugarPer100g = sugars,
                    carbsPer100g = carbs
                )

            carbs != null && carbs > CARBS_HIGH_THRESHOLD ->
                DiabetesRiskResult(
                    level = DiabetesRiskLevel.HIGH,
                    message = "Alto teor de carboidratos (${String.format("%.1f", carbs)}g/100g). " +
                            "Pode causar picos de glicemia.",
                    sugarPer100g = sugars,
                    carbsPer100g = carbs
                )

            // Risco moderado: açúcar ou carboidratos elevados, ou ultra-processado com açúcar
            sugars != null && sugars > SUGAR_MODERATE_THRESHOLD ->
                DiabetesRiskResult(
                    level = DiabetesRiskLevel.MODERATE,
                    message = "Teor moderado de açúcar (${String.format("%.1f", sugars)}g/100g). " +
                            "Consuma com moderação e monitore a glicemia.",
                    sugarPer100g = sugars,
                    carbsPer100g = carbs
                )

            carbs != null && carbs > CARBS_MODERATE_THRESHOLD ->
                DiabetesRiskResult(
                    level = DiabetesRiskLevel.MODERATE,
                    message = "Teor moderado de carboidratos (${String.format("%.1f", carbs)}g/100g). " +
                            "Atenção ao controle da glicemia.",
                    sugarPer100g = sugars,
                    carbsPer100g = carbs
                )

            isUltraProcessed && sugars != null && sugars > 0 ->
                DiabetesRiskResult(
                    level = DiabetesRiskLevel.MODERATE,
                    message = "Produto ultra-processado com açúcar. " +
                            "Prefira alimentos naturais.",
                    sugarPer100g = sugars,
                    carbsPer100g = carbs
                )

            // Dados nutricionais indisponíveis — não podemos avaliar
            sugars == null && carbs == null ->
                DiabetesRiskResult(
                    level = DiabetesRiskLevel.SAFE,
                    message = "Dados nutricionais insuficientes para avaliação. " +
                            "Consulte o rótulo do produto.",
                    sugarPer100g = null,
                    carbsPer100g = null
                )

            // Seguro
            else ->
                DiabetesRiskResult(
                    level = DiabetesRiskLevel.SAFE,
                    message = "Baixo teor de açúcar e carboidratos. Adequado para diabéticos.",
                    sugarPer100g = sugars,
                    carbsPer100g = carbs
                )
        }
    }
}
