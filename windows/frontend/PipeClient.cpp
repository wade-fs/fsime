// PipeClient.cpp — Implementation of the Named Pipe client and minimal JSON parser.
#include "PipeClient.h"
#include <algorithm>
#include <cctype>

// ---------------------------------------------------------------------------
// Minimal JSON parser — extracts string fields from a flat JSON object.
// Sufficient for the simple Response structure.  Replace with nlohmann/json
// for production robustness.
// ---------------------------------------------------------------------------

static std::wstring utf8ToUtf16(const std::string& s) {
    if (s.empty()) return {};
    int size = MultiByteToWideChar(CP_UTF8, 0, s.data(), (int)s.size(),
                                   nullptr, 0);
    std::wstring ws(size, 0);
    MultiByteToWideChar(CP_UTF8, 0, s.data(), (int)s.size(),
                        ws.data(), size);
    return ws;
}

// Extract value for a simple string key from JSON.
static std::string extractString(const std::string& json, const std::string& key) {
    std::string search = "\"" + key + "\"";
    auto pos = json.find(search);
    if (pos == std::string::npos) return {};
    pos += search.size();
    // Skip whitespace and colon
    while (pos < json.size() && (json[pos] == ' ' || json[pos] == ':')) pos++;
    if (pos >= json.size() || json[pos] != '"') return {};
    pos++; // skip opening quote
    std::string result;
    for (; pos < json.size(); pos++) {
        char c = json[pos];
        if (c == '\\') {
            if (pos + 1 < json.size()) {
                pos++;
                switch (json[pos]) {
                case '"':  result += '"';  break;
                case '\\': result += '\\'; break;
                case '/':  result += '/';  break;
                case 'n':  result += '\n'; break;
                case 'r':  result += '\r'; break;
                case 't':  result += '\t'; break;
                default:   result += json[pos]; break;
                }
            }
        } else if (c == '"') {
            break;
        } else {
            result += c;
        }
    }
    return result;
}

// Extract JSON array of strings for a given key.
static std::vector<std::wstring> extractStringArray(const std::string& json, const std::string& key) {
    std::vector<std::wstring> result;
    std::string search = "\"" + key + "\"";
    auto pos = json.find(search);
    if (pos == std::string::npos) return result;
    pos += search.size();
    while (pos < json.size() && (json[pos] == ' ' || json[pos] == ':')) pos++;
    if (pos >= json.size() || json[pos] != '[') return result;
    pos++; // skip '['
    while (pos < json.size()) {
        while (pos < json.size() && (json[pos] == ' ' || json[pos] == ',')) pos++;
        if (pos >= json.size() || json[pos] == ']') break;
        if (json[pos] != '"') { pos++; continue; }
        pos++; // skip opening quote
        std::string item;
        for (; pos < json.size(); pos++) {
            char c = json[pos];
            if (c == '\\' && pos + 1 < json.size()) {
                pos++;
                item += json[pos];
            } else if (c == '"') {
                pos++;
                break;
            } else {
                item += c;
            }
        }
        result.push_back(utf8ToUtf16(item));
    }
    return result;
}

FsimeResponse PipeClient::ParseResponse(const std::string& json) {
    FsimeResponse r;
    r.ok          = true;
    r.composition = utf8ToUtf16(extractString(json, "composition"));
    r.commit      = utf8ToUtf16(extractString(json, "commit"));
    r.mode        = utf8ToUtf16(extractString(json, "mode"));
    r.error       = utf8ToUtf16(extractString(json, "error"));
    r.candidates  = extractStringArray(json, "candidates");
    return r;
}
