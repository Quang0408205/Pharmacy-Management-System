FROM nginx:1.27-alpine
ARG API_MODE=mock
ARG API_BASE_URL=/api/v1
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY index.html /usr/share/nginx/html/index.html
COPY assets /usr/share/nginx/html/assets
COPY pages /usr/share/nginx/html/pages
RUN sed -i "s|apiMode: 'mock'|apiMode: '${API_MODE}'|; s|apiBaseUrl: '/api/v1'|apiBaseUrl: '${API_BASE_URL}'|" /usr/share/nginx/html/assets/js/config/app-config.js
EXPOSE 80
HEALTHCHECK --interval=30s --timeout=3s CMD wget -qO- http://127.0.0.1/ >/dev/null || exit 1
