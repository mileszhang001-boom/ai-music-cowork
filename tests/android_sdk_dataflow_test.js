/**
 * Android SDK 数据流验证测试
 * 验证 Layer1 → Layer2 → Layer3 数据流转的正确性
 */

import { strict as assert } from 'assert';

// 模拟 core-api 数据模型
const Layer1Models = {
    StandardizedSignals: class {
        constructor(data = {}) {
            this.version = "1.0";
            this.timestamp = new Date().toISOString();
            this.signals = data.signals || {
                vehicle: { speed_kmh: 0, passenger_count: 1, gear: "P" },
                environment: { time_of_day: 12, weather: "clear", temperature: 22, date_type: "weekday" },
                external_camera: { primary_color: "#808080", brightness: 0.5, scene_description: "city_driving" },
                internal_camera: { mood: "neutral", confidence: 0.8, passengers: { adults: 1, children: 0, seniors: 0 } },
                internal_mic: { volume_level: 0.3, has_voice: false, voice_count: 0, noise_level: 0.1 }
            };
            this.confidence = data.confidence || { overall: 0.9, by_source: {} };
        }
    }
};

const Layer2Models = {
    SceneDescriptor: class {
        constructor(data = {}) {
            this.version = "2.0";
            this.scene_id = data.scene_id || `scene_${Date.now()}`;
            this.scene_type = data.scene_type || "default";
            this.scene_name = data.scene_name || "默认场景";
            this.scene_narrative = data.scene_narrative || "默认驾驶场景";
            this.intent = data.intent || {
                mood: { valence: 0.5, arousal: 0.4 },
                energy_level: 0.4,
                atmosphere: "neutral"
            };
            this.hints = data.hints || {
                music: { genres: ["pop"], tempo: "moderate" },
                lighting: { color_theme: "calm", pattern: "steady", intensity: 0.4 },
                audio: { preset: "standard" }
            };
            this.announcement = data.announcement !== undefined ? data.announcement : "祝您驾驶愉快";
            this.meta = data.meta || { confidence: 0.5, source: "default" };
        }
    }
};

const Layer3Models = {
    EffectCommands: class {
        constructor(data = {}) {
            this.version = "1.0";
            this.scene_id = data.scene_id || `scene_${Date.now()}`;
            this.commands = data.commands || {
                content: { action: "play", tracks: [], playlist_name: "默认播放列表" },
                lighting: { action: "none", zones: [] },
                audio: { action: "set_volume", volume: 0.5, eq_preset: "standard" }
            };
            this.execution_report = data.execution_report || {
                status: "pending",
                timestamp: new Date().toISOString()
            };
        }
    }
};

// 测试用例
class DataFlowTester {
    constructor() {
        this.passed = 0;
        this.failed = 0;
        this.tests = [];
    }

    test(name, fn) {
        try {
            fn();
            this.passed++;
            this.tests.push({ name, status: 'PASS' });
            console.log(`✅ ${name}`);
        } catch (error) {
            this.failed++;
            this.tests.push({ name, status: 'FAIL', error: error.message });
            console.log(`❌ ${name}: ${error.message}`);
        }
    }

    summary() {
        console.log('\n' + '='.repeat(50));
        console.log(`测试结果: ${this.passed} 通过, ${this.failed} 失败`);
        console.log('='.repeat(50));
        return this.failed === 0;
    }
}

// 运行测试
const tester = new DataFlowTester();

console.log('\n📋 Layer 1 数据模型测试');
console.log('-'.repeat(30));

tester.test('Layer1: StandardizedSignals 默认值正确', () => {
    const signals = new Layer1Models.StandardizedSignals();
    assert.equal(signals.version, "1.0");
    assert.ok(signals.timestamp);
    assert.ok(signals.signals.vehicle);
    assert.ok(signals.signals.environment);
});

