FROM golang:1.22 as builder

WORKDIR /app

COPY go.mod go.sum ./

RUN go mod download

COPY ./app ./app

RUN CGO_ENABLED=0 GOOS=linux go build -a -installsuffix cgo -o main ./app/main.go

FROM alpine:latest

WORKDIR /app

RUN apk --no-cache add ca-certificates tzdata libc6-compat

ENV TZ=Asia/Bangkok

COPY --from=builder /app/main .

ENTRYPOINT ["./main"]
