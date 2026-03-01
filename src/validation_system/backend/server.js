const express = require('express');
const multer = require('multer');
const cors = require('cors');
const path = require('path');
const fs = require('fs');
const processor = require('./processor');
const validator = require('./validator');

const app = express();
const PORT = 3001;

// Configure Multer for file upload
const storage = multer.diskStorage({
    destination: (req, file, cb) => {
        cb(null, path.join(__dirname, 'uploads'));
    },
    filename: (req, file, cb) => {
        cb(null, `${Date.now()}-${file.originalname}`);
    }
});

const upload = multer({
    storage,
    limits: { fileSize: 500 * 1024 * 1024 }, // 500MB limit
    fileFilter: (req, file, cb) => {
        const allowedTypes = ['video/mp4', 'video/quicktime', 'video/x-msvideo', 'video/webm'];
        if (allowedTypes.includes(file.mimetype)) {
            cb(null, true);
        } else {
            cb(new Error('Invalid file type. Only MP4, MOV, AVI are allowed.'));
        }
    }
});

app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, '../frontend')));

// API Endpoint
app.post('/api/upload', upload.single('video'), async (req, res) => {
    const startTime = Date.now();
    let filePath = null;

    try {
        if (req.file) {
            filePath = req.file.path;
            console.log(`Processing file: ${req.file.originalname}`);
        } else {
            console.log('Processing simulation without video file');
        }

        // Parse overrides from form data
        let overrides = {};
        if (req.body.overrides) {
            try {
                overrides = JSON.parse(req.body.overrides);
            } catch (e) {
                console.warn('Failed to parse overrides JSON:', e);
            }
        }

        // 1. Process Video (Simulate Layer1)
        const processingResult = await processor.process(filePath, overrides);

        // 2. Validate against Layer1 Schema
        const validationResult = validator.validate(processingResult.layer1Output);

        // 3. Generate Report
        const report = {
            status: validationResult.valid ? 'success' : 'failure',
            timestamp: new Date().toISOString(),
            processingTimeMs: Date.now() - startTime,
            fileInfo: req.file ? {
                name: req.file.originalname,
                size: req.file.size,
                ...processingResult.metadata
            } : null,
            layer1Output: processingResult.layer1Output,
            validation: validationResult,
            layer1SimulationStats: processingResult.processingStats
        };

        res.json(report);

    } catch (error) {
        console.error('Processing error:', error);
        res.status(500).json({ error: error.message });
    } finally {
        // Clean up: delete uploaded file if it exists
        if (filePath) {
            fs.unlink(filePath, (err) => {
                if (err) console.error('Failed to delete temp file:', err);
            });
        }
    }
});

// Serve Frontend
app.get('*', (req, res) => {
    res.sendFile(path.join(__dirname, '../frontend/index.html'));
});

app.listen(PORT, () => {
    console.log(`Validation System running at http://localhost:${PORT}`);
});