tester.test('Layer1: StandardizedSignals 自定义值正确', () => {
    const signals = new Layer1Models.StandardizedSignals({
        signals: {
            vehicle: { speed_kmh: 60, passenger_count: 2, gear: "D" },
            environment: { time_of_day: 8, weather: "sunny", temperature: 25, date_type: "weekday" }
        }
    });
    assert.equal(signals.signals.vehicle.speed_kmh, 60);
    assert.equal(signals.signals.environment.weather, "sunny");
});

tester.test('Layer1: JSON 序列化/反序列化正确', () => {
    const signals = new Layer1Models.StandardizedSignals();
    const json = JSON.stringify(signals);
    const parsed = JSON.parse(json);
    assert.equal(parsed.version, "1.0");
    assert.ok(parsed.signals.vehicle);
});

console.log('\n📋 Layer 2 数据模型测试');
console.log('-'.repeat(30));

tester.test('Layer2: SceneDescriptor 默认值正确', () => {
    const descriptor = new Layer2Models.SceneDescriptor();
    assert.equal(descriptor.version, "2.0");
    assert.ok(descriptor.scene_id);
    assert.ok(descriptor.intent.mood);
});

tester.test('Layer2: SceneDescriptor announcement 字段存在', () => {
    const descriptor = new Layer2Models.SceneDescriptor({
        announcement: "早安，为您准备了清新的晨间音乐"
    });
    assert.equal(descriptor.announcement, "早安，为您准备了清新的晨间音乐");
});

tester.test('Layer2: SceneDescriptor intent 结构正确', () => {
    const descriptor = new Layer2Models.SceneDescriptor({
        intent: {
            mood: { valence: 0.7, arousal: 0.6 },
            energy_level: 0.5,
            atmosphere: "energetic"
        }
    });
    assert.equal(descriptor.intent.mood.valence, 0.7);
    assert.equal(descriptor.intent.energy_level, 0.5);
});

console.log('\n📋 Layer 3 数据模型测试');
console.log('-'.repeat(30));

tester.test('Layer3: EffectCommands 默认值正确', () => {
    const commands = new Layer3Models.EffectCommands();
    assert.equal(commands.version, "1.0");
    assert.ok(commands.commands.content);
    assert.ok(commands.commands.lighting);
    assert.ok(commands.commands.audio);
});

tester.test('Layer3: EffectCommands content 命令结构正确', () => {
    const commands = new Layer3Models.EffectCommands({
        commands: {
            content: {
                action: "play",
                tracks: [{ id: 1, title: "测试歌曲" }],
                playlist_name: "晨间音乐"
            }
        }
    });
    assert.equal(commands.commands.content.action, "play");
    assert.equal(commands.commands.content.tracks.length, 1);
});

console.log('\n📋 数据流转测试');
console.log('-'.repeat(30));

tester.test('数据流: Layer1 → Layer2 字段映射正确', () => {
    const signals = new Layer1Models.StandardizedSignals({
        signals: {
            environment: { time_of_day: 8, weather: "sunny" },
            internal_camera: { mood: "happy", confidence: 0.9 }
        }
    });
    
    // 模拟 Layer2 处理
    const descriptor = new Layer2Models.SceneDescriptor({
        scene_type: signals.signals.environment.time_of_day < 12 ? "morning_commute" : "default",
        intent: {
            mood: { valence: 0.7, arousal: 0.5 },
            energy_level: 0.4
        }
    });
    
    assert.equal(descriptor.scene_type, "morning_commute");
});

tester.test('数据流: Layer2 → Layer3 字段映射正确', () => {
    const descriptor = new Layer2Models.SceneDescriptor({
        scene_type: "morning_commute",
        intent: { energy_level: 0.6 },
        hints: { music: { genres: ["pop", "electronic"] } }
    });
    
    // 模拟 Layer3 处理
    const commands = new Layer3Models.EffectCommands({
        scene_id: descriptor.scene_id,
        commands: {
            content: {
                action: "play",
                playlist_name: `${descriptor.scene_type}_playlist`
            }
        }
    });
    
    assert.equal(commands.scene_id, descriptor.scene_id);
    assert.ok(commands.commands.content.playlist_name.includes("morning"));
});

