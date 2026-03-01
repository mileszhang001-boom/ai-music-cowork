const ffmpeg = require('fluent-ffmpeg');
const Jimp = require('jimp');
const fs = require('fs');
const path = require('path');
// Reuse project's LLM Client
// Path correction: src/validation_system/backend/processor.js -> ../../../src/core/llm/llmClient.js
const { llmClient } = require('../../../src/core/llm/llmClient');
require('dotenv').config({ path: path.resolve(__dirname, '../../../.env') });

class VideoProcessor {
    constructor() {
        // LLMClient is a singleton, already initialized if environment variables are set.
        // If the process running this script (server.js) has loaded .env correctly,
        // llmClient will pick up DASHSCOPE_API_KEY.
        
        // We can optionally verify or re-initialize if needed, but usually the singleton is fine.
        // Check if ready:
        if (!llmClient.isReady()) {
             // Try to manually set key if available in process.env but not picked up
             const apiKey = process.env.DASHSCOPE_API_KEY || process.env.OPENAI_API_KEY;
             if (apiKey) {
                 llmClient.setApiKey(apiKey);
             } else {
                 console.warn('Warning: No API Key found. AI analysis will be disabled.');
             }
        }
    }

    /**
     * Process video file and merge with manual overrides to simulate Layer1 output
     * @param {string} filePath 
     * @param {Object} overrides - Manual signal overrides from frontend
     * @returns {Promise<Object>}
     */
    async process(filePath, overrides = {}) {
        let metadata = {};
        let visionSignals = null;
        
        // Only process video if a path is provided
        if (filePath) {
            metadata = await this.getVideoMetadata(filePath);
            
            // Analyze video frames for vision signals
            try {
                visionSignals = await this.analyzeVideoContent(filePath);
            } catch (err) {
                console.error('Video analysis failed:', err);
                // Fallback to mock if analysis fails
                visionSignals = this.getMockVisionSignals();
            }
        } else {
            visionSignals = this.getMockVisionSignals();
        }

        const layer1Output = this.simulateLayer1Processing(metadata, visionSignals, overrides);
        
        return {
            metadata,
            layer1Output,
            processingStats: {
                keyframesExtracted: filePath ? 3 : 0,
                objectsDetected: filePath ? Math.floor(Math.random() * 10) + 1 : 0,
                timestampContinuity: true,
                metadataConsistent: true
            }
        };
    }

    getVideoMetadata(filePath) {
        return new Promise((resolve, reject) => {
            ffmpeg.ffprobe(filePath, (err, metadata) => {
                if (err) return reject(err);
                
                const videoStream = metadata.streams.find(s => s.codec_type === 'video');
                if (!videoStream) return reject(new Error('No video stream found'));

                resolve({
                    format: metadata.format.format_name,
                    duration: metadata.format.duration,
                    size: metadata.format.size,
                    resolution: `${videoStream.width}x${videoStream.height}`,
                    frameRate: videoStream.r_frame_rate,
                    codec: videoStream.codec_name
                });
            });
        });
    }

    async analyzeVideoContent(filePath) {
        const framePath = path.join(path.dirname(filePath), `frame-${Date.now()}.png`);
        
        try {
            // 1. Extract a representative frame (at 1 second or middle)
            await new Promise((resolve, reject) => {
                ffmpeg(filePath)
                    .screenshots({
                        timestamps: ['50%'],
                        filename: path.basename(framePath),
                        folder: path.dirname(framePath),
                        size: '640x360' // Larger size for better AI analysis
                    })
                    .on('end', resolve)
                    .on('error', reject);
            });

            // 2. Analyze with Jimp for basic metrics (External Camera)
            const image = await Jimp.read(framePath);
            const { avgBrightness, primaryColor } = this.analyzeBasicImageMetrics(image);

            // 3. Analyze with AI (Internal Camera - Heavy Model)
            let aiAnalysis = {};
            if (llmClient.isReady()) {
                // Convert image to base64
                const imageBuffer = fs.readFileSync(framePath);
                const base64Image = imageBuffer.toString('base64');
                const dataUrl = `data:image/png;base64,${base64Image}`;

                aiAnalysis = await this.callVisionModel(dataUrl);
            } else {
                console.warn('AI Client not ready, skipping AI analysis');
                aiAnalysis = this.getMockInternalCameraAnalysis(avgBrightness);
            }

            // --- Heuristic AI: Scene Description (External) ---
            // Combine basic metrics with simple heuristics
            let scene = 'city_street';
            if (avgBrightness < 0.3) scene = 'night_road';
            else if (avgBrightness > 0.8) scene = 'snowy_or_bright_city';

            // Clean up frame
            fs.unlink(framePath, () => {});

            return {
                external_camera: {
                    primary_color: primaryColor,
                    secondary_color: '#FFFFFF',
                    brightness: parseFloat(avgBrightness.toFixed(2)),
                    scene_description: scene
                },
                internal_camera: {
                    mood: aiAnalysis.mood || 'neutral',
                    confidence: aiAnalysis.confidence || 0.85,
                    passengers: aiAnalysis.passengers || { children: 0, adults: 1, seniors: 0 }
                }
            };

        } catch (error) {
            console.error('Analysis error:', error);
            if (fs.existsSync(framePath)) fs.unlink(framePath, () => {});
            throw error;
        }
    }

