import { appConfig } from '../config/app-config.js';
import { persistent } from '../core/storage.js';
import { generateSeed } from './seed.js';
import { demoPassword } from '../config/demo-data.js';

const collectionNames = [
  'categories',
  'suppliers',
  'medicines',
  'customers',
  'employees',
  'receipts',
  'batches',
  'invoices',
  'movements',
  'stocktakes',
  'activityLogs',
  'invitations',
  'drafts',
  'settings',
];

const metaKey = 'db.meta';
const seedMaxAgeMillis = 7 * 24 * 60 * 60 * 1000;
const credentialKey = 'db.credentials';
const pad = (value, size) => String(value).padStart(size, '0');

const fallbackHash = (text) => {
  let hash = 2166136261;
  for (let index = 0; index < text.length; index += 1) {
    hash ^= text.charCodeAt(index);
    hash = Math.imul(hash, 16777619);
  }
  return (hash >>> 0).toString(16);
};

export const hashPassword = async (password, salt) => {
  const text = `${salt}:${password}`;
  if (globalThis.crypto && globalThis.crypto.subtle) {
    const digest = await globalThis.crypto.subtle.digest('SHA-256', new TextEncoder().encode(text));
    return [...new Uint8Array(digest)].map((byte) => byte.toString(16).padStart(2, '0')).join('');
  }
  return fallbackHash(text);
};

export const createSalt = () => Math.random().toString(36).slice(2, 12);

let data = null;
let readyPromise = null;

const persistAll = () => {
  collectionNames.forEach((name) => persistent.set(`db.${name}`, data[name]));
};

const seedDatabase = async () => {
  data = generateSeed(new Date());
  const credentials = {};
  for (const employee of data.employees) {
    const salt = createSalt();
    credentials[employee.id] = { salt, hash: await hashPassword(demoPassword, salt) };
  }
  persistAll();
  persistent.set(credentialKey, credentials);
  persistent.set(metaKey, { seedVersion: appConfig.mockSeedVersion, seededAt: new Date().toISOString() });
};

const loadDatabase = () => {
  const meta = persistent.get(metaKey);
  if (!meta || meta.seedVersion !== appConfig.mockSeedVersion) return false;
  if (Date.now() - new Date(meta.seededAt).getTime() > seedMaxAgeMillis) return false;
  const loaded = {};
  for (const name of collectionNames) {
    const value = persistent.get(`db.${name}`);
    if (value === null) return false;
    loaded[name] = value;
  }
  data = loaded;
  return true;
};

export const database = {
  ready() {
    if (!readyPromise) {
      readyPromise = (async () => {
        if (!loadDatabase()) await seedDatabase();
      })();
    }
    return readyPromise;
  },

  get data() {
    return data;
  },

  save(...names) {
    names.forEach((name) => persistent.set(`db.${name}`, data[name]));
  },

  async reset() {
    collectionNames.forEach((name) => persistent.remove(`db.${name}`));
    persistent.remove(metaKey);
    persistent.remove(credentialKey);
    await seedDatabase();
  },

  credentials() {
    return persistent.get(credentialKey, {});
  },

  saveCredential(employeeId, credential) {
    const credentials = persistent.get(credentialKey, {});
    credentials[employeeId] = credential;
    persistent.set(credentialKey, credentials);
  },

  nextId(prefix, collection, size = 4) {
    const max = data[collection].reduce((highest, item) => {
      const value = Number(String(item.id).split('-').pop());
      return Number.isFinite(value) && value > highest ? value : highest;
    }, 0);
    return `${prefix}-${pad(max + 1, size)}`;
  },

  log(user, action, entity, entityCode, description) {
    data.activityLogs.push({
      id: this.nextId('log', 'activityLogs'),
      createdAt: new Date().toISOString(),
      actorId: user ? user.id : null,
      actorName: user ? user.name : 'Hệ thống',
      role: user ? user.role : 'SYSTEM',
      action,
      entity,
      entityCode,
      description,
    });
    if (data.activityLogs.length > 600) data.activityLogs.splice(0, data.activityLogs.length - 600);
    this.save('activityLogs');
  },
};
