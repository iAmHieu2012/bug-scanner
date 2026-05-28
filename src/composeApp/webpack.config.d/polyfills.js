// Khởi tạo đối tượng resolve nếu chưa tồn tại
config.resolve = config.resolve || {};

// Khởi tạo đối tượng fallback (nơi chứa các polyfill cho module Node.js)
config.resolve.fallback = config.resolve.fallback || {};

// Cung cấp bản thay thế (polyfill) cho module 'os' của Node.js để chạy được trên trình duyệt
config.resolve.fallback.os = require.resolve("os-browserify/browser");

// Cung cấp bản thay thế (polyfill) cho module 'path' của Node.js để chạy được trên trình duyệt
config.resolve.fallback.path = require.resolve("path-browserify");