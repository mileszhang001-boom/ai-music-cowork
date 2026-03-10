package com.music.appmain

import com.music.core.api.models.EffectCommands
import com.music.core.api.models.SceneDescriptor
import com.music.core.api.models.StandardizedSignals

class AnnouncementBuilder {

    fun build(
        signals: StandardizedSignals?,
        scene: SceneDescriptor?,
        effects: EffectCommands?
    ): String? {
        if (scene == null) return null

        val context = buildContext(signals, scene)
        val music = buildMusic(effects, scene)

        if (context == null && music == null) return null

        return listOfNotNull(context, music).joinToString("，")
    }

    private fun buildContext(signals: StandardizedSignals?, scene: SceneDescriptor): String? {
        val narrative = scene.scene_narrative
        if (!narrative.isNullOrBlank()) return narrative

        val env = buildEnvSnippet(signals)
        val mood = buildMoodSnippet(signals, scene)

        return listOfNotNull(env, mood).takeIf { it.isNotEmpty() }?.joinToString("，")
    }

    private fun buildEnvSnippet(signals: StandardizedSignals?): String? {
        val timeOfDay = signals?.signals?.environment?.time_of_day
        val weather = signals?.signals?.environment?.weather

        val timeText = when {
            timeOfDay == null -> null
            timeOfDay < 6.0 -> "夜深了"
            timeOfDay < 9.0 -> "清晨"
            timeOfDay < 14.0 -> null
            timeOfDay < 17.0 -> null
            timeOfDay < 19.0 -> "傍晚"
            timeOfDay < 22.0 -> "晚上"
            else -> "深夜"
        }
        val weatherText = when {
            weather == null -> null
            weather.contains("rain", true) || weather.contains("雨") -> "下着雨"
            weather.contains("snow", true) || weather.contains("雪") -> "飘着雪"
            weather.contains("fog", true) || weather.contains("雾") -> "起了雾"
            else -> null
        }

        return when {
            timeText != null && weatherText != null -> "${timeText}${weatherText}"
            weatherText != null -> "外面$weatherText"
            timeText != null -> timeText
            else -> null
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun buildMoodSnippet(signals: StandardizedSignals?, scene: SceneDescriptor): String? {
        val mood = signals?.signals?.internal_camera?.mood
        val moodText = when {
            mood == null -> null
            mood.contains("tired", true) || mood.contains("fatigue", true) -> "你有些疲惫"
            mood.contains("sad", true) || mood.contains("低落") -> "心情有些低落"
            mood.contains("happy", true) || mood.contains("开心") -> "心情不错"
            mood.contains("angry", true) || mood.contains("烦躁") -> "有些烦躁"
            else -> null
        }
        if (moodText != null) return moodText

        val passengers = signals?.signals?.internal_camera?.passengers
        val children = passengers?.children ?: 0
        val adults = (passengers?.adults ?: 0) + (signals?.signals?.vehicle?.passenger_count ?: 0)
        return when {
            children > 0 -> "车里有小朋友"
            adults > 2 -> "车里挺热闹"
            else -> null
        }
    }

    private fun buildMusic(effects: EffectCommands?, scene: SceneDescriptor): String? {
        val tracks = effects?.commands?.content?.playlist
        val genres = scene.hints.music?.genres

        if (!tracks.isNullOrEmpty()) {
            val first = tracks.first()
            val genreName = genres?.firstOrNull()?.let { mapGenre(it) }
            return if (genreName != null) {
                "给你挑了首${genreName}的歌，${first.artist}的「${first.title}」"
            } else {
                "给你放一首${first.artist}的「${first.title}」"
            }
        }

        if (!genres.isNullOrEmpty()) {
            return "给你选了些${mapGenre(genres.first())}的歌"
        }

        return null
    }

    private fun mapGenre(genre: String): String = when (genre.lowercase()) {
        "jazz" -> "爵士"
        "pop" -> "流行"
        "rock" -> "摇滚"
        "electronic", "edm" -> "电子"
        "classical" -> "古典"
        "r&b", "rnb" -> "R&B"
        "hip-hop", "hiphop", "rap" -> "说唱"
        "folk" -> "民谣"
        "ambient" -> "氛围"
        "lo-fi", "lofi" -> "Lo-Fi"
        "blues" -> "蓝调"
        "country" -> "乡村"
        "reggae" -> "雷鬼"
        "soul" -> "灵魂乐"
        "indie" -> "独立"
        "metal" -> "金属"
        "punk" -> "朋克"
        "latin" -> "拉丁"
        "children", "kids" -> "儿歌"
        "synthwave" -> "合成器浪潮"
        "acoustic" -> "原声"
        "chill" -> "放松"
        "bossa_nova", "bossa nova" -> "波萨诺瓦"
        else -> genre
    }
}
