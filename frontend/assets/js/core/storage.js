import { appConfig } from '../config/app-config.js';

const createStore = (kind) => {
  const memory = new Map();
  const resolve = () => {
    try {
      const store = window[kind];
      const probe = `${appConfig.storagePrefix}.probe`;
      store.setItem(probe, '1');
      store.removeItem(probe);
      return store;
    } catch {
      return null;
    }
  };
  const backend = resolve();
  const key = (name) => `${appConfig.storagePrefix}.${name}`;

  return {
    get(name, fallback = null) {
      try {
        const value = backend ? backend.getItem(key(name)) : memory.get(key(name));
        return value === null || value === undefined ? fallback : JSON.parse(value);
      } catch {
        return fallback;
      }
    },
    set(name, value) {
      const serialized = JSON.stringify(value);
      try {
        if (backend) backend.setItem(key(name), serialized);
        else memory.set(key(name), serialized);
        return true;
      } catch {
        memory.set(key(name), serialized);
        return false;
      }
    },
    remove(name) {
      try {
        if (backend) backend.removeItem(key(name));
      } catch {
        memory.delete(key(name));
      }
      memory.delete(key(name));
    },
    keys() {
      if (!backend) return [...memory.keys()];
      return Object.keys(backend).filter((name) => name.startsWith(`${appConfig.storagePrefix}.`));
    },
  };
};

export const persistent = createStore('localStorage');
export const session = createStore('sessionStorage');
