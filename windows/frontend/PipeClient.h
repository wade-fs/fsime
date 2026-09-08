// PipeClient.h — Named Pipe client for communication with the Go backend server.
// This is the only IPC layer the C++ TSF DLL needs.
#pragma once
#include <windows.h>
#include <string>
#include <vector>
#include <functional>

// Mirrors ipc.Response from the Go server (JSON fields).
struct FsimeResponse {
    std::wstring composition;
    std::vector<std::wstring> candidates;
    std::wstring commit;
    std::wstring mode;
    std::wstring error;
    bool ok = false;
};

class PipeClient {
public:
    static constexpr wchar_t PIPE_NAME[] = L"\\\\.\\pipe\\FsimeServer";

    PipeClient() : pipe_(INVALID_HANDLE_VALUE) {}
    ~PipeClient() { Disconnect(); }

    // Connect (or reconnect) to the Go server Named Pipe.
    bool Connect() {
        Disconnect();
        pipe_ = CreateFileW(
            PIPE_NAME,
            GENERIC_READ | GENERIC_WRITE,
            0, nullptr,
            OPEN_EXISTING,
            FILE_FLAG_OVERLAPPED,
            nullptr
        );
        if (pipe_ == INVALID_HANDLE_VALUE) {
            // Server not yet ready — caller should retry
            return false;
        }
        DWORD mode = PIPE_READMODE_BYTE;
        SetNamedPipeHandleState(pipe_, &mode, nullptr, nullptr);
        return true;
    }

    void Disconnect() {
        if (pipe_ != INVALID_HANDLE_VALUE) {
            CloseHandle(pipe_);
            pipe_ = INVALID_HANDLE_VALUE;
        }
    }

    bool IsConnected() const { return pipe_ != INVALID_HANDLE_VALUE; }

    // Send a JSON request line and receive the JSON response line.
    FsimeResponse SendRequest(const std::string& jsonLine) {
        if (!EnsureConnected()) return MakeError(L"not connected");

        std::string payload = jsonLine + "\n";
        DWORD written = 0;
        if (!WriteFile(pipe_, payload.data(), (DWORD)payload.size(), &written, nullptr)) {
            Disconnect();
            return MakeError(L"write failed");
        }

        // Read response line (terminated by '\n', at most 64KB)
        std::string response;
        response.reserve(4096);
        char buf[4096];
        DWORD read = 0;
        while (true) {
            if (!ReadFile(pipe_, buf, sizeof(buf), &read, nullptr)) {
                Disconnect();
                return MakeError(L"read failed");
            }
            response.append(buf, read);
            if (!response.empty() && response.back() == '\n') break;
        }

        return ParseResponse(response);
    }

private:
    HANDLE pipe_;

    bool EnsureConnected() {
        if (IsConnected()) return true;
        return Connect();
    }

    static FsimeResponse MakeError(const wchar_t* msg) {
        FsimeResponse r;
        r.error = msg;
        r.candidates = {};
        return r;
    }

    // Minimal JSON parser — only reads the fields we need.
    // For production, replace with a proper JSON library (e.g., nlohmann/json).
    static FsimeResponse ParseResponse(const std::string& json);
};
