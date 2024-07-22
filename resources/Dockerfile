# syntax=docker/dockerfile:1

# Build stage
FROM node:18-alpine AS builder

# Set working directory
WORKDIR /app

# Install dependencies
COPY package*.json ./
RUN npm config set fetch-retry-mintimeout 20000
RUN npm config set fetch-retry-maxtimeout 120000
RUN npm install

# Copy application source code
COPY . .

# Build the application
ARG BUILD_ENV=production
RUN npm run build -- --configuration $BUILD_ENV

# Serve stage
FROM nginx:alpine

# Copy the built app from the 'builder' stage to the Nginx public folder
COPY --from=builder /app/dist/ /usr/share/nginx/html

# Expose port 80
EXPOSE 80

# Command to start Nginx
CMD ["nginx", "-g", "daemon off;"]
