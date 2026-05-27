import js from '@eslint/js';
import astroParser from 'astro-eslint-parser';
import tseslint from 'typescript-eslint';

export default [
  js.configs.recommended,
  ...tseslint.configs.recommended,
  {
    files: ['**/*.astro'],
    languageOptions: {
      parser: astroParser,
      parserOptions: {
        parser: tseslint.parser,
        extraFileExtensions: ['.astro'],
      },
    },
  },
  {
    ignores: ['.astro/**', 'dist/**', 'node_modules/**'],
  },
  {
    rules: {
      'no-unused-vars': 'warn',
      '@typescript-eslint/no-unused-vars': 'warn',
      '@typescript-eslint/no-explicit-any': 'off',
    },
  },
];
