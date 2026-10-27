import js from '@eslint/js';
import globals from 'globals';

export default [
  { ignores: ['node_modules/**', 'assets/vendor/**'] },
  js.configs.recommended,
  {
    files: ['assets/js/**/*.js'],
    languageOptions: { ecmaVersion: 'latest', sourceType: 'module', globals: { ...globals.browser } },
    rules: {
      'no-unused-vars': ['error', { argsIgnorePattern: '^_|^event$', varsIgnorePattern: '^_' }],
      'no-console': 'error',
      'no-alert': 'error',
      'no-var': 'error',
      'prefer-const': 'error',
      eqeqeq: ['error', 'always'],
    },
  },
  {
    files: ['tools/**/*.mjs', 'eslint.config.js'],
    languageOptions: { ecmaVersion: 'latest', sourceType: 'module', globals: { ...globals.node } },
    rules: { 'no-unused-vars': ['error', { argsIgnorePattern: '^_' }] },
  },
  {
    files: ['tools/e2e.mjs'],
    languageOptions: { globals: { ...globals.node, ...globals.browser } },
  },
];
