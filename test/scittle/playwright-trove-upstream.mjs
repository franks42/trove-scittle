#!/usr/bin/env node
/**
 * Playwright test for upstream Trove in Scittle
 *
 * Tests that Peter's upstream fixes to Trove work correctly in Scittle/SCI.
 * This verifies the changes before a new release is tagged.
 *
 * KEY FIXES BEING VERIFIED:
 * 1. utils.cljc: const-form? uses platform-agnostic Cons check
 * 2. utils.cljc: callsite-coords works without #?(:clj ...) wrapper
 * 3. console.cljc: Works in ClojureScript/Scittle
 * 4. trove.cljc: log! macro works with SCI
 */

import { chromium } from 'playwright';
import { createServer } from 'http';
import { readFileSync, existsSync } from 'fs';
import { join, extname } from 'path';
import { fileURLToPath } from 'url';
import { dirname } from 'path';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
const projectRoot = join(__dirname, '../..');

// MIME types for serving files
const mimeTypes = {
  '.html': 'text/html',
  '.js': 'application/javascript',
  '.cljc': 'text/plain',
  '.cljs': 'text/plain',
  '.clj': 'text/plain',
  '.css': 'text/css',
  '.json': 'application/json',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.svg': 'image/svg+xml',
};

// Simple HTTP server
function startServer(port) {
  return new Promise((resolve, reject) => {
    const server = createServer((req, res) => {
      const url = new URL(req.url, `http://localhost:${port}`);
      let filePath;
      
      if (url.pathname === '/test-cons') {
        // Serve cons test file
        filePath = '/tmp/test-cons.html';
      } else if (url.pathname === '/') {
        // Serve the test file
        filePath = join(__dirname, 'test-trove-upstream.html');
      } else if (url.pathname.startsWith('/tmp/')) {
        // Serve from /tmp directory (for modified upstream files)
        filePath = url.pathname;
      } else if (url.pathname.startsWith('/src/')) {
        // Serve from project src directory (for local Trove files)
        filePath = join(projectRoot, url.pathname);
      } else {
        // Try relative to scittle-demo directory first
        filePath = join(__dirname, url.pathname);
        if (!existsSync(filePath)) {
          // Then try relative to project root
          filePath = join(projectRoot, url.pathname);
        }
      }

      console.log(`  [Server] ${req.url} -> ${filePath}`);

      try {
        if (existsSync(filePath)) {
          const ext = extname(filePath);
          const contentType = mimeTypes[ext] || 'application/octet-stream';
          const content = readFileSync(filePath);
          res.writeHead(200, { 'Content-Type': contentType });
          res.end(content);
        } else {
          console.log(`  [Server] 404: ${filePath}`);
          res.writeHead(404);
          res.end(`Not found: ${req.url} (tried: ${filePath})`);
        }
      } catch (err) {
        console.log(`  [Server] Error: ${err.message}`);
        res.writeHead(500);
        res.end(`Error: ${err.message}`);
      }
    });

    server.listen(port, () => {
      console.log(`📡 Server running at http://localhost:${port}`);
      resolve(server);
    });

    server.on('error', reject);
  });
}

async function testTroveUpstream() {
  console.log('🧪 Trove Upstream Test - Verifying Scittle Compatibility\n');
  console.log('=' .repeat(60) + '\n');

  // Start local server
  const PORT = 8765;
  let server;
  try {
    server = await startServer(PORT);
  } catch (err) {
    console.error('❌ Failed to start server:', err.message);
    process.exit(1);
  }

  const browser = await chromium.launch({
    headless: true,  // Run headless for automated testing
  });

  const context = await browser.newContext();
  const page = await context.newPage();

  // Collect console messages
  const consoleLogs = [];
  const consoleErrors = [];

  page.on('console', msg => {
    const text = msg.text();
    consoleLogs.push({ type: msg.type(), text });
    
    // Print ALL console messages for debugging
    const prefix = msg.type() === 'error' ? '❌' : msg.type() === 'warning' ? '⚠️' : '  ';
    console.log(`${prefix} [${msg.type()}] ${text}`);
  });

  page.on('pageerror', error => {
    consoleErrors.push(error.message);
    console.error(`  ❌ [Page Error] ${error.message}`);
  });

  // Capture request failures
  page.on('requestfailed', request => {
    console.error(`  ❌ [Request Failed] ${request.url()} - ${request.failure()?.errorText}`);
  });

  let testsPassed = 0;
  let testsFailed = 0;

  try {
    console.log(`📄 Loading test page: http://localhost:${PORT}\n`);
    
    const response = await page.goto(`http://localhost:${PORT}`, { waitUntil: 'domcontentloaded' });
    console.log(`  Response status: ${response.status()}`);
    
    // Get page title
    const title = await page.title();
    console.log(`  Page title: ${title}`);
    
    // Get page HTML to verify it loaded
    const html = await page.content();
    console.log(`  Page HTML length: ${html.length} chars`);
    if (html.length < 500) {
      console.log(`  Page content: ${html.substring(0, 500)}`);
    }

    // Wait for tests to complete - give more time for CDN loads
    console.log('\n⏳ Waiting for Scittle tests to execute (10s for CDN loads)...\n');
    await page.waitForTimeout(10000);

    // Check results from the page
    const results = await page.evaluate(() => {
      const resultDivs = document.querySelectorAll('.test');
      const results = [];
      resultDivs.forEach(div => {
        const passed = div.classList.contains('pass');
        const name = div.querySelector('strong')?.textContent || 'Unknown';
        const details = div.querySelector('pre')?.textContent || '';
        results.push({ passed, name, details });
      });
      return results;
    });

    console.log('\n' + '=' .repeat(60));
    console.log('📋 TEST RESULTS\n');

    for (const result of results) {
      if (result.passed) {
        testsPassed++;
        console.log(`  ✅ ${result.name}`);
      } else {
        testsFailed++;
        console.log(`  ❌ ${result.name}`);
        console.log(`     Details: ${result.details.substring(0, 200)}`);
      }
    }

    // Check for page errors
    if (consoleErrors.length > 0) {
      console.log('\n⚠️  Page Errors Detected:');
      consoleErrors.forEach(err => console.log(`  - ${err}`));
    }

    // Summary
    console.log('\n' + '=' .repeat(60));
    console.log('📊 SUMMARY\n');
    console.log(`  Total:  ${testsPassed + testsFailed}`);
    console.log(`  Passed: ${testsPassed}`);
    console.log(`  Failed: ${testsFailed}`);

    if (testsFailed === 0 && testsPassed > 0) {
      console.log('\n🎉 ALL TESTS PASSED!');
      console.log('✅ Upstream Trove is ready for release tagging!\n');
    } else if (testsFailed > 0) {
      console.log('\n⚠️  SOME TESTS FAILED');
      console.log('   Review the errors above before tagging release.\n');
    } else {
      console.log('\n⚠️  NO TESTS DETECTED');
      console.log('   Check if the test page loaded correctly.\n');
    }

    // Take screenshot
    const screenshotPath = join(__dirname, 'screenshot-trove-upstream.png');
    await page.screenshot({ path: screenshotPath, fullPage: true });
    console.log(`📸 Screenshot saved: ${screenshotPath}\n`);

  } catch (error) {
    console.error('\n❌ Test execution failed:', error.message);
    testsFailed++;
  } finally {
    await browser.close();
    server.close();
    console.log('👋 Browser and server closed\n');
  }

  // Exit with appropriate code
  process.exit(testsFailed > 0 ? 1 : 0);
}

// Run the test
testTroveUpstream().catch(error => {
  console.error('Fatal error:', error);
  process.exit(1);
});
