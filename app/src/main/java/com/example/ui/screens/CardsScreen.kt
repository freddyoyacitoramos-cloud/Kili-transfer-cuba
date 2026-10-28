package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.BankCard
import com.example.data.model.BankType
import com.example.data.model.Currency
import com.example.ui.theme.BandecGreen
import com.example.ui.theme.BpaBlue
import com.example.ui.theme.CobaltPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardsScreen(
    cards: List<BankCard>,
    onAddCard: (BankType, String, Currency, Double) -> Unit,
    onUpdateBalance: (String, Double) -> Unit,
    onDeleteCard: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var cardToEdit by remember { mutableStateOf<BankCard?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Mis Tarjetas Bancarias",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Gestiona tus tarjetas BPA, BANDEC, BANMET y MLC",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { showAddDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("add_card_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Añadir")
                    }
                }
            }

            items(cards) { card ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_manage_${card.cardLast4}"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = card.bankType.getColor(),
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.CreditCard,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = card.bankType.displayName,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "Terminada en ${card.cardLast4} • ${card.currency.code}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row {
                                IconButton(onClick = { cardToEdit = card }) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Editar Saldo",
                                        tint = CobaltPrimary
                                    )
                                }
                                IconButton(onClick = { onDeleteCard(card.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Eliminar Tarjeta",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SALDO FIJADO:",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            )

                            Text(
                                text = "$ ${formatMoney(card.balance)} ${card.currency.code}",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = BandecGreen
                                )
                            )
                        }
                    }
                }
            }
        }

        // Add Card Dialog
        if (showAddDialog) {
            var selectedBank by remember { mutableStateOf(BankType.BPA) }
            var last4 by remember { mutableStateOf("") }
            var selectedCurrency by remember { mutableStateOf(Currency.CUP) }
            var balanceInput by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Registrar Nueva Tarjeta Bancaria") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Últimos 4 dígitos:", style = MaterialTheme.typography.labelMedium)
                        OutlinedTextField(
                            value = last4,
                            onValueChange = { if (it.length <= 4) last4 = it },
                            placeholder = { Text("ej. 9224") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().testTag("add_card_last4_input")
                        )

                        Text("Saldo Inicial:", style = MaterialTheme.typography.labelMedium)
                        OutlinedTextField(
                            value = balanceInput,
                            onValueChange = { balanceInput = it },
                            placeholder = { Text("ej. 15000.00") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth().testTag("add_card_balance_input")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (last4.isNotEmpty()) {
                                onAddCard(
                                    selectedBank,
                                    last4,
                                    selectedCurrency,
                                    balanceInput.toDoubleOrNull() ?: 0.0
                                )
                                showAddDialog = false
                            }
                        },
                        modifier = Modifier.testTag("confirm_add_card_btn")
                    ) {
                        Text("Guardar Tarjeta")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        // Edit Balance Dialog
        cardToEdit?.let { card ->
            var newBalanceInput by remember { mutableStateOf(card.balance.toString()) }

            AlertDialog(
                onDismissRequest = { cardToEdit = null },
                title = { Text("Ajustar Saldo Manualmente") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Tarjeta: ${card.bankType.displayName} (•••• ${card.cardLast4})")
                        OutlinedTextField(
                            value = newBalanceInput,
                            onValueChange = { newBalanceInput = it },
                            label = { Text("Nuevo Saldo Restante") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("edit_balance_input")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val newBal = newBalanceInput.toDoubleOrNull()
                            if (newBal != null) {
                                onUpdateBalance(card.cardLast4, newBal)
                                cardToEdit = null
                            }
                        },
                        modifier = Modifier.testTag("confirm_edit_balance_btn")
                    ) {
                        Text("Fijar Saldo")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { cardToEdit = null }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}
