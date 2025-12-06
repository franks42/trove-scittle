/**
 * Playwright test for Trove in Scittle
 * 
 * Tests that the :scittle reader conditional fix works correctly.
 * 
 * Usage:
 *   cd test/scittle
 *   npm install
 *   npm run install-playwright
 *   npm test
 */

import { chromium } from 'playwright';
import { createServer } from 'http';
import { readFileSync, existsSync } from 'fs';
import { join, dirname } from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
const projectRoot = join(__dirname, '../..');

const MIME_TYPES = {
  '.html': 'text/html',
  '.js': 'application/javascript',
  '.cljc': 'text/plain',
  '.cljs': 'text/plain',
  '.css': 'text/css',
};

// Simple HTTP server
function startServer(port) {
  return new Promise((resolve, reject) => {
    const server = createServer((req, res) => {
      const url = new URL(req.url, `http://localhost:${port}`);
      let filePath;
      
      if (url.pathname === '/') {
        filePath = join(__dirname, 'test-trove.html');
      } else if (url.pathname.startsWith('/src/')) {
        filePath = join(projectRoot, url.pathname);
      } else {
        filePath = join(__dirname, url.pathname);
      }
      
      console.log(`  [Server] ${url.pathname} -> ${filePath}`);
      
      if (existsSync(filePath)) {
        const ext = filePath.substring(filePath.lastIndexOf('.'));
        const contentType = MIME_TYPES[ext] || 'application/octet-stream';
        res.writeHead(200, { 'Content-Type': contentType });
        res.end(readFileSync(filePath));
      } else {
        console.log(`  [Server] 404: ${filePath}`);
        res.writeHead(404, { 'Content-Type': 'text/plain' });
        res.end(`Not found: ${url.pathname}`);
      }
    });
    
    server.listen(port, () => resolve(server));
    server.on('error', reject);
  });
}

async function runTests() {
  console.log('🧪 Trove Scittle Test\n');
  console.log('='.repeat(60));
  
  const port = 8765;
  const server = await startServer(port);
  console.log(`\n📡 Server running at http://localhost:${port}`);
  
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();
  
  const consoleLogs = [];
  const pageErrors = [];
  
  page.on('console', msg => {
    consoleLogs.push({ type: msg.type(), text: msg.text() });
    const icon = msg.type() === 'error' ? '❌' : 
                 msg.type() === 'warning' ? '⚠️' : '  ';
    console.log(`${icon} [${msg.type()}] ${msg.text()}`);
  });
  
  page.on('pageerror', error => {
    pageErrors.push(error.message);
    console.log(`  ❌ [Page Error] ${error.message}`);
  });
  
  console.log(`📄 Loading test page: http://localhost:${port}\n`);
  
  await page.goto(`http://localhost:${port}`, { waitUntil: 'networkidle' });
  
  // Wait for tests to complete
  await page.waitForTimeout(3000);
  
  // Get results from page
  const results = await page.evaluate(() => {
    const resultDivs = document.querySelectorAll('.test');
    return Array.from(resultDivs).map(div => ({
      passed: div.classList.contains('pass'),
      text: div.textContent
    }));
  });
  
  console.log('\n' + '='.repeat(60));
  console.log('📋 TEST RESULTS\n');
  
  let passed = 0;
  let failed = 0;
  
  for (const result of results) {
    if (result.passed) {
      passed++;
      console.log(`  ✅ ${result.text.split('\n')[0]}`);
    } else {
      failed++;
      console.log(`  ❌ ${result.text.split('\n')[0]}`);
    }
  }
  
  if (pageErrors.length > 0) {
    console.log('\n⚠️  Page Errors Detected:');
    for (const err of pageErrors) {
      console.log(`  - ${err.substring(0, 100)}`);
    }
  }
  
  console.log('\n' + '='.repeat(60));
  console.log('📊 SUMMARY\n');
  console.log(`  Total:  ${passed + failed}`);
  console.log(`  Passed: ${passed}`);
  console.log(`  Failed: ${failed}`);
  
  if (failed === 0 && passed > 0) {
    console.log('\n🎉 ALL TESTS PASSED!');
  } else if (failed > 0) {
    console.log('\n❌ SOME TESTS FAILED');
  } else {
    console.log('\n⚠️  NO TESTS DETECTED');
  }
  
  await browser.close();
  server.close();
  console.log('\n👋 Done');
  
  process.exit(failed > 0 ? 1 : 0);
}

runTests().catch(err => {
  console.error('Test runner error:', err);
  process.exit(1);
});
