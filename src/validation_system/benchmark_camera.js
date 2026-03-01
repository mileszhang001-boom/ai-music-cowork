
const { performance } = require('perf_hooks');

function runBenchmark() {
    const width = 1920;
    const height = 1080;
    const pixels = new Uint8Array(width * height * 4); // RGBA

    // Fill with random data
    for (let i = 0; i < pixels.length; i++) {
        pixels[i] = Math.floor(Math.random() * 256);
    }

    console.log(`Image size: ${width}x${height} (${(pixels.length / 1024 / 1024).toFixed(2)} MB)`);

    const start = performance.now();

    // Algorithm:
    // 1. Calculate average brightness
    // 2. Calculate average color (simple dominant color approximation)
    
    let totalR = 0;
    let totalG = 0;
    let totalB = 0;
    let totalBrightness = 0;
    const pixelCount = width * height;

    // Optimization: Skip every N pixels if needed, but let's try full resolution first
    const step = 4; // RGBA
    // To speed up, we can sample every 10th pixel, but let's test full scan first
    
    for (let i = 0; i < pixels.length; i += step) {
        const r = pixels[i];
        const g = pixels[i+1];
        const b = pixels[i+2];
        
        totalR += r;
        totalG += g;
        totalB += b;
        
        // Simple brightness: (R+G+B)/3 or luminance formula
        // Luminance: 0.299*R + 0.587*G + 0.114*B
        totalBrightness += (0.299 * r + 0.587 * g + 0.114 * b);
    }

    const avgR = Math.round(totalR / pixelCount);
    const avgG = Math.round(totalG / pixelCount);
    const avgB = Math.round(totalB / pixelCount);
    const avgBrightness = totalBrightness / pixelCount / 255.0;

    const end = performance.now();
    
    console.log(`Time taken: ${(end - start).toFixed(2)} ms`);
    console.log(`Result: R=${avgR}, G=${avgG}, B=${avgB}, Brightness=${avgBrightness.toFixed(2)}`);

    if ((end - start) < 500) {
        console.log("PASS: Processing time is within 500ms.");
    } else {
        console.log("FAIL: Processing time exceeds 500ms.");
    }
}

runBenchmark();
