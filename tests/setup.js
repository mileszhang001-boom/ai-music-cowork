'use strict';

const assert = require('assert');
const path = require('path');

global.assert = assert;

global.loadMockData = (filename) => {
  const fs = require('fs');
  const mockPath = path.join(__dirname, '../../mock_data', filename);
  if (fs.existsSync(mockPath)) {
    return JSON.parse(fs.readFileSync(mockPath, 'utf8'));
  }
  return null;
};

global.loadSchema = (filename) => {
  const fs = require('fs');
  const schemaPath = path.join(__dirname, '../../schemas', filename);
  if (fs.existsSync(schemaPath)) {
    return JSON.parse(fs.readFileSync(schemaPath, 'utf8'));
  }
  return null;
};

console.log('Test environment initialized');