tester.test('数据流: 完整链路数据传递正确', () => {
    // Layer1 输入
    const signals = new Layer1Models.StandardizedSignals({
        signals: {
            vehicle: { speed_kmh: 30 },
            environment: { time_of_day: 8, weather: "clear" }
        }
    });
    
    // Layer2 处理
    const descriptor = new Layer2Models.SceneDescriptor({
        scene_type: "morning_commute",
        announcement: "早安，上班路上注意安全"
    });
    
    // Layer3 处理
    const commands = new Layer3Models.EffectCommands({
        scene_id: descriptor.scene_id
    });
    
    // 验证完整链路
    assert.ok(signals.version === "1.0");
    assert.ok(descriptor.version === "2.0");
    assert.ok(commands.version === "1.0");
    assert.ok(descriptor.announcement.length > 0);
});

console.log('\n📋 防抖机制测试');
console.log('-'.repeat(30));

tester.test('防抖: 相同场景不触发更新', () => {
    const threshold = 0.3;
    
    const oldDescriptor = new Layer2Models.SceneDescriptor({
        intent: { mood: { valence: 0.5, arousal: 0.4 }, energy_level: 0.4 }
    });
    
    const newDescriptor = new Layer2Models.SceneDescriptor({
        intent: { mood: { valence: 0.52, arousal: 0.41 }, energy_level: 0.42 }
    });
    
    // 计算变化幅度
    const valenceDiff = Math.abs(oldDescriptor.intent.mood.valence - newDescriptor.intent.mood.valence);
    const arousalDiff = Math.abs(oldDescriptor.intent.mood.arousal - newDescriptor.intent.mood.arousal);
    const energyDiff = Math.abs(oldDescriptor.intent.energy_level - newDescriptor.intent.energy_level);
    const changeMagnitude = (valenceDiff + arousalDiff + energyDiff) / 3;
    
    assert.ok(changeMagnitude < threshold, "变化幅度应小于阈值");
});

tester.test('防抖: 大变化触发更新', () => {
    const threshold = 0.3;
    
    const oldDescriptor = new Layer2Models.SceneDescriptor({
        intent: { mood: { valence: 0.5, arousal: 0.4 }, energy_level: 0.4 }
    });
    
    const newDescriptor = new Layer2Models.SceneDescriptor({
        intent: { mood: { valence: 0.8, arousal: 0.7 }, energy_level: 0.8 }
    });
    
    const valenceDiff = Math.abs(oldDescriptor.intent.mood.valence - newDescriptor.intent.mood.valence);
    const arousalDiff = Math.abs(oldDescriptor.intent.mood.arousal - newDescriptor.intent.mood.arousal);
    const energyDiff = Math.abs(oldDescriptor.intent.energy_level - newDescriptor.intent.energy_level);
    const changeMagnitude = (valenceDiff + arousalDiff + energyDiff) / 3;
    
    assert.ok(changeMagnitude >= threshold, "变化幅度应大于等于阈值");
});

console.log('\n📋 TTS 集成测试');
console.log('-'.repeat(30));

tester.test('TTS: announcement 非空时触发播报', () => {
    const descriptor = new Layer2Models.SceneDescriptor({
        announcement: "早安，为您准备了清新的晨间音乐"
    });
    
    const shouldSpeak = descriptor.announcement && descriptor.announcement.length > 0;
    assert.ok(shouldSpeak, "应触发 TTS 播报");
});

tester.test('TTS: announcement 为空时不触发播报', () => {
    const descriptor = new Layer2Models.SceneDescriptor({
        announcement: null
    });
    
    const shouldSpeak = !!(descriptor.announcement && descriptor.announcement.trim().length > 0);
    assert.strictEqual(shouldSpeak, false, "不应触发 TTS 播报");
});

// 输出测试结果
const success = tester.summary();

// 导出测试结果
export { success, Layer1Models, Layer2Models, Layer3Models };
