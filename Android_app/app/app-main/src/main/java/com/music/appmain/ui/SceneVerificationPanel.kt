package com.music.appmain.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class SceneScenario(
    val id: String,
    val name: String,
    val description: String,
    val color: Color
)

@Composable
fun SceneVerificationPanel(
    onScenarioTrigger: (SceneScenario) -> Unit,
    modifier: Modifier = Modifier
) {
    val scenarios = listOf(
        SceneScenario("rainy_emo", "雨天emo", "雨天+低落情绪", Color(0xFF5C6BC0)),
        SceneScenario("children_board", "小朋友上车", "检测到儿童乘客", Color(0xFFFF9800)),
        SceneScenario("rain_stops", "雨过天晴", "天气转晴+愉悦", Color(0xFF4CAF50)),
        SceneScenario("noisy_car", "车内吵闹", "多人+高音量", Color(0xFFE91E63)),
        SceneScenario("user_pop", "我喜欢流行乐", "用户偏好输入", Color(0xFF9C27B0)),
        SceneScenario("beach_vacation", "海边度假", "海边场景+放松", Color(0xFF00BCD4)),
        SceneScenario("romantic_date", "浪漫约会", "夜晚+浪漫氛围", Color(0xFFE91E63)),
        SceneScenario("fatigue_alert", "疲劳提醒", "检测到疲劳", Color(0xFFF44336))
    )
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "场景验证",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier.verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                scenarios.chunked(4).forEach { rowScenarios ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowScenarios.forEach { scenario ->
                            ScenarioButton(
                                scenario = scenario,
                                onClick = { onScenarioTrigger(scenario) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(4 - rowScenarios.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScenarioButton(
    scenario: SceneScenario,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minHeight = 56.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = scenario.color
        )
    ) {
        Column(
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Text(
                text = scenario.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = scenario.description,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }
    }
}
