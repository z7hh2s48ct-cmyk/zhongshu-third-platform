import test from 'node:test';
import assert from 'node:assert/strict';
import { mkdtempSync, mkdirSync, writeFileSync, symlinkSync, chmodSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { gitBlobHash, verifyFiles, verifyManifest } from './verify-source-copy.mjs';

function fixture() {
  const root = mkdtempSync(join(tmpdir(), 'zhongshu-copy-test-'));
  mkdirSync(join(root, 'source'));
  writeFileSync(join(root, 'source', 'sample.txt'), 'hello\n');
  return root;
}

const entry = { path: 'source/sample.txt', mode: '100644', importedBlob: 'ce013625030ba8dba906f756967f9e9ca394464a' };

test('Git blob hash includes the Git header', () => {
  assert.equal(gitBlobHash(Buffer.from('hello\n')), entry.importedBlob);
});

test('an exact imported file passes', () => {
  assert.deepEqual(verifyFiles(fixture(), [entry]), []);
});

test('content changes and missing files are reported', () => {
  const root = fixture();
  writeFileSync(join(root, 'source', 'sample.txt'), 'changed');
  assert.equal(verifyFiles(root, [entry])[0].reason, 'content-mismatch');
  assert.equal(verifyFiles(root, [{ ...entry, path: 'source/missing.txt' }])[0].reason, 'missing');
});

test('paths outside the project and duplicate entries are rejected', () => {
  assert.equal(verifyFiles(fixture(), [{ ...entry, path: '../outside' }])[0].reason, 'unsafe-path');
  assert.equal(verifyFiles(fixture(), [entry, entry])[0].reason, 'duplicate-path');
});

test('a regular file may not silently become a symbolic link', () => {
  const root = fixture();
  symlinkSync('sample.txt', join(root, 'source', 'link.txt'));
  assert.equal(verifyFiles(root, [{ ...entry, path: 'source/link.txt' }])[0].reason, 'file-type-mismatch');
});

test('a recorded symlink is checked without following its target', () => {
  const root = fixture();
  symlinkSync('sample.txt', join(root, 'source', 'link.txt'));
  assert.deepEqual(verifyFiles(root, [{ path: 'source/link.txt', mode: '120000', importedBlob: gitBlobHash(Buffer.from('sample.txt')) }]), []);
});

test('non-canonical paths and unknown file modes are rejected', () => {
  const root = fixture();
  assert.equal(verifyFiles(root, [{ ...entry, path: 'source/../source/sample.txt' }])[0].reason, 'unsafe-path');
  assert.equal(verifyFiles(root, [{ ...entry, mode: 'invalid' }])[0].reason, 'invalid-entry');
});

// 可执行位语义在 Windows 上不可用（verifyFiles 在 win32 跳过 mode 校验），
// 该用例仅在 POSIX（含 CI ubuntu）执行；symlink 部分同样需要 POSIX 语义
const isPosix = process.platform !== 'win32';
(isPosix ? test : test.skip)('executable mode and parent symlink changes are rejected', () => {
  const root = fixture();
  chmodSync(join(root, entry.path), 0o755);
  assert.equal(verifyFiles(root, [entry])[0].reason, 'executable-mode-mismatch');
  symlinkSync('source', join(root, 'alias'));
  assert.equal(verifyFiles(root, [{ ...entry, path: 'alias/sample.txt' }])[0].reason, 'unsafe-parent-link');
});

function manifestFixture() {
  const root = fixture();
  const file = { ...entry, sourceId: 'example', sourcePath: 'sample.txt', sourceBlob: entry.importedBlob };
  const manifest = { schemaVersion: 1, sources: [{ id: 'example', target: 'source', fileCount: 1 }], files: [file] };
  const sanitization = { schemaVersion: 1, changes: [] };
  return { root, manifest, sanitization };
}

test('a nonempty, consistent manifest passes', () => {
  const { root, manifest, sanitization } = manifestFixture();
  assert.deepEqual(verifyManifest(root, manifest, sanitization), []);
});

test('empty or unsupported manifests fail', () => {
  const { root, manifest, sanitization } = manifestFixture();
  assert.equal(verifyManifest(root, { ...manifest, files: [] }, sanitization)[0].reason, 'invalid-manifest');
  assert.equal(verifyManifest(root, { ...manifest, schemaVersion: 2 }, sanitization)[0].reason, 'invalid-manifest');
});

test('source counts and source-to-target mapping must agree', () => {
  const { root, manifest, sanitization } = manifestFixture();
  manifest.sources[0].fileCount = 2;
  assert.ok(verifyManifest(root, manifest, sanitization).some(i => i.reason === 'source-count-mismatch'));
  manifest.sources[0].fileCount = 1;
  manifest.files[0].sourcePath = 'different.txt';
  assert.ok(verifyManifest(root, manifest, sanitization).some(i => i.reason === 'source-path-mismatch'));
});

test('unrecorded source files fail, generated build caches do not', () => {
  const { root, manifest, sanitization } = manifestFixture();
  mkdirSync(join(root, 'source', 'target'));
  writeFileSync(join(root, 'source', 'target', 'generated.txt'), 'build output');
  assert.deepEqual(verifyManifest(root, manifest, sanitization), []);
  writeFileSync(join(root, 'source', 'extra.txt'), 'unrecorded');
  assert.ok(verifyManifest(root, manifest, sanitization).some(i => i.reason === 'unrecorded-file'));
});

test('sanitization records must exactly match the imported differences', () => {
  const { root, manifest, sanitization } = manifestFixture();
  manifest.files[0].sourceBlob = '0'.repeat(40);
  assert.ok(verifyManifest(root, manifest, sanitization).some(i => i.reason === 'sanitization-mismatch'));
  sanitization.changes.push({ path: entry.path, sourceBlob: '0'.repeat(40), importedBlob: entry.importedBlob });
  assert.deepEqual(verifyManifest(root, manifest, sanitization), []);
  sanitization.changes.push({ ...sanitization.changes[0] });
  assert.ok(verifyManifest(root, manifest, sanitization).some(i => i.reason === 'invalid-sanitization'));
});