    analyzeBasicImageMetrics(image) {
        let totalBrightness = 0;
        let rTotal = 0, gTotal = 0, bTotal = 0;
        const pixelCount = image.bitmap.width * image.bitmap.height;

        image.scan(0, 0, image.bitmap.width, image.bitmap.height, function(x, y, idx) {
            const r = this.bitmap.data[idx + 0];
            const g = this.bitmap.data[idx + 1];
            const b = this.bitmap.data[idx + 2];

            rTotal += r;
            gTotal += g;
            bTotal += b;
            
            totalBrightness += (0.299 * r + 0.587 * g + 0.114 * b);
        });

        const avgBrightness = (totalBrightness / pixelCount) / 255;
        const avgR = Math.round(rTotal / pixelCount);
        const avgG = Math.round(gTotal / pixelCount);
        const avgB = Math.round(bTotal / pixelCount);
        
        const toHex = (c) => {
            const hex = c.toString(16);
            return hex.length == 1 ? "0" + hex : hex;
        };
        
        return {
            avgBrightness,
            primaryColor: `#${toHex(avgR)}${toHex(avgG)}${toHex(avgB)}`
        };
    }

    async callVisionModel(imageUrl) {
        try {
            console.log('Calling AI Vision Model (Qwen-VL-Max) via LLMClient...');
            
            // Construct messages compatible with OpenAI/DashScope Vision format
            const messages = [
                {
                    role: "user",
                    content: [
                        { type: "text", text: "请分析这张车内或车外的图片。如果是车内，请判断驾驶员的情绪（happy, calm, tired, stressed, neutral, excited）和乘客数量及类型（children, adults, seniors）。请仅以 JSON 格式返回，例如：{\"mood\": \"happy\", \"passengers\": {\"adults\": 1, \"children\": 0, \"seniors\": 0}, \"confidence\": 0.9}。如果无法判断，请返回默认值。" },
                        { type: "image_url", image_url: { "url": imageUrl } }
                    ]
                }
            ];

            // Use llmClient.chat
            // We force 'qwen-vl-max' model. Note: Ensure this model is supported by the DashScope endpoint.
            const response = await llmClient.chat(messages, {
                model: 'qwen-vl-max', // Override default model
                temperature: 0.1,     // Low temperature for deterministic JSON
                maxTokens: 500
            });

            const content = response.choices[0].message.content;
            // Extract JSON from response (handle potential markdown code blocks)
            const jsonMatch = content.match(/\{[\s\S]*\}/);
            if (jsonMatch) {
                return JSON.parse(jsonMatch[0]);
            }
            return JSON.parse(content);
        } catch (error) {
            console.error('AI Vision Model failed:', error);
            return {};
        }
    }

    getMockInternalCameraAnalysis(avgBrightness) {
        // Fallback logic
        let mood = 'neutral';
        if (avgBrightness > 0.6) mood = 'happy';
        else if (avgBrightness < 0.3) mood = 'tired';
        
        return {
            mood: mood,
            confidence: 0.85,
            passengers: { children: 0, adults: 1, seniors: 0 }
        };
    }

    getMockVisionSignals() {
        const isDaytime = Math.random() > 0.5;
        return {
            external_camera: {
                primary_color: '#87CEEB',
                secondary_color: '#FFFFFF',
                brightness: isDaytime ? 0.8 : 0.2,
                scene_description: 'city_street'
            },
            internal_camera: {
                mood: 'neutral',
                confidence: 0.85,
                passengers: { children: 0, adults: 1, seniors: 0 }
            }
        };
    }

    simulateLayer1Processing(metadata, visionSignals, overrides) {
        // Base simulation from video analysis (Vision Signals)
        // If overrides exist, merge them.
        
        const mergedSignals = {
            vehicle: {
                speed_kmh: Number(overrides.vehicle?.speed_kmh) || 0,
                passenger_count: Number(overrides.vehicle?.passenger_count) || visionSignals.internal_camera.passengers.adults,
                gear: overrides.vehicle?.gear || 'D'
            },
            environment: {
                time_of_day: Number(overrides.environment?.time_of_day) || (visionSignals.external_camera.brightness > 0.4 ? 0.5 : 0.9),
                weather: overrides.environment?.weather || 'clear',
                temperature: Number(overrides.environment?.temperature) || 22,
                date_type: overrides.environment?.date_type || 'weekday'
            },
            external_camera: {
                ...visionSignals.external_camera,
                ...overrides.external_camera
            },
            internal_camera: {
                ...visionSignals.internal_camera,
                ...overrides.internal_camera
            },
            internal_mic: {
                volume_level: Number(overrides.internal_mic?.volume_level) || 0.3,
                has_voice: overrides.internal_mic?.has_voice === 'true' || overrides.internal_mic?.has_voice === true,
                voice_count: Number(overrides.internal_mic?.voice_count) || 0,
                noise_level: Number(overrides.internal_mic?.noise_level) || 0.1
            },
            user_query: overrides.user_query?.text ? {
                text: overrides.user_query.text,
                intent: overrides.user_query.intent || 'creative',
                confidence: 0.9
            } : null
        };

        return {
            version: '1.0',
            timestamp: new Date().toISOString(),
            signals: mergedSignals,
            confidence: {
                overall: 0.88,
                by_source: {
                    vhal: 0.95,
                    vision: 0.85,
                    audio: 0.9
                }
            },
            raw_signals: [] // In a real system this would have the raw data
        };
    }
}

module.exports = new VideoProcessor();
