# Stage 1: Dependencies & Build
FROM node:20-alpine AS builder
WORKDIR /app

# Copy package files first for layer caching
COPY package*.json ./

# Install dependencies strictly from lockfile
RUN npm ci --legacy-peer-deps || npm install --legacy-peer-deps

# Copy remaining source code
COPY . .

# Build Vite static assets into /app/dist
RUN npm run build

# Stage 2: Ultra-lightweight Production Runtime
FROM nginx:alpine
WORKDIR /usr/share/nginx/html

# Clean default nginx static files
RUN rm -rf ./*

# Copy compiled static assets from builder stage
COPY --from=builder /app/dist .

# Copy custom Nginx config for SPA routing (React Router support)
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]