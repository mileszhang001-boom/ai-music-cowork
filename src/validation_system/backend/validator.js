const fs = require('fs');
const path = require('path');
const Ajv = require('ajv');
const addFormats = require('ajv-formats');

class Layer1Validator {
    constructor() {
        this.ajv = new Ajv({ allErrors: true });
        addFormats(this.ajv);
        this.schema = null;
        this.validateFn = null;
        this.loadSchema();
    }

    loadSchema() {
        try {
            // Use the runtime schema that matches actual code behavior
            const schemaPath = path.resolve(__dirname, 'layer1_runtime.schema.json');
            const schemaContent = fs.readFileSync(schemaPath, 'utf8');
            this.schema = JSON.parse(schemaContent);
            this.validateFn = this.ajv.compile(this.schema);
            console.log('Layer1 Runtime Schema loaded successfully.');
        } catch (error) {
            console.error('Failed to load Layer1 Schema:', error);
            throw error;
        }
    }

    validate(data) {
        if (!this.validateFn) {
            throw new Error('Validator not initialized');
        }
        const valid = this.validateFn(data);
        return {
            valid,
            errors: this.validateFn.errors
        };
    }
}

module.exports = new Layer1Validator();
