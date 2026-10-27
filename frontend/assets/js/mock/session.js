import { persistent, session } from '../core/storage.js';

const sessionKey = 'session';
const rememberKey = 'remember';
const rememberMillis = 8 * 60 * 60 * 1000;

export const getSessionUserId = () => {
  const current = session.get(sessionKey);
  if (current && current.userId) return current.userId;
  const remembered = persistent.get(rememberKey);
  if (remembered && remembered.userId && remembered.expiresAt > Date.now()) return remembered.userId;
  return null;
};

export const startSession = (userId, remember) => {
  session.set(sessionKey, { userId });
  if (remember) persistent.set(rememberKey, { userId, expiresAt: Date.now() + rememberMillis });
  else persistent.remove(rememberKey);
};

export const endSession = () => {
  session.remove(sessionKey);
  persistent.remove(rememberKey);
};
