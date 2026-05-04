package com.trading.app.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest

data class CurrencyInfo(
    val code: String,
    val name: String,
    val isFavorite: Boolean = false,
    val isCrypto: Boolean = false,
    val flagUrl: String? = null
)

@Composable
fun CurrencySelectionModal(
    currentSymbol: String,
    selectedCurrency: String,
    onCurrencySelect: (String) -> Unit,
    onClose: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    
    val cryptos = remember {
        listOf(
            CurrencyInfo("BTC", "Bitcoin", isCrypto = true, flagUrl = "https://assets.coingecko.com/coins/images/1/large/bitcoin.png"),
            CurrencyInfo("ETH", "Ethereum", isCrypto = true, flagUrl = "https://assets.coingecko.com/coins/images/279/large/ethereum.png"),
            CurrencyInfo("USDT", "Tether", isCrypto = true, flagUrl = "https://assets.coingecko.com/coins/images/325/large/tether.png"),
            CurrencyInfo("BNB", "BNB", isCrypto = true, flagUrl = "https://assets.coingecko.com/coins/images/825/large/bnb-icon2_2x.png"),
            CurrencyInfo("SOL", "Solana", isCrypto = true, flagUrl = "https://assets.coingecko.com/coins/images/4128/large/solana.png"),
            CurrencyInfo("XRP", "XRP", isCrypto = true, flagUrl = "https://assets.coingecko.com/coins/images/44/large/xrp-symbol-white-128.png"),
            CurrencyInfo("USDC", "USDC", isCrypto = true, flagUrl = "https://assets.coingecko.com/coins/images/6319/large/USD_Coin_icon.png"),
            CurrencyInfo("ADA", "Cardano", isFavorite = true, isCrypto = true, flagUrl = "https://assets.coingecko.com/coins/images/975/large/cardano.png"),
            CurrencyInfo("AVAX", "Avalanche", isCrypto = true, flagUrl = "https://assets.coingecko.com/coins/images/12559/large/Avalanche_Circle_RedWhite_Trans.png"),
            CurrencyInfo("DOGE", "Dogecoin", isCrypto = true, flagUrl = "https://assets.coingecko.com/coins/images/5/large/dogecoin.png")
        )
    }

    val fiats = remember {
        listOf(
            CurrencyInfo("USD", "US Dollar", flagUrl = "https://flagcdn.com/w160/us.png"),
            CurrencyInfo("EUR", "Euro", flagUrl = "https://flagcdn.com/w160/eu.png"),
            CurrencyInfo("JPY", "Japanese Yen", flagUrl = "https://flagcdn.com/w160/jp.png"),
            CurrencyInfo("GBP", "British Pound", flagUrl = "https://flagcdn.com/w160/gb.png"),
            CurrencyInfo("AUD", "Australian Dollar", flagUrl = "https://flagcdn.com/w160/au.png"),
            CurrencyInfo("CAD", "Canadian Dollar", flagUrl = "https://flagcdn.com/w160/ca.png"),
            CurrencyInfo("CHF", "Swiss Franc", flagUrl = "https://flagcdn.com/w160/ch.png"),
            CurrencyInfo("CNY", "Chinese Yuan", flagUrl = "https://flagcdn.com/w160/cn.png"),
            CurrencyInfo("HKD", "Hong Kong Dollar", flagUrl = "https://flagcdn.com/w160/hk.png"),
            CurrencyInfo("NZD", "New Zealand Dollar", flagUrl = "https://flagcdn.com/w160/nz.png"),
            CurrencyInfo("SEK", "Swedish Krona", flagUrl = "https://flagcdn.com/w160/se.png"),
            CurrencyInfo("KRW", "South Korean Won", flagUrl = "https://flagcdn.com/w160/kr.png"),
            CurrencyInfo("SGD", "Singapore Dollar", flagUrl = "https://flagcdn.com/w160/sg.png"),
            CurrencyInfo("NOK", "Norwegian Krone", flagUrl = "https://flagcdn.com/w160/no.png"),
            CurrencyInfo("MXN", "Mexican Peso", flagUrl = "https://flagcdn.com/w160/mx.png"),
            CurrencyInfo("INR", "Indian Rupee", flagUrl = "https://flagcdn.com/w160/in.png"),
            CurrencyInfo("RUB", "Russian Ruble", flagUrl = "https://flagcdn.com/w160/ru.png"),
            CurrencyInfo("ZAR", "South African Rand", flagUrl = "https://flagcdn.com/w160/za.png"),
            CurrencyInfo("TRY", "Turkish Lira", flagUrl = "https://flagcdn.com/w160/tr.png"),
            CurrencyInfo("BRL", "Brazilian Real", flagUrl = "https://flagcdn.com/w160/br.png"),
            CurrencyInfo("TWD", "Taiwan New Dollar", flagUrl = "https://flagcdn.com/w160/tw.png"),
            CurrencyInfo("DKK", "Danish Krone", flagUrl = "https://flagcdn.com/w160/dk.png"),
            CurrencyInfo("PLN", "Polish Zloty", flagUrl = "https://flagcdn.com/w160/pl.png"),
            CurrencyInfo("THB", "Thai Baht", flagUrl = "https://flagcdn.com/w160/th.png"),
            CurrencyInfo("IDR", "Indonesian Rupiah", flagUrl = "https://flagcdn.com/w160/id.png"),
            CurrencyInfo("HUF", "Hungarian Forint", flagUrl = "https://flagcdn.com/w160/hu.png"),
            CurrencyInfo("CZK", "Czech Koruna", flagUrl = "https://flagcdn.com/w160/cz.png"),
            CurrencyInfo("ILS", "Israeli New Shekel", flagUrl = "https://flagcdn.com/w160/il.png"),
            CurrencyInfo("CLP", "Chilean Peso", flagUrl = "https://flagcdn.com/w160/cl.png"),
            CurrencyInfo("PHP", "Philippine Peso", flagUrl = "https://flagcdn.com/w160/ph.png"),
            CurrencyInfo("NGN", "Nigerian Naira", flagUrl = "https://flagcdn.com/w160/ng.png")
        )
    }

    val allCurrencies = (cryptos + fiats).sortedBy { it.code }
    val filteredCurrencies = if (searchQuery.isEmpty()) {
        allCurrencies
    } else {
        allCurrencies.filter { 
            it.code.contains(searchQuery, ignoreCase = true) || 
            it.name.contains(searchQuery, ignoreCase = true) 
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header with Search
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                    
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Search", color = Color(0xFF787B86), fontSize = 18.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )
                }

                // List
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    if (searchQuery.isEmpty()) {
                        // Default Currency Section
                        item {
                            SectionHeader("DEFAULT CURRENCY FOR $currentSymbol")
                        }
                        
                        val defaultCurrency = fiats.find { it.code == "USD" } ?: fiats.first()
                        item {
                            CurrencyItem(
                                currency = defaultCurrency,
                                isSelected = true,
                                onSelect = {
                                    onCurrencySelect(defaultCurrency.code)
                                    onClose()
                                }
                            )
                        }

                        // Favorites Section
                        item {
                            SectionHeader("FAVORITES")
                        }
                        
                        val favorites = allCurrencies.filter { it.isFavorite }
                        items(favorites) { currency ->
                            CurrencyItem(
                                currency = currency,
                                isSelected = currency.code == selectedCurrency,
                                onSelect = {
                                    onCurrencySelect(currency.code)
                                    onClose()
                                }
                            )
                        }

                        // All Currencies Section
                        item {
                            SectionHeader("ALL CURRENCIES")
                        }
                    }

                    items(filteredCurrencies) { currency ->
                        CurrencyItem(
                            currency = currency,
                            isSelected = currency.code == selectedCurrency,
                            onSelect = {
                                onCurrencySelect(currency.code)
                                onClose()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrencyItem(
    currency: CurrencyInfo,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelect() }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flag/Icon from URL
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(currency.flagUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "${currency.name} flag",
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        currency.code,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    if (currency.isFavorite) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Star,
                            null,
                            tint = Color(0xFFF7931A),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Text(
                    currency.name,
                    color = Color(0xFF787B86),
                    fontSize = 13.sp
                )
            }
            
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Divider(
            modifier = Modifier.padding(start = 16.dp),
            color = Color(0xFF2A2E39),
            thickness = 0.5.dp
        )
    }
}
