FROM nginxinc/nginx-unprivileged:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY security-headers.conf /etc/nginx/security-headers.conf
COPY index.html styles.css fonts.css app.js robots.txt sitemap.xml /usr/share/nginx/html/
COPY img /usr/share/nginx/html/img
COPY fonts /usr/share/nginx/html/fonts
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --retries=3 CMD wget -qO- http://localhost:8080/healthz >/dev/null || exit 1
