import { mkdirSync, writeFileSync } from 'fs';
import { resolve, dirname } from 'path';
import { fileURLToPath, pathToFileURL } from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const projectRoot = resolve(__dirname, '..');
const frontendDir = resolve(projectRoot, 'frontend');

const mod = await import(pathToFileURL(resolve(frontendDir, 'src/data/mockRecords.js')).href);
const records = mod.mockRecords;

mkdirSync(resolve(__dirname, '.temp'), { recursive: true });
const out = resolve(__dirname, '.temp/records.json');
writeFileSync(out, JSON.stringify(records, null, 2));
console.log(`Exported ${records.length} records to ${out}`);
