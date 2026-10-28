package com.example.ui.screens

import android.content.Context
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.BankType
import com.example.ui.theme.BandecGreen
import com.example.ui.theme.BpaBlue
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.GoldAccent

data class OperationPreset(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val requireTarget: Boolean,
    val targetPlaceholder: String,
    val requireAmount: Boolean
)

@Composable
fun OperationsScreen(
    bankKey: String,
    onLaunchUssd: (Context, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val operations = listOf(
        OperationPreset(
            id = "BALANCE",
            title = "Consultar Saldo",
            description = "Consulta tu saldo actual por USSD (*444*40#)",
            icon = Icons.Default.Smartphone,
            color = CobaltPrimary,
            requireTarget = false,
            targetPlaceholder = "",
            requireAmount = false
        ),
        OperationPreset(
            id = "TRANSFER",
            title = "Transferir a Tarjeta",
            description = "Envía dinero a tarjetas BPA, BANDEC o BANMET",
            icon = Icons.AutoMirrored.Filled.Send,
            color = BandecGreen,
            requireTarget = true,
            targetPlaceholder = "Número de tarjeta destino (16 dígitos)",
            requireAmount = true
        ),
        OperationPreset(
            id = "ELECTRICITY",
            title = "Pago de Electricidad",
            description = "Paga tu factura eléctrica con tu ID de cobro fijo",
            icon = Icons.Default.ElectricBolt,
            color = GoldAccent,
            requireTarget = true,
            targetPlaceholder = "ID de Cobro Fijo (11 dígitos)",
            requireAmount = false
        ),
        OperationPreset(
            id = "TELEPHONE",
            title = "Factura Telefónica",
            description = "Paga el teléfono fijo o móvil ETECSA",
            icon = Icons.Default.Phone,
            color = Color(0xFF3B82F6),
            requireTarget = true,
            targetPlaceholder = "Número de teléfono o cliente ETECSA",
            requireAmount = true
        ),
        OperationPreset(
            id = "GAS",
            title = "Gas Manufacturado",
            description = "Paga el recibo del gas manufacturado",
            icon = Icons.Default.LocalGasStation,
            color = Color(0xFFEC4899),
            requireTarget = true,
            targetPlaceholder = "Número de cuenta de Gas",
            requireAmount = false
        ),
        OperationPreset(
            id = "RECHARGE",
            title = "Recarga Nauta / Celular",
            description = "Recarga saldo a móvil o cuenta Nauta",
            icon = Icons.Default.PhoneAndroid,
            color = Color(0xFF8B5CF6),
            requireTarget = true,
            targetPlaceholder = "Número de teléfono (8 dígitos)",
            requireAmount = true
        ),
        OperationPreset(
            id = "ONAT",
            title = "Pago ONAT",
            description = "Pago de tributos e impuestos ONAT",
            icon = Icons.Default.ReceiptLong,
            color = Color(0xFF14B8A6),
            requireTarget = true,
            targetPlaceholder = "NIT (Número de Identificación Tributaria)",
            requireAmount = true
        )
    )

    var selectedOp by remember { mutableStateOf(operations[1]) } // Transfer selected by default
    var targetInput by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }

    val generatedUssd = remember(selectedOp, targetInput, amountInput, bankKey) {
        when (selectedOp.id) {
            "BALANCE" -> "*444*40#"
            "TRANSFER" -> "*444*45*$bankKey*${targetInput.ifEmpty { "DESTINO" }}*${amountInput.ifEmpty { "MONTO" }}#"
            "ELECTRICITY" -> "*444*52*$bankKey*${targetInput.ifEmpty { "ID_COBRO" }}#"
            "TELEPHONE" -> "*444*53*$bankKey*${targetInput.ifEmpty { "TELEFONO" }}*${amountInput.ifEmpty { "MONTO" }}#"
            "GAS" -> "*444*55*$bankKey*${targetInput.ifEmpty { "CUENTA_GAS" }}#"
            "RECHARGE" -> "*444*70*$bankKey*${targetInput.ifEmpty { "TELEFONO" }}*${amountInput.ifEmpty { "MONTO" }}#"
            "ONAT" -> "*444*60*$bankKey*${targetInput.ifEmpty { "NIT" }}*${amountInput.ifEmpty { "MONTO" }}#"
            else -> "*444#"
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // Title Header
        item {
            Text(
                text = "Generador y Ejecutor de Operaciones USSD",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Selecciona la operación bancaria y genera el código oficial de Transfermóvil.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Operation Selection Grid
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Tipo de Operación Bancaria:",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        operations.forEach { op ->
                            val isSelected = selectedOp.id == op.id
                            Card(
                                onClick = {
                                    selectedOp = op
                                    targetInput = ""
                                    amountInput = ""
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) op.color.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("op_select_${op.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isSelected) op.color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = op.icon,
                                                contentDescription = null,
                                                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = op.title,
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        )
                                        Text(
                                            text = op.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Operation Dynamic Form
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Datos de la Operación (${selectedOp.title}):",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (selectedOp.requireTarget) {
                        OutlinedTextField(
                            value = targetInput,
                            onValueChange = { targetInput = it },
                            label = { Text(selectedOp.targetPlaceholder) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("op_target_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (selectedOp.requireAmount) {
                        OutlinedTextField(
                            value = amountInput,
                            onValueChange = { amountInput = it },
                            label = { Text("Monto ($ CUP/MLC)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("op_amount_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Clave de Acceso Note
                    Text(
                        text = "Clave de Acceso Bancaria configurada: ••••• (Edítala en Ajustes)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // USSD Code Generated Preview & Call Button
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "CÓDIGO USSD GENERADO:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = generatedUssd,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = CobaltPrimary
                            )
                        )

                        IconButton(
                            onClick = { clipboardManager.setText(AnnotatedString(generatedUssd)) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copiar USSD",
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { onLaunchUssd(context, generatedUssd) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("execute_ussd_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BandecGreen)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Llamar / Ejecutar USSD",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }
            }
        }
    }
}
