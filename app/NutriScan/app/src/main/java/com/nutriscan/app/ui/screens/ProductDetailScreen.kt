package com.nutriscan.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import com.nutriscan.app.ui.viewmodel.CartViewModel
import kotlinx.coroutines.launch
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.nutriscan.app.data.diabetic.DiabetesRiskCalculator
import com.nutriscan.app.data.local.DiabeticPreference
import com.nutriscan.app.data.model.Nutriments
import com.nutriscan.app.data.model.Product
import com.nutriscan.app.ui.components.DiabeticAlertCard
import com.nutriscan.app.ui.viewmodel.ProductDetailViewModel

/** Retorna a cor correspondente ao grau do Nutri-Score (A=verde, E=vermelho). */
private fun nutriscoreColor(grade: String?): Color = when (grade?.lowercase()) {
    "a" -> Color(0xFF1B8A2E)
    "b" -> Color(0xFF7AC143)
    "c" -> Color(0xFFFFC107)
    "d" -> Color(0xFFFF8C00)
    "e" -> Color(0xFFE53935)
    else -> Color.Gray
}

/** Retorna o rótulo descritivo do grupo NOVA de processamento. */
private fun novaLabel(group: Int?): String = when (group) {
    1 -> "Grupo 1 — Sem processamento"
    2 -> "Grupo 2 — Ingredientes culinários"
    3 -> "Grupo 3 — Processado"
    4 -> "Grupo 4 — Ultra-processado"
    else -> "Indisponível"
}

/**
 * Tela de detalhe do produto.
 * Exibe todas as informações nutricionais, classificações e alternativas mais saudáveis.
 *
 * @param barcode Código de barras do produto a ser carregado
 * @param onBackClick Callback para voltar à tela anterior
 * @param onProductClick Callback ao clicar em uma alternativa (navega para detalhe dela)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    barcode: String,
    onBackClick: () -> Unit = {},
    onProductClick: (String) -> Unit = {},
    viewModel: ProductDetailViewModel = viewModel(),
    cartViewModel: CartViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Preferência e estado do modo diabético
    val diabeticPreference = remember { DiabeticPreference(context) }
    var isDiabeticMode by rememberSaveable { mutableStateOf(diabeticPreference.isDiabeticModeEnabled()) }

    // Carrega o produto quando a tela é composta com um novo barcode
    LaunchedEffect(barcode) {
        viewModel.loadProduct(barcode)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Detalhes do Produto") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    // Toggle modo diabético
                    IconButton(
                        onClick = {
                            isDiabeticMode = !isDiabeticMode
                            diabeticPreference.setDiabeticModeEnabled(isDiabeticMode)
                            if (isDiabeticMode) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Modo diabético ativado")
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isDiabeticMode) Icons.Filled.Medication else Icons.Outlined.Medication,
                            contentDescription = if (isDiabeticMode) "Desativar modo diabético" else "Ativar modo diabético",
                            tint = if (isDiabeticMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (state.product != null) {
                FloatingActionButton(
                    onClick = {
                        state.product?.code?.let {
                            cartViewModel.addToCart(it)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Produto adicionado ao carrinho")
                            }
                        } ?: run {
                            // Fallback se code for nulo
                            cartViewModel.addToCart(barcode)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Produto adicionado ao carrinho")
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.AddShoppingCart, contentDescription = "Adicionar ao Carrinho")
                }
            }
        }
    ) { innerPadding ->
        when {
            state.isLoading -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Carregando produto...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            state.error != null -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(innerPadding).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(state.error!!, color = MaterialTheme.colorScheme.error)
                }
            }
            state.product != null -> {
                val diabeticResult = remember(state.product) {
                    DiabetesRiskCalculator.calculate(state.product!!)
                }
                ProductDetailContent(
                    product = state.product!!,
                    alternatives = state.alternatives,
                    isLoadingAlternatives = state.isLoadingAlternatives,
                    isDiabeticMode = isDiabeticMode,
                    diabeticResult = diabeticResult,
                    onProductClick = onProductClick,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

/**
 * Conteúdo principal da tela de detalhe, organizado em seções:
 * 1. Imagem do produto (se disponível)
 * 2. Nome e marca
 * 3. Cards de Nutri-Score e NOVA group
 * 4. Tabela nutricional
 * 5. Alternativas mais saudáveis
 */
