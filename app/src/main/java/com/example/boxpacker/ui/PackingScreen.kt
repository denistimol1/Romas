package com.example.boxpacker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.boxpacker.packing.Dim
import com.example.boxpacker.packing.PlacedBox
import com.example.boxpacker.packing.Packer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackingScreen() {
    var cellW by remember { mutableStateOf("1200") }
    var cellD by remember { mutableStateOf("800") }
    var cellH by remember { mutableStateOf("1000") }
    var boxW by remember { mutableStateOf("300") }
    var boxD by remember { mutableStateOf("200") }
    var boxH by remember { mutableStateOf("150") }

    var result by remember { mutableStateOf<List<PlacedBox>?>(null) }
    var cell by remember { mutableStateOf<Dim?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(topBar = { TopAppBar(title = { Text("Упаковка коробок в ячейку") }) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Размеры ячейки, мм", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("Ширина (X)", cellW) { cellW = it }
                NumberField("Глубина (Y)", cellD) { cellD = it }
                NumberField("Высота (Z)", cellH) { cellH = it }
            }

            Spacer(Modifier.height(16.dp))
            Text("Размеры коробки, мм", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("Ширина", boxW) { boxW = it }
                NumberField("Глубина", boxD) { boxD = it }
                NumberField("Высота", boxH) { boxH = it }
            }

            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                error = null
                try {
                    val W = cellW.toInt(); val D = cellD.toInt(); val H = cellH.toInt()
                    val bw = boxW.toInt(); val bd = boxD.toInt(); val bh = boxH.toInt()
                    require(W > 0 && D > 0 && H > 0 && bw > 0 && bd > 0 && bh > 0) { "Все размеры должны быть положительными" }
                    val packer = Packer(Dim(bw, bd, bh))
                    result = packer.pack(W, D, H)
                    cell = Dim(W, D, H)
                } catch (e: Exception) {
                    error = "Проверьте введённые числа: ${e.message}"
                    result = null
                }
            }) {
                Text("Рассчитать укладку")
            }

            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            result?.let { boxes ->
                val c = cell!!
                Spacer(Modifier.height(16.dp))
                Text(
                    "Влезает коробок: ${boxes.size}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))

                ProjectionCanvas(c.w, c.d, c.h, boxes, Projection.TOP, "Вид сверху (X-Y)")
                ProjectionCanvas(c.w, c.d, c.h, boxes, Projection.FRONT, "Вид спереди (X-Z)")
                ProjectionCanvas(c.w, c.d, c.h, boxes, Projection.SIDE, "Вид сбоку (Y-Z)")

                Spacer(Modifier.height(16.dp))
                Text(
                    "Каждый цвет — одна коробка. Координаты угла коробки (мм от нижнего левого переднего угла ячейки):",
                    style = MaterialTheme.typography.bodySmall
                )
                boxes.forEachIndexed { i, b ->
                    Text(
                        "#${i + 1}: (x=${b.x}, y=${b.y}, z=${b.z}), размеры ${b.w}×${b.d}×${b.h}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.NumberField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter { c -> c.isDigit() }) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.weight(1f)
    )
}
