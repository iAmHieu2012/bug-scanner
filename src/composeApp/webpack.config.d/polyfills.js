config.resolve = config.resolve || {};
config.resolve.fallback = config.resolve.fallback || {};

config.resolve.fallback.os = require.resolve("os-browserify/browser");
config.resolve.fallback.path = require.resolve("path-browserify");