package com.music.localmusic.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import com.music.localmusic.models.Track

object CoverGenerator {
    
    private var defaultConfig = Config()
    private var currentPrimaryColor: Int = Color.parseColor("#3D5AFE")
    private var currentSecondaryColor: Int = Color.parseColor("#1A237E")
    
    fun generate(track: Track): Bitmap {
        return generate(track, defaultConfig, currentPrimaryColor, currentSecondaryColor)
    }
    
    fun generate(track: Track, config: Config, primaryColor: Int, secondaryColor: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(config.width, config.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        val gradient = LinearGradient(
            0f, 0f, 
            config.width.toFloat(), config.height.toFloat(),
            primaryColor,
            secondaryColor,
            Shader.TileMode.CLAMP
        )
        
        val backgroundPaint = Paint().apply {
            shader = gradient
        }
        
        canvas.drawRect(0f, 0f, config.width.toFloat(), config.height.toFloat(), backgroundPaint)
        
        val displayText = track.title.firstOrNull()?.toString() ?: "♪"
        
        val paint = Paint().apply {
            this.color = config.textColor
            this.textSize = config.textSize
            this.textAlign = Paint.Align.CENTER
            this.isAntiAlias = true
            this.isFakeBoldText = true
            this.setShadowLayer(10f, 2f, 2f, Color.argb(100, 0, 0, 0))
        }
        
        val x = config.width / 2f
        val y = config.height / 2f - (paint.descent() + paint.ascent()) / 2
        
        canvas.drawText(displayText, x, y, paint)
        
        return bitmap
    }
    
    fun setSceneColors(primaryColor: String?, secondaryColor: String?) {
        currentPrimaryColor = try {
            Color.parseColor(primaryColor ?: "#3D5AFE")
        } catch (e: Exception) {
            Color.parseColor("#3D5AFE")
        }
        
        currentSecondaryColor = try {
            Color.parseColor(secondaryColor ?: "#1A237E")
        } catch (e: Exception) {
            Color.parseColor("#1A237E")
        }
    }
    
    fun setDefaultConfig(config: Config) {
        defaultConfig = config
    }
    
    fun getDefaultConfig(): Config = defaultConfig
    
    data class Config(
        val width: Int = 512,
        val height: Int = 512,
        val textSize: Float = 200f,
        val textColor: Int = Color.WHITE
    )
}