@Composable
private fun ProductDetailContent(
    product: Product,
    alternatives: List<Product>,
    isLoadingAlternatives: Boolean,
    isDiabeticMode: Boolean,
    diabeticResult: com.nutriscan.app.data.diabetic.DiabetesRiskResult,
    onProductClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Imagem do produto (só exibe se image_url estiver disponível)
        product.imageUrl?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = product.productName,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Text(
            text = product.productName ?: "Sem nome",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = product.brands ?: "Marca desconhecida",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Cards lado a lado: Nutri-Score (colorido) e NOVA group
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = nutriscoreColor(product.nutriscoreGrade).copy(alpha = 0.15f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Nutri-Score", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = (product.nutriscoreGrade ?: "?").uppercase(),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = nutriscoreColor(product.nutriscoreGrade)
                    )
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("NOVA", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = (product.novaGroup ?: "?").toString(),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = novaLabel(product.novaGroup),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Alerta para diabéticos (só exibe se modo estiver ativado)
        if (isDiabeticMode) {
            DiabeticAlertCard(result = diabeticResult)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Tabela nutricional (apenas se houver dados de nutriments)
        product.nutriments?.let { nutriments ->
            NutritionTable(nutriments, product.nutritionDataPer)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Seção de alternativas mais saudáveis
        if (isLoadingAlternatives) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Buscando alternativas...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else if (alternatives.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Alternativas mais saudáveis",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    alternatives.forEach { alt ->
                        AlternativeCard(
                            product = alt,
                            onClick = { alt.code?.let(onProductClick) }
                        )
                        if (alt != alternatives.last()) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * Tabela nutricional com valores por porção (geralmente 100g).
 * Todos os valores são formatados com 2 casas decimais.
 * Só exibe as linhas cujo valor está disponível (não null).
 */
@Composable
private fun NutritionTable(nutriments: Nutriments, per: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "Informação Nutricional",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Porção: ${per ?: "100g"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Formata valores com 2 casas decimais (ex: "12.50 g")
            val fmt: (Double?, String) -> String? = { v, u -> v?.let { "%.2f $u".format(it) } }

            val rows = listOf(
                "Calorias" to fmt(nutriments.energyKcal100g, "kcal"),
                "Carboidratos" to fmt(nutriments.carbohydrates100g, "g"),
                "Açúcares" to fmt(nutriments.sugars100g, "g"),
                "Proteínas" to fmt(nutriments.proteins100g, "g"),
                "Gorduras" to fmt(nutriments.fat100g, "g"),
                "Gordura saturada" to fmt(nutriments.saturatedFat100g, "g"),
                "Fibras" to fmt(nutriments.fiber100g, "g"),
                "Sódio" to fmt(nutriments.sodium100g, "g"),
                "Sal" to fmt(nutriments.salt100g, "g"),
            )

            rows.forEach { (label, value) ->
                if (value != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

/**
 * Card de um produto alternativo na lista de recomendações.
 * Exibe imagem, nome, marca e badge do Nutri-Score com cor.
 */
@Composable
private fun AlternativeCard(product: Product, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            product.imageUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = product.productName,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.productName ?: "Sem nome",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = product.brands ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // Badge do Nutri-Score colorido
            product.nutriscoreGrade?.let { grade ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = nutriscoreColor(grade).copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = grade.uppercase(),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Black,
                        color = nutriscoreColor(grade)
                    )
                }
            }
        }
    }
}
