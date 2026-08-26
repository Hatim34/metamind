FROM node:20-alpine AS build
WORKDIR /app
COPY package.json .
COPY package-lock.json .
RUN npm ci
COPY . .
RUN npm run build -- --progress=false

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/metamind-frontend/browser /usr/share/nginx/html
EXPOSE 80
