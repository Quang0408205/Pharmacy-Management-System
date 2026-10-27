import { appConfig } from '../../config/app-config.js';
import { sleep } from '../../core/dom.js';
import { dispatch } from '../../mock/index.js';

const latency = () => {
  const { min, max } = appConfig.mockLatency;
  return Math.round(min + Math.random() * (max - min));
};

export const mockAdapter = {
  async request({ method, path, params, body }) {
    await sleep(latency());
    const result = await dispatch({ method, path, params: params || {}, body: body ? structuredClone(body) : {} });
    return structuredClone(result);
  },
};
