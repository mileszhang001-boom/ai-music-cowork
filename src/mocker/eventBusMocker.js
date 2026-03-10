const fs = require('fs');
const path = require('path');
const EventEmitter = require('events');

class EventBus extends EventEmitter {}
const eventBus = new EventBus();

// 监听事件并打印日志
eventBus.on('music.track_changed', (data) => {
  console.log(`[EventBus] 🎵 Track Changed: ${data.track_name} (ID: ${data.track_id})`);
});

eventBus.on('music.beat', (data) => {
  console.log(`[EventBus] 🥁 Beat Injected: Timestamp ${data.timestamp_ms}ms, Energy ${data.energy}`);
});

class EventBusMocker {
  constructor(beatsDir) {
    this.beatsDir = beatsDir;
    this.currentTimeout = null;
  }

  async startInjection(trackFileName) {
    const filePath = path.join(this.beatsDir, trackFileName);
    if (!fs.existsSync(filePath)) {
      console.error(`[Error] Beat file not found: ${filePath}`);
      return;
    }

    const trackData = JSON.parse(fs.readFileSync(filePath, 'utf-8'));
    
    // 触发切歌事件
    eventBus.emit('music.track_changed', {
      track_id: trackData.track_id,
      track_name: trackData.track_name,
      duration_ms: trackData.duration_ms
    });

    console.log(`[Mocker] Starting beat injection for ${trackData.track_name}...`);
    
    const startTime = Date.now();
    
    const scheduleNextBeat = (beatIndex) => {
      if (beatIndex >= trackData.beats.length) {
        console.log(`[Mocker] Finished beat injection for ${trackData.track_name}`);
        return;
      }

      const beat = trackData.beats[beatIndex];
      const now = Date.now();
      const elapsed = now - startTime;
      const delay = beat.timestamp_ms - elapsed;

      if (delay > 0) {
        this.currentTimeout = setTimeout(() => {
          eventBus.emit('music.beat', beat);
          scheduleNextBeat(beatIndex + 1);
        }, delay);
      } else {
        // 如果已经过了时间，立即触发并调度下一个
        eventBus.emit('music.beat', beat);
        scheduleNextBeat(beatIndex + 1);
      }
    };

    scheduleNextBeat(0);
  }

  stopInjection() {
    if (this.currentTimeout) {
      clearTimeout(this.currentTimeout);
      this.currentTimeout = null;
      console.log('[Mocker] Stopped beat injection.');
    }
  }
}

// 运行 Mocker
const mocker = new EventBusMocker(path.join(__dirname, '../../mock_data/beats'));

// 启动测试
console.log('--- Event Bus Mocker Started ---');
mocker.startInjection('track_001.json');
