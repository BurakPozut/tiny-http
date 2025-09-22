# Tiny HTTP Server 🚀

[![codecov](https://codecov.io/gh/BurakPozut/tiny-http/branch/main/graph/badge.svg)](https://codecov.io/gh/BurakPozut/tiny-http)
[![CI](https://github.com/BurakPozut/tiny-http/workflows/CI/badge.svg)](https://github.com/BurakPozut/tiny-http/actions)

A lightweight HTTP server implementation in Java with both a **pure Java core** and a **Spring Boot wrapper**. Demonstrates core networking concepts, HTTP protocol handling, and modern Java web development.

## ✨ Features

### Core Server (`tiny-http-core`)
- **Pure Java Implementation**: Built with standard Java networking APIs
- **Multi-threaded**: Thread pool for handling concurrent requests
- **HTTP Protocol Support**: Complete HTTP/1.1 request/response handling
- **Routing System**: Flexible URL pattern matching with path variables
- **JSON Support**: Built-in JSON parsing and response generation
- **CORS Support**: Cross-origin resource sharing capabilities
- **Request Tracking**: Unique request ID generation and logging
- **Configuration**: Environment-based configuration system

### Spring Boot Wrapper (`tiny-http-boot`)
- **Spring Boot Integration**: Modern web framework wrapper
- **REST Controllers**: Clean, annotation-based endpoint definitions
- **Automatic JSON Serialization**: Records and DTOs with automatic conversion
- **Filter Chain**: Request/response processing with custom filters
- **Error Handling**: Global exception handling with structured error responses
- **CORS Configuration**: Declarative CORS setup

## 🛠️ Technology Stack

- **Language**: Java 21
- **Build Tool**: Maven 3.9+
- **Core Server**: Pure Java (NIO, Sockets, Threading)
- **Spring Boot**: 3.3.3
- **Testing**: JUnit 5, Spring Boot Test
- **JSON Processing**: Jackson

## 🚀 Quick Start

### Prerequisites

- Java 21 or higher
- Maven 3.9+

### Running the Core Server

```bash
# Clone the repository
git clone https://github.com/yourusername/tiny-http.git
cd tiny-http

# Run the core server
mvn compile exec:java -pl tiny-http-core
```

### Running the Spring Boot Server

```bash
# Run the Spring Boot version
mvn spring-boot:run -pl tiny-http-boot
```

Both servers will start listening on `http://localhost:8080`

### Testing the Servers

```bash
# Test core server
curl -i 'http://localhost:8080/hello?name=World'
curl -i 'http://localhost:8080/users/123'
curl -i -X POST -H "Content-Type: application/json" -d '{"test":"data"}' 'http://localhost:8080/echo'

# Test Spring Boot server (same endpoints)
curl -i 'http://localhost:8080/hello?name=World'
curl -i 'http://localhost:8080/users/123'
curl -i -X POST -H "Content-Type: application/json" -d '{"test":"data"}' 'http://localhost:8080/echo'
```

## 📁 Project Structure

```
tiny-http/
├── tiny-http-core/                 # Pure Java HTTP server
│   ├── src/main/java/org/example/tinyhttp/
│   │   ├── config/                 # Configuration management
│   │   ├── context/                # Request context
│   │   ├── http/                   # HTTP request/response handling
│   │   ├── logging/                # Access logging
│   │   ├── parsing/                # JSON and URL parsing
│   │   ├── routing/                # URL routing system
│   │   ├── server/                 # Server implementation
│   │   └── util/                   # Utility classes
│   └── src/test/java/              # Core server tests
├── tiny-http-boot/                 # Spring Boot wrapper
│   ├── src/main/java/org/example/tinyboot/
│   │   ├── config/                 # Spring configuration
│   │   ├── dto/                    # Data transfer objects
│   │   └── web/                    # REST controllers and filters
│   └── src/test/java/              # Spring Boot tests
└── pom.xml                         # Parent POM
```

## 🌐 Available Endpoints

Both servers provide the same API:

- `GET /hello?name={name}` - Hello world with optional name parameter
- `GET /users/{id}` - Get user by ID  
- `POST /echo` - Echo back request body (JSON or raw)
- `GET /health` - Server health check
- `GET /debug/config` - Server configuration info

## 🔧 Key Implementation Details

### Socket Management
- Uses `ServerSocket` for accepting client connections
- Implements try-with-resources for automatic resource cleanup
- Configures socket reuse address for better performance

### Exception Handling
- Comprehensive `IOException` handling for network operations
- Graceful error reporting with descriptive messages
- Proper resource cleanup even when exceptions occur

### HTTP Protocol
- Basic HTTP request/response handling
- Client connection logging
- Single-connection model (accepts one client then exits)

## 🎯 Learning Objectives

This project demonstrates:

### Core Server
- **Java Networking**: Sockets, NIO, multi-threading
- **HTTP Protocol**: Request parsing, response generation
- **Resource Management**: Proper cleanup and exception handling
- **Design Patterns**: Builder pattern, strategy pattern
- **Configuration**: Environment-based settings

### Spring Boot Integration
- **Modern Web Development**: REST APIs, dependency injection
- **Spring Boot Features**: Auto-configuration, starters
- **Testing**: Integration and unit testing
- **Filter Chain**: Request/response processing
- **Error Handling**: Global exception management

## 🔮 Future Enhancements

Core Features (Already Implemented):
- [x] Multi-threaded client handling
- [x] HTTP request parsing and routing
- [x] HTTP response generation
- [x] Basic error handling
- [x] Keep-alive connections
Future Enhancements:
- [ ] Static file serving
- [ ] Configuration file support
- [ ] Logging framework integration
- [ ] Performance metrics and monitoring
- [ ] HTTPS support
- [ ] Compression support

## 🤝 Contributing

This is a learning project, but contributions are welcome! Feel free to:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## 📄 License

This project is open source and available under the [MIT License](LICENSE).

## 👨‍💻 About the Developer

This project was created as a demonstration of Java networking capabilities and HTTP protocol understanding. It showcases clean code practices, proper resource management, and professional project structure.

---

⭐ **Star this repository if you found it helpful!**
