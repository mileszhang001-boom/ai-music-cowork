// Tab Switching Logic
document.querySelectorAll('.tab-btn').forEach(btn => {
    btn.addEventListener('click', () => {
        document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');

        const mode = btn.dataset.tab;
        if (mode === 'upload') {
            document.getElementById('upload-mode').classList.remove('hidden');
            document.getElementById('webcam-mode').classList.add('hidden');
            stopCamera(); // Ensure camera is stopped when switching away
        } else {
            document.getElementById('upload-mode').classList.add('hidden');
            document.getElementById('webcam-mode').classList.remove('hidden');
        }
    });
});

// Simulation Inputs
document.getElementById('sim_time').addEventListener('input', (e) => {
    document.getElementById('sim_time_val').textContent = e.target.value;
});

document.getElementById('sim_volume').addEventListener('input', (e) => {
    document.getElementById('sim_volume_val').textContent = e.target.value;
});

// --- File Upload Logic ---
document.getElementById('uploadBtn').addEventListener('click', async () => {
    const fileInput = document.getElementById('videoInput');
    const file = fileInput.files[0];
    
    if (file && file.size > 500 * 1024 * 1024) {
        alert('文件大小超过 500MB 限制');
        return;
    }

    await processAndUpload(file);
});

// --- Webcam Logic ---
let mediaStream = null;
let mediaRecorder = null;
let recordedChunks = [];

const webcamPreview = document.getElementById('webcamPreview');
const startCameraBtn = document.getElementById('startCameraBtn');
const captureBtn = document.getElementById('captureBtn');
const stopCameraBtn = document.getElementById('stopCameraBtn');
const recordingIndicator = document.getElementById('recordingIndicator');

startCameraBtn.addEventListener('click', async () => {
    try {
        mediaStream = await navigator.mediaDevices.getUserMedia({ video: true, audio: false });
        webcamPreview.srcObject = mediaStream;
        
        startCameraBtn.disabled = true;
        captureBtn.disabled = false;
        stopCameraBtn.disabled = false;
    } catch (err) {
        console.error("Error accessing webcam:", err);
        alert("无法访问摄像头，请检查权限设置。");
    }
});

stopCameraBtn.addEventListener('click', stopCamera);

function stopCamera() {
    if (mediaStream) {
        mediaStream.getTracks().forEach(track => track.stop());
        mediaStream = null;
        webcamPreview.srcObject = null;
    }
    startCameraBtn.disabled = false;
    captureBtn.disabled = true;
    stopCameraBtn.disabled = true;
    recordingIndicator.classList.add('hidden');
}

captureBtn.addEventListener('click', () => {
    if (!mediaStream) return;

    recordedChunks = [];
    // Try to use a supported mime type
    const options = { mimeType: 'video/webm;codecs=vp9' };
    
    try {
        mediaRecorder = new MediaRecorder(mediaStream, options);
    } catch (e) {
        // Fallback
        mediaRecorder = new MediaRecorder(mediaStream);
    }

    mediaRecorder.ondataavailable = (event) => {
        if (event.data.size > 0) {
            recordedChunks.push(event.data);
        }
    };

    mediaRecorder.onstop = async () => {
        recordingIndicator.classList.add('hidden');
        captureBtn.disabled = false;
        
        const blob = new Blob(recordedChunks, { type: 'video/webm' });
        const file = new File([blob], "webcam-simulation.webm", { type: 'video/webm' });
        
        await processAndUpload(file);
    };

    mediaRecorder.start();
    recordingIndicator.classList.remove('hidden');
    captureBtn.disabled = true;

    // Record for 3 seconds then stop
    setTimeout(() => {
        if (mediaRecorder && mediaRecorder.state === 'recording') {
            mediaRecorder.stop();
        }
    }, 3000);
});

