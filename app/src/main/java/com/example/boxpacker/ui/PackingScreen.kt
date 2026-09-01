package com.example.boxpacker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.boxpacker.packing.BoxType
import com.example.boxpacker.packing.MultiPacker
import com.example.boxpacker.packing.PlacedBox

private data class BoxTypeInput(
    val id: Int,
    var name: String = "",
    var w: String = "",
    var d: String = "",
    var h: String = "",
    var unlimited: Boolean = true,
    var qty: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackingScreen() {
    var cellW by remember { mutableStateOf("1200") }
    var cellD by remember { mutableStateOf("800") }
    var cellH by remember { mutableStateOf("1000") }

    var nextId by remember { mutableStateOf(1) }
    val boxTypeInputs = remember {
        mutableStateListOf(
            BoxTypeInput(id = 0, name = "Коробка 1", w = "300", d = "200", h = "150", unlimited = true)
        )
    }

    var result by remember { mutableStateOf<List<PlacedBox>?>(null) }
    var cellDims by remember { mutableStateOf<Triple<Int, Int, Int>?>(null) }
    var typesUsed by remember { mutableStateOf<List<BoxType>>(emptyList()) }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Типы коробок", fontWeight = FontWeight.Bold)
                TextButton(onClick = {
                    boxTypeInputs.add(
                        BoxTypeInput(id = nextId, name = "Коробка ${nextId + 1}")
                    )
                    nextId++
                }) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Добавить коробку")
                }
            }

            boxTypeInputs.forEachIndexed { index, item ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedTextField(
                            value = item.name,
                            onValueChange = { item.name = it },
                            label = { Text("Название") },
                            modifier = Modifier.weight(1f)
                        )
                        if (boxTypeInputs.size > 1) {
                            IconButton(onClick = { boxTypeInputs.removeAt(index) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Удалить")
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField("Ширина", item.w) { item.w = it }
                        NumberField("Глубина", item.d) { item.d = it }
                        NumberField("Высота", item.h) { item.h = it }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Неограниченно")
                        Switch(
                            checked = item.unlimited,
                            onCheckedChange = { item.unlimited = it }
                        )
                        if (!item.unlimited) {
                            Spacer(Modifier.width(8.dp))
                            OutlinedTextField(
                                value = item.qty,
                                onValueChange = { item.qty = it.filter { c -> c.isDigit() } },
                                label = { Text("Кол-во, шт") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                error = null
                try {
                    val W = cellW.toInt(); val D = cellD.toInt(); val H = cellH.toInt()
                    require(W > 0 && D > 0 && H > 0) { "Размеры ячейки должны быть положительными" }

                    val types = boxTypeInputs.mapIndexed { idx, item ->
                        val bw = item.w.toInt(); val bd = item.d.toInt(); val bh = item.h.toInt()
                        require(bw > 0 && bd > 0 && bh > 0) { "Проверь размеры коробки \"${item.name}\"" }
                        val qty = if (item.unlimited) null else {
                            val q = item.qty.toIntOrNull()
                            require(q != null && q > 0) { "Укажи количество для \"${item.name}\"" }
                            q
                        }
                        BoxType(id = idx, name = item.name.ifBlank { "Коробка ${idx + 1}" }, w = bw, d = bd, h = bh, quantity = qty)
                    }

                    val packer = MultiPacker(types)
                    result = packer.pack(W, D, H)
                    cellDims = Triple(W, D, H)
                    typesUsed = types
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
                val (W, D, H) = cellDims!!
                Spacer(Modifier.height(16.dp))
                Text(
                    "Влезает коробок всего: ${boxes.size}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(4.dp))
                typesUsed.forEach { t ->
                    val count = boxes.count { it.typeId == t.id }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(colorForType(t.id), RoundedCornerShape(3.dp))
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("${t.name} (${t.w}×${t.d}×${t.h}): $count шт")
                    }
                }

                Spacer(Modifier.height(8.dp))
                ProjectionCanvas(W, D, H, boxes, Projection.TOP, "Вид сверху (X-Y)")
                ProjectionCanvas(W, D, H, boxes, Projection.FRONT, "Вид спереди (X-Z)")
                ProjectionCanvas(W, D, H, boxes, Projection.SIDE, "Вид сбоку (Y-Z)")
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
