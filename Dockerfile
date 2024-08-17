# Stage 1: Build the Go binary
FROM golang:1.20-alpine AS builder

# Set the working directory
WORKDIR /app

# Copy the Go modules and dependencies files
COPY go.mod go.sum ./

# Download dependencies
RUN go mod download

# Copy the source code
COPY . .

# Build the Go binary
RUN go build -o myapp .

# Stage 2: Create the final lightweight image
FROM alpine:3.18

# Set the working directory
WORKDIR /app

# Copy the Go binary from the builder stage
COPY --from=builder /app/golang-jenkin .

# Expose the application port (if necessary)
EXPOSE 8080

# Run the Go binary
CMD ["./golang-jenkin"]