// --- Shared Upload Function ---
async function processAndUpload(file) {
    // Collect Simulation Data
    const overrides = {
        vehicle: {
            speed_kmh: document.getElementById('sim_speed').value,
            passenger_count: document.getElementById('sim_passenger_count').value,
            gear: document.getElementById('sim_gear').value
        },
        environment: {
            time_of_day: document.getElementById('sim_time').value / 24, // Normalize to 0-1
            weather: document.getElementById('sim_weather').value,
            temperature: document.getElementById('sim_temp').value
        },
        internal_mic: {
            volume_level: document.getElementById('sim_volume').value,
            has_voice: document.getElementById('sim_has_voice').checked,
            voice_count: document.getElementById('sim_has_voice').checked ? 1 : 0
        },
        user_query: document.getElementById('sim_query_text').value ? {
            text: document.getElementById('sim_query_text').value,
            intent: document.getElementById('sim_query_intent').value
        } : null
    };

    // UI Reset
    const progressContainer = document.getElementById('progressContainer');
    const resultContainer = document.getElementById('resultContainer');
    const progressFill = document.getElementById('progressFill');
    const statusText = document.getElementById('statusText');
    
    resultContainer.classList.add('hidden');
    progressContainer.classList.remove('hidden');
    progressFill.style.width = '0%';
    statusText.textContent = '正在处理...';

    const formData = new FormData();
    if (file) {
        formData.append('video', file);
    }
    formData.append('overrides', JSON.stringify(overrides));

    try {
        const xhr = new XMLHttpRequest();
        xhr.open('POST', '/api/upload', true);

        xhr.upload.onprogress = (e) => {
            if (e.lengthComputable) {
                const percent = (e.loaded / e.total) * 100;
                progressFill.style.width = `${percent}%`;
                if (percent === 100) {
                    statusText.textContent = '正在进行 Layer1 模拟处理与校验...';
                }
            }
        };

        xhr.onload = function() {
            if (xhr.status === 200) {
                const report = JSON.parse(xhr.responseText);
                displayReport(report);
            } else {
                alert('请求失败: ' + xhr.statusText);
            }
            progressContainer.classList.add('hidden');
        };

        xhr.onerror = function() {
            alert('网络错误');
            progressContainer.classList.add('hidden');
        };

        xhr.send(formData);

    } catch (error) {
        console.error(error);
        alert('发生错误');
        progressContainer.classList.add('hidden');
    }
}

function displayReport(report) {
    const resultContainer = document.getElementById('resultContainer');
    const statusSpan = document.getElementById('validationStatus');
    const timeSpan = document.getElementById('processingTime');
    const fileInfoList = document.getElementById('fileInfoList');
    const fileInfoCard = document.getElementById('fileInfoCard');
    const statsList = document.getElementById('statsList');
    const jsonOutput = document.getElementById('jsonOutput');
    const errorContainer = document.getElementById('errorContainer');
    const errorList = document.getElementById('errorList');

    resultContainer.classList.remove('hidden');

    // Status
    statusSpan.textContent = report.status === 'success' ? '通过 ✅' : '失败 ❌';
    statusSpan.className = report.status === 'success' ? 'success' : 'failure';
    timeSpan.textContent = report.processingTimeMs;

    // File Info
    if (report.fileInfo) {
        fileInfoCard.classList.remove('hidden');
        fileInfoList.innerHTML = `
            <li>文件名: ${report.fileInfo.name}</li>
            <li>大小: ${(report.fileInfo.size / 1024 / 1024).toFixed(2)} MB</li>
            <li>分辨率: ${report.fileInfo.resolution}</li>
            <li>时长: ${report.fileInfo.duration} 秒</li>
            <li>编码: ${report.fileInfo.codec}</li>
        `;
    } else {
        fileInfoCard.classList.add('hidden');
    }

    // Stats
    statsList.innerHTML = `
        <li>提取关键帧: ${report.layer1SimulationStats.keyframesExtracted}</li>
        <li>目标检测数量: ${report.layer1SimulationStats.objectsDetected}</li>
        <li>时间戳连续性: ${report.layer1SimulationStats.timestampContinuity ? '通过' : '失败'}</li>
        <li>元数据一致性: ${report.layer1SimulationStats.metadataConsistent ? '通过' : '失败'}</li>
    `;

    // JSON Output
    jsonOutput.textContent = JSON.stringify(report.layer1Output, null, 2);

    // Errors
    if (report.status === 'failure' && report.validation.errors) {
        errorContainer.classList.remove('hidden');
        errorList.innerHTML = report.validation.errors.map(err => 
            `<li>${err.instancePath} ${err.message}</li>`
        ).join('');
    } else {
        errorContainer.classList.add('hidden');
    }
}
