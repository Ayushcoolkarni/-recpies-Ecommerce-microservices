# ─────────────────────────────────────────────────────────────────────────────
#  Multi-stage Dockerfile for the RasoiKit React frontend.
#
#  Stage 1 — Node: installs deps and builds the production bundle.
#  Stage 2 — Nginx: serves the static bundle on port 80.
#
#  The API_URL build arg (forwarded from REACT_APP_API_URL in .env) tells
#  the frontend where to find the API Gateway.
# ─────────────────────────────────────────────────────────────────────────────

# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM node:20-alpine AS build

WORKDIR /app

# Copy package files first — Docker cache skips npm install on code-only changes
COPY package.json package-lock.json ./
RUN npm ci --silent

# Pass the API URL in at build time so the React bundle bakes it in
ARG REACT_APP_API_URL=http://localhost:8080
ENV REACT_APP_API_URL=$REACT_APP_API_URL

COPY public ./public
COPY src    ./src

RUN npm run build

# ── Stage 2: Serve ────────────────────────────────────────────────────────────
FROM nginx:1.27-alpine

# Remove default nginx page
RUN rm -rf /usr/share/nginx/html/*

# Copy built assets
COPY --from=build /app/build /usr/share/nginx/html

# Nginx config — sends all paths to index.html for React Router to handle
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
