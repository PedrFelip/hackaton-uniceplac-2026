package com.nutriscan.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.material.icons.filled.ShoppingCart
import com.nutriscan.app.ui.screens.CartScreen
import com.nutriscan.app.ui.screens.HistoryScreen
import com.nutriscan.app.ui.screens.ProductDetailScreen
import com.nutriscan.app.ui.screens.ScannerScreen
import com.nutriscan.app.ui.screens.SearchScreen
import com.nutriscan.app.ui.theme.NutriScanTheme

/**
 * Rotas de navegação do app.
 * Cada tela tem uma rota única, label e ícone associado.
 * ProductDetail usa rota dinâmica com o barcode como parâmetro.
 */
sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Home : Screen("home", "Histórico", Icons.AutoMirrored.Filled.List)
    data object Search : Screen("search", "Pesquisar", Icons.Filled.Search)
    data object Scan : Screen("scan", "Escanear", Icons.Filled.QrCodeScanner)
    data object Cart : Screen("cart", "Carrinho", Icons.Filled.ShoppingCart)
    data object ProductDetail : Screen("product/{barcode}", "Detalhe", Icons.Filled.Search) {
        fun createRoute(barcode: String) = "product/$barcode"
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NutriScanTheme {
                NutriScanApp()
            }
        }
    }
}

/**
 * Composable raiz do app.
 * Monta a navegação com bottom bar (Histórico, Pesquisar, Escanear)
 * e gerencia as rotas incluindo a tela de detalhe do produto.
 */
@Composable
fun NutriScanApp() {
    val navController = rememberNavController()
    val screens = listOf(Screen.Home, Screen.Search, Screen.Scan, Screen.Cart)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            // Esconde a bottom bar na tela de detalhe do produto
            if (currentDestination?.route != Screen.ProductDetail.route) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    screens.forEach { screen ->
                        NavigationBarItem(
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HistoryScreen(
                    onProductClick = { barcode ->
                        navController.navigate(Screen.ProductDetail.createRoute(barcode))
                    }
                )
            }
            // Tela de busca: ao clicar num produto, navega para detalhe pelo barcode
            composable(Screen.Search.route) {
                SearchScreen(
                    onProductClick = { product ->
                        product.code?.let { code ->
                            navController.navigate(Screen.ProductDetail.createRoute(code))
                        }
                    }
                )
            }
            // Scanner: ao detectar barcode, navega direto para detalhe
            composable(Screen.Scan.route) {
                ScannerScreen(
                    onBarcodeScanned = { barcode ->
                        navController.navigate(Screen.ProductDetail.createRoute(barcode))
                    }
                )
            }
            composable(Screen.Cart.route) {
                CartScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            // Detalhe do produto: recebe barcode como argumento da rota
            composable(
                route = Screen.ProductDetail.route,
                arguments = listOf(navArgument("barcode") { type = NavType.StringType })
            ) { backStackEntry ->
                val barcode = backStackEntry.arguments?.getString("barcode") ?: ""
                ProductDetailScreen(
                    barcode = barcode,
                    onBackClick = { navController.popBackStack() },
                    // Ao clicar numa alternativa, navega para o detalhe dela (empilha)
                    onProductClick = { code ->
                        navController.navigate(Screen.ProductDetail.createRoute(code))
                    }
                )
            }
        }
    }
}
