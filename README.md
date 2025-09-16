# Tiny HTTP Server 🚀

[![codecov](https://codecov.io/gh/BurakPozut/tiny-http/branch/main/graph/badge.svg)](https://codecov.io/gh/BurakPozut/tiny-http)
[![CI](https://github.com/BurakPozut/tiny-http/workflows/CI/badge.svg)](https://github.com/BurakPozut/tiny-http/actions)

A lightweight, single-threaded HTTP server implementation in Java that demonstrates core networking concepts and HTTP protocol handling.

## ✨ Features

- **Pure Java Implementation**: Built with standard Java networking APIs
- **HTTP Protocol Support**: Handles basic HTTP requests and responses
- **Resource Management**: Proper use of try-with-resources for socket cleanup
- **Exception Handling**: Robust error handling with meaningful error messages
- **Maven Build System**: Professional project structure with dependency management

## 🛠️ Technology Stack

- **Language**: Java 21
- **Build Tool**: Maven 3.9+
- **Networking**: Java NIO and Socket APIs
- **Testing**: JUnit for unit tests

## 🚀 Quick Start

### Prerequisites

- Java 21 or higher
- Maven 3.9+

### Running the Server

```bash
# Clone the repository
git clone https://github.com/yourusername/tiny-http.git
cd tiny-http

# Compile and run
mvn compile exec:java
```

The server will start listening on `http://localhost:8080`

### Testing the Server

Open your browser and navigate to `http://localhost:8080` or use curl:

```bash
curl http://localhost:8080
```

## 📁 Project Structure

```
tiny-http/
├── src/
│   ├── main/java/org/example/tinyhttp/
│   │   └── HttpServer.java          # Main server implementation
│   └── test/java/org/example/
│       └── AppTest.java       # Unit tests
├── pom.xml                    # Maven configuration
└── README.md                  # This file
```

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

- **Java Networking Fundamentals**: Understanding of sockets, ports, and client-server communication
- **Resource Management**: Proper handling of system resources with try-with-resources
- **Exception Handling**: Robust error handling in network applications
- **HTTP Protocol Basics**: Understanding of HTTP request/response cycle
- **Maven Project Structure**: Professional Java project organization

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
