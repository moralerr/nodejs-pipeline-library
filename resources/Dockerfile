# syntax=docker/dockerfile:1

# Use node:18-alpine as the base image
FROM node:18-alpine

# Set working directory
WORKDIR /app

# Install dependencies
COPY package*.json ./
RUN npm config set fetch-retry-mintimeout 20000 \
    && npm config set fetch-retry-maxtimeout 120000 \
    && npm install

# Copy application source code
COPY . .

# Build the application
ARG BUILD_ENV=production
RUN npm run build -- --configuration $BUILD_ENV

# Expose port 3000 (assuming your app runs on port 3000)
EXPOSE 4200

# Command to start the application
CMD ["npm", "run", "start"]
