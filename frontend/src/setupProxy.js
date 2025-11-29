const { createProxyMiddleware } = require('http-proxy-middleware');

module.exports = function(app) {
  // Proxy for User Service
  app.use(
    '/api/users',
    createProxyMiddleware({
      target: process.env.USER_SERVICE_URL || 'http://localhost:3002',
      changeOrigin: true,
      pathRewrite: {
        '^/api/users': '/users',
      },
    })
  );
  app.use(
    '/api/login',
    createProxyMiddleware({
      target: process.env.USER_SERVICE_URL || 'http://localhost:3002',
      changeOrigin: true,
      pathRewrite: {
        '^/api/login': '/login',
      },
    })
  );

  // Proxy for Event Service
  app.use(
    '/api/events',
    createProxyMiddleware({
      target: process.env.EVENT_SERVICE_URL || 'http://localhost:3001',
      changeOrigin: true,
      pathRewrite: {
        '^/api/events': '/events',
      },
    })
  );
  app.use(
    '/api/tickets',
    createProxyMiddleware({
      target: process.env.EVENT_SERVICE_URL || 'http://localhost:3001',
      changeOrigin: true,
      pathRewrite: {
        '^/api/tickets': '/tickets', // tickets endpoint might not need rewrite if it matches
      },
    })
  );

  // Proxy for Order Service
  app.use(
    '/api/orders',
    createProxyMiddleware({
      target: process.env.ORDER_SERVICE_URL || 'http://localhost:8080',
      changeOrigin: true,
      // No rewrite needed for orders as the controller maps /api/orders
    })
  );
};

