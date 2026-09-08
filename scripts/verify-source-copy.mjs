import { createHash } from 'node:crypto';
import { lstatSync, readFileSync, readlinkSync, readdirSync } from 'node:fs';
import { dirname, isAbsolute, posix, resolve, sep } from 'node:path';
import { fileURLToPath } from 'node:url';

export function gitBlobHash(content) {
  return createHash('sha1')
    .update(`blob ${content.length}\0`)
    .update(content)
    .digest('hex');
}

const blobPattern = /^[a-f0-9]{40}$/;
const fileModes = new Set(['100644', '100755', '120000']);

function safePath(value) {
  return typeof value === 'string' && !isAbsolute(value) && !value.includes('\\')
    && value.split('/').every(part => part !== '' && part !== '.' && part !== '..');
}

// Verify the recorded import snapshot, not a future edited business checkout.
export function verifyFiles(root, entries) {
  const projectRoot = resolve(root);
  const seen = new Set();
  const issues = [];
  for (const entry of entries) {
    const fail = (reason) => issues.push({ path: entry?.path ?? '', reason });
    if (!safePath(entry?.path)) {
      fail('unsafe-path');
      continue;
    }
    const target = resolve(projectRoot, entry.path);
    if (!target.startsWith(projectRoot + sep)) {
      fail('unsafe-path');
      continue;
    }
    if (!fileModes.has(entry.mode) || !blobPattern.test(entry.importedBlob)) {
      fail('invalid-entry');
      continue;
    }
    if (seen.has(entry.path)) {
      fail('duplicate-path');
      continue;
    }
    seen.add(entry.path);
    try {
      let parent = dirname(target);
      let linkedParent = false;
      while (parent !== projectRoot) {
        if (lstatSync(parent).isSymbolicLink()) linkedParent = true;
        parent = dirname(parent);
      }
      if (linkedParent) {
        fail('unsafe-parent-link');
        continue;
      }
      const stat = lstatSync(target);
      const linkExpected = entry.mode === '120000';
      if (linkExpected ? !stat.isSymbolicLink() : !stat.isFile()) {
        fail('file-type-mismatch');
        continue;
      }
      const content = linkExpected ? Buffer.from(readlinkSync(target)) : readFileSync(target);
      if (gitBlobHash(content) !== entry.importedBlob) fail('content-mismatch');
      if (!linkExpected && process.platform !== 'win32') {
        if ((entry.mode === '100755') !== Boolean(stat.mode & 0o111)) fail('executable-mode-mismatch');
      }
    } catch (error) {
      fail(error.code === 'ENOENT' ? 'missing' : `read-error:${error.code ?? 'unknown'}`);
    }
  }
  return issues;
}

export function verifyManifest(root, manifest, sanitization) {
  const issues = [];
  const fail = (reason, path = '') => issues.push({ path, reason });
  if (manifest?.schemaVersion !== 1 || !Array.isArray(manifest.files) || !manifest.files.length
      || !Array.isArray(manifest.sources) || !manifest.sources.length) {
    return [{ path: '', reason: 'invalid-manifest' }];
  }
  const sources = new Map();
  for (const source of manifest.sources) {
    if (!source || typeof source.id !== 'string' || !source.id || sources.has(source.id)
        || !safePath(source.target) || !Number.isInteger(source.fileCount) || source.fileCount < 1) {
      return [{ path: '', reason: 'invalid-source' }];
    }
    sources.set(source.id, { ...source, count: 0 });
  }
  const expected = new Map();
  const recordedDirectories = new Set();
  for (const entry of manifest.files) {
    const source = sources.get(entry?.sourceId);
    if (!source || !safePath(entry?.sourcePath) || !safePath(entry?.path)
        || entry.path !== `${source.target}/${entry.sourcePath}` || !blobPattern.test(entry.sourceBlob)) {
      fail('source-path-mismatch', entry?.path ?? '');
      continue;
    }
    source.count++;
    expected.set(entry.path, entry);
    let parent = posix.dirname(entry.path);
    while (parent !== '.') {
      recordedDirectories.add(parent);
      parent = posix.dirname(parent);
    }
  }
  for (const source of sources.values()) {
    if (source.count !== source.fileCount) fail('source-count-mismatch', source.target);
  }
  issues.push(...verifyFiles(root, manifest.files));

  // Only known generated artifacts are outside the source-copy contract.
  const generatedDirs = new Set(['node_modules', 'target', 'dist', 'dist-prod', 'unpackage', '.vite']);
  const generatedFiles = new Set(['.DS_Store', '.flattened-pom.xml']);
  function checkDirectory(relative) {
    try {
      if (lstatSync(resolve(root, relative)).isSymbolicLink()) {
        fail('unsafe-parent-link', relative);
        return;
      }
      for (const child of readdirSync(resolve(root, relative), { withFileTypes: true })) {
        const childPath = `${relative}/${child.name}`;
        if (child.name === '.git') {
          fail('nested-git', childPath);
        } else if (child.isDirectory()) {
          if (!generatedDirs.has(child.name) || recordedDirectories.has(childPath)) checkDirectory(childPath);
        } else if (!expected.has(childPath) && !generatedFiles.has(child.name)) {
          fail('unrecorded-file', childPath);
        }
      }
    } catch (error) {
      fail(`read-error:${error.code ?? 'unknown'}`, relative);
    }
  }
  for (const source of sources.values()) checkDirectory(source.target);

  if (sanitization?.schemaVersion !== 1 || !Array.isArray(sanitization.changes)) {
    fail('invalid-sanitization');
    return issues;
  }
  const recordedChanges = new Set();
  for (const change of sanitization.changes) {
    if (!change || recordedChanges.has(change.path)) {
      fail('invalid-sanitization', change?.path ?? '');
      continue;
    }
    recordedChanges.add(change.path);
    const entry = expected.get(change.path);
    if (!entry || entry.sourceBlob === entry.importedBlob || entry.sourceBlob !== change.sourceBlob
        || entry.importedBlob !== change.importedBlob) fail('sanitization-mismatch', change.path);
  }
  for (const entry of expected.values()) {
    if (entry.sourceBlob !== entry.importedBlob && !recordedChanges.has(entry.path)) {
      fail('sanitization-mismatch', entry.path);
    }
  }
  return issues;
}

const invokedDirectly = process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url);
if (invokedDirectly) {
  try {
    const root = fileURLToPath(new URL('../', import.meta.url));
    const manifest = JSON.parse(readFileSync(resolve(root, 'third_party/source-copy-manifest.json'), 'utf8'));
    const sanitization = JSON.parse(readFileSync(resolve(root, 'third_party/source-sanitization.json'), 'utf8'));
    const issues = verifyManifest(root, manifest, sanitization);
    console.log(JSON.stringify({ checked: manifest.files?.length ?? 0, issues: issues.slice(0, 20), issueCount: issues.length }, null, 2));
    process.exitCode = issues.length ? 1 : 0;
  } catch (error) {
    console.error(`Source copy verification failed: ${error.message}`);
    process.exitCode = 1;
  }
}
